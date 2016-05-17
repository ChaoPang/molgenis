package org.molgenis.data.discovery.job;

import static java.util.stream.Collectors.toList;
import static org.molgenis.data.discovery.job.BiobankUniverseJobExecutionMetaData.MEMBERS;
import static org.molgenis.data.discovery.job.BiobankUniverseJobExecutionMetaData.UNIVERSE;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.discovery.model.BiobankUniverse;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.jobs.JobExecution;
import org.molgenis.data.meta.EntityMetaDataMetaData;

import static java.util.Objects.requireNonNull;

public class BiobankUniverseJobExecution extends JobExecution
{
	private final DataService dataService;
	private final BiobankUniverseService biobankUniverseService;

	private static final long serialVersionUID = -1159893768494727598L;
	private static final String BIOBANK_UNIVERSE_JOB_TYPE = "BiobankUniverse";

	public BiobankUniverseJobExecution(DataService dataService, BiobankUniverseService biobankUniverseService)
	{
		super(dataService, BiobankUniverseJobExecutionMetaData.INSTANCE);
		setType(BIOBANK_UNIVERSE_JOB_TYPE);

		this.dataService = requireNonNull(dataService);
		this.biobankUniverseService = requireNonNull(biobankUniverseService);
	}

	public BiobankUniverse getUniverse()
	{
		Entity biobankUniverseEntity = getEntity(UNIVERSE);
		return biobankUniverseService.getBiobankUniverse(biobankUniverseEntity.getIdValue().toString());
	}

	public void setUniverse(BiobankUniverse biobankUniverse)
	{
		set(BiobankUniverseJobExecutionMetaData.UNIVERSE, biobankUniverse.getIdentifier());
	}

	public List<EntityMetaData> getMembers()
	{
		Iterable<Entity> entities = getEntities(MEMBERS);
		if (entities != null)
		{
			return StreamSupport.stream(entities.spliterator(), false)
					.map(entity -> dataService.getEntityMetaData(entity.getString(EntityMetaDataMetaData.FULL_NAME)))
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	public void setMembers(List<EntityMetaData> members)
	{
		set(MEMBERS, members.stream().map(EntityMetaData::getName).collect(toList()));
	}
}
