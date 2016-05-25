package org.molgenis.data.discovery.job;

import static org.molgenis.MolgenisFieldTypes.MREF;
import static org.molgenis.MolgenisFieldTypes.XREF;

import org.molgenis.data.discovery.meta.BiobankSampleCollectionMetaData;
import org.molgenis.data.discovery.meta.BiobankUniverseMetaData;
import org.molgenis.data.jobs.JobExecutionMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class BiobankUniverseJobExecutionMetaData extends DefaultEntityMetaData
{
	public final static String ENTITY_NAME = "BiobankUniverseJobExecution";
	public final static String UNIVERSE = "universe";
	public final static String MEMBERS = "members";
	public final static BiobankUniverseJobExecutionMetaData INSTANCE = new BiobankUniverseJobExecutionMetaData();

	public BiobankUniverseJobExecutionMetaData()
	{
		super(ENTITY_NAME, BiobankUniverseJobExecution.class);
		setExtends(new JobExecutionMetaData());
		addAttribute(UNIVERSE).setDataType(XREF).setRefEntity(BiobankUniverseMetaData.INSTANCE);
		addAttribute(MEMBERS).setDataType(MREF).setRefEntity(BiobankSampleCollectionMetaData.INSTANCE);
	}
}