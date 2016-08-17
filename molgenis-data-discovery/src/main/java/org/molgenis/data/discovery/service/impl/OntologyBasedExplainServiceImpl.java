package org.molgenis.data.discovery.service.impl;

import static com.google.common.collect.Sets.union;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.getLowerCaseTerms;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.splitIntoTerms;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.STOPWORDSLIST;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;
import static org.molgenis.ontology.utils.Stemmer.splitAndStem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

public class OntologyBasedExplainServiceImpl implements OntologyBasedExplainService
{
	private final static Logger LOG = LoggerFactory.getLogger(OntologyBasedExplainServiceImpl.class);
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
		Map<String, Boolean> matchedWordsExplained = new HashMap<>();

		List<AttributeMappingCandidate> candidates = new ArrayList<>();

		Set<OntologyTerm> targetOntologyTerms = getAllOntologyTerms(targetAttribute, biobankUniverse);

		if (LOG.isTraceEnabled())
		{
			LOG.trace("Started explaining the matched source attributes");
		}

		for (BiobankSampleAttribute sourceAttribute : sourceAttributes)
		{
			Set<OntologyTerm> sourceOntologyTerms = getAllOntologyTerms(sourceAttribute, biobankUniverse);
			Set<OntologyTermHit> findAllRelatedOntologyTerms = findAllRelatedOntologyTerms(targetOntologyTerms,
					sourceOntologyTerms, semanticSearchParam);

			MatchingExplanation explanation = null;

			if (findAllRelatedOntologyTerms.size() > 0)
			{
				Hit<String> computeScoreForMatchedSource = computeScoreForMatchedSource(findAllRelatedOntologyTerms,
						targetAttribute, sourceAttribute);

				Set<String> matchedWords = findMatchedWords(computeScoreForMatchedSource.getResult(),
						sourceAttribute.getLabel());

				List<OntologyTerm> ontologyTerms = findAllRelatedOntologyTerms.stream()
						.map(OntologyTermHit::getOntologyTerm).collect(toList());

				explanation = MatchingExplanation.create(idGenerator.generateId(), ontologyTerms,
						computeScoreForMatchedSource.getResult(), termJoiner.join(matchedWords),
						computeScoreForMatchedSource.getScore());
			}
			else
			{
				Set<String> matchedWords = findMatchedWords(targetAttribute.getLabel(), sourceAttribute.getLabel());

				double score = stringMatching(targetAttribute.getLabel(), sourceAttribute.getLabel()) / 100;

				if (matchedWords.isEmpty())
				{
					matchedWords = targetOntologyTerms.stream().flatMap(ot -> ot.getSynonyms().stream())
							.flatMap(synonym -> findMatchedWords(synonym, sourceAttribute.getLabel()).stream())
							.collect(toSet());
					score = stringMatching(termJoiner.join(targetAttribute.getLabel(), matchedWords),
							termJoiner.join(sourceAttribute.getLabel(), matchedWords)) / 100;
				}

				String joinedMatchedWords = matchedWords.stream().map(word -> word.replaceAll("\\d+", EMPTY))
						.filter(StringUtils::isNotBlank).collect(joining(" "));

				if (matchedWordsExplained.containsKey(joinedMatchedWords)
						&& matchedWordsExplained.get(joinedMatchedWords))
				{
					explanation = MatchingExplanation.create(idGenerator.generateId(), Collections.emptyList(),
							targetAttribute.getLabel(), joinedMatchedWords, score);
				}
				else if (!matchedWordsExplained.containsKey(joinedMatchedWords)
						&& (score > semanticSearchParam.getHighQualityThreshold()
								|| isMatchHighQuality(joinedMatchedWords, biobankUniverse)))
				{
					explanation = MatchingExplanation.create(idGenerator.generateId(), Collections.emptyList(),
							targetAttribute.getLabel(), joinedMatchedWords, score);

					matchedWordsExplained.put(joinedMatchedWords, true);
				}
				else
				{
					matchedWordsExplained.put(joinedMatchedWords, false);
				}
			}

			if (Objects.nonNull(explanation))
			{
				// The candidate matches are removed if 1. the matched words only consist of stop words; 2. the length
				// of matched words is less than 3;
				String matchedWords = splitIntoTerms(explanation.getMatchedWords()).stream().map(String::toLowerCase)
						.filter(word -> !STOPWORDSLIST.contains(word)).collect(joining(" "));

				if (matchedWords.length() >= 3)
				{
					candidates.add(AttributeMappingCandidate.create(idGenerator.generateId(), biobankUniverse,
							targetAttribute, sourceAttribute, explanation));
				}
			}
		}

