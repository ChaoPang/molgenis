package org.molgenis.data.mapper.jobs;

import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.jobs.JobExecution;
import org.molgenis.data.mapper.jobs.meta.MappingServiceJobExecutionMeta;
import org.molgenis.data.mapper.mapping.model.MappingProject;
import org.molgenis.data.mapper.service.MappingService;

import static java.util.Objects.requireNonNull;

public class MappingServiceJobExecution extends JobExecution
{
	private static final long serialVersionUID = 1003043918322982403L;

	public static final String ENTITY_NAME = "MappingServiceJobExecution";
	public static final String MAPPING_PROJECT = "mappingProject";
	public static final String TARGET_ENTITY = "targetEntity";
	public static final String SOURCE_ENTITY = "sourceEntity";
	public static final String JOB_TYPE = "MappingService";
	private final MappingService mappingService;

	public static final MappingServiceJobExecutionMeta META = new MappingServiceJobExecutionMeta();

	public MappingServiceJobExecution(DataService dataService, MappingService mappingService)
	{
		super(dataService, META);
		this.mappingService = requireNonNull(mappingService);
		setType(JOB_TYPE);
	}

	public Entity getTargetEntity()
	{
		return getEntity(TARGET_ENTITY);
	}

	public void setTargetEntity(Entity targetEntityMetaDataEntity)
	{
		set(TARGET_ENTITY, targetEntityMetaDataEntity);
	}

	public Entity getSourceEntity()
	{
		return getEntity(SOURCE_ENTITY);
	}

	public void setSourceEntity(Entity sourceEntityMetaDataEntity)
	{
		set(SOURCE_ENTITY, sourceEntityMetaDataEntity);
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