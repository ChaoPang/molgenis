package org.molgenis.data.mapper.service.impl;

import static java.util.stream.StreamSupport.stream;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.mapper.bean.AttributeLink;
import org.molgenis.data.mapper.bean.AttributeNode;
import org.molgenis.data.mapper.data.request.AttributeConnectionResponse;
import org.molgenis.data.mapper.mapping.model.AttributeMapping;
import org.molgenis.data.mapper.mapping.model.EntityMapping;
import org.molgenis.data.mapper.mapping.model.MappingProject;
import org.molgenis.data.mapper.mapping.model.MappingTarget;
import org.molgenis.data.mapper.service.MappingNetworkService;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.security.core.runas.RunAsSystem;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.Objects.requireNonNull;

public class MappingNetworkServiceImpl implements MappingNetworkService
{
	private final SemanticSearchService semanticSearchService;
	private final AttributeMappingExplainService attributeMappingExplainService;

	@Autowired
	public MappingNetworkServiceImpl(SemanticSearchService semanticSearchService,
			AttributeMappingExplainService attributeMappingExplainService)
	{
		this.semanticSearchService = requireNonNull(semanticSearchService);
		this.attributeMappingExplainService = requireNonNull(attributeMappingExplainService);
	}

	// @RunAsSystem
	// public AttributeConnectionResponse createDataSetConnections(List<MappingProject> mappingProjects)
	// {
	// List<EntityMapping> entityMappings = new ArrayList<>();
	//
	// for (MappingProject mappingProject : mappingProjects)
	// {
	// mappingProject.getMappingTargets().stream().map(MappingTarget::getEntityMappings)
	// .forEach(mappings -> entityMappings.addAll(mappings));
	// }
	//
	// List<EntityMetaData> entityMetaDatas = collectInvolvedEntityMetaDatas(entityMappings);
	// }

	@RunAsSystem
	public AttributeConnectionResponse createConnections(List<MappingProject> mappingProjects)
	{
		List<EntityMapping> entityMappings = new ArrayList<>();

		for (MappingProject mappingProject : mappingProjects)
		{
			mappingProject.getMappingTargets().stream().map(MappingTarget::getEntityMappings)
					.forEach(mappings -> entityMappings.addAll(mappings));
		}

		List<EntityMetaData> entityMetaDatas = collectInvolvedEntityMetaDatas(entityMappings);

		Map<String, Integer> attributeIdentifierMap = createAttributeIdentifierMap(entityMetaDatas);

		List<AttributeNode> attributeNodes = new ArrayList<>();

		List<AttributeLink> attributeLinks = new ArrayList<>();

		entityMetaDatas.stream().forEach(eemd -> attributeNodes
				.addAll(createGlobalNodes(eemd, entityMetaDatas.indexOf(eemd) + 1, attributeIdentifierMap)));

		entityMappings.stream().forEach(
				entityMapping -> attributeLinks.add(createDataSetLinks(entityMapping, attributeIdentifierMap)));
		entityMappings.stream()
				.forEach(mapping -> attributeLinks.addAll(createGlobalLinks(mapping, attributeIdentifierMap)));
		entityMetaDatas.stream().forEach(eemd -> attributeLinks.addAll(createLocalLinks(eemd, attributeIdentifierMap)));

		return AttributeConnectionResponse.create(attributeNodes, attributeLinks);
	}

	Map<String, Integer> createAttributeIdentifierMap(List<EntityMetaData> entityMetaDatas)
	{
		Map<String, Integer> attributeMap = new LinkedHashMap<>();
		AtomicInteger counter = new AtomicInteger(0);
		for (EntityMetaData entityMetaData : entityMetaDatas)
		{
			attributeMap.put(entityMetaData.getName(), counter.getAndIncrement());
			stream(entityMetaData.getAtomicAttributes().spliterator(), false).forEach(attribute -> attributeMap
					.put(createArtificialAttributeIdentifier(entityMetaData, attribute), counter.getAndIncrement()));
		}
		return attributeMap;
	}

	List<AttributeNode> createGlobalNodes(EntityMetaData entityMetaData, int groupNumber,
			Map<String, Integer> attributeIdentifierMap)
	{
		List<AttributeNode> attributeNodes = new ArrayList<>();

		attributeNodes.add(AttributeNode.create(entityMetaData.getName(), entityMetaData.getLabel(), groupNumber));
		attributeNodes.addAll(stream(entityMetaData.getAtomicAttributes().spliterator(), false)
				.filter(attr -> isAttributeInvolved(attr, entityMetaData, attributeIdentifierMap))
				.map(attr -> AttributeNode.create(attr.getName(), attr.getLabel(), groupNumber))
				.collect(Collectors.toList()));

		return attributeNodes;
	}

