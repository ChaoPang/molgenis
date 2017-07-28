package org.molgenis.data.mapper.repository.impl;

import com.google.common.collect.Lists;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.mapper.mapping.model.EntityMapping;
import org.molgenis.data.mapper.mapping.model.MappingTarget;
import org.molgenis.data.mapper.meta.MappingProjectMetaData;
import org.molgenis.data.mapper.meta.MappingTargetMetaData;
import org.molgenis.data.mapper.repository.EntityMappingRepository;
import org.molgenis.data.mapper.repository.MappingTargetRepository;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.populate.IdGenerator;
import org.molgenis.data.support.DynamicEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class MappingTargetRepositoryImpl implements MappingTargetRepository
{
	private final EntityMappingRepository entityMappingRepository;
	private final DataService dataService;
	private final IdGenerator idGenerator;
	private final MappingTargetMetaData mappingTargetMetaData;

	public MappingTargetRepositoryImpl(EntityMappingRepository entityMappingRepository, DataService dataService,
			IdGenerator idGenerator, MappingTargetMetaData mappingTargetMetaData)
	{
		this.entityMappingRepository = requireNonNull(entityMappingRepository);
		this.dataService = requireNonNull(dataService);
		this.idGenerator = requireNonNull(idGenerator);
		this.mappingTargetMetaData = requireNonNull(mappingTargetMetaData);
	}

	@Override
	public List<Entity> upsert(Collection<MappingTarget> collection)
	{
		return collection.stream().map(this::upsert).collect(Collectors.toList());
	}

	private Entity upsert(MappingTarget mappingTarget)
	{
		List<Entity> entityMappingEntities = entityMappingRepository.upsert(mappingTarget.getEntityMappings());
		Entity mappingTargetEntity;
		if (mappingTarget.getIdentifier() == null)
		{
			mappingTarget.setIdentifier(idGenerator.generateId());
			mappingTargetEntity = toMappingTargetEntity(mappingTarget, entityMappingEntities);
			dataService.add(mappingTargetMetaData.getId(), mappingTargetEntity);
		}
		else
		{
			mappingTargetEntity = toMappingTargetEntity(mappingTarget, entityMappingEntities);
			dataService.update(mappingTargetMetaData.getId(), mappingTargetEntity);
		}
		return mappingTargetEntity;
	}

	/**
	 * Creates a new {@link DynamicEntity} for this MappingProject. Doesn't yet fill the {@link EntityMapping}s.
	 */
	private Entity toMappingTargetEntity(MappingTarget mappingTarget, List<Entity> entityMappingEntities)
	{
		Entity mappingTargetEntity = new DynamicEntity(mappingTargetMetaData);
		mappingTargetEntity.set(MappingProjectMetaData.IDENTIFIER, mappingTarget.getIdentifier());
		mappingTargetEntity.set(MappingTargetMetaData.TARGET, mappingTarget.getTarget().getId());
		mappingTargetEntity.set(MappingTargetMetaData.ENTITY_MAPPINGS, entityMappingEntities);
		return mappingTargetEntity;
	}

	@Override
	public List<MappingTarget> toMappingTargets(List<Entity> mappingTargetEntities)
	{
		return mappingTargetEntities.stream().map(this::toMappingTarget).collect(Collectors.toList());
	}

	/**
	 * Creates a fully reconstructed MappingProject from an Entity retrieved from the repository.
	 *
	 * @param mappingTargetEntity Entity with {@link MappingProjectMetaData} metadata
	 * @return fully reconstructed MappingProject
	 */
	private MappingTarget toMappingTarget(Entity mappingTargetEntity)
	{
		List<EntityMapping> entityMappings = Collections.emptyList();
		String identifier = mappingTargetEntity.getString(MappingTargetMetaData.IDENTIFIER);

		if (!dataService.hasRepository(mappingTargetEntity.getString(MappingTargetMetaData.TARGET)))
		{
			return null;
		}

		EntityType target = dataService.getEntityType(mappingTargetEntity.getString(MappingTargetMetaData.TARGET));

		if (mappingTargetEntity.getEntities(MappingTargetMetaData.ENTITY_MAPPINGS) != null)
		{
			List<Entity> entityMappingEntities = Lists.newArrayList(
					mappingTargetEntity.getEntities(MappingTargetMetaData.ENTITY_MAPPINGS));
			entityMappings = entityMappingRepository.toEntityMappings(entityMappingEntities);
		}

		return new MappingTarget(identifier, target, entityMappings);
	}

	@Override
	public void delete(List<MappingTarget> mappingTargets)
	{
		if (mappingTargets.size() > 0)
		{
			List<EntityMapping> entityMappings = new ArrayList<>();
			mappingTargets.stream().forEach(mappingTarget -> entityMappings.addAll(mappingTarget.getEntityMappings()));

			Stream<Entity> stream = mappingTargets.stream()
					.map(mappingTarget -> toMappingTargetEntity(mappingTarget, Collections.emptyList()));
			dataService.delete(mappingTargetMetaData.getId(), stream);
			entityMappingRepository.delete(entityMappings);
		}
	}
}