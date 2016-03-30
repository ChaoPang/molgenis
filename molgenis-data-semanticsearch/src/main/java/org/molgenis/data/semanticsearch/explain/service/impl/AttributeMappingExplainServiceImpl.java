package org.molgenis.data.semanticsearch.explain.service.impl;

import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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
import org.molgenis.ontology.utils.Stemmer;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import static java.util.Objects.requireNonNull;

public class AttributeMappingExplainServiceImpl implements AttributeMappingExplainService
{
	private final SemanticSearchService semanticSearchService;
	private final OntologyService ontologyService;
	private final SemanticSearchServiceHelper semanticSearchServiceHelper;
	private final Joiner termJoiner = Joiner.on(' ');
	private final static double HIGH_QUALITY_THRESHOLD = 70;
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

		float score = (targetQueryTermHit.getScore() > ontologyTermHit.getScore() ? targetQueryTermHit.getScore()
				: ontologyTermHit.getScore()) * 100;

		boolean isHighQuality = score >= HIGH_QUALITY_THRESHOLD;

		Set<ExplainedQueryString> explainedQueryStrings = new HashSet<>();

		for (String queryFromSourceAttribute : queriesFromSourceAttribute)
		{
			Set<String> labelTokens = Stemmer.splitAndStem(queryFromSourceAttribute);
			Set<String> bestMatchingQueryTokens = Stemmer.splitAndStem(bestMatchingQuery);
			labelTokens.retainAll(bestMatchingQueryTokens);
			explainedQueryStrings.add(
					ExplainedQueryString.create(termJoiner.join(labelTokens), bestMatchingQuery, queryOrigin, score));
			break;
		}

		return ExplainedAttributeMetaData.create(matchedSourceAttribute, explainedQueryStrings, isHighQuality);
	}

	List<OntologyTerm> getExpandedOntologyTerms(List<OntologyTerm> ontologyTerms)
	{
		List<OntologyTerm> expandedOntologyTerms = Lists.newArrayList(ontologyTerms);
		for (OntologyTerm ot : ontologyTerms)
		{
			ontologyService.getAtomicOntologyTerms(ot).forEach(atomicOntologyTerm -> expandedOntologyTerms
					.addAll(ontologyService.getChildren(atomicOntologyTerm)));
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
				if (score > highestScore || bestTargetQuery == null)
				{
					bestTargetQuery = targetQuery;
					highestScore = score;
				}
			}
		}
		return Hit.<String> create(bestTargetQuery, (float) highestScore / 100);
	}
}