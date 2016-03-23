package org.molgenis.data.mapper.jobs;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.jobs.Job;
import org.molgenis.data.jobs.Progress;
import org.molgenis.data.mapper.mapping.model.EntityMapping;
import org.molgenis.data.mapper.mapping.model.MappingProject;
import org.molgenis.data.mapper.service.AlgorithmService;
import org.molgenis.data.mapper.service.MappingService;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.Iterables;

import static java.util.Objects.requireNonNull;

public class MappingServiceJob extends Job<Void>
{
	private static final int PROGRESS_UPDATE_BATCH_SIZE = 50;

	private final MappingProject mappingProject;
	private final EntityMetaData targetEntityMetaData;
	private final EntityMetaData sourceEntityMetaData;
	private final AlgorithmService algorithmService;
	private final MappingService mappingService;
	private final AtomicInteger counter;

	public MappingServiceJob(MappingProject mappingProject, EntityMetaData targetEntityMetaData,
			EntityMetaData sourceEntityMetaData, MappingService mappingService, AlgorithmService algorithmService,
			Progress progress, TransactionTemplate transactionTemplate, Authentication authentication)
	{
		super(progress, transactionTemplate, authentication);
		this.mappingProject = requireNonNull(mappingProject);
		this.targetEntityMetaData = requireNonNull(targetEntityMetaData);
		this.sourceEntityMetaData = requireNonNull(sourceEntityMetaData);
		this.mappingService = requireNonNull(mappingService);
		this.algorithmService = requireNonNull(algorithmService);
		this.counter = new AtomicInteger(0);
	}

	@Override
	public Void call(Progress progress) throws Exception
	{
		int sizeOfTargetAttributes = Iterables.size(targetEntityMetaData.getAtomicAttributes());
		progress.setProgressMax(sizeOfTargetAttributes);

		EntityMapping mappingForSource = mappingProject.getMappingTarget(targetEntityMetaData.getName())
				.getMappingForSource(sourceEntityMetaData.getName());

		for (AttributeMetaData targetAttribute : targetEntityMetaData.getAtomicAttributes())
		{
			algorithmService.autoGenerateAlgorithm(sourceEntityMetaData, targetEntityMetaData, mappingForSource,
					targetAttribute);

			// Increase the number of the progress
			counter.incrementAndGet();

			// Update the progress only when the progress proceeds the threshold
			if (counter.get() % PROGRESS_UPDATE_BATCH_SIZE == 0)
			{
				progress.progress(counter.get(), StringUtils.EMPTY);
			}
		}
		mappingService.updateMappingProject(mappingProject);

		progress.progress(sizeOfTargetAttributes, StringUtils.EMPTY);

		return null;
	}
}
