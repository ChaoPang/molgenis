package org.molgenis.data.discovery.service.impl;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.model.matching.MatchedOntologyTermHit;
import org.molgenis.data.discovery.scoring.attributes.NgramAttributeSimilarity;
import org.molgenis.data.discovery.scoring.attributes.VectorSpaceModelAttributeSimilarity;
import org.molgenis.data.discovery.utils.MatchingExplanationHit;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.ic.TermFrequencyService;
import org.molgenis.ontology.utils.Stemmer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.findMatchedWords;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.splitIntoUniqueTerms;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.STOPWORDSLIST;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;

/**
 * This scoring class computes a similarity score between the target {@link BiobankSampleAttribute} and the source
 * {@link BiobankSampleAttribute}.
 *
 * @author chaopang
 */
public class AttributeCandidateScoringImpl
{
	private final OntologyService ontologyService;
	private final VectorSpaceModelAttributeSimilarity vectorSpaceModelAttributeSimilarity;
	private final NgramAttributeSimilarity ngramAttributeSimilarity;
	private final static String SINGLE_SPACE_CHAR = " ";

	private final Joiner termJoiner = Joiner.on(SINGLE_SPACE_CHAR);

	public AttributeCandidateScoringImpl(OntologyService ontologyService, TermFrequencyService termFrequencyService)
	{
		this.ontologyService = requireNonNull(ontologyService);
		this.vectorSpaceModelAttributeSimilarity = new VectorSpaceModelAttributeSimilarity(termFrequencyService);
		this.ngramAttributeSimilarity = new NgramAttributeSimilarity(termFrequencyService);
	}

	public MatchingExplanationHit score(BiobankSampleAttribute targetAttribute, BiobankSampleAttribute sourceAttribute,
			Multimap<OntologyTerm, OntologyTerm> relatedOntologyTerms, boolean strictMatch)
	{
		List<MatchingExplanationHit> allMatchedStrings = new ArrayList<>();

		for (IdentifiableTagGroup targetTagGroup : targetAttribute.getTagGroups())
		{
			for (IdentifiableTagGroup sourceTagGroup : sourceAttribute.getTagGroups())
			{
				MatchingExplanationHit calculateScoreForTagPair = calculateScoreForTagPair(targetAttribute,
						sourceAttribute, targetTagGroup, sourceTagGroup, relatedOntologyTerms, strictMatch);

				if (nonNull(calculateScoreForTagPair))
				{
					allMatchedStrings.add(calculateScoreForTagPair);
				}
			}
		}

		//TODO: consider using org.molgenis.data.discovery.model.matching.MatchingExplanation directly here

		//calculate the score using the ontology terms
		if (!allMatchedStrings.isEmpty())
		{
			return allMatchedStrings.stream().sorted().findFirst().get();
		}

		//calculate the score without using ontology terms
		return calculate(targetAttribute, sourceAttribute, strictMatch, emptyList());
	}

