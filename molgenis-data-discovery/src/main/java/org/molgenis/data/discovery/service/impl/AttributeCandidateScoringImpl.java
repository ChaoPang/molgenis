package org.molgenis.data.discovery.service.impl;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.findMatchedWords;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.splitIntoTerms;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.splitIntoUniqueTerms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.model.matching.MatchedAttributeTagGroup;
import org.molgenis.data.discovery.scoring.attributes.AttributeSimilarity;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.utils.NGramDistanceAlgorithm;
import org.molgenis.ontology.utils.Stemmer;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * This scoring class computes a similarity score between the target {@link BiobankSampleAttribute} and the source
 * {@link BiobankSampleAttribute}.
 * 
 * @author chaopang
 *
 */
public class AttributeCandidateScoringImpl
{
	private final OntologyService ontologyService;
	private final AttributeSimilarity attributeSimilarity;
	private final Joiner termJoiner = Joiner.on(' ');

	public AttributeCandidateScoringImpl(OntologyService ontologyService, AttributeSimilarity attributeSimilarity)
	{
		this.ontologyService = requireNonNull(ontologyService);
		this.attributeSimilarity = requireNonNull(attributeSimilarity);
	}

	public Hit<String> score(BiobankSampleAttribute targetAttribute, BiobankSampleAttribute sourceAttribute,
			BiobankUniverse biobankUniverse, Multimap<OntologyTerm, OntologyTerm> relatedOntologyTerms,
			boolean strictMatch)
	{
		List<Hit<String>> allMatchedStrings = new ArrayList<>();

		for (IdentifiableTagGroup targetTagGroup : targetAttribute.getTagGroups())
		{
			for (IdentifiableTagGroup sourceTagGroup : sourceAttribute.getTagGroups())
			{
				Hit<String> calculateScoreForTagPair = calculateScoreForTagPair(targetAttribute, sourceAttribute,
						targetTagGroup, sourceTagGroup, relatedOntologyTerms, strictMatch);

				if (Objects.nonNull(calculateScoreForTagPair))
				{
					allMatchedStrings.add(calculateScoreForTagPair);
				}
			}
		}

		Optional<Hit<String>> findFirst = allMatchedStrings.stream().sorted(Comparator.reverseOrder()).findFirst();

		return findFirst.isPresent() ? findFirst.get() : Hit.create(StringUtils.EMPTY, 0.0f);
	}

	Hit<String> calculateScoreForTagPair(BiobankSampleAttribute targetAttribute, BiobankSampleAttribute sourceAttribute,
			IdentifiableTagGroup targetGroup, IdentifiableTagGroup sourceGroup,
			Multimap<OntologyTerm, OntologyTerm> relatedOntologyTerms, boolean strictMatch)
	{
		List<MatchedAttributeTagGroup> allRelatedOntologyTerms = new ArrayList<>();

		for (OntologyTerm targetOntologyTerm : targetGroup.getOntologyTerms())
		{
			for (OntologyTerm sourceOntologyTerm : sourceGroup.getOntologyTerms())
			{
				if (relatedOntologyTerms.containsEntry(targetOntologyTerm, sourceOntologyTerm))
				{
					TagGroup targetOntologyTermTag = createOntologyTermTag(targetOntologyTerm, targetAttribute);
					TagGroup sourceOntologyTermTag = createOntologyTermTag(sourceOntologyTerm, sourceAttribute);

					Double relatedness = ontologyService.getOntologyTermSemanticRelatedness(targetOntologyTerm,
							sourceOntologyTerm);

					if (nonNull(targetOntologyTermTag) && nonNull(sourceOntologyTermTag))
					{
						allRelatedOntologyTerms.add(MatchedAttributeTagGroup.create(targetOntologyTermTag,
								sourceOntologyTermTag, relatedness));
					}
				}
			}
		}

		if (!allRelatedOntologyTerms.isEmpty())
		{
			Collections.sort(allRelatedOntologyTerms);

			List<MatchedAttributeTagGroup> filteredRelatedOntologyTerms = new ArrayList<>();
			List<OntologyTerm> occupiedTargetOntologyTerms = new ArrayList<>();
			List<OntologyTerm> occupiedSourceOntologyTerms = new ArrayList<>();
			for (MatchedAttributeTagGroup matchedTagGroup : allRelatedOntologyTerms)
			{
				OntologyTerm targetOntologyTerm = matchedTagGroup.getTarget().getCombinedOntologyTerm();
				OntologyTerm sourceOntologyTerm = matchedTagGroup.getSource().getCombinedOntologyTerm();
				if (!occupiedTargetOntologyTerms.contains(targetOntologyTerm)
						&& !occupiedSourceOntologyTerms.contains(sourceOntologyTerm))
				{
					filteredRelatedOntologyTerms.add(matchedTagGroup);
					occupiedTargetOntologyTerms.add(targetOntologyTerm);
					occupiedSourceOntologyTerms.add(sourceOntologyTerm);
				}
			}

			String targetLabel = targetAttribute.getLabel();
			String sourceLabel = sourceAttribute.getLabel();

			Hit<String> scoreForLabels = calculate(targetLabel, sourceLabel, filteredRelatedOntologyTerms, strictMatch);

			String targetDescription = targetAttribute.getDescription();
			String sourceDescription = sourceAttribute.getDescription();

			if (isNotBlank(targetDescription) && isNotBlank(sourceDescription)
					&& ((!targetLabel.equals(targetDescription) || !sourceLabel.equals(sourceDescription))
							&& !(targetLabel.equals(targetDescription) && sourceLabel.equals(sourceDescription))))
			{
				Hit<String> scoreForDescriptions = calculate(targetDescription, sourceDescription,
						filteredRelatedOntologyTerms, strictMatch);

				float score = scoreForDescriptions.getScore() > scoreForLabels.getScore()
						? scoreForDescriptions.getScore()
						: (scoreForLabels.getScore() + scoreForDescriptions.getScore()) / 2;
				scoreForLabels = Hit.create(scoreForLabels.getResult(), score);
			}

			return scoreForLabels;
		}

		return null;
	}

