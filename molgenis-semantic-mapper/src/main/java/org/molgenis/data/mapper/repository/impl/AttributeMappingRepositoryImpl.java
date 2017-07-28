package org.molgenis.data.mapper.repository.impl;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.mapper.mapping.model.AttributeMapping;
import org.molgenis.data.mapper.meta.AttributeMappingMetaData;
import org.molgenis.data.mapper.repository.AttributeMappingRepository;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.populate.IdGenerator;
import org.molgenis.data.support.DynamicEntity;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.mapper.meta.AttributeMappingMetaData.*;

public class AttributeMappingRepositoryImpl implements AttributeMappingRepository
{
	private final IdGenerator idGenerator;
	private final DataService dataService;
	private final AttributeMappingMetaData attributeMappingMetaData;

	@Autowired
	public AttributeMappingRepositoryImpl(DataService dataService, IdGenerator idGenerator,
			AttributeMappingMetaData attributeMappingMetaData)
	{
		this.dataService = requireNonNull(dataService);
		this.idGenerator = requireNonNull(idGenerator);
		this.attributeMappingMetaData = requireNonNull(attributeMappingMetaData);
	}

	@Override
	public List<Entity> upsert(Collection<AttributeMapping> attributeMappings)
	{
		List<Entity> result = new ArrayList<Entity>();
		List<Entity> mappingEntitiesToAdd = new ArrayList<>();
		List<Entity> mappingEntitiesToUpdate = new ArrayList<>();

		for (AttributeMapping attributeMapping : attributeMappings)
		{
			Entity entity;
			if (StringUtils.isBlank(attributeMapping.getIdentifier()))
			{
				attributeMapping.setIdentifier(idGenerator.generateId());
				entity = toAttributeMappingEntity(attributeMapping);
				mappingEntitiesToAdd.add(entity);
			}
			else
			{
				entity = toAttributeMappingEntity(attributeMapping);
				mappingEntitiesToUpdate.add(entity);
			}
			result.add(entity);
		}

		if (mappingEntitiesToAdd.size() > 0)
		{
			dataService.add(attributeMappingMetaData.getId(), mappingEntitiesToAdd.stream());
		}

		if (mappingEntitiesToUpdate.size() > 0)
		{
			dataService.update(attributeMappingMetaData.getId(), mappingEntitiesToUpdate.stream());
		}

		return result;
	}

	@Override
	public List<AttributeMapping> getAttributeMappings(List<Entity> attributeMappingEntities,
			EntityType sourceEntityType, EntityType targetEntityType)
	{
		return Lists.transform(attributeMappingEntities,
				attributeMappingEntity -> toAttributeMapping(attributeMappingEntity, sourceEntityType, targetEntityType));

	}

	@Override
	public List<Attribute> retrieveAttributesFromAlgorithm(String algorithm, EntityType sourceEntityType)
	{
		List<Attribute> sourceAttributes = Lists.newArrayList();

		Pattern pattern = Pattern.compile("\\$\\('([^']+)'\\)");
		Matcher matcher = pattern.matcher(algorithm);

		while (matcher.find())
		{
			Attribute attribute = sourceEntityType.getAttribute(matcher.group(1));
			if (!sourceAttributes.contains(attribute))
			{
				sourceAttributes.add(attribute);
			}
		}

		return sourceAttributes;
	}

	private AttributeMapping toAttributeMapping(Entity attributeMappingEntity, EntityType sourceEntityType,
			EntityType targetEntityType)
	{
		String identifier = attributeMappingEntity.getString(IDENTIFIER);
		String targetAtributeName = attributeMappingEntity.getString(TARGET_ATTRIBUTE);
		Attribute targetAttribute = targetEntityType.getAttribute(targetAtributeName);
		String algorithm = attributeMappingEntity.getString(ALGORITHM);
		String algorithmState = attributeMappingEntity.getString(ALGORITHM_STATE);
		List<Attribute> sourceAttributes = retrieveAttributesFromAlgorithm(algorithm, sourceEntityType);

		return new AttributeMapping(identifier, targetAttribute, algorithm, sourceAttributes, algorithmState);
	}

	private Entity toAttributeMappingEntity(AttributeMapping attributeMapping)
	{
		Entity attributeMappingEntity = new DynamicEntity(attributeMappingMetaData);
		attributeMappingEntity.set(IDENTIFIER, attributeMapping.getIdentifier());
		attributeMappingEntity.set(TARGET_ATTRIBUTE,
				attributeMapping.getTargetAttribute() != null ? attributeMapping.getTargetAttribute().getName() : null);
		attributeMappingEntity.set(ALGORITHM, attributeMapping.getAlgorithm());
		attributeMappingEntity.set(SOURCE_ATTRIBUTES, attributeMapping.getSourceAttributes()
																	  .stream()
																	  .map(Attribute::getName)
																	  .collect(Collectors.joining(",")));
		attributeMappingEntity.set(ALGORITHM_STATE, attributeMapping.getAlgorithmState().toString());
		return attributeMappingEntity;
	}

	@Override
	public void delete(List<AttributeMapping> attributeMappings)
	{
		if (attributeMappings.size() > 0)
		{
			Stream<Entity> stream = StreamSupport.stream(attributeMappings.spliterator(), false)
					.map(this::toAttributeMappingEntity);
			dataService.delete(attributeMappingMetaData.getId(), stream);
		}
	}
}
