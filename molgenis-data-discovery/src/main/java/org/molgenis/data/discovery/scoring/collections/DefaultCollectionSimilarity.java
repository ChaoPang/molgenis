package org.molgenis.data.discovery.scoring.collections;

import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultCollectionSimilarity extends CollectionSimilarity
{
	private static final Logger LOG = LoggerFactory.getLogger(DefaultCollectionSimilarity.class);

	public DefaultCollectionSimilarity(BiobankUniverseRepository biobankUniverseRepository,
			OntologyService ontologyService)
	{
		super(biobankUniverseRepository, ontologyService, LOG);
	}

	@Override
	public List<CollectionSimilarityResult> score(List<BiobankSampleCollection> biobankSampleCollections,
			BiobankUniverse biobankUniverse)
	{
		List<CollectionSimilarityResult> collectionSimilarityResults = new ArrayList<>();

		List<Map<OntologyTerm, Integer>> ontologyTermFrequencyList = biobankSampleCollections.stream()
				.map(collection -> getOntologyTermFrequency(collection, biobankUniverse)).collect(Collectors.toList());

		for (int i = 0; i < ontologyTermFrequencyList.size(); i++)
		{
			BiobankSampleCollection target = biobankSampleCollections.get(i);
			Map<OntologyTerm, Integer> targetOntologyTermFrequency = ontologyTermFrequencyList.get(i);
			int targetTotal = targetOntologyTermFrequency.keySet().size();

			for (int j = i + 1; j < ontologyTermFrequencyList.size(); j++)
			{
				BiobankSampleCollection source = biobankSampleCollections.get(j);
				Map<OntologyTerm, Integer> sourceOntologyTermFrequency = ontologyTermFrequencyList.get(j);
				int sourceTotal = sourceOntologyTermFrequency.keySet().size();
				float similarity = computeSimilarity(targetOntologyTermFrequency, sourceOntologyTermFrequency);
				int coverage = (int) sqrt(targetTotal * sourceTotal);
				collectionSimilarityResults
						.add(CollectionSimilarityResult.create(target, source, similarity, coverage));
			}
		}

		return collectionSimilarityResults;
	}

	private float computeSimilarity(Map<OntologyTerm, Integer> targetOntologyTermFrequency,
			Map<OntologyTerm, Integer> sourceOntologyTermFrequency)
	{
		double similarity = 0;

		double base = Math.sqrt(targetOntologyTermFrequency.values().stream().mapToInt(Integer::valueOf).sum()
				* sourceOntologyTermFrequency.values().stream().mapToInt(Integer::valueOf).sum());

		for (Entry<OntologyTerm, Integer> targetEntry : targetOntologyTermFrequency.entrySet())
		{
			OntologyTerm targetOntologyTerm = targetEntry.getKey();

			Integer targetFrequency = targetEntry.getValue();

			for (Entry<OntologyTerm, Integer> sourceEntry : sourceOntologyTermFrequency.entrySet())
			{
				OntologyTerm sourceOntologyTerm = sourceEntry.getKey();

				Integer sourceFrequency = sourceEntry.getValue();

				Double relatedNess = getRelatedness(targetOntologyTerm, sourceOntologyTerm);

				similarity += relatedNess * targetFrequency * sourceFrequency;
			}
		}

		return (float) (similarity / base);
	}
}
