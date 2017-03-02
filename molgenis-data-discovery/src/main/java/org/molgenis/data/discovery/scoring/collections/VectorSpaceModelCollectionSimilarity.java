package org.molgenis.data.discovery.scoring.collections;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.biobank.BiobankUniverseMemberVector;
import org.molgenis.data.discovery.model.matching.BiobankSampleCollectionSimilarity;
import org.molgenis.data.discovery.model.matching.OntologyTermMatch;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.populate.IdGenerator;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.service.OntologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * This class creates the {@link BiobankUniverseMemberVector} representations for all the
 * {@link BiobankSampleCollection}s in the {@link BiobankUniverse}
 *
 * @author chaopang
 */
public class VectorSpaceModelCollectionSimilarity
{
	private final IdGenerator idGenerator;
	private final BiobankUniverseRepository biobankUniverseRepository;
	private final OntologyService ontologyService;
	private static final Logger LOG = LoggerFactory.getLogger(VectorSpaceModelCollectionSimilarity.class);

	final static int DISTANCE = 5;

	private LoadingCache<OntologyTermMatch, Double> cachedOntologyTermSemanticRelateness = CacheBuilder.newBuilder()
			.maximumSize(2000).expireAfterWrite(1, TimeUnit.HOURS).build(new CacheLoader<OntologyTermMatch, Double>()
			{
				public Double load(OntologyTermMatch ontologyTermMatch)
				{
					if (ontologyService.related(ontologyTermMatch.getTarget(), ontologyTermMatch.getSource(),
							ontologyTermMatch.getStopLevel()))
					{
						return ontologyService.getOntologyTermSemanticRelatedness(ontologyTermMatch.getTarget(),
								ontologyTermMatch.getSource());
					}
					return 0.0d;
				}
			});

	public VectorSpaceModelCollectionSimilarity(BiobankUniverseRepository biobankUniverseRepository,
			OntologyService ontologyService, IdGenerator idGenerator)
	{
		this.biobankUniverseRepository = requireNonNull(biobankUniverseRepository);
		this.ontologyService = requireNonNull(ontologyService);
		this.idGenerator = requireNonNull(idGenerator);
	}

	public List<BiobankUniverseMemberVector> createBiobankUniverseMemberVectors(BiobankUniverse biobankUniverse)
	{
		List<BiobankSampleCollection> biobankSampleCollections = biobankUniverse.getMembers();

		List<Map<OntologyTerm, Integer>> collect = biobankSampleCollections.stream()
				.map(collection -> getOntologyTermFrequency(collection, biobankUniverse)).collect(toList());

		List<OntologyTerm> uniqueOntologyTermList = collect.stream().flatMap(map -> map.keySet().stream()).distinct()
				.collect(toList());

		List<double[]> vectors = collect.stream()
				.map(ontologyTermFrequency -> createVector(ontologyTermFrequency, uniqueOntologyTermList))
				.collect(toList());

		List<BiobankUniverseMemberVector> biobankSampleCollectionVectors = biobankSampleCollections.stream()
				.map(biobankSampleCollections::indexOf).map(index -> BiobankUniverseMemberVector
						.create(idGenerator.generateId(), biobankSampleCollections.get(index), vectors.get(index)))
				.collect(toList());

		return biobankSampleCollectionVectors;
	}

	/**
	 * Compute the cosine angle between two {@link BiobankUniverseMemberVector}s
	 *
	 * @param biobankUniverseMemberVectorOne
	 * @param biobankUniverseMemberVectorTwo
	 * @return cosine angle
	 */
	public BiobankSampleCollectionSimilarity cosineValue(BiobankUniverseMemberVector biobankUniverseMemberVectorOne,
			BiobankUniverseMemberVector biobankUniverseMemberVectorTwo)
	{
		double[] vectorOne = biobankUniverseMemberVectorOne.getPoint();

		double[] vectorTwo = biobankUniverseMemberVectorTwo.getPoint();

		float cosineSimilarity = 0.0f;

		String label = EMPTY;

		if (vectorOne.length == vectorTwo.length)
		{

			for (int i = 0; i < vectorOne.length; i++)
			{
				cosineSimilarity += vectorOne[i] * vectorTwo[i];
			}

			cosineSimilarity = (cosineSimilarity / (euclideanNorms(vectorOne) * euclideanNorms(vectorTwo)));
			label = Math.round(cosineSimilarity * 100) + "%";

			return BiobankSampleCollectionSimilarity.create(biobankUniverseMemberVectorOne.getBiobankSampleCollection(),
					biobankUniverseMemberVectorTwo.getBiobankSampleCollection(), cosineSimilarity, label);
		}
		return BiobankSampleCollectionSimilarity.create(biobankUniverseMemberVectorOne.getBiobankSampleCollection(),
				biobankUniverseMemberVectorTwo.getBiobankSampleCollection(), cosineSimilarity, label);
	}

