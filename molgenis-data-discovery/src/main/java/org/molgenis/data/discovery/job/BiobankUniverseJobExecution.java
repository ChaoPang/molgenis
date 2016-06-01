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
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.jobs.JobExecution;

import static java.util.Objects.requireNonNull;

public class BiobankUniverseJobExecution extends JobExecution
{
	private final BiobankUniverseService biobankUniverseService;

	private static final long serialVersionUID = -1159893768494727598L;
	private static final String BIOBANK_UNIVERSE_JOB_TYPE = "BiobankUniverse";

	public BiobankUniverseJobExecution(DataService dataService, BiobankUniverseService biobankUniverseService)
	{
		super(dataService, BiobankUniverseJobExecutionMetaData.INSTANCE);
		setType(BIOBANK_UNIVERSE_JOB_TYPE);
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

	public List<BiobankSampleCollection> getMembers()
	{
		Iterable<Entity> entities = getEntities(MEMBERS);
		if (entities != null)
		{
			List<String> biobankSampleCollectionIds = StreamSupport.stream(entities.spliterator(), false)
					.map(Entity::getIdValue).map(Object::toString).collect(Collectors.toList());

			return biobankUniverseService.getBiobankSampleCollections(biobankSampleCollectionIds);
		}
		return Collections.emptyList();
	}

	public void setMembers(List<BiobankSampleCollection> members)
	{
		set(MEMBERS, members.stream().map(BiobankSampleCollection::getName).collect(toList()));
	}
}
