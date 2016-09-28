package org.molgenis.data.discovery.scoring.collections;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.OntologyTermRelated;
import org.molgenis.ontology.core.service.OntologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VectorSpaceModelCollectionSimilarity extends CollectionSimilarity
{
	private static final Logger LOG = LoggerFactory.getLogger(VectorSpaceModelCollectionSimilarity.class);

	public VectorSpaceModelCollectionSimilarity(BiobankUniverseRepository biobankUniverseRepository,
			OntologyService ontologyService)
	{
		super(biobankUniverseRepository, ontologyService, LOG);
	}

	@Override
	public List<CollectionSimilarityResult> score(List<BiobankSampleCollection> biobankSampleCollections,
			BiobankUniverse biobankUniverse)
	{
		List<Map<OntologyTerm, Integer>> collect = biobankSampleCollections.stream()
				.map(collection -> getOntologyTermFrequency(collection, biobankUniverse)).collect(Collectors.toList());

		List<OntologyTerm> uniqueOntologyTermList = collect.stream().flatMap(map -> map.keySet().stream()).distinct()
				.collect(Collectors.toList());

		List<double[]> vectors = collect.stream()
				.map(ontologyTermFrequency -> createVector(ontologyTermFrequency, uniqueOntologyTermList))
				.collect(Collectors.toList());

		List<BiobankSampleCollectionVector> biobankSampleCollectionVectors = biobankSampleCollections.stream()
				.map(biobankSampleCollections::indexOf)
				.map(index -> new BiobankSampleCollectionVector(biobankSampleCollections.get(index), vectors.get(index),
						collect.get(index).keySet().size()))
				.collect(toList());

		for (BiobankSampleCollectionVector biobankSampleCollectionVector : biobankSampleCollectionVectors)
		{
			System.out.println(biobankSampleCollectionVector.getBiobankSampleCollection().getName() + ":"
					+ Arrays.toString(biobankSampleCollectionVector.getPoint()));
		}

		// DBSCANClusterer<BiobankSampleCollectionVector> clusterer = new DBSCANClusterer<>(0.05, 2);
		//
		// List<Cluster<BiobankSampleCollectionVector>> clusters = clusterer.cluster(biobankSampleCollectionVectors);
		//
		// for (Cluster<BiobankSampleCollectionVector> cluster : clusters)
		// {
		// List<BiobankSampleCollectionVector> points = cluster.getPoints();
		// }

		List<CollectionSimilarityResult> similarities = new ArrayList<>();

		for (int i = 0; i < biobankSampleCollectionVectors.size(); i++)
		{
			BiobankSampleCollectionVector vectorOne = biobankSampleCollectionVectors.get(i);
			for (int j = i + 1; j < biobankSampleCollectionVectors.size(); j++)
			{
				BiobankSampleCollectionVector vectorTwo = biobankSampleCollectionVectors.get(j);

				BiobankSampleCollection target = vectorOne.getBiobankSampleCollection();
				BiobankSampleCollection source = vectorTwo.getBiobankSampleCollection();
				float similarity = cosineValue(vectorOne.getPoint(), vectorTwo.getPoint());
				int coverage = (int) Math.sqrt(vectorOne.getCoverage() * vectorTwo.getCoverage());
				similarities.add(CollectionSimilarityResult.create(target, source, similarity, coverage));
			}
		}

		return similarities;
	}

	private float cosineValue(double[] vectorOne, double[] vectorTwo)
	{
		double docProduct = 0.0;

		if (vectorOne.length != vectorTwo.length) return 0;

		for (int i = 0; i < vectorOne.length; i++)
		{
			docProduct += vectorOne[i] * vectorTwo[i];
		}

		return (float) (docProduct / (euclideanNorms(vectorOne) * euclideanNorms(vectorTwo)));
	}

	private double euclideanNorms(double[] vector)
	{
		double sum = DoubleStream.of(vector).map(f -> Math.pow(f, 2.0)).sum();
		return Math.sqrt(sum);
	}

	private double[] createVector(Map<OntologyTerm, Integer> targetOntologyTermFrequency,
			List<OntologyTerm> uniqueOntologyTermList)
	{
		Set<OntologyTerm> uniqueTargetOntologyTerms = targetOntologyTermFrequency.keySet();

		// For the unmatched ontology terms, we try to pair them with the closest neighbor in the ontology structure
		List<Hit<OntologyTermRelated>> relatedOntologyTerms = uniqueTargetOntologyTerms.stream()
				.flatMap(ot -> findBestNeighbor(ot, uniqueOntologyTermList).stream()).collect(Collectors.toList());

		double[] vector = new double[uniqueOntologyTermList.size()];

		for (Hit<OntologyTermRelated> relatedOntologyTermHit : relatedOntologyTerms)
		{
			OntologyTermRelated ontologyTermRelated = relatedOntologyTermHit.getResult();
			OntologyTerm sourceOntologyTerm = ontologyTermRelated.getSource();
			int index = uniqueOntologyTermList.indexOf(sourceOntologyTerm);
			vector[index] = vector[index] + relatedOntologyTermHit.getScore();
		}

		return vector;
	}
}