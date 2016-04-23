package org.molgenis.data.semanticsearch.explain.service.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString.create;
import static org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceUtils.MAX_NUM_TAGS;
import static org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceUtils.UNIT_ONTOLOGY_IRI;
import static org.molgenis.ontology.core.model.OntologyTerm.create;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;
import static org.molgenis.ontology.utils.Stemmer.splitAndStem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermQueryExpansion;
import org.molgenis.data.semanticsearch.explain.bean.QueryExpansionSolution;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParameters;
import org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceUtils;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.utils.Stemmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Joiner;

import static java.util.Objects.requireNonNull;

import autovalue.shaded.com.google.common.common.collect.Sets;

public class AttributeMappingExplainServiceImpl implements AttributeMappingExplainService
{
	private final OntologyService ontologyService;
	private final SemanticSearchServiceUtils semanticSearchServiceUtils;
	private final Joiner termJoiner = Joiner.on(' ');
	private final static float HIGH_QUALITY_THRESHOLD = 0.85f;
	private final static OntologyTermHit EMPTY_ONTOLOGYTERM_HIT = OntologyTermHit.create(create(EMPTY, EMPTY), EMPTY);

	private static final Logger LOG = LoggerFactory.getLogger(AttributeMappingExplainServiceImpl.class);

	@Autowired
	public AttributeMappingExplainServiceImpl(OntologyService ontologyService,
			SemanticSearchServiceUtils semanticSearchServiceUtils)
	{
		this.ontologyService = requireNonNull(ontologyService);
		this.semanticSearchServiceUtils = requireNonNull(semanticSearchServiceUtils);
	}

	@Override
	public ExplainedAttributeMetaData explainAttributeMapping(SemanticSearchParameters semanticSearchParameters,
			AttributeMetaData matchedSourceAttribute)
	{
		AttributeMetaData targetAttribute = semanticSearchParameters.getTargetAttribute();
		Set<String> userQueries = semanticSearchParameters.getUserQueries();
		EntityMetaData targetEntityMetaData = semanticSearchParameters.getTargetEntityMetaData();
		boolean semanticSearchEnabled = semanticSearchParameters.isSemanticSearchEnabled();
		boolean childOntologyTermExpansionEnabled = semanticSearchParameters.isChildOntologyTermExpansionEnabled();

		// Collect all terms from the target attribute
		Set<String> queriesFromTargetAttribute = semanticSearchServiceUtils.getQueryTermsFromAttribute(targetAttribute,
				userQueries);
		// If semantic search is enabled, collect all the ontology terms that are associated with the target attribute,
		// which were used in query expansion for finding the relevant source attributes.
		List<OntologyTermQueryExpansion> ontologyTermQueryExpansions;
		if (semanticSearchEnabled)
		{
			List<String> ontologyTermIds = ontologyService.getOntologies().stream()
					.filter(ontology -> !ontology.getIRI().equals(UNIT_ONTOLOGY_IRI)).map(Ontology::getId)
					.collect(toList());

			ontologyTermQueryExpansions = semanticSearchServiceUtils
					.findOntologyTermsForAttr(targetAttribute, targetEntityMetaData, userQueries, ontologyTermIds)
					.stream().map(hit -> new OntologyTermQueryExpansion(hit.getResult().getOntologyTerm(),
							ontologyService, childOntologyTermExpansionEnabled))
					.collect(toList());
		}
		else ontologyTermQueryExpansions = Collections.emptyList();

		return explainExactMapping(queriesFromTargetAttribute, ontologyTermQueryExpansions, matchedSourceAttribute);
	}

