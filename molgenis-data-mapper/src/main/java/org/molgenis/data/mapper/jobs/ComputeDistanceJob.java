package org.molgenis.data.mapper.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.jobs.Job;
import org.molgenis.data.jobs.Progress;
import org.molgenis.data.mapper.mapping.model.AttributeMapping;
import org.molgenis.data.mapper.mapping.model.AttributeMapping.AlgorithmState;
import org.molgenis.data.mapper.repository.AttributeMappingRepository;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.DistanceMetric;
import org.molgenis.data.semanticsearch.service.impl.OntologyTermSemanticSearchImpl;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.Iterables;

import static java.util.Objects.requireNonNull;

public class ComputeDistanceJob extends Job<Void>
{
	private static final int BATCH_SIZE = 50;
	private static final double HIGH_QUALITY_THRESHOLD = 60.0;

	private final EntityMetaData targetEntityMetaData;
	private final EntityMetaData sourceEntityMetaData;
	private final OntologyTermSemanticSearchImpl ontologyTermBasedSemanticSearch;
	private final SemanticSearchService semanticSearchService;
	private final AttributeMappingRepository attributeMappingRepository;
	private final AtomicInteger counter;

	public ComputeDistanceJob(EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData,
			OntologyTermSemanticSearchImpl ontologyTermBasedSemanticSearch, SemanticSearchService semanticSearchService,
			AttributeMappingRepository attributeMappingRepository, Progress progress,
			TransactionTemplate transactionTemplate, Authentication authentication)
	{
		super(progress, transactionTemplate, authentication);
		this.targetEntityMetaData = requireNonNull(targetEntityMetaData);
		this.sourceEntityMetaData = requireNonNull(sourceEntityMetaData);
		this.ontologyTermBasedSemanticSearch = requireNonNull(ontologyTermBasedSemanticSearch);
		this.semanticSearchService = requireNonNull(semanticSearchService);
		this.attributeMappingRepository = requireNonNull(attributeMappingRepository);
		this.counter = new AtomicInteger(0);
	}

	@Override
	public Void call(Progress progress) throws Exception
	{
		progress.setProgressMax(Iterables.size(targetEntityMetaData.getAtomicAttributes()));

		List<AttributeMapping> mappings = new ArrayList<>();

		for (AttributeMetaData targetAttribute : targetEntityMetaData.getAtomicAttributes())
		{
			DistanceMetric bestMatch = getBestMatch(targetAttribute);

			AttributeMetaData sourceAttribute = bestMatch.getSourceAttribute();

			if (bestMatch.isValid())
			{
				AttributeMapping attributeMapping = new AttributeMapping(targetAttribute);
				attributeMapping.setAlgorithm("$(" + sourceAttribute.getName() + ").value();");
				attributeMapping.setSimilarity(bestMatch.getLogDistance());
				attributeMapping.setAlgorithmState(bestMatch.getLogDistance() > HIGH_QUALITY_THRESHOLD
						? AlgorithmState.GENERATED_HIGH : AlgorithmState.GENERATED_LOW);
				mappings.add(attributeMapping);
			}

			if (counter.incrementAndGet() % BATCH_SIZE == 0)
			{
				progress.progress(counter.get(), StringUtils.EMPTY);
			}
		}

		attributeMappingRepository.upsert(mappings);

		progress.progress(counter.get(), StringUtils.EMPTY);

		return null;
	}

	private DistanceMetric getBestMatch(AttributeMetaData targetAttribute) throws ExecutionException
	{
		List<DistanceMetric> topTenMatches = new ArrayList<>();
		for (AttributeMetaData sourceAttribute : semanticSearchService.findAttributes(targetAttribute,
				targetEntityMetaData, sourceEntityMetaData, null))
		{
			if (topTenMatches.size() < 0)
			{
				topTenMatches.add(ontologyTermBasedSemanticSearch.getAttrDistance(targetAttribute, sourceAttribute,
						targetEntityMetaData, sourceEntityMetaData));
			}
		}
		Collections.sort(topTenMatches);
		return topTenMatches.size() > 0 ? topTenMatches.get(0) : null;
	}
}
