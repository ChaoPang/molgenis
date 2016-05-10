package org.molgenis.data.mapper.jobs.mappingservice.meta;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.jobs.JobExecutionMetaData;
import org.molgenis.data.mapper.jobs.mappingservice.MappingServiceJobExecution;
import org.molgenis.data.mapper.meta.MappingProjectMetaData;
import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class MappingServiceJobExecutionMeta extends DefaultEntityMetaData
{
	public final static MappingServiceJobExecutionMeta INSTANCE = new MappingServiceJobExecutionMeta();

	public MappingServiceJobExecutionMeta()
	{
		super(MappingServiceJobExecution.ENTITY_NAME, MappingServiceJobExecution.class);
		setExtends(new JobExecutionMetaData());
		addAttribute(MappingServiceJobExecution.MAPPING_PROJECT).setDataType(MolgenisFieldTypes.XREF)
				.setLabel("Mapping Project").setNillable(false).setRefEntity(new MappingProjectMetaData());
		addAttribute(MappingServiceJobExecution.TARGET_ENTITY).setDataType(MolgenisFieldTypes.XREF).setLabel("Target")
				.setNillable(false).setRefEntity(EntityMetaDataMetaData.INSTANCE);
		addAttribute(MappingServiceJobExecution.SOURCE_ENTITIES).setDataType(MolgenisFieldTypes.MREF)
				.setLabel("Added Source").setNillable(false).setRefEntity(EntityMetaDataMetaData.INSTANCE);
	}
}