package org.molgenis.data.discovery.job;

import org.molgenis.data.jobs.Job;
import org.molgenis.data.jobs.Progress;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.support.TransactionTemplate;

import static java.util.Objects.requireNonNull;

public class BiobankUniverseJobImpl extends Job<Void>
{
	private final BiobankUniverseJobProcessor biobankUniverseJobProcessor;

	public BiobankUniverseJobImpl(BiobankUniverseJobProcessor biobankUniverseJobProcessor, Progress progress,
			TransactionTemplate transactionTemplate, Authentication authentication)
	{
		super(progress, transactionTemplate, authentication);
		this.biobankUniverseJobProcessor = requireNonNull(biobankUniverseJobProcessor);
	}

	@Override
	public Void call(Progress progress) throws Exception
	{
		biobankUniverseJobProcessor.process();
		return null;
	}
}