	/**
	 * Calculate the vsm and ngram scores for the target {@link BiobankSampleAttribute} and the source {@link BiobankSampleAttribute} based on single pair of the target and the source {@link IdentifiableTagGroup}s
	 *
	 * @param targetAttribute
	 * @param sourceAttribute
	 * @param targetTagGroup
	 * @param sourceTagGroup
	 * @param relatedOntologyTerms
	 * @param strictMatch
	 * @return
	 */
	MatchingExplanationHit calculateScoreForTagPair(BiobankSampleAttribute targetAttribute,
			BiobankSampleAttribute sourceAttribute, IdentifiableTagGroup targetTagGroup,
			IdentifiableTagGroup sourceTagGroup, Multimap<OntologyTerm, OntologyTerm> relatedOntologyTerms,
			boolean strictMatch)
	{
		List<MatchedOntologyTermHit> allRelatedOntologyTermTagGroups = new ArrayList<>();

		for (OntologyTerm targetOntologyTerm : targetTagGroup.getOntologyTerms())
		{
			for (OntologyTerm sourceOntologyTerm : sourceTagGroup.getOntologyTerms())
			{
				if (relatedOntologyTerms.containsEntry(targetOntologyTerm, sourceOntologyTerm))
				{
					OntologyTermHit targetOntologyTermHit = createOntologyTermTag(targetOntologyTerm, targetAttribute);
					OntologyTermHit sourceOntologyTermHit = createOntologyTermTag(sourceOntologyTerm, sourceAttribute);

					Double relatedness = ontologyService
							.getOntologyTermSemanticRelatedness(targetOntologyTerm, sourceOntologyTerm);

					if (nonNull(targetOntologyTermHit) && nonNull(sourceOntologyTermHit))
					{
						allRelatedOntologyTermTagGroups.add(MatchedOntologyTermHit
								.create(targetOntologyTermHit, sourceOntologyTermHit, relatedness));
					}
				}
			}
		}

		Collections.sort(allRelatedOntologyTermTagGroups);

		//The same ontologyTerm might be matched to multiple ontologyTerms. Once the ontologyTerm A is paired with the ontologyTerm B, it's not allowed to be paired with other ontology terms.
		List<MatchedOntologyTermHit> matchedOntologyTermHits = new ArrayList<>();
		List<OntologyTerm> occupiedTargetOntologyTerms = new ArrayList<>();
		List<OntologyTerm> occupiedSourceOntologyTerms = new ArrayList<>();
		for (MatchedOntologyTermHit matchedTagGroup : allRelatedOntologyTermTagGroups)
		{
			OntologyTerm targetOntologyTerm = matchedTagGroup.getTarget().getOntologyTerm();
			OntologyTerm sourceOntologyTerm = matchedTagGroup.getSource().getOntologyTerm();
			if (!occupiedTargetOntologyTerms.contains(targetOntologyTerm) && !occupiedSourceOntologyTerms
					.contains(sourceOntologyTerm))
			{
				matchedOntologyTermHits.add(matchedTagGroup);
				occupiedTargetOntologyTerms.add(targetOntologyTerm);
				occupiedSourceOntologyTerms.add(sourceOntologyTerm);
			}
		}

		return calculate(targetAttribute, sourceAttribute, strictMatch, matchedOntologyTermHits);
	}

	/**
	 * Calculate the semantic/lexical vsm and ngram scores for the target {@link BiobankSampleAttribute} and the source {@link BiobankSampleAttribute} based on labels, descriptions and involved {@link OntologyTerm}s
	 *
	 * @param targetAttribute
	 * @param sourceAttribute
	 * @param strictMatch
	 * @param matchedOntologyTermHits a list of {@link OntologyTerm} pairs that involved in matching the target {@link BiobankSampleAttribute} to the source {@link BiobankSampleAttribute}
	 * @return
	 */
	MatchingExplanationHit calculate(BiobankSampleAttribute targetAttribute, BiobankSampleAttribute sourceAttribute,
			boolean strictMatch, List<MatchedOntologyTermHit> matchedOntologyTermHits)
	{
		String targetLabel = targetAttribute.getLabel();
		String sourceLabel = sourceAttribute.getLabel();

		String targetDescription = targetAttribute.getDescription();
		String sourceDescription = sourceAttribute.getDescription();

		MatchingExplanationHit hitForLabel = calculate(targetLabel, sourceLabel, matchedOntologyTermHits, strictMatch);

		//If both of the target and source descriptions are not empty and they are not same as the corresponding labels. Then we calculate new scores based on the dsecriptions
		if (isNotBlank(targetDescription) && isNotBlank(sourceDescription) && (
				(!targetLabel.equals(targetDescription) || !sourceLabel.equals(sourceDescription)) && !(
						targetLabel.equals(targetDescription) && sourceLabel.equals(sourceDescription))))
		{
			MatchingExplanationHit hitForDescription = calculate(targetDescription, sourceDescription,
					matchedOntologyTermHits, strictMatch);

			float vsmScore =
					hitForDescription.getVsmScore() > hitForLabel.getVsmScore() ? hitForDescription.getVsmScore() :
							(hitForLabel.getVsmScore() + hitForDescription.getVsmScore()) / 2;

			float ngramScore = hitForDescription.getNgramScore() > hitForLabel.getNgramScore() ? hitForDescription
					.getNgramScore() : (hitForLabel.getNgramScore() + hitForDescription.getNgramScore()) / 2;

			hitForLabel = MatchingExplanationHit
					.create(hitForLabel.getMatchedWords(), matchedOntologyTermHits, vsmScore, ngramScore);
		}
		return hitForLabel;
	}

