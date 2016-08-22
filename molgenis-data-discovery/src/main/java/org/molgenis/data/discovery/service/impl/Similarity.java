package org.molgenis.data.discovery.service.impl;

import static java.util.Objects.nonNull;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.findMatchedWords;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.splitIntoTerms;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.model.matching.MatchedAttributeTagGroup;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.utils.NGramDistanceAlgorithm;
import org.molgenis.ontology.utils.Stemmer;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class Similarity
{
	private final OntologyService ontologyService;
	private final Joiner termJoiner = Joiner.on(' ');

	public Similarity(OntologyService ontologyService)
	{
		this.ontologyService = Objects.requireNonNull(ontologyService);
	}

	public Hit<String> score(BiobankSampleAttribute targetAttribute, BiobankSampleAttribute sourceAttribute,
			BiobankUniverse biobankUniverse, SemanticSearchParam parameter)
	{
		// Get all the ontology terms
		Multimap<OntologyTerm, IdentifiableTagGroup> targetOntologyTermToTagMap = createOntologyTermToTagMap(
				targetAttribute, biobankUniverse);

		Multimap<OntologyTerm, IdentifiableTagGroup> sourceOntologyTermToTagMap = createOntologyTermToTagMap(
				sourceAttribute, biobankUniverse);

		Multimap<IdentifiableTagGroup, IdentifiableTagGroup> relatedTagGroups = findRelatedTagGroups(
				targetOntologyTermToTagMap, sourceOntologyTermToTagMap, parameter);

		List<Hit<String>> allMatchedStrings = new ArrayList<>();

		for (Entry<IdentifiableTagGroup, Collection<IdentifiableTagGroup>> entry : relatedTagGroups.asMap().entrySet())
		{
			allMatchedStrings
					.addAll(entry.getValue()
							.stream().distinct().map(sourceGroup -> calculateScoreForTagPair(targetAttribute,
									sourceAttribute, entry.getKey(), sourceGroup, parameter))
					.collect(Collectors.toList()));
		}

		Optional<Hit<String>> findFirst = allMatchedStrings.stream().sorted().findFirst();
		return findFirst.isPresent() ? findFirst.get() : Hit.create(StringUtils.EMPTY, 0.0f);
	}

	private Hit<String> calculateScoreForTagPair(BiobankSampleAttribute targetAttribute,
			BiobankSampleAttribute sourceAttribute, IdentifiableTagGroup targetGroup, IdentifiableTagGroup sourceGroup,
			SemanticSearchParam parameter)
	{
		List<MatchedAttributeTagGroup> allRelatedOntologyTerms = new ArrayList<>();

		for (OntologyTerm targetOntologyTerm : targetGroup.getOntologyTerms())
		{
			for (OntologyTerm sourceOntologyTerm : sourceGroup.getOntologyTerms())
			{
				if (ontologyTermsRelated(targetOntologyTerm, sourceOntologyTerm, parameter))
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

		Collections.sort(allRelatedOntologyTerms);
		List<MatchedAttributeTagGroup> filteredRelatedOntologyTerms = new ArrayList<>();
		List<OntologyTerm> occupiedTargetOntologyTerms = new ArrayList<>();
		List<OntologyTerm> occupiedSourceOntologyTerms = new ArrayList<>();
		for (MatchedAttributeTagGroup matchedTagGroup : allRelatedOntologyTerms)
		{
			OntologyTerm targetOntologyTerm = matchedTagGroup.getTarget().getOntologyTerm();
			OntologyTerm sourceOntologyTerm = matchedTagGroup.getSource().getOntologyTerm();
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

		Set<Hit<String>> matchedWords = new HashSet<>();
		Set<String> queryString = new HashSet<>();

		for (MatchedAttributeTagGroup matchedTagGroup : filteredRelatedOntologyTerms)
		{
			TagGroup targetTagGroup = matchedTagGroup.getTarget();
			TagGroup sourceTagGroup = matchedTagGroup.getSource();

			Set<String> targetMatchedWords = splitIntoTerms(targetTagGroup.getMatchedWords());
			Set<String> sourceMatchedWords = splitIntoTerms(sourceTagGroup.getMatchedWords());
			queryString.addAll(targetMatchedWords);
			queryString.addAll(sourceMatchedWords);
			// The source ontologyTerm is more specific therefore we replace it with a more general target ontologyTerm
			if (ontologyService.isDescendant(sourceTagGroup.getOntologyTerm(), targetTagGroup.getOntologyTerm()))
			{
				Set<String> sourceLabelWords = splitIntoTerms(sourceLabel);
				sourceLabelWords.removeAll(sourceMatchedWords);
				sourceLabel = termJoiner.join(Sets.union(sourceLabelWords, targetMatchedWords));

				matchedWords.add(
						Hit.create(termJoiner.join(targetMatchedWords), matchedTagGroup.getSimilarity().floatValue()));
			}
			else
			{
				Set<String> targetLabelWords = splitIntoTerms(targetLabel);
				targetLabelWords.removeAll(targetMatchedWords);
				targetLabel = termJoiner.join(Sets.union(targetLabelWords, sourceMatchedWords));

				matchedWords.add(
						Hit.create(termJoiner.join(sourceMatchedWords), matchedTagGroup.getSimilarity().floatValue()));
			}
		}

		float adjustedScore = (float) stringMatching(targetLabel, sourceLabel) / 100;

		for (Hit<String> matchedWord : matchedWords)
		{
			float ngramContribution = adjustedScore * 2 * matchedWord.getResult().length()
					/ (targetLabel.length() + sourceLabel.length());
			adjustedScore = adjustedScore - ngramContribution
					+ ngramContribution * (float) Math.pow(matchedWord.getScore(), 2.0);
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

	private Multimap<IdentifiableTagGroup, IdentifiableTagGroup> findRelatedTagGroups(
			Multimap<OntologyTerm, IdentifiableTagGroup> targetOntologyTermToTagMap,
			Multimap<OntologyTerm, IdentifiableTagGroup> sourceOntologyTermToTagMap, SemanticSearchParam parameter)
	{
		Multimap<IdentifiableTagGroup, IdentifiableTagGroup> relatedTagGroups = LinkedHashMultimap.create();

		for (OntologyTerm targetOntologyTerm : targetOntologyTermToTagMap.asMap().keySet())
		{
			for (OntologyTerm sourceOntologyTerm : sourceOntologyTermToTagMap.asMap().keySet())
			{
				if (ontologyTermsRelated(targetOntologyTerm, sourceOntologyTerm, parameter))
				{
					targetOntologyTermToTagMap.get(targetOntologyTerm).stream()
							.forEach(targetTag -> sourceOntologyTermToTagMap.get(sourceOntologyTerm).stream()
									.forEach(sourceTag -> relatedTagGroups.put(targetTag, sourceTag)));
				}
			}
		}
		return relatedTagGroups;
	}

	private boolean ontologyTermsRelated(OntologyTerm targetOntologyTerm, OntologyTerm sourceOntologyTerm,
			SemanticSearchParam semanticSearchParam)
	{
		return ontologyService.related(targetOntologyTerm, sourceOntologyTerm)
				&& ontologyService.areWithinDistance(targetOntologyTerm, sourceOntologyTerm,
						semanticSearchParam.getQueryExpansionParameter().getExpansionLevel());
	}

	private Multimap<OntologyTerm, IdentifiableTagGroup> createOntologyTermToTagMap(
			BiobankSampleAttribute biobankSampleAttribute, BiobankUniverse biobankUniverse)
	{
		Multimap<OntologyTerm, IdentifiableTagGroup> map = LinkedHashMultimap.create();
		List<SemanticType> keyConcepts = biobankUniverse.getKeyConcepts();
		for (IdentifiableTagGroup tagGroup : biobankSampleAttribute.getTagGroups())
		{
			tagGroup.getOntologyTerms().stream()
					.filter(ot -> ot.getSemanticTypes().isEmpty()
							|| ot.getSemanticTypes().stream().allMatch(st -> !keyConcepts.contains(st)))
					.forEach(ot -> map.put(ot, tagGroup));
		}
		return map;
	}
}
