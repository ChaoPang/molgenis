package org.molgenis.data.semanticsearch.service.impl;

import static java.util.Collections.emptyList;
import static org.molgenis.data.semanticsearch.semantic.Hit.create;
import static org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceUtils.UNIT_ONTOLOGY_IRI;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.common.collect.Lists;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.Objects.requireNonNull;

public class SemanticSearchServiceImpl implements SemanticSearchService
{
	private final DataService dataService;
	private final OntologyService ontologyService;
	private final SemanticSearchServiceUtils semanticSearchServiceUtils;
	private final AttributeMappingExplainService attributeMappingExplainService;

	private static final int MAX_NUMBER_ATTRIBTUES = 100;

	@Autowired
	public SemanticSearchServiceImpl(DataService dataService, OntologyService ontologyService,
			SemanticSearchServiceUtils semanticSearchServiceUtils,
			AttributeMappingExplainService attributeMappingExplainService)
	{
		this.dataService = requireNonNull(dataService);
		this.ontologyService = requireNonNull(ontologyService);
		this.semanticSearchServiceUtils = requireNonNull(semanticSearchServiceUtils);
		this.attributeMappingExplainService = requireNonNull(attributeMappingExplainService);
	}

	@Override
	public List<AttributeMetaData> findAttributesLazy(AttributeMetaData targetAttribute,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData, Set<String> searchTerms)
	{
		Set<String> queryTerms = semanticSearchServiceUtils.getQueryTermsFromAttribute(targetAttribute, searchTerms);

		// Find attributes by the query terms collected from the target attribute
		List<AttributeMetaData> matchedSourceAttributes = findAttributes(queryTerms, emptyList(), sourceEntityMetaData,
				false);

		if (matchedSourceAttributes.size() > 0)
		{
			ExplainedAttributeMetaData explainByAttribute = attributeMappingExplainService
					.explainByAttribute(searchTerms, targetAttribute, matchedSourceAttributes.get(0));

			// If the matched attribute found by query terms from the target attribute is not good enough, we try to use
			// the ontology term synonyms to improve the matching result.
			if (!explainByAttribute.isHighQuality())
			{
				matchedSourceAttributes = findAttributeByOntologyTerms(targetAttribute, targetEntityMetaData,
						sourceEntityMetaData, searchTerms, false);

				if (matchedSourceAttributes.size() > 0)
				{
					explainByAttribute = attributeMappingExplainService.explainBySynonyms(searchTerms, targetAttribute,
							matchedSourceAttributes.get(0), targetEntityMetaData);

					// if the matched attribute found by the query terms from the target attribute as well as the
					// ontology term synonyms is not good enough, we try to use the full set of query terms from the
					// ontology terms to improve the matchign result.
					if (!explainByAttribute.isHighQuality())
					{
						matchedSourceAttributes = findAttributeByOntologyTerms(targetAttribute, targetEntityMetaData,
								sourceEntityMetaData, searchTerms, true);
					}
				}
			}
		}
		return matchedSourceAttributes;
	}

	@Override
	public List<AttributeMetaData> findAttributes(AttributeMetaData targetAttribute,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData, Set<String> searchTerms)
	{
		return findAttributeByOntologyTerms(targetAttribute, targetEntityMetaData, sourceEntityMetaData, searchTerms,
				true);
	}

	private List<AttributeMetaData> findAttributeByOntologyTerms(AttributeMetaData targetAttribute,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData, Set<String> searchTerms,
			boolean expand)
	{
		Set<String> queryTerms = semanticSearchServiceUtils.getQueryTermsFromAttribute(targetAttribute, searchTerms);

		List<String> ontologyTermIds = ontologyService.getOntologies().stream()
				.filter(ontology -> !ontology.getIRI().equals(UNIT_ONTOLOGY_IRI)).map(Ontology::getId)
				.collect(Collectors.toList());
		List<OntologyTerm> ontologyTerms = semanticSearchServiceUtils.findOntologyTermsForAttr(targetAttribute,
				targetEntityMetaData, searchTerms, ontologyTermIds);

		return findAttributes(queryTerms, ontologyTerms, sourceEntityMetaData, expand);
	}

	private List<AttributeMetaData> findAttributes(Set<String> queryTerms, List<OntologyTerm> ontologyTerms,
			EntityMetaData sourceEntityMetaData, boolean expand)
	{
		QueryRule disMaxQueryRule = semanticSearchServiceUtils.createDisMaxQueryRule(queryTerms, ontologyTerms, expand);

		Iterable<String> attributeIdentifiers = semanticSearchServiceUtils
				.getAttributeIdentifiers(sourceEntityMetaData);

		List<QueryRule> finalQueryRules = Lists
				.newArrayList(new QueryRule(AttributeMetaDataMetaData.IDENTIFIER, Operator.IN, attributeIdentifiers));

		if (disMaxQueryRule.getNestedRules().size() > 0)
		{
			finalQueryRules.addAll(Arrays.asList(new QueryRule(Operator.AND), disMaxQueryRule));
		}

		Stream<Entity> attributeMetaDataEntities = dataService.findAll(AttributeMetaDataMetaData.ENTITY_NAME,
				new QueryImpl(finalQueryRules).pageSize(MAX_NUMBER_ATTRIBTUES));

		List<AttributeMetaData> attributes = attributeMetaDataEntities
				.map(entity -> semanticSearchServiceUtils.entityToAttributeMetaData(entity, sourceEntityMetaData))
				.collect(Collectors.toList());

		return attributes;
	}

	@Override
	public Map<AttributeMetaData, Hit<OntologyTerm>> findTagsForEntity(String entity, List<String> ontologyIds)
	{
		Map<AttributeMetaData, Hit<OntologyTerm>> result = new LinkedHashMap<AttributeMetaData, Hit<OntologyTerm>>();
		EntityMetaData emd = dataService.getEntityMetaData(entity);
		for (AttributeMetaData amd : emd.getAtomicAttributes())
		{
			Hit<OntologyTerm> tag = findTagForAttr(amd, ontologyIds);
			if (tag != null)
			{
				result.put(amd, tag);
			}
		}
		return result;
	}

	@Override
	public Hit<OntologyTerm> findTagForAttr(AttributeMetaData attribute, List<String> ontologyIds)
	{
		List<Hit<OntologyTermHit>> ontologyTermHits = findAllTagsForAttr(attribute, ontologyIds);
		return ontologyTermHits.stream().findFirst()
				.map(otHit -> create(otHit.getResult().getOntologyTerm(), otHit.getScore())).orElse(null);
	}

	@Override
	public Hit<OntologyTerm> findTag(String description, List<String> ontologyIds)
	{
		List<Hit<OntologyTermHit>> ontologyTermHits = findAllTags(description, ontologyIds);
		return ontologyTermHits.stream().findFirst()
				.map(otHit -> create(otHit.getResult().getOntologyTerm(), otHit.getScore())).orElse(null);
	}

	@Override
	public List<Hit<OntologyTermHit>> findAllTagsForAttr(AttributeMetaData attribute, List<String> ontologyIds)
	{
		String description = attribute.getDescription() == null ? attribute.getLabel() : attribute.getDescription();
		return findAllTags(description, ontologyIds);
	}

	@Override
	public List<Hit<OntologyTermHit>> findAllTags(String description, List<String> ontologyIds)
	{
		Set<String> searchTerms = semanticSearchServiceUtils.splitIntoTerms(description);
		return semanticSearchServiceUtils.findOntologyTermCombination(searchTerms, ontologyIds);
	}
}
