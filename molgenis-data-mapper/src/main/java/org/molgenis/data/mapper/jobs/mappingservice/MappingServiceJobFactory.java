package org.molgenis.data.mapper.jobs.mappingservice;

import org.molgenis.data.DataService;
import org.molgenis.data.jobs.JobExecutionUpdater;
import org.molgenis.data.jobs.Progress;
import org.molgenis.data.jobs.ProgressImpl;
import org.molgenis.data.mapper.jobs.mappingservice.meta.MappingServiceJobExecutionMetaData;
import org.molgenis.data.mapper.repository.EntityMappingRepository;
import org.molgenis.data.mapper.service.AlgorithmService;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.meta.model.EntityTypeMetadata;
import org.molgenis.security.core.runas.RunAsSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@Component
public class MappingServiceJobFactory
{
	private final DataService dataService;
	private final EntityMappingRepository entityMappingRepository;
	private final AlgorithmService algorithmService;
	private PlatformTransactionManager transactionManager;
	private final JobExecutionUpdater jobExecutionUpdater;
	private final MailSender mailSender;

	@Autowired
	public MappingServiceJobFactory(DataService dataService, EntityMappingRepository entityMappingRepository,
			AlgorithmService algorithmService, PlatformTransactionManager transactionManager,
			JobExecutionUpdater jobExecutionUpdater, MailSender mailSender)
	{
		this.dataService = requireNonNull(dataService);
		this.entityMappingRepository = requireNonNull(entityMappingRepository);
		this.algorithmService = requireNonNull(algorithmService);
		this.transactionManager = requireNonNull(transactionManager);
		this.jobExecutionUpdater = requireNonNull(jobExecutionUpdater);
		this.mailSender = requireNonNull(mailSender);
	}

	@RunAsSystem
	public MappingServiceJob create(MappingServiceJobExecution mappingServiceJobExecution,
			Authentication authentication)
	{
		dataService.add(MappingServiceJobExecutionMetaData.MAPPING_SERVICE_JOB_EXECUTION, mappingServiceJobExecution);

		List<String> sourceEntityNames = mappingServiceJobExecution.getSourceEntities().stream()
				.map(e -> e.getString(EntityTypeMetadata.ID)).collect(toList());
		String targetEntityName = mappingServiceJobExecution.getTargetEntity().getString(EntityTypeMetadata.ID);

		EntityType sourceEntityMetaData = dataService.getEntityType(sourceEntityNames.get(0));
		EntityType targetEntityMetaData = dataService.getEntityType(targetEntityName);

		Progress progress = new ProgressImpl(mappingServiceJobExecution, jobExecutionUpdater, mailSender);

		return new MappingServiceJob(mappingServiceJobExecution.getMappingProject(), targetEntityMetaData,
				sourceEntityMetaData, entityMappingRepository, algorithmService, progress,
				new TransactionTemplate(transactionManager), authentication);
	}
}
