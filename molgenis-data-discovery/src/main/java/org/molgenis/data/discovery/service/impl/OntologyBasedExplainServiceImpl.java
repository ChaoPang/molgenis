package org.molgenis.data.discovery.service.impl;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.molgenis.data.discovery.service.impl.OntologyBasedMatcher.EXPANSION_LEVEL;
import static org.molgenis.data.discovery.service.impl.OntologyBasedMatcher.STOP_LEVEL;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.findMatchedWords;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.splitIntoUniqueTerms;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.STOPWORDSLIST;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.molgenis.data.IdGenerator;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.MatchingExplanation;
import org.molgenis.data.discovery.service.OntologyBasedExplainService;
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

/**
 * This is the new explain API that explains the candidate matches produced by {@link OntologyBasedMatcher}
 * 
 * @author chaopang
 *
 */
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
			List<BiobankSampleAttribute> sourceAttributes, AttributeCandidateScoringImpl attributeCandidateScoring)
	{
		Map<String, Boolean> matchedWordsExplained = new HashMap<>();

		List<AttributeMappingCandidate> candidates = new ArrayList<>();

		LOG.trace("Started explaining the matched source attributes");

		for (BiobankSampleAttribute sourceAttribute : sourceAttributes)
		{
			Multimap<OntologyTerm, OntologyTerm> relatedOntologyTerms = findAllRelatedOntologyTerms(targetAttribute,
					sourceAttribute, biobankUniverse);

			MatchingExplanation explanation = null;

			if (!relatedOntologyTerms.isEmpty())
			{
				Hit<String> computeScoreForMatchedSource = attributeCandidateScoring.score(targetAttribute,
						sourceAttribute, biobankUniverse, relatedOntologyTerms, semanticSearchParam.isStrictMatch());

				Set<String> matchedWords = findMatchedWords(computeScoreForMatchedSource.getResult(),
						sourceAttribute.getLabel());

				matchedWords
						.addAll(findMatchedWords(computeScoreForMatchedSource.getResult(), targetAttribute.getLabel()));

				List<OntologyTerm> ontologyTerms = relatedOntologyTerms.values().stream().distinct()
						.collect(Collectors.toList());

				explanation = MatchingExplanation.create(idGenerator.generateId(), ontologyTerms,
						computeScoreForMatchedSource.getResult(), termJoiner.join(matchedWords),
						computeScoreForMatchedSource.getScore());
			}
			else
			{
				Set<String> matchedWords = findMatchedWords(targetAttribute.getLabel(), sourceAttribute.getLabel());

				double score = stringMatching(targetAttribute.getLabel(), sourceAttribute.getLabel()) / 100;

				explanation = MatchingExplanation.create(idGenerator.generateId(), Collections.emptyList(),
						targetAttribute.getLabel(), termJoiner.join(matchedWords), score);
			}

			String matchedWords = explanation.getMatchedWords();

			if (matchedWordsExplained.containsKey(matchedWords) && matchedWordsExplained.get(matchedWords))
			{
				candidates.add(AttributeMappingCandidate.create(idGenerator.generateId(), biobankUniverse,
						targetAttribute, sourceAttribute, explanation));
			}
			else if (!matchedWordsExplained.containsKey(matchedWords)
					&& (explanation.getNgramScore() > semanticSearchParam.getHighQualityThreshold()
							|| isMatchHighQuality(explanation, biobankUniverse)))
			{
				candidates.add(AttributeMappingCandidate.create(idGenerator.generateId(), biobankUniverse,
						targetAttribute, sourceAttribute, explanation));

				matchedWordsExplained.put(matchedWords, true);
			}
			else
			{
				matchedWordsExplained.put(matchedWords, false);
			}
		}

		LOG.trace("Finished explaining the matched source attributes");

		return candidates.stream().sorted().collect(Collectors.toList());
	}

	private boolean isMatchHighQuality(MatchingExplanation explanation, BiobankUniverse biobankUniverse)
	{
		List<OntologyTerm> ontologyTerms = explanation.getOntologyTerms();

		if (ontologyTerms.isEmpty())
		{
			ontologyTerms = ontologyService.findExcatOntologyTerms(ontologyService.getAllOntologiesIds(),
					splitIntoUniqueTerms(explanation.getMatchedWords()), 10);
		}

		List<SemanticType> conceptFilter = biobankUniverse.getKeyConcepts();
		Multimap<String, OntologyTerm> ontologyTermWithSameSynonyms = LinkedHashMultimap.create();
		Set<String> stemmedMatchedWords = Stemmer.splitAndStem(explanation.getMatchedWords());

		for (OntologyTerm ontologyTerm : ontologyTerms)
		{
			Optional<String> findFirst = ontologyTerm.getSynonyms().stream().map(Stemmer::splitAndStem)
					.filter(stemmedSynonymWords -> stemmedMatchedWords.containsAll(stemmedSynonymWords))
					.map(words -> words.stream().sorted().collect(Collectors.joining(" "))).findFirst();
			if (findFirst.isPresent())
			{
				ontologyTermWithSameSynonyms.put(findFirst.get(), ontologyTerm);
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

		String matchedWords = SemanticSearchServiceUtils.splitIntoUniqueTerms(explanation.getMatchedWords()).stream()
				.map(String::toLowerCase).filter(word -> !STOPWORDSLIST.contains(word)).collect(joining(" "));

		// TODO: for testing purpose
		return !collect.isEmpty() && matchedWords.length() >= 3;
		// return true;
	}

	private Multimap<OntologyTerm, OntologyTerm> findAllRelatedOntologyTerms(BiobankSampleAttribute targetAttribute,
			BiobankSampleAttribute sourceAttribute, BiobankUniverse biobankUniverse)
	{
		Multimap<OntologyTerm, OntologyTerm> relatedOntologyTerms = LinkedHashMultimap.create();

		Set<OntologyTerm> targetOntologyTerms = getAllOntologyTerms(targetAttribute, biobankUniverse);

		Set<OntologyTerm> sourceOntologyTerms = getAllOntologyTerms(sourceAttribute, biobankUniverse);

		for (OntologyTerm targetOntologyTerm : targetOntologyTerms)
		{
			for (OntologyTerm sourceOntologyTerm : sourceOntologyTerms)
			{
				if (ontologyService.related(targetOntologyTerm, sourceOntologyTerm, STOP_LEVEL)
						&& ontologyService.areWithinDistance(targetOntologyTerm, sourceOntologyTerm, EXPANSION_LEVEL))
				{
					relatedOntologyTerms.put(targetOntologyTerm, sourceOntologyTerm);
				}
			}
		}

		return relatedOntologyTerms;
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
}
