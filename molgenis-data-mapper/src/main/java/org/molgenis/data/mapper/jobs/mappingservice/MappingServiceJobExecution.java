package org.molgenis.data.mapper.jobs.mappingservice;

import com.google.common.collect.Lists;
import org.molgenis.data.Entity;
import org.molgenis.data.jobs.model.JobExecution;
import org.molgenis.data.mapper.mapping.model.MappingProject;
import org.molgenis.data.mapper.service.MappingService;
import org.molgenis.data.meta.model.EntityType;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.mapper.jobs.mappingservice.meta.MappingServiceJobExecutionMetaData.*;

public class MappingServiceJobExecution extends JobExecution
{
	private static final long serialVersionUID = 1003043918322982403L;

	private final MappingService mappingService;

	public MappingServiceJobExecution(EntityType entityType, MappingService mappingService)
	{
		super(entityType);
		setType(JOB_TYPE);
		this.mappingService = requireNonNull(mappingService);
	}

	public EntityType getTargetEntity()
	{
		return getEntity(TARGET_ENTITY, EntityType.class);
	}

	public void setTargetEntity(EntityType targetEntitType)
	{
		set(TARGET_ENTITY, targetEntitType);
	}

	public List<EntityType> getSourceEntities()
	{
		return Lists.newArrayList(getEntities(SOURCE_ENTITIES, EntityType.class));
	}

	public void setSourceEntity(List<EntityType> sourceEntityMetaDatas)
	{
		set(SOURCE_ENTITIES, sourceEntityMetaDatas);
	}

	public MappingProject getMappingProject()
	{
		Entity mappingProjectEntity = getEntity(MAPPING_PROJECT);
		return mappingService.getMappingProject(mappingProjectEntity.getIdValue().toString());
	}

	public void setMappingProject(Entity mappingProjectEntity)
	{
		set(MAPPING_PROJECT, mappingProjectEntity);
	}
}