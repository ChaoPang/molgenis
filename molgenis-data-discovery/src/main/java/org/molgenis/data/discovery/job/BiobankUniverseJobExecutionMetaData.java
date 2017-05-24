package org.molgenis.data.discovery.job;

import org.molgenis.data.discovery.meta.BiobankUniversePackage;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleCollectionMetaData;
import org.molgenis.data.discovery.meta.biobank.BiobankUniverseMetaData;
import org.molgenis.data.jobs.model.JobExecutionMetaData;
import org.molgenis.data.meta.AttributeType;
import org.molgenis.data.meta.SystemEntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.discovery.meta.BiobankUniversePackage.PACKAGE_UNIVERSE;
import static org.molgenis.data.meta.model.Package.PACKAGE_SEPARATOR;

@Component
public class BiobankUniverseJobExecutionMetaData extends SystemEntityType
{
	public final static String SIMPLE_NAME = "BiobankUniverseJobExecution";
	public final static String BIOBANK_UNIVERSE_JOB_EXECUTION = PACKAGE_UNIVERSE + PACKAGE_SEPARATOR + SIMPLE_NAME;

	public final static String UNIVERSE = "universe";
	public final static String MEMBERS = "members";

	private final BiobankUniversePackage biobankUniversePackage;
	private final BiobankUniverseMetaData biobankUniverseMetaData;
	private final BiobankSampleCollectionMetaData biobankSampleCollectionMetaData;
	private final JobExecutionMetaData jobExecutionMetaData;

	@Autowired
	public BiobankUniverseJobExecutionMetaData(BiobankUniversePackage biobankUniversePackage,
			BiobankUniverseMetaData biobankUniverseMetaData,
			BiobankSampleCollectionMetaData biobankSampleCollectionMetaData, JobExecutionMetaData jobExecutionMetaData)
	{
		super(SIMPLE_NAME, PACKAGE_UNIVERSE);
		this.biobankUniversePackage = requireNonNull(biobankUniversePackage);
		this.biobankUniverseMetaData = requireNonNull(biobankUniverseMetaData);
		this.biobankSampleCollectionMetaData = requireNonNull(biobankSampleCollectionMetaData);
		this.jobExecutionMetaData = requireNonNull(jobExecutionMetaData);
	}

	@Override
	protected void init()
	{
		setLabel("Biobank universe job execution");
		setPackage(biobankUniversePackage);
		setExtends(jobExecutionMetaData);

		addAttribute(UNIVERSE).setDataType(AttributeType.XREF).setRefEntity(biobankUniverseMetaData);
		addAttribute(MEMBERS).setDataType(AttributeType.MREF).setRefEntity(biobankSampleCollectionMetaData);
	}
}