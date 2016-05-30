package org.molgenis.data.discovery.job;

import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.jobs.JobExecutionUpdater;
import org.molgenis.data.jobs.ProgressImpl;
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
	private BiobankUniverseService biobankUniverseService;

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
	public BiobankUniverseJobImpl create(BiobankUniverseJobExecution biobankUniverseJobExecution)
	{
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		ProgressImpl progress = new ProgressImpl(biobankUniverseJobExecution, jobExecutionUpdater, mailSender);
		String username = biobankUniverseJobExecution.getUser().getUsername();
		RunAsUserToken runAsAuthentication = new RunAsUserToken("Job Execution", username, null,
				userDetailsService.loadUserByUsername(username).getAuthorities(), null);

		BiobankUniverseJobProcessor biobankUniverseJobProcessor = new BiobankUniverseJobProcessor(
				biobankUniverseJobExecution.getUniverse(), biobankUniverseJobExecution.getMembers(),
				biobankUniverseService, biobankUniverseRepository, progress, menuReaderService);

		return new BiobankUniverseJobImpl(biobankUniverseJobProcessor, progress, transactionTemplate,
				runAsAuthentication);
	}
}
