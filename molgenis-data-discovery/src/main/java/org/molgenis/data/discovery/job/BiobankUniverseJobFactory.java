package org.molgenis.data.discovery.job;

import java.util.List;

import org.molgenis.data.EntityMetaData;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.discovery.model.BiobankUniverse;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.jobs.JobExecutionUpdater;
import org.molgenis.data.jobs.ProgressImpl;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.core.runas.RunAsSystem;
import org.molgenis.ui.menu.MenuReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.security.access.intercept.RunAsUserToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class BiobankUniverseJobFactory
{
	@Autowired
	private BiobankUniverseRepository biobankUniverseRepository;

	@Autowired
	private SemanticSearchService semanticSearchService;

	@Autowired
	private AttributeMappingExplainService attributeMappingExplainService;

	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JobExecutionUpdater jobExecutionUpdater;

	@Autowired
	private MailSender mailSender;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private MenuReaderService menuReaderService;

	@RunAsSystem
	public BiobankUniverseJobImpl create(BiobankUniverseJobExecution jobExecution)
	{
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		ProgressImpl progress = new ProgressImpl(jobExecution, jobExecutionUpdater, mailSender);

		String username = jobExecution.getUser().getUsername();

		RunAsUserToken runAsAuthentication = new RunAsUserToken("Job Execution", username, null,
				userDetailsService.loadUserByUsername(username).getAuthorities(), null);

		BiobankUniverse universe = jobExecution.getUniverse();
		List<EntityMetaData> members = jobExecution.getMembers();
		BiobankUniverseJobProcessor biobankUniverseJobProcessor = null;
		// BiobankUniverseJobProcessor biobankUniverseJobProcessor = new BiobankUniverseJobProcessor(universe, members,
		// biobankUniverseRepository, semanticSearchService, attributeMappingExplainService, ontologyService,
		// idGenerator, progress, menuReaderService);

		return new BiobankUniverseJobImpl(biobankUniverseJobProcessor, progress, transactionTemplate,
				runAsAuthentication);
	}
}