	/**
	 * Calculate the semantic/lexical vsm and ngram scores for the target {@link String} (label or description) and the source {@link String} (label or description) based on the involved {@link OntologyTerm}s
	 *
	 * @param target
	 * @param source
	 * @param matchedOntologyTermHits a list of {@link OntologyTerm} pairs that involved in matching the target {@link String} to the source {@link String}
	 * @param strictMatch
	 * @return
	 */
	MatchingExplanationHit calculate(String target, String source, List<MatchedOntologyTermHit> matchedOntologyTermHits,
			boolean strictMatch)
	{
		for (MatchedOntologyTermHit matchedOntologyTermHit : matchedOntologyTermHits)
		{
			OntologyTermHit targetOntologyTermHit = matchedOntologyTermHit.getTarget();
			OntologyTermHit sourceOntologyTermHit = matchedOntologyTermHit.getSource();

			Set<String> targetMatchedWords = splitIntoUniqueTerms(targetOntologyTermHit.getMatchedWords());
			Set<String> sourceMatchedWords = splitIntoUniqueTerms(sourceOntologyTermHit.getMatchedWords());

			if (targetOntologyTermHit.getMatchedWords().length() > sourceOntologyTermHit.getMatchedWords().length())
			{
				//Replace the source label words with the matched target words
				source = concat(
						splitIntoUniqueTerms(source).stream().filter(word -> !sourceMatchedWords.contains(word)),
						targetMatchedWords.stream()).distinct().collect(joining(SINGLE_SPACE_CHAR));
			}
			else
			{
				//Replace the target label words with the matched source words
				target = concat(
						splitIntoUniqueTerms(target).stream().filter(word -> !targetMatchedWords.contains(word)),
						sourceMatchedWords.stream()).distinct().collect(joining(SINGLE_SPACE_CHAR));
			}
		}

		String matchedWords = matchedOntologyTermHits.isEmpty() ? findMatchedWords(target, source).stream()
				.filter(word -> !STOPWORDSLIST.contains(word))
				.collect(joining(SINGLE_SPACE_CHAR)) : matchedOntologyTermHits.stream()
				.map(MatchedOntologyTermHit::getCombinedMatchedWords)
				.map(SemanticSearchServiceUtils::splitIntoUniqueTerms).flatMap(Set::stream).distinct()
				.collect(joining(SINGLE_SPACE_CHAR));

		float vsmScore = vectorSpaceModelAttributeSimilarity.score(target, source, strictMatch);

		float ngramScore = ngramAttributeSimilarity.score(target, source, strictMatch);

		for (MatchedOntologyTermHit matchedOntologyTermHit : matchedOntologyTermHits)
		{
			float semanticSimilarity = matchedOntologyTermHit.getSimilarity().floatValue();
			float semanticContribution =
					semanticSimilarity * matchedOntologyTermHit.getCombinedMatchedWords().length() / (target.length()
							+ source.length());
			float calibratedContribution = semanticContribution * (float) Math.pow(semanticSimilarity, 3.0);

			vsmScore = vsmScore - semanticContribution + calibratedContribution;
			ngramScore = ngramScore - semanticContribution + calibratedContribution;
		}

		return MatchingExplanationHit.create(matchedWords, matchedOntologyTermHits, vsmScore, ngramScore);
	}

	private OntologyTermHit createOntologyTermTag(OntologyTerm ontologyTerm,
			BiobankSampleAttribute biobankSampleAttribute)
	{
		Set<String> stemmedAttributeLabelWords = Stemmer.splitAndStem(biobankSampleAttribute.getLabel());

		for (String synonym : ontologyTerm.getSynonyms())
		{
			if (stemmedAttributeLabelWords.containsAll(Stemmer.splitAndStem(synonym)))
			{
				String matchedWords = termJoiner.join(findMatchedWords(biobankSampleAttribute.getLabel(), synonym));
				float score = (float) stringMatching(biobankSampleAttribute.getLabel(), synonym) / 100;

				return OntologyTermHit.create(ontologyTerm, matchedWords, score);
			}
		}
		return null;
	}
}