		if (LOG.isTraceEnabled())
		{
			LOG.trace("Finished explaining the matched source attributes");
		}

		return candidates.stream().sorted().collect(Collectors.toList());
	}

	private boolean isMatchHighQuality(String joinedMatchedWords, BiobankUniverse biobankUniverse)
	{
		List<OntologyTerm> ontologyTerms = ontologyService.findExcatOntologyTerms(ontologyService.getAllOntologiesIds(),
				SemanticSearchServiceUtils.splitIntoTerms(joinedMatchedWords), 10);

		List<SemanticType> conceptFilter = biobankUniverse.getKeyConcepts();
		Multimap<String, OntologyTerm> ontologyTermWithSameSynonyms = LinkedHashMultimap.create();
		Set<String> stemmedMatchedWords = Stemmer.splitAndStem(joinedMatchedWords);

		for (OntologyTerm ontologyTerm : ontologyTerms)
		{
			Optional<String> findFirst = ontologyTerm.getSynonyms().stream()
					.filter(synonym -> stemmedMatchedWords.containsAll(splitAndStem(synonym))).findFirst();
			if (findFirst.isPresent())
			{
				ontologyTermWithSameSynonyms.put(Stemmer.cleanStemPhrase(findFirst.get()), ontologyTerm);
			}
		}

		List<Collection<OntologyTerm>> collect = ontologyTermWithSameSynonyms.asMap().values().stream().filter(ots -> {
			// Good ontology terms are defined as the ontology terms whose semantic types are global concepts and not in
			// the conceptFilter
			long countOfGoodOntologyTerms = ots.stream()
					.filter(ot -> ot.getSemanticTypes().isEmpty() || ot.getSemanticTypes().stream().allMatch(
							semanticType -> semanticType.isGlobalKeyConcept() && !conceptFilter.contains(semanticType)))
					.count();

			// Bad ontology terms are defined as the ontology terms whose any of the semantic types are not global
			// concepts or in the conceptFilter
			long countOfBadOntologyTerms = ots.stream()
					.filter(ot -> !ot.getSemanticTypes().isEmpty() && ot.getSemanticTypes().stream().anyMatch(
							semanticType -> !semanticType.isGlobalKeyConcept() || conceptFilter.contains(semanticType)))
					.count();

			// If there are more good ontology terms than the bad ones, we keep the ontology terms
			return countOfGoodOntologyTerms >= countOfBadOntologyTerms;

		}).collect(toList());

		return !collect.isEmpty();
	}

	private Set<OntologyTerm> getAllOntologyTerms(BiobankSampleAttribute biobankSampleAttribute,
			BiobankUniverse biobankUniverse)
	{
		List<SemanticType> keyConcepts = biobankUniverse.getKeyConcepts();

		return biobankSampleAttribute.getTagGroups().stream().flatMap(tagGroup -> tagGroup.getOntologyTerms().stream())
				.filter(ot -> ot.getSemanticTypes().isEmpty()
						|| ot.getSemanticTypes().stream().allMatch(st -> !keyConcepts.contains(st)))
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

		String transformedSourceLabel = termJoiner.join(union(
				matchedOntologyTermsInSource.stream().map(Hit::getResult).collect(toSet()), unmatchedWordsInTarget));

		float adjustedScore = (float) stringMatching(termJoiner.join(Stemmer.splitAndStem(transformedSourceLabel)),
				sourceLabel) / 100;

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
		for (String wordFromString1 : splitIntoTerms(string1))
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
						float score = origin == ontologyTerm ? 1.0f
								: ontologyService.getOntologyTermSemanticRelatedness(origin, ontologyTerm).floatValue();

						usedOntologyTermSynonymMap.put(stemmedSynonym, Hit.create(synonym, score));
					}
					break;
				}
			}
		}

		Set<String> existingSynonymWords = new HashSet<>();
		Set<Hit<String>> ontologyTermSynonymHits = new HashSet<>();
		usedOntologyTermSynonymMap.values().stream().sorted().forEach(ontologyTermSynonym -> {
			Set<String> splitAndStem = splitAndStem(ontologyTermSynonym.getResult());
			if (!existingSynonymWords.containsAll(splitAndStem))
			{
				splitAndStem.addAll(splitAndStem);
				ontologyTermSynonymHits.add(ontologyTermSynonym);
			}
		});
		return ontologyTermSynonymHits;
	}
}