	double[] createVector(Map<OntologyTerm, Integer> targetOntologyTermFrequency,
			List<OntologyTerm> uniqueOntologyTermList)
	{
		Set<OntologyTerm> uniqueTargetOntologyTerms = targetOntologyTermFrequency.keySet();

		// For the unmatched ontology terms, we try to pair them with the closest neighbor in the ontology structure
		List<Hit<OntologyTermMatch>> relatedOntologyTerms = uniqueTargetOntologyTerms.stream()
				.flatMap(ot -> findRelatedOntologyTerms(ot, uniqueOntologyTermList).stream()).collect(toList());

		double[] vector = new double[uniqueOntologyTermList.size()];

		for (Hit<OntologyTermMatch> relatedOntologyTermHit : relatedOntologyTerms)
		{
			OntologyTermMatch ontologyTermMatch = relatedOntologyTermHit.getResult();
			OntologyTerm sourceOntologyTerm = ontologyTermMatch.getSource();
			int index = uniqueOntologyTermList.indexOf(sourceOntologyTerm);
			vector[index] = vector[index] + relatedOntologyTermHit.getScore();
		}

		return vector;
	}

	private float euclideanNorms(double[] vector)
	{
		double sum = DoubleStream.of(vector).map(f -> Math.pow(f, 2.0)).sum();
		return (float) Math.sqrt(sum);
	}

	private List<Hit<OntologyTermMatch>> findRelatedOntologyTerms(OntologyTerm targetOntologyTerm,
			Collection<OntologyTerm> allOntologyTerms)
	{
		List<Hit<OntologyTermMatch>> ontologyTermHits = new ArrayList<>();

		for (OntologyTerm sourceOntologyTerm : allOntologyTerms)
		{
			try
			{
				OntologyTermMatch create = OntologyTermMatch
						.create(targetOntologyTerm, sourceOntologyTerm, DISTANCE);

				Double relatedness = cachedOntologyTermSemanticRelateness.get(create);

				if (relatedness != 0)
				{
					ontologyTermHits.add(Hit.create(create, relatedness.floatValue()));
				}
			}
			catch (ExecutionException e)
			{
				LOG.error(e.getMessage());
			}
		}
		return ontologyTermHits;
	}

	private List<OntologyTerm> getAllOntologyTerms(BiobankSampleCollection biobankSampleCollection,
			BiobankUniverse biobankUniverse)
	{
		List<SemanticType> semanticTypeFilter = biobankUniverse.getKeyConcepts();

		List<OntologyTerm> ontologyTerms = biobankUniverseRepository.getBiobankSampleAttributes(biobankSampleCollection)
				.stream().flatMap(attribute -> attribute.getTagGroups().stream())
				.flatMap(tag -> tag.getOntologyTerms().stream().distinct())
				.filter(ot -> ot.getSemanticTypes().stream().allMatch(st -> !semanticTypeFilter.contains(st)))
				.collect(Collectors.toList());

		return ontologyTerms;
	}

	private Map<OntologyTerm, Integer> getOntologyTermFrequency(BiobankSampleCollection biobankSampleCollection,
			BiobankUniverse biobankUniverse)
	{
		List<OntologyTerm> ontologyTermImpls = getAllOntologyTerms(biobankSampleCollection, biobankUniverse);

		Map<OntologyTerm, Integer> ontologyTermFrequency = ontologyTermImpls.stream().distinct()
				.collect(toMap(ot -> ot, ot -> 0));

		for (OntologyTerm ot : ontologyTermImpls)
		{
			ontologyTermFrequency.put(ot, ontologyTermFrequency.get(ot) + 1);
		}

		return ontologyTermFrequency;
	}
}