	AttributeLink createDataSetLinks(EntityMapping entityMapping, Map<String, Integer> attributeIdentifierMap)
	{
		EntityMetaData sourceEntityMetaData = entityMapping.getSourceEntityMetaData();
		EntityMetaData targetEntityMetaData = entityMapping.getTargetEntityMetaData();

		return AttributeLink.create(attributeIdentifierMap.get(sourceEntityMetaData.getName()),
				attributeIdentifierMap.get(targetEntityMetaData.getName()), 5);
	}

	List<AttributeLink> createGlobalLinks(EntityMapping entityMapping, Map<String, Integer> attributeIdentifierMap)
	{
		List<AttributeLink> attributeLinks = new ArrayList<>();

		EntityMetaData targetEntityMetaData = entityMapping.getTargetEntityMetaData();
		EntityMetaData sourceEntityMetaData = entityMapping.getSourceEntityMetaData();

		for (AttributeMapping attributeMapping : entityMapping.getAttributeMappings())
		{
			String targetArtificialIdentifier = createArtificialAttributeIdentifier(targetEntityMetaData,
					attributeMapping.getTargetAttributeMetaData());
			Integer target = attributeIdentifierMap.get(targetArtificialIdentifier);
			for (AttributeMetaData sourceAttribute : attributeMapping.getSourceAttributeMetaDatas())
			{
				String sourceArtificialIdentifier = createArtificialAttributeIdentifier(sourceEntityMetaData,
						sourceAttribute);
				Integer source = attributeIdentifierMap.get(sourceArtificialIdentifier);
				Integer value = Math.round((float) attributeMapping.getSimilarity() * 10);
				attributeLinks.add(AttributeLink.create(source, target, value));
			}
		}

		return attributeLinks;
	}

	List<AttributeLink> createLocalLinks(EntityMetaData entityMetaData, Map<String, Integer> attributeIdentifierMap)
	{
		List<AttributeLink> attributeLinks = new ArrayList<>();

		// List<String> classifiedAttributeIdentifiers = new ArrayList<>();

		for (AttributeMetaData targetAttribute : entityMetaData.getAtomicAttributes())
		{
			String targetArtificialIdentifier = createArtificialAttributeIdentifier(entityMetaData, targetAttribute);

			attributeLinks.add(AttributeLink.create(attributeIdentifierMap.get(targetArtificialIdentifier),
					attributeIdentifierMap.get(entityMetaData.getName()), 5));

			// if (!classifiedAttributeIdentifiers.contains(targetAttribute.getName()))
			// {
			// List<AttributeMetaData> findAttributes = semanticSearchService
			// .findAttributesByLexicalSearch(targetAttribute, entityMetaData, null).stream()
			// .filter(attr -> !classifiedAttributeIdentifiers.contains(attr.getName())).collect(toList());
			//
			// Integer target = attributeIdentifierMap.get(targetArtificialIdentifier);
			// for (AttributeMetaData sourceAttribute : findAttributes)
			// {
			// if (sourceAttribute.getName() != targetAttribute.getName())
			// {
			// String sourceArtificialIdentifier = createArtificialAttributeIdentifier(entityMetaData,
			// sourceAttribute);
			// Integer source = attributeIdentifierMap.get(sourceArtificialIdentifier);
			// attributeLinks.add(AttributeLink.create(source, target, 5));
			// }
			// }
			//
			// classifiedAttributeIdentifiers
			// .addAll(findAttributes.stream().map(AttributeMetaData::getName).collect(toList()));
			// }
		}

		return attributeLinks;
	}

	private List<EntityMetaData> collectInvolvedEntityMetaDatas(List<EntityMapping> entityMappings)
	{
		List<EntityMetaData> entityMetaDatas = new ArrayList<>();
		for (EntityMapping entityMapping : entityMappings)
		{
			EntityMetaData targetEntityMetaData = entityMapping.getTargetEntityMetaData();
			EntityMetaData sourceEntityMetaData = entityMapping.getSourceEntityMetaData();

			if (!entityMetaDatas.contains(targetEntityMetaData))
			{
				entityMetaDatas.add(targetEntityMetaData);
			}
			if (!entityMetaDatas.contains(sourceEntityMetaData))
			{
				entityMetaDatas.add(sourceEntityMetaData);
			}
		}
		return entityMetaDatas;
	}

	private boolean isAttributeInvolved(AttributeMetaData attributeMetaData, EntityMetaData entityMetaData,
			Map<String, Integer> attributeIdentifierMap)
	{
		String artificialIdentifier = createArtificialAttributeIdentifier(entityMetaData, attributeMetaData);
		return attributeIdentifierMap.containsKey(artificialIdentifier);
	}

	private String createArtificialAttributeIdentifier(EntityMetaData entityMetaData,
			AttributeMetaData attributeMetaData)
	{
		return entityMetaData.getName() + "_" + attributeMetaData.getName();
	}
}
