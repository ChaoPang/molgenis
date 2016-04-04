package org.molgenis.data.mapper.repository.impl;

import java.util.ArrayList;
import java.util.List;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.Query;
import org.molgenis.data.mapper.mapping.model.MappingProject;
import org.molgenis.data.mapper.mapping.model.MappingTarget;
import org.molgenis.data.mapper.meta.MappingProjectMetaData;
import org.molgenis.data.mapper.repository.MappingProjectRepository;
import org.molgenis.data.mapper.repository.MappingTargetRepository;
import org.molgenis.data.support.MapEntity;
import org.molgenis.security.user.MolgenisUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import static java.util.Objects.requireNonNull;

public class MappingProjectRepositoryImpl implements MappingProjectRepository
{
	public static final MappingProjectMetaData META_DATA = new MappingProjectMetaData();

	private final MappingTargetRepository mappingTargetRepository;
	private final DataService dataService;
	private final IdGenerator idGenerator;
	private final MolgenisUserService molgenisUserService;

	@Autowired
	public MappingProjectRepositoryImpl(MappingTargetRepository mappingTargetRepository, DataService dataService,
			IdGenerator idGenerator, MolgenisUserService molgenisUserService)
	{
		this.mappingTargetRepository = requireNonNull(mappingTargetRepository);
		this.dataService = requireNonNull(dataService);
		this.idGenerator = requireNonNull(idGenerator);
		this.molgenisUserService = requireNonNull(molgenisUserService);
	}

	@Override
	@Transactional
	public void add(MappingProject mappingProject)
	{
		if (mappingProject.getIdentifier() != null)
		{
			throw new MolgenisDataException("MappingProject already exists");
		}
		dataService.add(MappingProjectRepositoryImpl.META_DATA.getName(), toEntity(mappingProject));
	}

	@Override
	@Transactional
	public void update(MappingProject mappingProject)
	{
		MappingProject existing = getMappingProject(mappingProject.getIdentifier());
		if (existing == null)
		{
			throw new MolgenisDataException("MappingProject does not exist");
		}
		Entity mappingProjectEntity = toEntity(mappingProject);
		dataService.update(MappingProjectRepositoryImpl.META_DATA.getName(), mappingProjectEntity);
	}

	@Override
	public MappingProject getMappingProject(String identifier)
	{
		Entity mappingProjectEntity = dataService.findOne(MappingProjectRepositoryImpl.META_DATA.getName(), identifier);
		if (mappingProjectEntity == null)
		{
			return null;
		}
		return toMappingProject(mappingProjectEntity);
	}

	@Override
	public List<MappingProject> getAllMappingProjects()
	{
		List<MappingProject> results = new ArrayList<>();
		dataService.findAll(MappingProjectRepositoryImpl.META_DATA.getName()).forEach(entity -> {
			results.add(toMappingProject(entity));
		});
		return results;
	}

	@Override
	public List<MappingProject> getMappingProjects(Query q)
	{
		List<MappingProject> results = new ArrayList<>();
		dataService.findAll(MappingProjectRepositoryImpl.META_DATA.getName(), q).forEach(entity -> {
			results.add(toMappingProject(entity));
		});
		return results;
	}

	/**
	 * Creates a fully reconstructed MappingProject from an Entity retrieved from the repository.
	 * 
	 * @param mappingProjectEntity
	 *            Entity with {@link MappingProjectMetaData} metadata
	 * @return fully reconstructed MappingProject
	 */
	private MappingProject toMappingProject(Entity mappingProjectEntity)
	{
		String identifier = mappingProjectEntity.getString(MappingProjectMetaData.IDENTIFIER);
		String name = mappingProjectEntity.getString(MappingProjectMetaData.NAME);
		MolgenisUser owner = molgenisUserService.getUser(mappingProjectEntity.getString(MappingProjectMetaData.OWNER));
		List<Entity> mappingTargetEntities = Lists
				.newArrayList(mappingProjectEntity.getEntities(MappingProjectMetaData.MAPPINGTARGETS));
		List<MappingTarget> mappingTargets = mappingTargetRepository.toMappingTargets(mappingTargetEntities);

		return new MappingProject(identifier, name, owner, mappingTargets);
	}

	/**
	 * Creates a new Entity for a MappingProject. Upserts the {@link MappingProject}'s {@link MappingTarget}s in the
	 * {@link #mappingTargetRepository}.
	 * 
	 * @param mappingProject
	 *            the {@link MappingProject} used to create an Entity
	 * @return Entity filled with the data from the MappingProject
	 */
	private Entity toEntity(MappingProject mappingProject)
	{
		Entity result = new MapEntity(META_DATA);
		if (mappingProject.getIdentifier() == null)
		{
			mappingProject.setIdentifier(idGenerator.generateId());
		}
		result.set(MappingProjectMetaData.IDENTIFIER, mappingProject.getIdentifier());
		result.set(MappingProjectMetaData.OWNER, mappingProject.getOwner());
		result.set(MappingProjectMetaData.NAME, mappingProject.getName());
		List<Entity> mappingTargetEntities = mappingTargetRepository.upsert(mappingProject.getMappingTargets());
		result.set(MappingProjectMetaData.MAPPINGTARGETS, mappingTargetEntities);
		return result;
	}

	@Override
	public void delete(String mappingProjectId)
	{
		MappingProject mappingProject = getMappingProject(mappingProjectId);
		if (mappingProject != null)
		{
			List<MappingTarget> mappingTargets = mappingProject.getMappingTargets();
			dataService.delete(MappingProjectRepositoryImpl.META_DATA.getName(), mappingProjectId);
			mappingTargetRepository.delete(mappingTargets);
		}
	}
}