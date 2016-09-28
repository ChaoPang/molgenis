package org.molgenis.data.semanticsearch.explain.service.impl;

import static java.util.Collections.emptyList;
import static java.util.Comparator.naturalOrder;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceImpl.MAX_NUMBER_ATTRIBTUES;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.getLowerCaseTerms;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.splitRemoveStopWords;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;
import static org.molgenis.ontology.utils.Stemmer.splitAndStem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.semanticsearch.explain.bean.AttributeMatchExplanation;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermQueryExpansion;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermQueryExpansionSolution;
import org.molgenis.data.semanticsearch.explain.criteria.impl.StrictMatchingCriterion;
import org.molgenis.data.semanticsearch.explain.service.ExplainMappingService;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.utils.Stemmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Joiner;

import autovalue.shaded.com.google.common.common.collect.Sets;

public class ExplainMappingServiceImpl implements ExplainMappingService
{
	private final OntologyService ontologyService;
	private final TagGroupGenerator tagGroupGenerator;
	private final Joiner termJoiner = Joiner.on(' ');

	private final static AttributeMatchExplanation EMPTY_EXPLAINATION = AttributeMatchExplanation
			.create(StringUtils.EMPTY, StringUtils.EMPTY, null, 0.0f);

	private static final Logger LOG = LoggerFactory.getLogger(ExplainMappingServiceImpl.class);

	@Autowired
	public ExplainMappingServiceImpl(OntologyService ontologyService, TagGroupGenerator tagGroupGenerator)
	{
		this.ontologyService = requireNonNull(ontologyService);
		this.tagGroupGenerator = requireNonNull(tagGroupGenerator);
	}

	@Override
	public AttributeMatchExplanation explainMapping(SemanticSearchParam semanticSearchParam, String matchedResult)
	{
		List<OntologyTermQueryExpansion> ontologyTermQueryExpansions;
		if (semanticSearchParam.getQueryExpansionParameter().isSemanticSearchEnabled())
		{
			ontologyTermQueryExpansions = semanticSearchParam.getTagGroups().stream()
					.map(hit -> new OntologyTermQueryExpansion(hit.getCombinedOntologyTerm(), ontologyService,
							semanticSearchParam.getQueryExpansionParameter()))
					.collect(toList());
		}
		else ontologyTermQueryExpansions = emptyList();

		return explainMapping(semanticSearchParam.getLexicalQueries(), ontologyTermQueryExpansions, matchedResult);
	}

	AttributeMatchExplanation explainMapping(Set<String> lexicalQueries,
			List<OntologyTermQueryExpansion> ontologyTermQueryExpansions, String matchedSource)
	{
		// Compute the pairwise lexical similarities between the two sets of query terms and find the best matching
		// target query that yields the highest similarity score.
		AttributeMatchExplanation lexicalBasedExplanation = createLexicalBasedExplanation(lexicalQueries,
				matchedSource);

		// Unfortunately the ElasticSearch built-in explain-api doesn't scale up. In order to explain why the source
		// attribute was matched. Now we take the source attribute as the query and filter/find the best combination
		// of ontology terms from the relevantOntologyTerms that are associated with the target attribute. By doing
		// this, we can deduce which ontology terms were used as the expanded queries for finding that particular source
		// attribute.
		AttributeMatchExplanation ontologyBasedExplanation = createOntologyBasedExplanation(lexicalQueries,
				ontologyTermQueryExpansions, ontologyService.getAllOntologiesIds(), matchedSource);

		AttributeMatchExplanation explanation = lexicalBasedExplanation.getScore() > ontologyBasedExplanation.getScore()
				? lexicalBasedExplanation : ontologyBasedExplanation;

		return explanation;
	}

	/**
	 * Filters all the relevant {@link OntologyTerm}s and creates the {@link TagGroup}s for the attribute.
	 * {@link TagGroup} contains the best combination of ontology terms that yields the highest lexical similarity
	 * score.
	 * 
	 * @param matchedSourceAttribute
	 * @param ontologyIds
	 * @param scope
	 *            defines a scope of ontology terms in which the search is performed.
	 * @return
	 */
	private AttributeMatchExplanation createOntologyBasedExplanation(Set<String> lexicalQueries,
			List<OntologyTermQueryExpansion> ontologyTermQueryExpansions, List<String> ontologyIds,
			String matchedSource)
	{
		Set<String> matchedSourceWords = splitRemoveStopWords(matchedSource);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("findOntologyTerms({},{},{})", ontologyIds, matchedSourceWords, MAX_NUMBER_ATTRIBTUES);
		}

		List<OntologyTerm> ontologyTermScope = ontologyTermQueryExpansions.stream()
				.map(OntologyTermQueryExpansion::getOntologyTerms).flatMap(ots -> ots.stream())
				.collect(Collectors.toList());

		if (LOG.isDebugEnabled())
		{
			LOG.debug("OntologyTerms {}", ontologyTermScope);
		}

