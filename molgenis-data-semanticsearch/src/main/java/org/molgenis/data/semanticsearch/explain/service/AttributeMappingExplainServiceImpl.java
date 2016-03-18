package org.molgenis.data.semanticsearch.explain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.base.Joiner;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceHelper;
import org.molgenis.data.semanticsearch.string.NGramDistanceAlgorithm;
import org.molgenis.data.semanticsearch.string.Stemmer;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import static java.util.Objects.requireNonNull;

public class AttributeMappingExplainServiceImpl implements AttributeMappingExplainService
{
	private final SemanticSearchService semanticSearchService;
	private final OntologyService ontologyService;
	private final SemanticSearchServiceHelper semanticSearchServiceHelper;
	private final Splitter termSplitter = Splitter.onPattern("[^\\p{IsAlphabetic}]+");
	private final Joiner termJoiner = Joiner.on(' ');
	private final static float HIGH_QUALITY_THRESHOLD = 70;
	private final static String COMMA_CHAR = ",";
	private final static OntologyTermHit EMPTY_ONTOLOGYTERMHIT = OntologyTermHit
			.create(OntologyTerm.create(StringUtils.EMPTY, StringUtils.EMPTY), StringUtils.EMPTY);

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
		Set<String> queriesFromTargetAttribute = semanticSearchServiceHelper
				.createLexicalSearchQueryTerms(targetAttribute, userQueries);
		Set<String> queriesFromSourceAttribute = semanticSearchServiceHelper
				.createLexicalSearchQueryTerms(matchedSourceAttribute, null);

		Hit<String> targetQueryTermHit = findBestQueryTerm(queriesFromTargetAttribute, queriesFromSourceAttribute);

		List<OntologyTerm> ontologyTerms = getExpandedOntologyTerms(
				semanticSearchService.getOntologyTermsForAttr(targetAttribute, targetEntityMetaData, userQueries));

		Hit<OntologyTermHit> ontologyTermHit = semanticSearchService.findAndFilterTags(matchedSourceAttribute,
				ontologyService.getAllOntologiesIds(), ontologyTerms);

		if (ontologyTermHit == null) ontologyTermHit = Hit.<OntologyTermHit> create(EMPTY_ONTOLOGYTERMHIT, (float) 0);

		String bestMatchingQuery = targetQueryTermHit.getScore() >= ontologyTermHit.getScore()
				? targetQueryTermHit.getResult() : ontologyTermHit.getResult().getMatchedSynonym();

		String queryOrigin = targetQueryTermHit.getScore() > ontologyTermHit.getScore() ? targetQueryTermHit.getResult()
				: ontologyTermHit.getResult().getOntologyTerm().getLabel();

		float score = targetQueryTermHit.getScore() > ontologyTermHit.getScore() ? targetQueryTermHit.getScore()
				: ontologyTermHit.getScore();

		Set<ExplainedQueryString> explainedQueryStrings = new HashSet<>();

		for (String queryFromSourceAttribute : queriesFromSourceAttribute)
		{
			Set<String> labelTokens = splitIntoTermsStem(queryFromSourceAttribute);
			Set<String> bestMatchingQueryTokens = splitIntoTermsStem(bestMatchingQuery);
			labelTokens.retainAll(bestMatchingQueryTokens);
			explainedQueryStrings.add(
					ExplainedQueryString.create(termJoiner.join(labelTokens), bestMatchingQuery, queryOrigin, score));
			break;
		}

		return ExplainedAttributeMetaData.create(matchedSourceAttribute, explainedQueryStrings,
				score >= HIGH_QUALITY_THRESHOLD);
	}

	List<OntologyTerm> getExpandedOntologyTerms(List<OntologyTerm> ontologyTerms)
	{
		List<OntologyTerm> expandedOntologyTerms = new ArrayList<>();

		for (OntologyTerm ot : ontologyTerms)
		{
			for (String ontologyTermIri : ot.getIRI().split(COMMA_CHAR))
			{
				OntologyTerm atomicOntologyTerm = ontologyService.getOntologyTerm(ontologyTermIri);
				expandedOntologyTerms.addAll(ontologyService.getChildren(atomicOntologyTerm));
				expandedOntologyTerms.add(atomicOntologyTerm);
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
				double score = NGramDistanceAlgorithm.stringMatching(targetQuery, sourceQuery);
				if (score > highestScore || bestTargetQuery == null)
				{
					bestTargetQuery = targetQuery;
					highestScore = score;
				}
			}
		}
		return Hit.<String> create(bestTargetQuery, (float) highestScore);
	}

	Set<String> splitIntoTermsStem(String string)
	{
		Stemmer stemmer = new Stemmer();
		return Sets.newHashSet(splitIntoTerms(string).stream().map(stemmer::stem).collect(Collectors.toSet()));
	}

	Set<String> splitIntoTerms(String string)
	{
		return Sets.newHashSet(termSplitter.split(string.toLowerCase()));
	}
}