	ExplainedAttributeMetaData explainExactMapping(Set<String> queriesFromTargetAttribute,
			List<OntologyTermQueryExpansion> ontologyTermQueryExpansions, AttributeMetaData matchedSourceAttribute)
	{
		// Collect all terms from the source attribute
		Set<String> queriesFromSourceAttribute = semanticSearchServiceUtils
				.getQueryTermsFromAttribute(matchedSourceAttribute, null);

		// Compute the pairwise lexical similarities between the two sets of query terms and find the best matching
		// target query that yields the highest similarity score.
		Hit<String> targetQueryTermHit = findBestQueryTerm(queriesFromTargetAttribute, queriesFromSourceAttribute);

		// Unfortunately the ElasticSearch built-in explain-api doesn't scale up. In order to explain why the source
		// attribute was matched. Now we take the source attribute as the query and filter/find the best combination
		// of ontology terms from the relevantOntologyTerms that are associated with the target attribute. By doing
		// this, we can deduce which ontology terms were used as the expanded queries for finding that particular source
		// attribute.
		Hit<OntologyTermHit> ontologyTermHit = filterTagsForAttr(matchedSourceAttribute,
				ontologyService.getAllOntologiesIds(), ontologyTermQueryExpansions, queriesFromTargetAttribute);

		// Here we check if the source attribute is matched with the original target queries or the expanded
		// ontology term queries.
		String queryOrigin = targetQueryTermHit.getScore() >= ontologyTermHit.getScore()
				? targetQueryTermHit.getResult() : ontologyTermHit.getResult().getOntologyTerm().getLabel();

		// Here we get the best matching query depending on the origin of the query.
		String bestMatchingQuery = targetQueryTermHit.getScore() >= ontologyTermHit.getScore()
				? targetQueryTermHit.getResult() : ontologyTermHit.getResult().getJoinedSynonym();

		// We collect the final matching score.
		float score = (targetQueryTermHit.getScore() >= ontologyTermHit.getScore() ? targetQueryTermHit.getScore()
				: ontologyTermHit.getScore());

		boolean isHighQuality = score >= HIGH_QUALITY_THRESHOLD;

		// We get the matched words for the source attribute.
		List<String> matchedWords = getMatchedWords(bestMatchingQuery, queriesFromSourceAttribute);

		ExplainedQueryString explainedQueryString = create(termJoiner.join(matchedWords), bestMatchingQuery,
				queryOrigin, score);

		return ExplainedAttributeMetaData.create(matchedSourceAttribute, explainedQueryString, isHighQuality);
	}

	/**
	 * Filters all the relevant {@link OntologyTerm}s and creates the {@link OntologyTermHit}s for the attribute.
	 * {@link OntologyTermHit} contains the best combination of ontology terms that yields the highest lexical
	 * similarity score.
	 * 
	 * @param matchedSourceAttribute
	 * @param ontologyIds
	 * @param scope
	 *            defines a scope of ontology terms in which the search is performed.
	 * @return
	 */
	private Hit<OntologyTermHit> filterTagsForAttr(AttributeMetaData matchedSourceAttribute, List<String> ontologyIds,
			List<OntologyTermQueryExpansion> ontologyTermQueryExpansions, Set<String> queriesFromTargetAttribute)
	{
		String sourceLabel = matchedSourceAttribute.getDescription() == null ? matchedSourceAttribute.getLabel()
				: matchedSourceAttribute.getDescription();
		Set<String> searchTerms = semanticSearchServiceUtils.splitRemoveStopWords(sourceLabel);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("findOntologyTerms({},{},{})", ontologyIds, searchTerms, MAX_NUM_TAGS);
		}

		List<OntologyTerm> ontologyTermScope = new ArrayList<>();
		ontologyTermQueryExpansions.forEach(expansion -> ontologyTermScope.addAll(expansion.getOntologyTerms()));

