package org.molgenis.data.mapper.jobs;

import org.molgenis.data.DataService;
import org.molgenis.data.jobs.JobExecution;
import org.molgenis.data.mapper.jobs.meta.ComputeDistanceJobExecutionMeta;

public class ComputeDistanceJobExecution extends JobExecution
{
	private static final long serialVersionUID = 1003043918322982403L;

	public static final String ENTITY_NAME = "ComputeDistanceJobExecution";
	public static final String TARGET_ENTITY_NAME = "targetEntityName";
	public static final String SOURCE_ENTITY_NAME = "sourceEntityName";
	public static final String JOB_TYPE = "ComputeDistance";

	public static final ComputeDistanceJobExecutionMeta META = new ComputeDistanceJobExecutionMeta();

	public ComputeDistanceJobExecution(DataService dataService)
	{
		super(dataService, META);
		setType(JOB_TYPE);
	}

	public String getTargetEntityName()
	{
		return getString(TARGET_ENTITY_NAME);
	}

	public void setTargetEntityName(String value)
	{
		set(TARGET_ENTITY_NAME, value);
	}

	public String getSourceEntityName()
	{
		return getString(SOURCE_ENTITY_NAME);
	}

	public void setSourceEntityName(String value)
	{
		set(SOURCE_ENTITY_NAME, value);
	}
}