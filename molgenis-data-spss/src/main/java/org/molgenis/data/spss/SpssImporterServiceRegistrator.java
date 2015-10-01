package org.molgenis.data.spss;

import java.util.Objects;

import org.molgenis.data.FileRepositoryCollectionFactory;
import org.molgenis.data.importer.ImportServiceFactory;
import org.molgenis.data.support.GenericImporterExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class SpssImporterServiceRegistrator implements ApplicationListener<ContextRefreshedEvent>
{

	private static final Logger LOG = LoggerFactory.getLogger(SpssImporterServiceRegistrator.class);
	private final SpssImportService spssImportService;
	private final ImportServiceFactory importServiceFactory;
	private final FileRepositoryCollectionFactory fileRepositorySourceFactory;

	@Autowired
	public SpssImporterServiceRegistrator(SpssImportService spssImportService,
			ImportServiceFactory importServiceFactory, FileRepositoryCollectionFactory fileRepositorySourceFactory)
	{
		super();
		this.spssImportService = Objects.requireNonNull(spssImportService);
		this.importServiceFactory = Objects.requireNonNull(importServiceFactory);
		this.fileRepositorySourceFactory = Objects.requireNonNull(fileRepositorySourceFactory);
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event)
	{
		importServiceFactory.addImportService(spssImportService);
		LOG.info("Registered Spss import service");

		fileRepositorySourceFactory.addFileRepositoryCollectionClass(SpssRepositoryCollection.class,
				GenericImporterExtensions.getSpss());
		LOG.info("Registered Spss file extensions in the FileRepositorySourceFactory");
	}
}
