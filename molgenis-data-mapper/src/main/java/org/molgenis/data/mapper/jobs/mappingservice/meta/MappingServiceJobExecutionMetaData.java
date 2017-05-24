package org.molgenis.data.mapper.jobs.mappingservice.meta;

import org.molgenis.data.jobs.model.JobExecutionMetaData;
import org.molgenis.data.mapper.meta.MapperPackage;
import org.molgenis.data.mapper.meta.MappingProjectMetaData;
import org.molgenis.data.meta.AttributeType;
import org.molgenis.data.meta.SystemEntityType;
import org.molgenis.data.meta.model.EntityTypeMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.mapper.meta.MapperPackage.PACKAGE_MAPPER;
import static org.molgenis.data.meta.model.Package.PACKAGE_SEPARATOR;

@Component
public class MappingServiceJobExecutionMetaData extends SystemEntityType
{
	private final MapperPackage mapperPackage;
	private final JobExecutionMetaData jobExecutionMetaData;
	private final MappingProjectMetaData mappingProjectMetaData;
	private final EntityTypeMetadata entityTypeMetadata;

	public static final String SIMPLE_NAME = "MappingServiceJobExecution";
	public static final String MAPPING_SERVICE_JOB_EXECUTION = PACKAGE_MAPPER + PACKAGE_SEPARATOR + SIMPLE_NAME;

	public static final String MAPPING_PROJECT = "mappingProject";
	public static final String TARGET_ENTITY = "targetEntity";
	public static final String SOURCE_ENTITIES = "sourceEntities";
	public static final String JOB_TYPE = "MappingService";

	@Autowired
	MappingServiceJobExecutionMetaData(MapperPackage mapperPackage, JobExecutionMetaData jobExecutionMetaData,
			MappingProjectMetaData mappingProjectMetaData, EntityTypeMetadata entityTypeMetadata)
	{
		super(SIMPLE_NAME, PACKAGE_MAPPER);
		this.mapperPackage = requireNonNull(mapperPackage);
		this.jobExecutionMetaData = requireNonNull(jobExecutionMetaData);
		this.mappingProjectMetaData = requireNonNull(mappingProjectMetaData);
		this.entityTypeMetadata = requireNonNull(entityTypeMetadata);
	}

	@Override
	public void init()
	{
		setLabel("MappingService job execution");
		setPackage(mapperPackage);

		setExtends(jobExecutionMetaData);
		addAttribute(MAPPING_PROJECT).setDataType(AttributeType.XREF).setLabel("Mapping Project").setNillable(false)
				.setRefEntity(mappingProjectMetaData);
		addAttribute(TARGET_ENTITY).setDataType(AttributeType.XREF).setLabel("Target").setNillable(false)
				.setRefEntity(entityTypeMetadata);
		addAttribute(SOURCE_ENTITIES).setDataType(AttributeType.MREF).setLabel("Added source").setNillable(false)
				.setRefEntity(entityTypeMetadata);

	}
}