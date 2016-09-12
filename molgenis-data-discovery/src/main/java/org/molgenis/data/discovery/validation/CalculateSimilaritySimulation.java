package org.molgenis.data.discovery.validation;

import java.util.List;

import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.springframework.scheduling.annotation.Async;

import com.google.common.collect.Multimap;

public interface CalculateSimilaritySimulation
{
	@Async
	public abstract void testCollectionSimilarity(BiobankSampleCollection target, List<BiobankSampleCollection> sources,
			Multimap<String, String> relevantMatches);
}
