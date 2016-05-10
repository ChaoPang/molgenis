package org.molgenis.data.mapper.jobs.datadiscovery;

import org.molgenis.data.DataService;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.jobs.JobExecution;

public class DataDiscoveryJobExecution extends JobExecution
{
	private static final long serialVersionUID = -1174362703058512147L;

	public static final String ENTITY_NAME = "DataDiscoveryJobExecution";
	public static final String TARGET = "DataDiscoveryJobExecution";

	public DataDiscoveryJobExecution(DataService dataService, EntityMetaData emd)
	{
		super(dataService, emd);
	}
}
