package org.molgenis.data.semanticsearch.service.impl;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.elasticsearch.common.collect.Lists.newArrayList;
import static org.molgenis.data.QueryRule.Operator.IN;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.ENTITY_NAME;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.IDENTIFIER;
import static org.molgenis.data.semanticsearch.semantic.Hit.create;
import static org.molgenis.data.semanticsearch.utils.AttributeToMapUtil.explainedAttrToAttributeMetaData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisDataAccessException;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParameter;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParameter;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils;
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
	private final TagGroupGenerator tagGroupGenerator;
	private final QueryExpansionService queryExpansionService;
	private final AttributeMappingExplainService attributeMappingExplainService;

	public static final int MAX_NUMBER_ATTRIBTUES = 50;
	public static final int NUMBER_OF_EXPLAINED_ATTRS = 5;
	public static final String UNIT_ONTOLOGY_IRI = "http://purl.obolibrary.org/obo/uo.owl";

	@Autowired
	public SemanticSearchServiceImpl(DataService dataService, OntologyService ontologyService,
			TagGroupGenerator tagGroupGenerator, QueryExpansionService queryExpansionService,
			AttributeMappingExplainService attributeMappingExplainService)
	{
		this.dataService = requireNonNull(dataService);
		this.ontologyService = requireNonNull(ontologyService);
		this.tagGroupGenerator = requireNonNull(tagGroupGenerator);
		this.queryExpansionService = requireNonNull(queryExpansionService);
		this.attributeMappingExplainService = requireNonNull(attributeMappingExplainService);
	}

	@Override
	public List<AttributeMetaData> findAttributesLazy(SemanticSearchParameter semanticSearchParameters)
	{
		if (semanticSearchParameters.getSourceEntityMetaDatas().size() == 0) return Collections.emptyList();

		List<ExplainedAttributeMetaData> findAttributesLazyWithExplanations = findAttributesLazyWithExplanations(
				semanticSearchParameters);

		EntityMetaData sourceEntityMetaData = semanticSearchParameters.getSourceEntityMetaDatas().get(0);

		return findAttributesLazyWithExplanations.stream()
				.map(attr -> explainedAttrToAttributeMetaData(attr, sourceEntityMetaData)).collect(toList());
	}

	@Override
	public List<ExplainedAttributeMetaData> findAttributesLazyWithExplanations(
			SemanticSearchParameter semanticSearchParameters)
	{
		semanticSearchParameters = SemanticSearchParameter.create(semanticSearchParameters, false, false);

		// Find attributes by the query terms collected from the target attribute
		List<AttributeMetaData> matchedSourceAttributes = findAttributes(semanticSearchParameters);

		// If the matched attribute found by query terms from the target attribute is not good enough, we try to use
		// the ontology term synonyms to improve the matching result.
		if (isCurrentMatchBadQuality(semanticSearchParameters, matchedSourceAttributes))
		{
			// enable semantic search
			semanticSearchParameters = SemanticSearchParameter.create(semanticSearchParameters, true, false);
			matchedSourceAttributes = findAttributes(semanticSearchParameters);

			// if the matched attribute found by the query terms from the target attribute as well as the
			// ontology term synonyms is not good enough, we try to use the full set of query terms from the
			// ontology terms to improve the matchign result.
			if (isCurrentMatchBadQuality(semanticSearchParameters, matchedSourceAttributes))
			{
				// enable query expansion for child ontology terms
				semanticSearchParameters = SemanticSearchParameter.create(semanticSearchParameters, true, true);
				matchedSourceAttributes = findAttributes(semanticSearchParameters);
			}
		}

		return convertMatchedAttributesToExplainAttributes(semanticSearchParameters, matchedSourceAttributes);
	}

	@Override
	public List<ExplainedAttributeMetaData> findAttributesWithExplanation(
			SemanticSearchParameter semanticSearchParameters)
	{
		List<AttributeMetaData> findAttributes = findAttributes(semanticSearchParameters);
		return convertMatchedAttributesToExplainAttributes(semanticSearchParameters, findAttributes);
	}

	@Override
	public List<AttributeMetaData> findAttributes(SemanticSearchParameter semanticSearchParameters)
	{
		if (semanticSearchParameters.getSourceEntityMetaDatas().isEmpty()) return Collections.emptyList();

		Map<EntityMetaData, List<AttributeMetaData>> findMultiEntityAttributes = findMultiEntityAttributes(
				semanticSearchParameters);

		EntityMetaData entityMetaData = semanticSearchParameters.getSourceEntityMetaDatas().get(0);

		return findMultiEntityAttributes.get(entityMetaData);
	}

	@Override
	public Map<EntityMetaData, List<AttributeMetaData>> findMultiEntityAttributes(
			SemanticSearchParameter semanticSearchParameters)
	{
		AttributeMetaData targetAttribute = semanticSearchParameters.getTargetAttribute();
		Set<String> userQueries = semanticSearchParameters.getUserQueries();
		EntityMetaData targetEntityMetaData = semanticSearchParameters.getTargetEntityMetaData();
		List<EntityMetaData> sourceEntityMetaDatas = semanticSearchParameters.getSourceEntityMetaDatas();
		QueryExpansionParameter ontologyExpansionParameters = semanticSearchParameters.getExpansionParameter();
		boolean semanticSearchEnabled = ontologyExpansionParameters.isSemanticSearchEnabled();

		Set<String> queryTerms = SemanticSearchServiceUtils.getQueryTermsFromAttribute(targetAttribute, userQueries);

		List<TagGroup> ontologyTermHits;
		if (semanticSearchEnabled)
		{
			List<String> ontologyIds = ontologyService.getOntologies().stream()
					.filter(ontology -> !ontology.getIRI().equals(UNIT_ONTOLOGY_IRI)).map(Ontology::getId)
					.collect(Collectors.toList());
			ontologyTermHits = tagGroupGenerator.findTagGroups(targetAttribute, targetEntityMetaData, userQueries,
					ontologyIds);
		}
		else ontologyTermHits = emptyList();

		return findAttributes(queryTerms, ontologyTermHits, ontologyExpansionParameters, sourceEntityMetaDatas);
	}

	@Override
	public Map<EntityMetaData, List<AttributeMetaData>> findAttributes(Set<String> queryTerms,
			List<TagGroup> ontologyTermHits, QueryExpansionParameter ontologyExpansionParameters,
			List<EntityMetaData> sourceEntityMetaDatas)
	{
		QueryRule disMaxQueryRule = queryExpansionService.expand(queryTerms, ontologyTermHits,
				ontologyExpansionParameters);

		if (disMaxQueryRule != null)
		{
			Map<EntityMetaData, List<AttributeMetaData>> matchedAttributes = new LinkedHashMap<>();

			for (EntityMetaData sourceEntityMetaData : sourceEntityMetaDatas)
			{
				Iterable<String> attributeIdentifiers = getAttributeIdentifiers(sourceEntityMetaData);

				List<QueryRule> finalQueryRules = newArrayList(new QueryRule(IDENTIFIER, IN, attributeIdentifiers));

				if (disMaxQueryRule.getNestedRules().size() > 0)
				{
					finalQueryRules.addAll(Arrays.asList(new QueryRule(Operator.AND), disMaxQueryRule));
				}

				Stream<Entity> attributeMetaDataEntities = dataService.findAll(ENTITY_NAME,
						new QueryImpl(finalQueryRules).pageSize(MAX_NUMBER_ATTRIBTUES));

				List<AttributeMetaData> attributes = attributeMetaDataEntities
						.map(entity -> entityToAttributeMetaData(entity, sourceEntityMetaData)).collect(toList());

				matchedAttributes.put(sourceEntityMetaData, attributes);
			}
			return matchedAttributes;
		}

		return Collections.emptyMap();
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
		List<TagGroup> ontologyTermHits = findAllTagsForAttr(attribute, ontologyIds);
		return ontologyTermHits.stream().findFirst().map(otHit -> create(otHit.getOntologyTerm(), otHit.getScore()))
				.orElse(null);
	}

	@Override
	public Hit<OntologyTerm> findTag(String description, List<String> ontologyIds)
	{
		List<TagGroup> ontologyTermHits = findAllTags(description, ontologyIds);
		return ontologyTermHits.stream().findFirst().map(otHit -> create(otHit.getOntologyTerm(), otHit.getScore()))
				.orElse(null);
	}

	@Override
	public List<TagGroup> findAllTagsForAttr(AttributeMetaData attribute, List<String> ontologyIds)
	{
		String description = attribute.getDescription() == null ? attribute.getLabel() : attribute.getDescription();
		return findAllTags(description, ontologyIds);
	}

	@Override
	public List<TagGroup> findAllTags(String description, List<String> ontologyIds)
	{
		return tagGroupGenerator.findTagGroups(description, ontologyIds);
	}

	private boolean isCurrentMatchBadQuality(SemanticSearchParameter semanticSearchParameters,
			List<AttributeMetaData> matchedSourceAttributes)
	{
		if (matchedSourceAttributes.size() == 0) return true;

		ExplainedAttributeMetaData explainAttributeMapping = attributeMappingExplainService
				.explainAttributeMapping(semanticSearchParameters, matchedSourceAttributes.get(0));

		return !explainAttributeMapping.isHighQuality();
	}

	private List<ExplainedAttributeMetaData> convertMatchedAttributesToExplainAttributes(
			SemanticSearchParameter semanticSearchParameters, List<AttributeMetaData> matchedSourceAttributes)
	{
		List<ExplainedAttributeMetaData> explainedAttributes = new ArrayList<>(matchedSourceAttributes.size());

		for (int i = 0; i < matchedSourceAttributes.size(); i++)
		{
			if (i >= NUMBER_OF_EXPLAINED_ATTRS) break;

			ExplainedAttributeMetaData explainAttributeMapping = attributeMappingExplainService
					.explainAttributeMapping(semanticSearchParameters, matchedSourceAttributes.get(i));

			explainedAttributes.add(explainAttributeMapping);
		}

		Collections.sort(explainedAttributes);

		if (matchedSourceAttributes.size() > NUMBER_OF_EXPLAINED_ATTRS)
		{
			explainedAttributes
					.addAll(matchedSourceAttributes.subList(NUMBER_OF_EXPLAINED_ATTRS, matchedSourceAttributes.size())
							.stream().map(ExplainedAttributeMetaData::create).collect(Collectors.toList()));
		}

		return explainedAttributes;
	}

	/**
	 * A helper function that gets identifiers of all the attributes from one entityMetaData
	 * 
	 * @param sourceEntityMetaData
	 * @return
	 */
	public List<String> getAttributeIdentifiers(EntityMetaData sourceEntityMetaData)
	{
		Entity entityMetaDataEntity = dataService.findOne(EntityMetaDataMetaData.ENTITY_NAME,
				new QueryImpl().eq(EntityMetaDataMetaData.FULL_NAME, sourceEntityMetaData.getName()));

		if (entityMetaDataEntity == null) throw new MolgenisDataAccessException(
				"Could not find EntityMetaDataEntity by the name of " + sourceEntityMetaData.getName());

		List<String> attributeIdentifiers = new ArrayList<String>();

		recursivelyCollectAttributeIdentifiers(entityMetaDataEntity.getEntities(EntityMetaDataMetaData.ATTRIBUTES),
				attributeIdentifiers);

		return attributeIdentifiers;
	}

	private void recursivelyCollectAttributeIdentifiers(Iterable<Entity> attributeEntities,
			List<String> attributeIdentifiers)
	{
		for (Entity attributeEntity : attributeEntities)
		{
			if (!attributeEntity.getString(AttributeMetaDataMetaData.DATA_TYPE)
					.equals(MolgenisFieldTypes.COMPOUND.toString()))
			{
				attributeIdentifiers.add(attributeEntity.getString(AttributeMetaDataMetaData.IDENTIFIER));
			}
			Iterable<Entity> entities = attributeEntity.getEntities(AttributeMetaDataMetaData.PARTS);

			if (entities != null)
			{
				recursivelyCollectAttributeIdentifiers(entities, attributeIdentifiers);
			}
		}
	}

	private AttributeMetaData entityToAttributeMetaData(Entity attributeEntity, EntityMetaData entityMetaData)
	{
		String attributeName = attributeEntity.getString(AttributeMetaDataMetaData.NAME);
		AttributeMetaData attribute = entityMetaData.getAttribute(attributeName);
		if (attribute == null)
		{
			throw new MolgenisDataAccessException("The attributeMetaData : " + attributeName
					+ " does not exsit in EntityMetaData : " + entityMetaData.getName());
		}
		return attribute;
	}

}