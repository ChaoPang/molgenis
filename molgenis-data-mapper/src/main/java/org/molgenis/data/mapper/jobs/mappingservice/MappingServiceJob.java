package org.molgenis.data.mapper.jobs.mappingservice;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.jobs.Job;
import org.molgenis.data.jobs.Progress;
import org.molgenis.data.mapper.mapping.model.EntityMapping;
import org.molgenis.data.mapper.mapping.model.MappingProject;
import org.molgenis.data.mapper.repository.EntityMappingRepository;
import org.molgenis.data.mapper.service.AlgorithmService;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.meta.model.EntityType;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

public class MappingServiceJob extends Job<Void>
{
	private static final int PROGRESS_UPDATE_BATCH_SIZE = 5;

	private final MappingProject mappingProject;
	private final EntityType targetEntityMetaData;
	private final EntityType sourceEntityMetaData;
	private final AlgorithmService algorithmService;
	private final AtomicInteger counter;
	private final EntityMappingRepository entityMappingRepository;

	public MappingServiceJob(MappingProject mappingProject, EntityType targetEntityMetaData,
			EntityType sourceEntityMetaData, EntityMappingRepository entityMappingRepository,
			AlgorithmService algorithmService, Progress progress, TransactionTemplate transactionTemplate,
			Authentication authentication)
	{
		super(progress, transactionTemplate, authentication);
		this.mappingProject = requireNonNull(mappingProject);
		this.targetEntityMetaData = requireNonNull(targetEntityMetaData);
		this.sourceEntityMetaData = requireNonNull(sourceEntityMetaData);
		this.entityMappingRepository = requireNonNull(entityMappingRepository);
		this.algorithmService = requireNonNull(algorithmService);
		this.counter = new AtomicInteger(0);
	}

	@Override
	public Void call(Progress progress) throws Exception
	{
		int sizeOfTargetAttributes = Iterables.size(targetEntityMetaData.getAtomicAttributes());
		progress.setProgressMax(sizeOfTargetAttributes);

		EntityMapping mappingForSource = mappingProject.getMappingTarget(targetEntityMetaData.getId())
				.getMappingForSource(sourceEntityMetaData.getId());

		for (Attribute targetAttribute : targetEntityMetaData.getAtomicAttributes())
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

		entityMappingRepository.upsert(Arrays.asList(mappingForSource));

		progress.progress(sizeOfTargetAttributes, StringUtils.EMPTY);

		return null;
	}
}
