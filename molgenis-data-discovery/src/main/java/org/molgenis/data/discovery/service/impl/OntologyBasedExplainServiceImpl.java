package org.molgenis.data.discovery.service.impl;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.getLowerCaseTerms;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;
import static org.molgenis.ontology.utils.Stemmer.splitAndStem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.molgenis.data.IdGenerator;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.MatchingExplanation;
import org.molgenis.data.discovery.service.OntologyBasedExplainService;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.utils.Stemmer;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public class OntologyBasedExplainServiceImpl implements OntologyBasedExplainService
{
	private final Joiner termJoiner = Joiner.on(' ');

	private final IdGenerator idGenerator;
	private final OntologyService ontologyService;

	@Autowired
	public OntologyBasedExplainServiceImpl(IdGenerator idGenerator, OntologyService ontologyService)
	{
		this.idGenerator = requireNonNull(idGenerator);
		this.ontologyService = requireNonNull(ontologyService);
	}

	@Override
	public List<AttributeMappingCandidate> explain(BiobankUniverse biobankUniverse,
			SemanticSearchParam semanticSearchParam, BiobankSampleAttribute targetAttribute,
			List<BiobankSampleAttribute> sourceAttributes)
	{
		List<AttributeMappingCandidate> candidates = new ArrayList<>();

		Set<OntologyTerm> targetOntologyTerms = getAllOntologyTerms(targetAttribute, biobankUniverse);

		for (BiobankSampleAttribute sourceAttribute : sourceAttributes)
		{
			Set<OntologyTermHit> findAllRelatedOntologyTerms = findAllRelatedOntologyTerms(targetOntologyTerms,
					getAllOntologyTerms(sourceAttribute, biobankUniverse), semanticSearchParam);
			MatchingExplanation explanation;
			if (findAllRelatedOntologyTerms.size() > 0)
			{
				Hit<String> computeScoreForMatchedSource = computeScoreForMatchedSource(findAllRelatedOntologyTerms,
						targetAttribute, sourceAttribute);
				float score = computeScoreForMatchedSource.getScore();
				String queryString = computeScoreForMatchedSource.getResult();
				String matchedWords = termJoiner.join(findMatchedWords(queryString, sourceAttribute.getLabel()));
				List<OntologyTerm> ontologyTerms = findAllRelatedOntologyTerms.stream()
						.map(OntologyTermHit::getOntologyTerm).collect(Collectors.toList());
				explanation = MatchingExplanation.create(idGenerator.generateId(), ontologyTerms, queryString,
						matchedWords, score);
			}
			else
			{
				Set<String> matchedWords = findMatchedWords(targetAttribute.getLabel(), sourceAttribute.getLabel());
				double score = stringMatching(targetAttribute.getLabel(), sourceAttribute.getLabel()) / 100;
				explanation = MatchingExplanation.create(idGenerator.generateId(), emptyList(),
						targetAttribute.getLabel(), termJoiner.join(matchedWords), score);
			}

			candidates.add(AttributeMappingCandidate.create(idGenerator.generateId(), biobankUniverse, targetAttribute,
					sourceAttribute, explanation));
		}

		return candidates;
	}

	private Set<OntologyTerm> getAllOntologyTerms(BiobankSampleAttribute biobankSampleAttribute,
			BiobankUniverse biobankUniverse)
	{
		List<SemanticType> keyConcepts = biobankUniverse.getKeyConcepts();
		return biobankSampleAttribute.getTagGroups().stream()
				.flatMap(tagGroup -> tagGroup.getOntologyTerms().stream()
						.filter(ot -> ot.getSemanticTypes().stream().anyMatch(st -> !keyConcepts.contains(st))))
				.collect(toSet());
	}

	private Set<OntologyTermHit> findAllRelatedOntologyTerms(Set<OntologyTerm> targetOntologyTerms,
			Set<OntologyTerm> sourceOntologyTerms, SemanticSearchParam semanticSearchParam)
	{
		int expansionLevel = semanticSearchParam.getQueryExpansionParameter().getExpansionLevel();
		Set<OntologyTermHit> relatedOntologyTerms = new LinkedHashSet<>();
		for (OntologyTerm targetOntologyTerm : targetOntologyTerms)
		{
			for (OntologyTerm sourceOntologyTerm : sourceOntologyTerms)
			{
				if (ontologyService.related(targetOntologyTerm, sourceOntologyTerm)
						&& ontologyService.areWithinDistance(targetOntologyTerm, sourceOntologyTerm, expansionLevel))
				{
					relatedOntologyTerms.add(OntologyTermHit.create(targetOntologyTerm, sourceOntologyTerm));
				}
			}
		}
		return relatedOntologyTerms;
	}

	Hit<String> computeScoreForMatchedSource(Set<OntologyTermHit> ontologyTermHits, BiobankSampleAttribute target,
			BiobankSampleAttribute source)
	{
		String targetLabel = target.getLabel();
		String sourceLabel = source.getLabel();

		Set<Hit<String>> matchedOntologyTermsInTarget = findMatchedSynonymsInAttribute(ontologyTermHits.stream()
				.map(OntologyTermHit::getOrigin).map(ot -> OntologyTermHit.create(ot, ot)).collect(toSet()),
				targetLabel);

		Set<Hit<String>> matchedOntologyTermsInSource = findMatchedSynonymsInAttribute(ontologyTermHits, sourceLabel);

		String joinedMatchedOntologyTermsInTarget = termJoiner
				.join(matchedOntologyTermsInTarget.stream().map(Hit::getResult).collect(Collectors.toSet()));

		String joinedMatchedOntologyTermsInSource = termJoiner
				.join(matchedOntologyTermsInSource.stream().map(Hit::getResult).collect(Collectors.toSet()));

		Set<String> unmatchedWordsInTarget = findLeftUnmatchedWords(targetLabel, joinedMatchedOntologyTermsInTarget);

		String transformedSourceLabel = termJoiner.join(Sets.union(
				matchedOntologyTermsInSource.stream().map(Hit::getResult).collect(toSet()), unmatchedWordsInTarget));

		float adjustedScore = (float) stringMatching(transformedSourceLabel, sourceLabel) / 100;

		for (Hit<String> hit : matchedOntologyTermsInSource)
		{
			float ngramContribution = adjustedScore * hit.getResult().length() / transformedSourceLabel.length();
			adjustedScore = adjustedScore - ngramContribution
					+ ngramContribution * (float) Math.pow(hit.getScore(), 2.0);
		}

		return Hit.create(joinedMatchedOntologyTermsInSource, adjustedScore);
	}

	/**
	 * find the unmatched word from the left argument
	 * 
	 * @param stringOne
	 * @param stringTwo
	 * @return
	 */
	private Set<String> findLeftUnmatchedWords(String stringOne, String stringTwo)
	{
		Set<String> additionalMatchedWords = new LinkedHashSet<>();
		Set<String> stemmedStringOneWords = splitAndStem(stringTwo);
		for (String sourceWord : SemanticSearchServiceUtils.splitIntoTerms(stringOne))
		{
			String stemmedSourceWord = Stemmer.stem(sourceWord);
			if (!stemmedStringOneWords.contains(stemmedSourceWord))
			{
				additionalMatchedWords.add(sourceWord);
			}
		}
		return additionalMatchedWords;
	}

	/**
	 * find matched words between two {@link String}s
	 * 
	 * @param string1
	 * @param string2
	 * @return
	 */
	private Set<String> findMatchedWords(String string1, String string2)
	{
		Set<String> intersectedWords = new LinkedHashSet<>();
		Set<String> stemmedWordsFromString2 = splitAndStem(string2);
		for (String wordFromString1 : SemanticSearchServiceUtils.splitIntoTerms(string1))
		{
			String stemmedSourceWord = Stemmer.stem(wordFromString1);
			if (stemmedWordsFromString2.contains(stemmedSourceWord))
			{
				intersectedWords.add(wordFromString1);
			}
		}
		return intersectedWords;
	}

	/**
	 * Get a list of matched Synonym from {@link OntologyTerm}s for the given target query
	 * 
	 * @param ontologyTerms
	 * @param targetQueryTerm
	 * @return
	 */
	private Set<Hit<String>> findMatchedSynonymsInAttribute(Set<OntologyTermHit> ontologyTermHits,
			String targetQueryTerm)
	{
		Set<String> targetQueryTermWords = splitAndStem(targetQueryTerm);

		Map<String, Hit<String>> usedOntologyTermSynonymMap = new HashMap<>();

		for (OntologyTermHit ontologyTermHit : ontologyTermHits)
		{
			OntologyTerm origin = ontologyTermHit.getOrigin();
			OntologyTerm ontologyTerm = ontologyTermHit.getOntologyTerm();

			for (String synonym : getLowerCaseTerms(ontologyTerm))
			{
				Set<String> splitAndStem = splitAndStem(synonym);
				if (targetQueryTermWords.containsAll(splitAndStem))
				{
					String stemmedSynonym = termJoiner.join(splitAndStem.stream().sorted().collect(toSet()));

					if (!usedOntologyTermSynonymMap.containsKey(stemmedSynonym))
					{
						float score = ontologyService.isDescendant(origin, ontologyTerm)
								? ontologyService.getOntologyTermSemanticRelatedness(origin, ontologyTerm).floatValue()
								: 1.0f;

						usedOntologyTermSynonymMap.put(stemmedSynonym, Hit.create(synonym, score));
					}

					break;
				}
			}
		}

		return Sets.newHashSet(usedOntologyTermSynonymMap.values());
	}
}
