package org.molgenis.data.mapper.jobs.meta;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.jobs.JobExecutionMetaData;
import org.molgenis.data.mapper.jobs.ComputeDistanceJobExecution;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class ComputeDistanceJobExecutionMeta extends DefaultEntityMetaData
{
	public ComputeDistanceJobExecutionMeta()
	{
		super(ComputeDistanceJobExecution.ENTITY_NAME, ComputeDistanceJobExecution.class);
		setExtends(new JobExecutionMetaData());
		addAttribute(ComputeDistanceJobExecution.TARGET_ENTITY_NAME).setDataType(MolgenisFieldTypes.STRING)
				.setLabel("target entity Name").setNillable(false);
		addAttribute(ComputeDistanceJobExecution.SOURCE_ENTITY_NAME).setDataType(MolgenisFieldTypes.STRING)
				.setLabel("source entity name").setNillable(false);
	}
}