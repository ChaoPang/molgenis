package org.molgenis.data.semanticsearch.explain.service.impl;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString.create;
import static org.molgenis.data.semanticsearch.semantic.Hit.create;
import static org.molgenis.ontology.core.model.OntologyTerm.create;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;
import static org.molgenis.ontology.utils.Stemmer.splitAndStem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.elasticsearch.common.base.Joiner;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceHelper;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.Objects.requireNonNull;

public class AttributeMappingExplainServiceImpl implements AttributeMappingExplainService
{
	private final SemanticSearchService semanticSearchService;
	private final OntologyService ontologyService;
	private final SemanticSearchServiceHelper semanticSearchServiceHelper;
	private final Joiner termJoiner = Joiner.on(' ');
	private final static float HIGH_QUALITY_THRESHOLD = 0.7f;
	private final static OntologyTermHit EMPTY_ONTOLOGYTERM_HIT = OntologyTermHit.create(create(EMPTY, EMPTY), EMPTY);

	@Autowired
	public AttributeMappingExplainServiceImpl(SemanticSearchService semanticSearchService,
			OntologyService ontologyService, SemanticSearchServiceHelper semanticSearchServiceHelper)
	{
		this.semanticSearchService = requireNonNull(semanticSearchService);
		this.ontologyService = requireNonNull(ontologyService);
		this.semanticSearchServiceHelper = requireNonNull(semanticSearchServiceHelper);
	}

	@Override
	public ExplainedAttributeMetaData explainAttributeMapping(AttributeMetaData targetAttribute,
			AttributeMetaData matchedSourceAttribute, EntityMetaData targetEntityMetaData,
			EntityMetaData sourceEntityMetaData)
	{
		return explainAttributeMapping(Collections.emptySet(), targetAttribute, matchedSourceAttribute,
				targetEntityMetaData, sourceEntityMetaData);
	}

	@Override
	public ExplainedAttributeMetaData explainAttributeMapping(Set<String> userQueries,
			AttributeMetaData targetAttribute, AttributeMetaData matchedSourceAttribute,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData)
	{
		// Collect all terms from the target attribute
		Set<String> queriesFromTargetAttribute = semanticSearchServiceHelper
				.createLexicalSearchQueryTerms(targetAttribute, userQueries);

		// Collect all terms from the source attribute
		Set<String> queriesFromSourceAttribute = semanticSearchServiceHelper
				.createLexicalSearchQueryTerms(matchedSourceAttribute, null);

		// Compute the pairwise lexical similarities between the two sets of query terms and find the best matching
		// target query that yields the highest similarity score.
		Hit<String> targetQueryTermHit = findBestQueryTerm(queriesFromTargetAttribute, queriesFromSourceAttribute);

		// If the similariy score is high enough by using query terms from the source attribute, we don't explain it
		// using the ontology terms because collecting all relevant ontology terms is very expensive
		if (targetQueryTermHit.getScore() >= HIGH_QUALITY_THRESHOLD)
		{
			// We get the matched words for the source attribute.
			Set<String> matchedWords = getMatchedWords(targetQueryTermHit.getResult(), queriesFromSourceAttribute);
			ExplainedQueryString explainedQueryString = create(termJoiner.join(matchedWords),
					targetQueryTermHit.getResult(), targetQueryTermHit.getResult(), targetQueryTermHit.getScore());
			return ExplainedAttributeMetaData.create(matchedSourceAttribute, explainedQueryString, true);
		}

		// Collect all the ontology terms that are associated with the target attribute, which were used in query
		// expansion for finding the relevant source attributes.
		List<OntologyTerm> findOntologyTermsForAttr = semanticSearchService.findOntologyTermsForAttr(targetAttribute,
				targetEntityMetaData, userQueries);

		List<OntologyTerm> relevantOntologyTerms = getExpandedOntologyTerms(findOntologyTermsForAttr);

		// Unfortunately the ElasticSearch built-in explain-api doesn't scale up. In order to explain why the source
		// attribute was matched. Now we take the source attribute as the query and filter/find the best combination
		// of ontology terms from the relevantOntologyTerms that are associated with the target attribute. By doing
		// this, we can deduce which ontology terms were used as the expanded queries for finding that particular source
		// attribute.
		Hit<OntologyTermHit> ontologyTermHit = semanticSearchService
				.filterTagsForAttr(matchedSourceAttribute, ontologyService.getAllOntologiesIds(), relevantOntologyTerms)
				.stream().findFirst().orElse(create(EMPTY_ONTOLOGYTERM_HIT, (float) 0));

		// Here we check if the source attribute is matched with the original target queries or the expanded
		// ontology
		// term queries.
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
		Set<String> matchedWords = getMatchedWords(bestMatchingQuery, queriesFromSourceAttribute);

		ExplainedQueryString explainedQueryString = create(termJoiner.join(matchedWords), bestMatchingQuery,
				queryOrigin, score);

		return ExplainedAttributeMetaData.create(matchedSourceAttribute, explainedQueryString, isHighQuality);
	}

	Set<String> getMatchedWords(String bestMatchingQuery, Set<String> queriesFromSourceAttribute)
	{
		Set<String> matchedWords = new HashSet<>();
		for (String queryFromSourceAttribute : queriesFromSourceAttribute)
		{
			Set<String> labelTokens = splitAndStem(queryFromSourceAttribute);
			Set<String> bestMatchingQueryTokens = splitAndStem(bestMatchingQuery);
			labelTokens.retainAll(bestMatchingQueryTokens);
			if (matchedWords.size() < labelTokens.size())
			{
				matchedWords = labelTokens;
			}
		}
		return matchedWords;
	}

	List<OntologyTerm> getExpandedOntologyTerms(List<OntologyTerm> ontologyTerms)
	{
		List<OntologyTerm> expandedOntologyTerms = new ArrayList<>();
		for (OntologyTerm ontologyTerm : ontologyTerms)
		{
			List<OntologyTerm> atomicOntologyTerms = ontologyService.getAtomicOntologyTerms(ontologyTerm);
			expandedOntologyTerms.addAll(atomicOntologyTerms);
			for (OntologyTerm atomicOntologyTerm : atomicOntologyTerms)
			{
				expandedOntologyTerms.addAll(ontologyService.getLevelThreeChildren(atomicOntologyTerm));
			}
		}
		return expandedOntologyTerms;
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