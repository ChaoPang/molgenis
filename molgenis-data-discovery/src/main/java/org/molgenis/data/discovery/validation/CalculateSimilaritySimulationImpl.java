package org.molgenis.data.discovery.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.model.matching.MatchingExplanation;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.discovery.service.impl.OntologyBasedMatcher;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParam;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.core.runas.RunAsSystem;
import org.springframework.scheduling.annotation.Async;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class CalculateSimilaritySimulationImpl implements CalculateSimilaritySimulation
{
	private final OntologyService ontologyService;
	private final BiobankUniverseService biobankUniverseService;
	private final BiobankUniverseRepository biobankUniverseRepository;
	private final QueryExpansionService queryExpansionService;

	public CalculateSimilaritySimulationImpl(OntologyService ontologyService,
			BiobankUniverseService biobankUniverseService, BiobankUniverseRepository biobankUniverseRepository,
			QueryExpansionService queryExpansionService)
	{
		this.ontologyService = Objects.requireNonNull(ontologyService);
		this.biobankUniverseService = Objects.requireNonNull(biobankUniverseService);
		this.biobankUniverseRepository = Objects.requireNonNull(biobankUniverseRepository);
		this.queryExpansionService = Objects.requireNonNull(queryExpansionService);
	}

	List<String> getRandaomIds(List<String> ids, int size)
	{
		int total = ids.size();
		List<String> randomIds = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
		{
			int index = (int) Math.floor(Math.random() * total);
			String randomid = ids.get(index);
			if (!randomIds.contains(randomid)) randomIds.add(randomid);
		}
		return randomIds;
	}

	@Async
	@Override
	@RunAsSystem
	public void testCollectionSimilarity(BiobankSampleCollection target, List<BiobankSampleCollection> sources,
			Multimap<String, String> relevantMatches)
	{
		List<BiobankSampleAttribute> targetAttributes = StreamSupport
				.stream(biobankUniverseService.getBiobankSampleAttributes(target).spliterator(), false)
				.collect(Collectors.toList());

		List<BiobankSampleAttribute> sourceAttributes = sources.stream()
				.flatMap(source -> StreamSupport
						.stream(biobankUniverseService.getBiobankSampleAttributes(source).spliterator(), false))
				.collect(Collectors.toList());

		Map<String, Double> cachedRelatedness = new HashMap<>();

		Map<String, Boolean> cachedRelationships = new HashMap<>();

		// System.out.println("OntologyTerm experiments");
		// CsvRepository csvRepository = new CsvRepository(
		// "/Users/chaopang/Desktop/BiobankUniverse_HOP_evaluation/queryExpansion_ontologyExplainApi/test_new_algorithm/ontologyTerm_ids.csv");
		//
		// List<String> collect = StreamSupport.stream(csvRepository.spliterator(), false)
		// .map(entity -> entity.getString("id")).collect(Collectors.toList());
		//
		// for (int i = 0; i < 100; i++)
		// {
		// List<OntologyTerm> ontologyTerms1 = ontologyService.getRandomontologyTerms(getRandaomIds(collect, 1000));
		//
		// List<OntologyTerm> ontologyTerms2 = ontologyService.getRandomontologyTerms(getRandaomIds(collect, 1000));
		//
		// int tagSize = (int) Math.sqrt(
		// Math.sqrt(ontologyTerms1.stream().distinct().count() * ontologyTerms2.stream().distinct().count()));
		//
		// double similarity = calculateSimilarityBasedonTags(ontologyTerms1, ontologyTerms2, cachedRelatedness);
		//
		// System.out.format("Experiment: %d;%d;%.4f%n", i + 1, tagSize, similarity);
		// }

		System.out.println("Experiment 1: random matches only");

		for (int i = 0; i < 300; i++)
		{
			int targetTotal = (int) Math.floor(Math.random() * targetAttributes.size());

			int sourceTotal = (int) Math.floor(Math.random() * sourceAttributes.size());

			int population = (int) Math.sqrt(targetTotal * sourceTotal);

			List<BiobankSampleAttribute> randomTargetAttributes = randomizeSlicedAttributes(targetAttributes,
					targetTotal);

			List<BiobankSampleAttribute> randomSourceAttributes = randomizeSlicedAttributes(sourceAttributes,
					sourceTotal);

			int numberOfMatches = getNumberOfMatches(randomTargetAttributes, randomSourceAttributes, relevantMatches);

			int tagSize = (int) Math.sqrt(Math.sqrt(
					getNumberOfUniqueTags(randomTargetAttributes) * getNumberOfUniqueTags(randomSourceAttributes)));

			double similarity = population == 0 ? 0 : calculateSimilarityBasedonTags(randomTargetAttributes,
					randomSourceAttributes, cachedRelatedness, cachedRelationships);

			System.out.format("Experiment: %d;%d;%d;%d;%.4f%n", i + 1, population, tagSize, numberOfMatches,
					similarity);
		}

		for (int i = 0; i < 200; i++)
		{
			int targetTotal = (int) Math.floor(Math.random() * targetAttributes.size());

			List<BiobankSampleAttribute> randomTargetAttributes = randomizeSlicedAttributes(targetAttributes,
					targetTotal);

			Set<String> collect = randomTargetAttributes.stream().map(BiobankSampleAttribute::getName)
					.filter(relevantMatches::containsKey)
					.flatMap(targetAttributeName -> relevantMatches.get(targetAttributeName).stream())
					.collect(Collectors.toSet());

			List<BiobankSampleAttribute> validSourceAttributes = sourceAttributes.stream()
					.filter(sourceAttribute -> !collect.contains(sourceAttribute.getName()))
					.collect(Collectors.toList());

			int sourceTotal = (int) Math.floor(Math.random() * validSourceAttributes.size());

			List<BiobankSampleAttribute> randomSourceAttributes = randomizeSlicedAttributes(validSourceAttributes,
					sourceTotal);

			int population = (int) Math.sqrt(randomTargetAttributes.size() * randomSourceAttributes.size());

			double similarity = population == 0 ? 0 : calculateSimilarityBasedonTags(randomTargetAttributes,
					randomSourceAttributes, cachedRelatedness, cachedRelationships);

			int tagSize = (int) Math.sqrt(Math.sqrt(
					getNumberOfUniqueTags(randomTargetAttributes) * getNumberOfUniqueTags(randomSourceAttributes)));

			System.out.format("Experiment: %d;%d;%d;%d;%.4f%n", i + 1, population, tagSize, 0, similarity);
		}

		System.out.println("\nExperiment 2: true matches only");

		for (int i = 0; i < 1000; i++)
		{
			int size = i < 500 ? 0 : ((int) Math.floor(Math.random() * relevantMatches.size()) + 1);

			Multimap<String, String> randomizeSlicedMatches = randomizeSlicedMatches(relevantMatches, size);

			List<BiobankSampleAttribute> randomTargetAttributes = targetAttributes.stream()
					.filter(targetAttribute -> randomizeSlicedMatches.containsKey(targetAttribute.getName()))
					.collect(Collectors.toList());

			List<BiobankSampleAttribute> randomSourceAttributes = sourceAttributes.stream()
					.filter(sourceAttribute -> randomizeSlicedMatches.containsValue(sourceAttribute.getName()))
					.collect(Collectors.toList());

			int targetTotal = (int) Math.floor(Math.random() * targetAttributes.size());

			List<BiobankSampleAttribute> randomTargetAttributes2 = randomizeSlicedAttributes(targetAttributes,
					targetTotal);

			Set<String> collect = randomTargetAttributes2.stream().map(BiobankSampleAttribute::getName)
					.filter(relevantMatches::containsKey)
					.flatMap(targetAttributeName -> relevantMatches.get(targetAttributeName).stream())
					.collect(Collectors.toSet());

			collect.addAll(
					randomSourceAttributes.stream().map(BiobankSampleAttribute::getName).collect(Collectors.toSet()));

			List<BiobankSampleAttribute> validSourceAttributes = sourceAttributes.stream()
					.filter(sourceAttribute -> !collect.contains(sourceAttribute.getName()))
					.collect(Collectors.toList());

			int sourceTotal = (int) Math.floor(Math.random() * validSourceAttributes.size());

			List<BiobankSampleAttribute> randomSourceAttributes2 = randomizeSlicedAttributes(validSourceAttributes,
					sourceTotal);

			randomTargetAttributes.addAll(randomTargetAttributes2);

			randomSourceAttributes.addAll(randomSourceAttributes2);

			randomTargetAttributes = randomTargetAttributes.stream().distinct().collect(Collectors.toList());

			randomSourceAttributes = randomSourceAttributes.stream().distinct().collect(Collectors.toList());

			int numberOfMatches = getNumberOfMatches(randomTargetAttributes, randomSourceAttributes, relevantMatches);

			int population = (int) Math.sqrt(randomTargetAttributes.size() * randomSourceAttributes.size());

			double similarity = population == 0 ? 0 : calculateSimilarityBasedonTags(randomTargetAttributes,
					randomSourceAttributes, cachedRelatedness, cachedRelationships);

			int tagSize = (int) Math.sqrt(Math.sqrt(
					getNumberOfUniqueTags(randomTargetAttributes) * getNumberOfUniqueTags(randomSourceAttributes)));

			System.out.format("Experiment: %d;%d;%d;%d;%.4f%n", i, population, tagSize, numberOfMatches, similarity);
		}
	}

	double calculateSimilarityBasedonMatches(List<BiobankSampleAttribute> randomTargetAttributes,
			List<BiobankSampleAttribute> randomSourceAttributes, Map<String, Double> cachedRelatedness)
	{
		OntologyBasedMatcher matcher = new OntologyBasedMatcher(randomSourceAttributes, biobankUniverseRepository,
				queryExpansionService, ontologyService);
		BiobankUniverse biobankUniverse = BiobankUniverse.create("1", "test", Collections.emptyList(),
				new MolgenisUser(), Collections.emptyList());

		double similarity = 0;

		for (BiobankSampleAttribute targetAttribute : randomTargetAttributes)
		{
			SemanticSearchParam semanticSearchParam = SemanticSearchParam.create(
					Sets.newHashSet(targetAttribute.getLabel()), Collections.emptyList(),
					QueryExpansionParam.create(true, false), true);

			List<AttributeMappingCandidate> findCandidateMappingsOntologyBased = biobankUniverseService
					.generateAttributeCandidateMappings(biobankUniverse, targetAttribute, semanticSearchParam,
							Lists.newArrayList(matcher));

			double sum = findCandidateMappingsOntologyBased.stream().sorted()
					.map(AttributeMappingCandidate::getExplanation).map(MatchingExplanation::getNgramScore)
					.mapToDouble(Double::valueOf).sum();
			similarity += sum;
		}
		return similarity;
	}

	Map<OntologyTerm, Integer> getOntologyTermFrequency(List<OntologyTerm> ontologyTerms)
	{
		Map<OntologyTerm, Integer> ontologyTermFrequency = new HashMap<>();

		ontologyTerms.stream().forEach(ot -> {

			if (!ontologyTermFrequency.containsKey(ot))
			{
				ontologyTermFrequency.put(ot, 0);
			}

			ontologyTermFrequency.put(ot, ontologyTermFrequency.get(ot) + 1);
		});

		return ontologyTermFrequency;
	}

	int getNumberOfUniqueTags(List<BiobankSampleAttribute> attributes)
	{
		return (int) attributes.stream().flatMap(attribute -> attribute.getTagGroups().stream())
				.flatMap(tag -> tag.getOntologyTerms().stream()).distinct().count();
	}

	// TODO: maybe remove this?
	List<OntologyTerm> removeParentOntologyTerms(List<IdentifiableTagGroup> tagGroups,
			Map<String, Boolean> cachedRelationships)
	{
		List<OntologyTerm> uniqueOntologyTerms = tagGroups.stream().flatMap(tag -> tag.getOntologyTerms().stream())
				.distinct().collect(Collectors.toList());

		OntologyTerm[] array = uniqueOntologyTerms.stream().toArray(OntologyTerm[]::new);

		for (int i = 0; i < array.length; i++)
		{
			OntologyTerm ot1 = array[i];

			for (int j = i + 1; j < array.length; j++)
			{
				OntologyTerm ot2 = array[j];

				String identifier1 = ot1.getIRI() + ot2.getIRI();
				String identifier2 = ot2.getIRI() + ot1.getIRI();

				if (cachedRelationships.containsKey(identifier1))
				{
					if (cachedRelationships.get(identifier1)) uniqueOntologyTerms.remove(ot1);
				}
				else if (cachedRelationships.containsKey(identifier2))
				{
					if (cachedRelationships.get(identifier2)) uniqueOntologyTerms.remove(ot2);
				}
				else
				{
					if (ontologyService.isDescendant(ot1, ot2))
					{
						uniqueOntologyTerms.remove(ot2);

						cachedRelationships.put(identifier2, true);
					}
					else
					{
						cachedRelationships.put(identifier2, false);
					}

					if (ontologyService.isDescendant(ot2, ot1))
					{
						uniqueOntologyTerms.remove(ot1);

						cachedRelationships.put(identifier1, true);
					}
					else
					{
						cachedRelationships.put(identifier1, false);
					}
				}
			}
		}

		return uniqueOntologyTerms;

	}

	double calculateSimilarityBasedonTags(List<OntologyTerm> targetOntologyTerms,
			List<OntologyTerm> sourceOntologyTerms, Map<String, Double> cachedRelatedness)
	{
		Map<OntologyTerm, Integer> targetOntologyTermFrequency = getOntologyTermFrequency(targetOntologyTerms);

		Map<OntologyTerm, Integer> sourceOntologyTermFrequency = getOntologyTermFrequency(sourceOntologyTerms);

		double similarity = 0;

		double base = Math.sqrt(targetOntologyTerms.size() * sourceOntologyTerms.size());

		for (Entry<OntologyTerm, Integer> targetEntry : targetOntologyTermFrequency.entrySet())
		{
			OntologyTerm targetOntologyTerm = targetEntry.getKey();

			Integer targetFrequency = targetEntry.getValue();

			for (Entry<OntologyTerm, Integer> sourceEntry : sourceOntologyTermFrequency.entrySet())
			{
				OntologyTerm sourceOntologyTerm = sourceEntry.getKey();

				Integer sourceFrequency = sourceEntry.getValue();

				String identifier = targetOntologyTerm.getIRI() + sourceOntologyTerm.getIRI();

				if (cachedRelatedness.containsKey(identifier))
				{
					similarity += cachedRelatedness.get(identifier) * targetFrequency * sourceFrequency;
				}
				else
				{
					if (ontologyService.related(targetOntologyTerm, sourceOntologyTerm,
							OntologyBasedMatcher.STOP_LEVEL))
					{
						Double ontologyTermSemanticRelatedness = ontologyService
								.getOntologyTermSemanticRelatedness(targetOntologyTerm, sourceOntologyTerm);

						similarity += ontologyTermSemanticRelatedness * targetFrequency * sourceFrequency;

						cachedRelatedness.put(identifier, ontologyTermSemanticRelatedness);
					}
					else
					{
						cachedRelatedness.put(identifier, 0.0);
					}
				}
			}
		}

		return similarity / base;
	}

	double calculateSimilarityBasedonTags(List<BiobankSampleAttribute> randomTargetAttributes,
			List<BiobankSampleAttribute> randomSourceAttributes, Map<String, Double> cachedRelatedness,
			Map<String, Boolean> cachedRelationships)
	{
		List<OntologyTerm> targetOntologyTerms = randomTargetAttributes.stream()
				.flatMap(attribute -> attribute.getTagGroups().stream())
				.flatMap(tag -> tag.getOntologyTerms().stream().distinct()).collect(Collectors.toList());

		List<OntologyTerm> sourceOntologyTerms = randomSourceAttributes.stream()
				.flatMap(attribute -> attribute.getTagGroups().stream())
				.flatMap(tag -> tag.getOntologyTerms().stream().distinct()).collect(Collectors.toList());

		Map<OntologyTerm, Integer> targetOntologyTermFrequency = getOntologyTermFrequency(targetOntologyTerms);

		Map<OntologyTerm, Integer> sourceOntologyTermFrequency = getOntologyTermFrequency(sourceOntologyTerms);

		double similarity = 0;

		double base = Math.sqrt(targetOntologyTerms.size() * sourceOntologyTerms.size());

		for (Entry<OntologyTerm, Integer> targetEntry : targetOntologyTermFrequency.entrySet())
		{
			OntologyTerm targetOntologyTerm = targetEntry.getKey();

			Integer targetFrequency = targetEntry.getValue();

			for (Entry<OntologyTerm, Integer> sourceEntry : sourceOntologyTermFrequency.entrySet())
			{
				OntologyTerm sourceOntologyTerm = sourceEntry.getKey();

				Integer sourceFrequency = sourceEntry.getValue();

				String identifier = targetOntologyTerm.getIRI() + sourceOntologyTerm.getIRI();

				if (cachedRelatedness.containsKey(identifier))
				{
					similarity += cachedRelatedness.get(identifier) * targetFrequency * sourceFrequency;
				}
				else
				{
					if (ontologyService.related(targetOntologyTerm, sourceOntologyTerm,
							OntologyBasedMatcher.STOP_LEVEL))
					{
						Double ontologyTermSemanticRelatedness = ontologyService
								.getOntologyTermSemanticRelatedness(targetOntologyTerm, sourceOntologyTerm);

						similarity += ontologyTermSemanticRelatedness * targetFrequency * sourceFrequency;

						cachedRelatedness.put(identifier, ontologyTermSemanticRelatedness);
					}
					else
					{
						cachedRelatedness.put(identifier, 0.0);
					}
				}
			}
		}

		return similarity / base;
	}

	int getNumberOfMatches(List<BiobankSampleAttribute> randomTargetAttributes,
			List<BiobankSampleAttribute> randomSourceAttributes, Multimap<String, String> relevantMatches)
	{
		int totalMatches = 0;

		for (BiobankSampleAttribute targetAttribute : randomTargetAttributes)
		{
			for (BiobankSampleAttribute sourceAttribute : randomSourceAttributes)
			{
				if (relevantMatches.containsEntry(targetAttribute.getName(), sourceAttribute.getName()))
				{
					totalMatches++;
				}
			}
		}

		return totalMatches;
	}

	List<BiobankSampleAttribute> randomizeSlicedAttributes(List<BiobankSampleAttribute> attributes, int size)
	{
		int total = attributes.size() > size ? size : attributes.size();
		Set<Integer> uniqueIndics = new HashSet<>();

		while (uniqueIndics.size() < total)
		{
			uniqueIndics.add((int) Math.floor(Math.random() * attributes.size()));
		}

		return uniqueIndics.stream().map(attributes::get).collect(Collectors.toList());
	}

	Multimap<String, String> randomizeSlicedMatches(Multimap<String, String> matches, int size)
	{
		Multimap<String, String> randomMatches = LinkedHashMultimap.create();

		int total = matches.size() > size ? size : matches.size();

		List<String> targetAttributes = Lists.newArrayList(matches.keySet());

		while (randomMatches.size() < total)
		{
			String randomTargetAttribute = targetAttributes
					.get((int) Math.floor(Math.random() * targetAttributes.size()));

			List<String> sourceAttributes = Lists.newArrayList(matches.get(randomTargetAttribute));

			String randomSourceAttribute = sourceAttributes
					.get((int) Math.floor(Math.random() * sourceAttributes.size()));

			if (!randomMatches.containsEntry(randomTargetAttribute, randomSourceAttribute))
			{
				randomMatches.put(randomTargetAttribute, randomSourceAttribute);
			}
		}

		return randomMatches;
	}
}
