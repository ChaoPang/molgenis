package org.molgenis.data.mapper.repository.impl;

import com.google.common.collect.Lists;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.UnknownEntityException;
import org.molgenis.data.mapper.controller.MappingServiceController;
import org.molgenis.data.mapper.mapping.model.AttributeMapping;
import org.molgenis.data.mapper.mapping.model.EntityMapping;
import org.molgenis.data.mapper.meta.EntityMappingMetaData;
import org.molgenis.data.mapper.repository.AttributeMappingRepository;
import org.molgenis.data.mapper.repository.EntityMappingRepository;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.populate.IdGenerator;
import org.molgenis.data.support.DynamicEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

/**
 * O/R mapping between EntityMapping Entity and EntityMapping POJO
 */
public class EntityMappingRepositoryImpl implements EntityMappingRepository
{
	private static final Logger LOG = LoggerFactory.getLogger(MappingServiceController.class);

	private final DataService dataService;
	private final IdGenerator idGenerator;
	private final AttributeMappingRepository attributeMappingRepository;
	private final EntityMappingMetaData entityMappingMetaData;

	@Autowired
	public EntityMappingRepositoryImpl(AttributeMappingRepository attributeMappingRepository, DataService dataService,
			IdGenerator idGenerator, EntityMappingMetaData entityMappingMetaData)
	{
		this.attributeMappingRepository = requireNonNull(attributeMappingRepository);
		this.dataService = requireNonNull(dataService);
		this.idGenerator = requireNonNull(idGenerator);
		this.entityMappingMetaData = requireNonNull(entityMappingMetaData);
	}

	@Override
	public EntityMapping getEntityMapping(String identifier)
	{
		Entity entityMappingEntity = dataService.findOneById(entityMappingMetaData.getId(), identifier);
		if (entityMappingEntity == null)
		{
			return null;
		}
		return toEntityMapping(entityMappingEntity);
	}

	@Override
	public List<EntityMapping> toEntityMappings(List<Entity> entityMappingEntities)
	{
		return Lists.transform(entityMappingEntities, this::toEntityMapping);
	}

	private EntityMapping toEntityMapping(Entity entityMappingEntity)
	{
		String identifier = entityMappingEntity.getString(EntityMappingMetaData.IDENTIFIER);

		EntityType targetEntityType;
		try
		{
			targetEntityType = dataService.getEntityType(
					entityMappingEntity.getString(EntityMappingMetaData.TARGET_ENTITY_TYPE));
		}
		catch (UnknownEntityException uee)
		{
			LOG.error(uee.getMessage());
			targetEntityType = null;
		}

		EntityType sourceEntityType;
		try
		{
			sourceEntityType = dataService.getEntityType(
					entityMappingEntity.getString(EntityMappingMetaData.SOURCE_ENTITY_TYPE));
		}
		catch (UnknownEntityException uee)
		{
			LOG.error(uee.getMessage());
			sourceEntityType = null;
		}

		List<Entity> attributeMappingEntities = Lists.newArrayList(
				entityMappingEntity.getEntities(EntityMappingMetaData.ATTRIBUTE_MAPPINGS));
		List<AttributeMapping> attributeMappings = attributeMappingRepository.getAttributeMappings(
				attributeMappingEntities, sourceEntityType, targetEntityType);

		return new EntityMapping(identifier, sourceEntityType, targetEntityType, attributeMappings);
	}

	@Override
	public List<Entity> upsert(Collection<EntityMapping> entityMappings)
	{
		return entityMappings.stream().map(this::upsert).collect(Collectors.toList());
	}

	private Entity upsert(EntityMapping entityMapping)
	{
		// add or update the new list of attribute mappings
		List<Entity> attributeMappingEntities = attributeMappingRepository.upsert(entityMapping.getAttributeMappings());
		Entity entityMappingEntity;
		if (entityMapping.getIdentifier() == null)
		{
			entityMapping.setIdentifier(idGenerator.generateId());
			entityMappingEntity = toEntityMappingEntity(entityMapping, attributeMappingEntities);
			dataService.add(entityMappingMetaData.getId(), entityMappingEntity);
		}
		else
		{
			// Delete the attribute mappings does not exist in the new list of attribute mappings any more
			EntityMapping existing = getEntityMapping(entityMapping.getIdentifier());
			if (existing == null)
			{
				throw new MolgenisDataException("EntityMapping " + existing + " does not exist");
			}
			List<AttributeMapping> attributeMappings = new ArrayList<>(entityMapping.getAttributeMappings());
			List<AttributeMapping> attributeMappingsToRemove = StreamSupport
					.stream(existing.getAttributeMappings().spliterator(), false)
					.filter(attributeMapping -> !attributeMappings.contains(attributeMapping))
					.collect(Collectors.toList());
			attributeMappingRepository.delete(attributeMappingsToRemove);

			entityMappingEntity = toEntityMappingEntity(entityMapping, attributeMappingEntities);
			dataService.update(entityMappingMetaData.getId(), entityMappingEntity);
		}
		return entityMappingEntity;
	}

	private Entity toEntityMappingEntity(EntityMapping entityMapping, List<Entity> attributeMappingEntities)
	{
		Entity entityMappingEntity = new DynamicEntity(entityMappingMetaData);
		entityMappingEntity.set(EntityMappingMetaData.IDENTIFIER, entityMapping.getIdentifier());
		entityMappingEntity.set(EntityMappingMetaData.SOURCE_ENTITY_TYPE, entityMapping.getName());
		entityMappingEntity.set(EntityMappingMetaData.TARGET_ENTITY_TYPE,
				entityMapping.getTargetEntityType() != null ? entityMapping.getTargetEntityType().getId() : null);
		entityMappingEntity.set(EntityMappingMetaData.ATTRIBUTE_MAPPINGS, attributeMappingEntities);
		return entityMappingEntity;
	}

	@Override
	public void delete(List<EntityMapping> entityMappings)
	{
		if (entityMappings.size() > 0)
		{
			List<AttributeMapping> attributeMappings = new ArrayList<>();
			entityMappings.stream()
					.forEach(entityMapping -> attributeMappings.addAll(entityMapping.getAttributeMappings()));

			Stream<Entity> stream = StreamSupport.stream(entityMappings.spliterator(), false)
					.map(entityMapping -> toEntityMappingEntity(entityMapping, Collections.emptyList()));

			dataService.delete(entityMappingMetaData.getId(), stream);

			attributeMappingRepository.delete(attributeMappings);
		}
	}
}