package org.molgenis.data.discovery.scoring.collections;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.OntologyTermRelated;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.service.OntologyService;
import org.slf4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public abstract class CollectionSimilarity
{
	protected BiobankUniverseRepository biobankUniverseRepository;
	protected OntologyService ontologyService;
	private final Logger log;

	private final static int DISTANCE = 5;

	private LoadingCache<OntologyTermRelated, Double> cachedOntologyTermSemanticRelateness = CacheBuilder.newBuilder()
			.maximumSize(2000).expireAfterWrite(1, TimeUnit.HOURS).build(new CacheLoader<OntologyTermRelated, Double>()
			{

				public Double load(OntologyTermRelated ontologyTermRelated)
				{
					if (ontologyService.related(ontologyTermRelated.getTarget(), ontologyTermRelated.getSource(),
							ontologyTermRelated.getStopLevel()))
					{
						return ontologyService.getOntologyTermSemanticRelatedness(ontologyTermRelated.getTarget(),
								ontologyTermRelated.getSource());
					}
					return 0.0d;
				}

			});

	public CollectionSimilarity(BiobankUniverseRepository biobankUniverseRepository, OntologyService ontologyService,
			Logger log)
	{
		this.biobankUniverseRepository = requireNonNull(biobankUniverseRepository);
		this.ontologyService = requireNonNull(ontologyService);
		this.log = requireNonNull(log);
	}

	public abstract List<CollectionSimilarityResult> score(List<BiobankSampleCollection> biobankSampleCollections,
			BiobankUniverse biobankUniverse);

	protected List<Hit<OntologyTermRelated>> findBestNeighbor(OntologyTerm targetOntologyTerm,
			Collection<OntologyTerm> allOntologyTerms)
	{
		List<Hit<OntologyTermRelated>> ontologyTermHits = new ArrayList<>();

		for (OntologyTerm sourceOntologyTerm : allOntologyTerms)
		{
			try
			{
				OntologyTermRelated create = OntologyTermRelated.create(targetOntologyTerm, sourceOntologyTerm,
						DISTANCE);

				Double relatedness = cachedOntologyTermSemanticRelateness.get(create);

				if (relatedness != 0)
				{
					ontologyTermHits.add(Hit.create(create, relatedness.floatValue()));
				}
			}
			catch (ExecutionException e)
			{
				log.error(e.getMessage());
			}
		}
		return ontologyTermHits;
		// Collections.sort(ontologyTermHits, Comparator.reverseOrder());
		//
		// return ontologyTermHits.isEmpty() ? null : ontologyTermHits.get(0);
	}

	protected Double getRelatedness(OntologyTerm targetOntologyTerm, OntologyTerm sourceOntologyTerm)
	{
		try
		{
			OntologyTermRelated create = OntologyTermRelated.create(targetOntologyTerm, sourceOntologyTerm, DISTANCE);
			return cachedOntologyTermSemanticRelateness.get(create);
		}
		catch (ExecutionException e)
		{
			log.error(e.getMessage());
		}

		return 0.0d;
	}

	protected List<OntologyTerm> getUniqueOntologyTerms(BiobankSampleCollection biobankSampleCollection,
			BiobankUniverse biobankUniverse)
	{
		List<SemanticType> semanticTypeFilter = biobankUniverse.getKeyConcepts();

		List<OntologyTerm> ontologyTerms = biobankUniverseRepository.getBiobankSampleAttributes(biobankSampleCollection)
				.stream().flatMap(attribute -> attribute.getTagGroups().stream())
				.flatMap(tag -> tag.getOntologyTerms().stream())
				.filter(ot -> ot.getSemanticTypes().stream().allMatch(st -> !semanticTypeFilter.contains(st)))
				.distinct().collect(Collectors.toList());

		return ontologyTerms;
	}

	protected List<OntologyTerm> getOntologyTerms(BiobankSampleCollection biobankSampleCollection,
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

	protected Map<OntologyTerm, Integer> getOntologyTermFrequency(BiobankSampleCollection biobankSampleCollection,
			BiobankUniverse biobankUniverse)
	{
		List<OntologyTerm> ontologyTerms = getOntologyTerms(biobankSampleCollection, biobankUniverse);

		Map<OntologyTerm, Integer> ontologyTermFrequency = ontologyTerms.stream().distinct()
				.collect(toMap(ot -> ot, ot -> 0));

		for (OntologyTerm ot : ontologyTerms)
		{
			ontologyTermFrequency.put(ot, ontologyTermFrequency.get(ot) + 1);
		}

		return ontologyTermFrequency;
	}
}