	Hit<String> calculate(String targetLabel, String sourceLabel,
			List<MatchedAttributeTagGroup> filteredRelatedOntologyTerms, boolean strictMatch)
	{
		// Remove the duplicated words from the attribute labels
		targetLabel = termJoiner.join(splitIntoTerms(targetLabel));

		sourceLabel = termJoiner.join(splitIntoTerms(sourceLabel));

		Set<Hit<String>> matchedWords = new HashSet<>();

		Set<String> queryString = new LinkedHashSet<>();

		for (MatchedAttributeTagGroup matchedTagGroup : filteredRelatedOntologyTerms)
		{
			TagGroup targetTagGroup = matchedTagGroup.getTarget();
			TagGroup sourceTagGroup = matchedTagGroup.getSource();

			Set<String> targetMatchedWords = splitIntoUniqueTerms(targetTagGroup.getMatchedWords());
			Set<String> sourceMatchedWords = splitIntoUniqueTerms(sourceTagGroup.getMatchedWords());
			queryString.addAll(targetMatchedWords);
			queryString.addAll(sourceMatchedWords);
			// The source ontologyTerm is more specific therefore we replace it with a more general target
			// ontologyTerm
			if (ontologyService.isDescendant(sourceTagGroup.getCombinedOntologyTerm(),
					targetTagGroup.getCombinedOntologyTerm()))
			{
				Set<String> sourceLabelWords = splitIntoUniqueTerms(sourceLabel);
				sourceLabelWords.removeAll(sourceMatchedWords);
				sourceLabel = termJoiner.join(Sets.union(sourceLabelWords, targetMatchedWords));

				matchedWords.add(
						Hit.create(termJoiner.join(targetMatchedWords), matchedTagGroup.getSimilarity().floatValue()));
			}
			else
			{
				Set<String> targetLabelWords = splitIntoUniqueTerms(targetLabel);
				targetLabelWords.removeAll(targetMatchedWords);
				targetLabel = termJoiner.join(Sets.union(targetLabelWords, sourceMatchedWords));

				matchedWords.add(
						Hit.create(termJoiner.join(sourceMatchedWords), matchedTagGroup.getSimilarity().floatValue()));
			}
		}

		float adjustedScore = attributeSimilarity.score(targetLabel, sourceLabel, strictMatch);

		for (Hit<String> matchedWord : matchedWords)
		{
			float ngramContribution = adjustedScore * 2 * matchedWord.getResult().length()
					/ (targetLabel.length() + sourceLabel.length());
			adjustedScore = adjustedScore - ngramContribution
					+ ngramContribution * (float) Math.pow(matchedWord.getScore(), 3.0);
		}

		return Hit.create(termJoiner.join(queryString), adjustedScore);
	}

	private TagGroup createOntologyTermTag(OntologyTerm ontologyTerm, BiobankSampleAttribute biobankSampleAttribute)
	{
		Set<String> stemmedAttributeLabelWords = Stemmer.splitAndStem(biobankSampleAttribute.getLabel());

		for (String synonym : ontologyTerm.getSynonyms())
		{
			if (stemmedAttributeLabelWords.containsAll(Stemmer.splitAndStem(synonym)))
			{
				String matchedWords = termJoiner.join(findMatchedWords(biobankSampleAttribute.getLabel(), synonym));
				float score = (float) NGramDistanceAlgorithm.stringMatching(biobankSampleAttribute.getLabel(), synonym)
						/ 100;
				return TagGroup.create(ontologyTerm, matchedWords, score);
			}
		}
		return null;
	}
}
