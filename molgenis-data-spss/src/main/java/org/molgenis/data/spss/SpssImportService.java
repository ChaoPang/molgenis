package org.molgenis.data.spss;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.molgenis.data.DataService;
import org.molgenis.data.DatabaseAction;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.FileRepositoryCollectionFactory;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.Repository;
import org.molgenis.data.RepositoryCollection;
import org.molgenis.data.importer.EntitiesValidationReportImpl;
import org.molgenis.data.importer.ImportService;
import org.molgenis.data.meta.MetaDataService;
import org.molgenis.data.support.GenericImporterExtensions;
import org.molgenis.framework.db.EntitiesValidationReport;
import org.molgenis.framework.db.EntityImportReport;
import org.molgenis.security.permission.PermissionSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

@Service
public class SpssImportService implements ImportService
{
	private final DataService dataService;
	private final PermissionSystemService permissionSystemService;

	@Autowired
	public SpssImportService(FileRepositoryCollectionFactory fileRepositoryCollectionFactory, DataService dataService,
			PermissionSystemService permissionSystemService)
	{
		this.dataService = requireNonNull(dataService);
		this.permissionSystemService = requireNonNull(permissionSystemService);
	}

	@Override
	@Transactional
	public EntityImportReport doImport(RepositoryCollection source, DatabaseAction databaseAction, String defaultPackage)
	{
		if (databaseAction != DatabaseAction.ADD) throw new IllegalArgumentException("Only ADD is supported");

		List<EntityMetaData> addedEntities = Lists.newArrayList();
		EntityImportReport report = new EntityImportReport();
		try
		{
			Iterator<String> it = source.getEntityNames().iterator();
			while (it.hasNext())
			{
				String entityNameToImport = it.next();
				Repository repo = source.getRepository(entityNameToImport);
				try
				{
					report = new EntityImportReport();

					dataService.getMeta().addEntityMeta(repo.getEntityMetaData());

					dataService.getRepository(repo.getName()).add(repo);

					List<String> entityNames = addedEntities.stream().map(emd -> emd.getName())
							.collect(Collectors.toList());
					permissionSystemService.giveUserEntityPermissions(SecurityContextHolder.getContext(), entityNames);
					int count = 1;
					for (String entityName : entityNames)
					{
						report.addEntityCount(entityName, count++);
					}
				}
				finally
				{
					IOUtils.closeQuietly(repo);
				}
			}
		}
		catch (Exception e)
		{
			// Remove created repositories
			for (EntityMetaData emd : addedEntities)
			{
				if (dataService.hasRepository(emd.getName()))
				{
					dataService.deleteAll(emd.getName());
				}
			}

			throw new MolgenisDataException(e);
		}

		return report;
	}

	@Override
	/**
	 * Spss entity validation 
	 */
	public EntitiesValidationReport validateImport(File file, RepositoryCollection source)
	{
		EntitiesValidationReport report = new EntitiesValidationReportImpl();
		for (String entityName : source.getEntityNames())
		{
			report.getSheetsImportable().put(entityName, !dataService.hasRepository(entityName));
		}
		return report;
	}

	@Override
	public boolean canImport(File file, RepositoryCollection source)
	{
		for (String extension : GenericImporterExtensions.getSpss())
		{
			if (file.getName().toLowerCase().endsWith(extension))
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public int getOrder()
	{
		return 10;
	}

	@Override
	public List<DatabaseAction> getSupportedDatabaseActions()
	{
		return Lists.newArrayList(DatabaseAction.ADD);
	}

	@Override
	public boolean getMustChangeEntityName()
	{
		return false;
	}

	@Override
	public Set<String> getSupportedFileExtensions()
	{
		return GenericImporterExtensions.getSpss();
	}

	@Override
	public LinkedHashMap<String, Boolean> integrationTestMetaData(MetaDataService metaDataService,
			RepositoryCollection repositoryCollection, String defaultPackage)
	{
		return metaDataService.integrationTestMetaData(repositoryCollection);
	}
}
