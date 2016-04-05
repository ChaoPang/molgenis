package org.molgenis.data.semanticsearch.explain.service.impl;

import static org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString.create;
import static org.molgenis.data.semanticsearch.semantic.Hit.create;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;
import static org.molgenis.ontology.utils.Stemmer.splitAndStem;

import java.util.ArrayList;
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
import org.springframework.beans.factory.annotation.Autowired;

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
				semanticSearchService.findOntologyTermsForAttr(targetAttribute, targetEntityMetaData, userQueries));

		Hit<OntologyTermHit> ontologyTermHit = semanticSearchService
				.findAllTagsForAttribute(matchedSourceAttribute, ontologyService.getAllOntologiesIds(), ontologyTerms)
				.stream().findFirst().orElse(null);

		ontologyTermHit = ontologyTermHit == null ? create(EMPTY_ONTOLOGYTERMHIT, (float) 0) : ontologyTermHit;

		String bestMatchingQuery = targetQueryTermHit.getScore() >= ontologyTermHit.getScore()
				? targetQueryTermHit.getResult() : ontologyTermHit.getResult().getJoinedSynonym();

		String queryOrigin = targetQueryTermHit.getScore() > ontologyTermHit.getScore() ? targetQueryTermHit.getResult()
				: ontologyTermHit.getResult().getOntologyTerm().getLabel();

		float score = (targetQueryTermHit.getScore() > ontologyTermHit.getScore() ? targetQueryTermHit.getScore()
				: ontologyTermHit.getScore()) * 100;

		boolean isHighQuality = score >= HIGH_QUALITY_THRESHOLD;

		Set<ExplainedQueryString> explainedQueryStrings = new HashSet<>();

		for (String queryFromSourceAttribute : queriesFromSourceAttribute)
		{
			Set<String> labelTokens = splitAndStem(queryFromSourceAttribute);
			Set<String> bestMatchingQueryTokens = splitAndStem(bestMatchingQuery);
			labelTokens.retainAll(bestMatchingQueryTokens);
			explainedQueryStrings.add(create(termJoiner.join(labelTokens), bestMatchingQuery, queryOrigin, score));
			break;
		}

		return ExplainedAttributeMetaData.create(matchedSourceAttribute, explainedQueryStrings, isHighQuality);
	}

	List<OntologyTerm> getExpandedOntologyTerms(List<OntologyTerm> ontologyTerms)
	{
		List<OntologyTerm> expandedOntologyTerms = new ArrayList<>();
		for (OntologyTerm ot : ontologyTerms)
		{
			for (OntologyTerm atomicOntologyTerm : ontologyService.getAtomicOntologyTerms(ot))
			{
				expandedOntologyTerms.add(atomicOntologyTerm);
				expandedOntologyTerms.addAll(ontologyService.getChildren(atomicOntologyTerm));
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