package org.molgenis.data.mapper.jobs;

import org.molgenis.data.DataService;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisDataAccessException;
import org.molgenis.data.jobs.JobExecutionUpdater;
import org.molgenis.data.jobs.Progress;
import org.molgenis.data.jobs.ProgressImpl;
import org.molgenis.data.mapper.repository.AttributeMappingRepository;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.impl.OntologyTermSemanticSearchImpl;
import org.molgenis.security.core.runas.RunAsSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static java.util.Objects.requireNonNull;

@Component
public class ComputeDistanceJobFactory
{
	private final DataService dataService;
	private final OntologyTermSemanticSearchImpl ontologyTermBasedSemanticSearch;
	private final SemanticSearchService semanticSearchService;
	private final AttributeMappingRepository attributeMappingRepository;
	private PlatformTransactionManager transactionManager;
	private final JobExecutionUpdater jobExecutionUpdater;
	private final MailSender mailSender;

	@Autowired
	public ComputeDistanceJobFactory(DataService dataService,
			OntologyTermSemanticSearchImpl ontologyTermBasedSemanticSearch,
			SemanticSearchService semanticSearchService, AttributeMappingRepository attributeMappingRepository,
			PlatformTransactionManager transactionManager, JobExecutionUpdater jobExecutionUpdater,
			MailSender mailSender)
	{
		this.dataService = requireNonNull(dataService);
		this.ontologyTermBasedSemanticSearch = requireNonNull(ontologyTermBasedSemanticSearch);
		this.semanticSearchService = requireNonNull(semanticSearchService);
		this.attributeMappingRepository = requireNonNull(attributeMappingRepository);
		this.transactionManager = requireNonNull(transactionManager);
		this.jobExecutionUpdater = requireNonNull(jobExecutionUpdater);
		this.mailSender = requireNonNull(mailSender);
	}

	@RunAsSystem
	public ComputeDistanceJob create(ComputeDistanceJobExecution computeDistanceJobExecution,
			Authentication authentication)
	{
		dataService.add(ComputeDistanceJobExecution.ENTITY_NAME, computeDistanceJobExecution);

		Progress progress = new ProgressImpl(computeDistanceJobExecution, jobExecutionUpdater, mailSender);

		EntityMetaData targetEntityMetaData = dataService
				.getEntityMetaData(computeDistanceJobExecution.getTargetEntityName());
		EntityMetaData sourceEntityMetaData = dataService
				.getEntityMetaData(computeDistanceJobExecution.getSourceEntityName());

		if (targetEntityMetaData == null) throw new MolgenisDataAccessException(
				"Cannot find the entity with the name: " + computeDistanceJobExecution.getTargetEntityName());
		if (sourceEntityMetaData == null) throw new MolgenisDataAccessException(
				"Cannot find the entity with the name: " + computeDistanceJobExecution.getSourceEntityName());

		return new ComputeDistanceJob(targetEntityMetaData, sourceEntityMetaData, ontologyTermBasedSemanticSearch,
				semanticSearchService, attributeMappingRepository, progress,
				new TransactionTemplate(transactionManager), authentication);
	}
}