		List<OntologyTerm> relevantOntologyTerms = ontologyService.filterOntologyTerms(ontologyIds, matchedSourceWords,
				ontologyTermScope.size(), ontologyTermScope);

		List<TagGroup> tagGroups = tagGroupGenerator.applyTagMatchingCriterion(relevantOntologyTerms,
				matchedSourceWords, new StrictMatchingCriterion());

		// TODO
		List<TagGroup> matchedSourceTagGroups = tagGroupGenerator.combineTagGroups(matchedSourceWords, tagGroups);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", matchedSourceTagGroups);
		}

		if (matchedSourceTagGroups.size() > 0)
		{
			TagGroup tagGroup = matchedSourceTagGroups.get(0);

			OntologyTermQueryExpansionSolution queryExpansionSolution = ontologyTermQueryExpansions.stream()
					.map(expansion -> expansion.getQueryExpansionSolution(tagGroup)).sorted().findFirst().orElse(null);

			Optional<Hit<String>> max = stream(lexicalQueries.spliterator(), false)
					.map(lexicalQuery -> computeScoreForMatchedSource(tagGroup, queryExpansionSolution, lexicalQuery,
							matchedSource))
					.max(naturalOrder());

			if (max.isPresent())
			{
				OntologyTerm origin = OntologyTerm
						.and(queryExpansionSolution.getMatchOntologyTerms().stream().toArray(OntologyTerm[]::new));
				OntologyTermHit ontologyTermHit = OntologyTermHit.create(origin, tagGroup.getCombinedOntologyTerm());

				Hit<String> joinedSynonymHit = max.get();
				String matchedWords = termJoiner.join(findMatchedWords(joinedSynonymHit.getResult(), matchedSource));
				return AttributeMatchExplanation.create(matchedWords, joinedSynonymHit.getResult(), ontologyTermHit,
						joinedSynonymHit.getScore());
			}
		}

		return EMPTY_EXPLAINATION;
	}

	Hit<String> computeScoreForMatchedSource(TagGroup tagGroup,
			OntologyTermQueryExpansionSolution queryExpansionSolution, String targetQueryTerm,
			String sourceAttributeDescription)
	{
		String matchedOntologyTermsInSource = tagGroup.getMatchedWords();

		if (queryExpansionSolution == null) return Hit.create(matchedOntologyTermsInSource, tagGroup.getScore());

		Set<String> matchedOntologyTermsInTarget = findMatchedSynonymsInTarget(
				queryExpansionSolution.getMatchOntologyTerms(), targetQueryTerm);

		Set<String> unmatchedWordsInSource = findLeftUnmatchedWords(sourceAttributeDescription,
				matchedOntologyTermsInSource);

		String transformedSourceDescription = termJoiner
				.join(Sets.union(matchedOntologyTermsInTarget, unmatchedWordsInSource));

		float adjustedScore = (float) stringMatching(transformedSourceDescription, targetQueryTerm) / 100;

		Set<String> additionalMatchedWords = findMatchedWords(targetQueryTerm, termJoiner.join(unmatchedWordsInSource));

		if (additionalMatchedWords.size() > 0)
		{
			matchedOntologyTermsInSource += ' ' + termJoiner.join(additionalMatchedWords);
		}

		return Hit.create(matchedOntologyTermsInSource, adjustedScore);
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
		for (String sourceWord : SemanticSearchServiceUtils.splitIntoUniqueTerms(stringOne))
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
		for (String wordFromString1 : SemanticSearchServiceUtils.splitIntoUniqueTerms(string1))
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
	private Set<String> findMatchedSynonymsInTarget(List<OntologyTerm> ontologyTerms, String targetQueryTerm)
	{
		Set<String> targetQueryTermWords = splitAndStem(targetQueryTerm);
		Set<String> usedOntologyTermQueries = new LinkedHashSet<>();

		for (OntologyTerm ontologyTerm : ontologyTerms)
		{
			for (String synonym : getLowerCaseTerms(ontologyTerm))
			{
				if (targetQueryTermWords.containsAll(splitAndStem(synonym)))
				{
					usedOntologyTermQueries.add(synonym);
					break;
				}
			}
		}
		return usedOntologyTermQueries;
	}

	AttributeMatchExplanation createLexicalBasedExplanation(Set<String> queriesFromTarget, String matchedSource)
	{
		double highestScore = 0;
		String bestTargetQuery = null;

		for (String targetQuery : queriesFromTarget)
		{
			double score = stringMatching(targetQuery, matchedSource);
			if (score > highestScore)
			{
				bestTargetQuery = targetQuery;
				highestScore = score;
			}
		}

		if (isNotBlank(bestTargetQuery))
		{
			float score = Math.round((float) highestScore / 100 * 100000) / 100000.0f;
			return AttributeMatchExplanation.create(termJoiner.join(findMatchedWords(bestTargetQuery, matchedSource)),
					bestTargetQuery, null, score);
		}
		return EMPTY_EXPLAINATION;
	}
}