		List<OntologyTerm> candidates = ontologyService.fileterOntologyTerms(ontologyIds, searchTerms,
				ontologyTermScope.size(), ontologyTermScope);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", candidates);
		}

		List<Hit<OntologyTermHit>> combineOntologyTerms = semanticSearchServiceUtils.combineOntologyTerms(searchTerms,
				ontologyIds, candidates);

		if (combineOntologyTerms.size() > 0)
		{
			Hit<OntologyTermHit> hit = combineOntologyTerms.get(0);

			Optional<Hit<String>> max = stream(queriesFromTargetAttribute.spliterator(), false)
					.map(targetQueryTerm -> computeAbsoluteScoreForSourceAttribute(hit, ontologyTermQueryExpansions,
							targetQueryTerm, sourceLabel))
					.max(Comparator.naturalOrder());

			if (max.isPresent())
			{
				Hit<String> joinedSynonymHit = max.get();
				Hit<OntologyTermHit> create = Hit.create(
						OntologyTermHit.create(hit.getResult().getOntologyTerm(), joinedSynonymHit.getResult()),
						joinedSynonymHit.getScore());
				return create;
			}
		}
		return Hit.create(EMPTY_ONTOLOGYTERM_HIT, 0.0f);
	}

	Hit<String> computeAbsoluteScoreForSourceAttribute(Hit<OntologyTermHit> hit,
			List<OntologyTermQueryExpansion> ontologyTermQueryExpansions, String targetQueryTerm,
			String sourceAttributeDescription)
	{
		QueryExpansionSolution queryExpansionSolution = ontologyTermQueryExpansions.stream()
				.map(expansion -> expansion.getQueryExpansionSolution(hit)).sorted().findFirst().orElse(null);

		String matchedOntologyTermsInSource = hit.getResult().getJoinedSynonym();

		if (queryExpansionSolution == null) return Hit.create(matchedOntologyTermsInSource, hit.getScore());

		Set<String> matchedOntologyTermsInTarget = findOntologyTermSynonymsInTarget(
				queryExpansionSolution.getMatchOntologyTerms(), targetQueryTerm);

		Set<String> unmatchedWordsInSource = findLeftUnmatchedWords(sourceAttributeDescription,
				matchedOntologyTermsInSource);

		String transformedSourceDescription = termJoiner
				.join(Sets.union(matchedOntologyTermsInTarget, unmatchedWordsInSource));

		float adjustedScore = (float) stringMatching(transformedSourceDescription, targetQueryTerm, false) / 100;

		Set<String> additionalMatchedWords = findMatchedWords(targetQueryTerm, termJoiner.join(unmatchedWordsInSource));

		if (additionalMatchedWords.size() > 0)
		{
			matchedOntologyTermsInSource += ' ' + termJoiner.join(additionalMatchedWords);
		}

		return Hit.create(matchedOntologyTermsInSource, adjustedScore);
	}

	private Set<String> findLeftUnmatchedWords(String stringOne, String stringTwo)
	{
		Set<String> additionalMatchedWords = new LinkedHashSet<>();
		Set<String> stemmedStringOneWords = splitAndStem(stringTwo);
		for (String sourceWord : semanticSearchServiceUtils.splitIntoTerms(stringOne))
		{
			String stemmedSourceWord = Stemmer.stem(sourceWord);
			if (!stemmedStringOneWords.contains(stemmedSourceWord))
			{
				additionalMatchedWords.add(sourceWord);
			}
		}
		return additionalMatchedWords;
	}

	private Set<String> findMatchedWords(String string1, String string2)
	{
		Set<String> intersectedWords = new LinkedHashSet<>();
		Set<String> stemmedWordsFromString2 = splitAndStem(string2);
		for (String wordFromString1 : semanticSearchServiceUtils.splitIntoTerms(string1))
		{
			String stemmedSourceWord = Stemmer.stem(wordFromString1);
			if (stemmedWordsFromString2.contains(stemmedSourceWord))
			{
				intersectedWords.add(wordFromString1);
			}
		}
		return intersectedWords;
	}

	private Set<String> findOntologyTermSynonymsInTarget(List<OntologyTerm> ontologyTerms, String targetQueryTerm)
	{
		Set<String> targetQueryTermWords = splitAndStem(targetQueryTerm);
		Set<String> usedOntologyTermQueries = new LinkedHashSet<>();

		for (OntologyTerm ontologyTerm : ontologyTerms)
		{
			for (String synonym : semanticSearchServiceUtils.getLowerCaseTermsFromOntologyTerm(ontologyTerm))
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

	private List<String> getMatchedWords(String bestMatchingQuery, Set<String> queriesFromSourceAttribute)
	{
		List<String> matchedWords = new ArrayList<>();
		Set<String> bestMatchingQueryTokens = splitAndStem(bestMatchingQuery);
		for (String queryFromSourceAttribute : queriesFromSourceAttribute)
		{
			List<String> collect = semanticSearchServiceUtils.splitIntoTerms(queryFromSourceAttribute).stream()
					.filter(word -> bestMatchingQueryTokens.contains(Stemmer.stem(word))).collect(Collectors.toList());

			if (matchedWords.size() < collect.size())
			{
				matchedWords = collect;
			}
		}
		return matchedWords;
	}

	Hit<String> findBestQueryTerm(Set<String> queriesFromTargetAttribute, Set<String> queriesFromSourceAttribute)
	{
		double highestScore = 0;
		String bestTargetQuery = null;

		for (String targetQuery : queriesFromTargetAttribute)
		{
			for (String sourceQuery : queriesFromSourceAttribute)
			{
				double score = stringMatching(targetQuery, sourceQuery);
				if (bestTargetQuery == null || score > highestScore)
				{
					bestTargetQuery = targetQuery;
					highestScore = score;
				}
			}
		}
		return Hit.<String> create(bestTargetQuery, (float) highestScore / 100);
	}
}