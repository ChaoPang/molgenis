package org.molgenis.data.discovery.service.impl;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.Entity;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.Query;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.BiobankCollectionSimilarity;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.discovery.service.OntologyBasedExplainService;
import org.molgenis.data.semanticsearch.explain.service.ExplainMappingService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.OntologyTermRelated;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.core.runas.RunAsSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

public class BiobankUniverseServiceImpl implements BiobankUniverseService
{
	private static final Logger LOG = LoggerFactory.getLogger(BiobankUniverseServiceImpl.class);
	private final static int MAX_NUMBER_MATCHES = 50;

	private final IdGenerator idGenerator;
	private final BiobankUniverseRepository biobankUniverseRepository;
	private final OntologyService ontologyService;
	private final TagGroupGenerator tagGroupGenerator;
	private final OntologyBasedExplainService ontologyBasedExplainService;

	private LoadingCache<OntologyTermRelated, Double> cachedOntologyTermSemanticRelateness = CacheBuilder.newBuilder()
			.maximumSize(2000).expireAfterWrite(1, TimeUnit.HOURS).build(new CacheLoader<OntologyTermRelated, Double>()
			{
				public Double load(OntologyTermRelated ontologyTermRelated)
				{
					if (ontologyService.related(ontologyTermRelated.getTarget(), ontologyTermRelated.getSource(),
							ontologyTermRelated.getStopLevel()))
					{
						Double ontologyTermSemanticRelatedness = ontologyService.getOntologyTermSemanticRelatedness(
								ontologyTermRelated.getTarget(), ontologyTermRelated.getSource());

						return ontologyTermSemanticRelatedness;
					}
					return 0.0d;
				}
			});

	@Autowired
	public BiobankUniverseServiceImpl(IdGenerator idGenerator, BiobankUniverseRepository biobankUniverseRepository,
			OntologyService ontologyService, TagGroupGenerator tagGroupGenerator,
			ExplainMappingService explainMappingService, OntologyBasedExplainService ontologyBasedExplainService)
	{
		this.idGenerator = requireNonNull(idGenerator);
		this.biobankUniverseRepository = biobankUniverseRepository;
		this.ontologyService = requireNonNull(ontologyService);
		this.tagGroupGenerator = requireNonNull(tagGroupGenerator);
		this.ontologyBasedExplainService = requireNonNull(ontologyBasedExplainService);
	}

	@RunAsSystem
	@Override
	public BiobankUniverse addBiobankUniverse(String universeName, List<String> semanticTypeNames, MolgenisUser owner)
	{
		List<SemanticType> semanticTypes = ontologyService.getSemanticTypesByNames(semanticTypeNames);

		BiobankUniverse biobankUniverse = BiobankUniverse.create(idGenerator.generateId(), universeName, emptyList(),
				owner, semanticTypes);

		biobankUniverseRepository.addBiobankUniverse(biobankUniverse);

		return biobankUniverseRepository.getUniverse(biobankUniverse.getIdentifier());
	}

	@RunAsSystem
	@Override
	public void deleteBiobankUniverse(String identifier)
	{
		biobankUniverseRepository.removeBiobankUniverse(biobankUniverseRepository.getUniverse(identifier));
	}

	@RunAsSystem
	@Override
	public BiobankUniverse getBiobankUniverse(String identifier)
	{
		return biobankUniverseRepository.getUniverse(identifier);
	}

	@RunAsSystem
	@Override
	public void addBiobankUniverseMember(BiobankUniverse biobankUniverse,
			List<BiobankSampleCollection> biobankSampleCollections)
	{
		biobankUniverseRepository.addUniverseMembers(biobankUniverse, biobankSampleCollections);
	}

	@RunAsSystem
	@Override
	public void addCollectionSimilarities(BiobankUniverse biobankUniverse,
			List<BiobankSampleCollection> biobankSampleCollections)
	{
		List<BiobankSampleCollection> previousMembers = Lists.newArrayList(biobankUniverse.getMembers().stream()
				.filter(member -> !biobankSampleCollections.contains(member)).collect(Collectors.toList()));

		// Add biobankCollectionSimilarities to the biobankUniverse
		List<BiobankCollectionSimilarity> biobankCollectionSimilarities = new ArrayList<>();

		for (BiobankSampleCollection target : biobankSampleCollections)
		{
			for (BiobankSampleCollection source : previousMembers)
			{
				double sampleCollectionSimilarity = computeSampleCollectionSimilarity(target, source);
				biobankCollectionSimilarities.add(BiobankCollectionSimilarity.create(idGenerator.generateId(), target,
						source, sampleCollectionSimilarity, biobankUniverse));
			}
			previousMembers.add(target);
		}
		biobankUniverseRepository.addCollectionSimilarities(biobankCollectionSimilarities);
	}

	@Override
	public List<BiobankCollectionSimilarity> getCollectionSimilarities(BiobankUniverse biobankUniverse)
	{
		return biobankUniverseRepository.getCollectionSimilaritiesFromUniverse(biobankUniverse);
	}

	@RunAsSystem
	@Override
	public List<BiobankSampleCollection> getAllBiobankSampleCollections()
	{
		return biobankUniverseRepository.getAllBiobankSampleCollections();
	}

	@RunAsSystem
	@Override
	public List<BiobankSampleCollection> getBiobankSampleCollections(List<String> biobankSampleCollectionNames)
	{
		return biobankSampleCollectionNames.stream().map(biobankUniverseRepository::getBiobankSampleCollection)
				.collect(toList());
	}

	@RunAsSystem
	@Override
	public BiobankSampleCollection getBiobankSampleCollection(String biobankSampleCollectionName)
	{
		return biobankUniverseRepository.getBiobankSampleCollection(biobankSampleCollectionName);
	}

	@RunAsSystem
	@Override
	public void removeBiobankSampleCollection(BiobankSampleCollection biobankSampleCollection)
	{
		biobankUniverseRepository.removeBiobankSampleCollection(biobankSampleCollection);
	}

	@Override
	public List<BiobankUniverse> getBiobankUniverses()
	{
		return biobankUniverseRepository.getAllUniverses();
	}

	@Override
	public List<BiobankSampleAttribute> getBiobankSampleAttributes(BiobankSampleCollection biobankSampleCollection)
	{
		return Lists.newArrayList(biobankUniverseRepository.getBiobankSampleAttributes(biobankSampleCollection));
	}

	@Override
	public int countBiobankSampleAttributes(BiobankSampleCollection biobankSampleCollection)
	{
		return biobankUniverseRepository.countBiobankSampleAttributes(biobankSampleCollection);
	}

	@Override
	public boolean isBiobankSampleCollectionTagged(BiobankSampleCollection biobankSampleCollection)
	{
		return biobankUniverseRepository.isBiobankSampleCollectionTagged(biobankSampleCollection);
	}

	@RunAsSystem
	@Override
	public void removeAllTagGroups(BiobankSampleCollection biobankSampleCollection)
	{
		Iterable<BiobankSampleAttribute> biobankSampleAttributes = biobankUniverseRepository
				.getBiobankSampleAttributes(biobankSampleCollection);

		biobankUniverseRepository.removeTagGroupsForAttributes(biobankSampleAttributes);
	}

	@RunAsSystem
	@Override
	public List<IdentifiableTagGroup> findTagGroupsForAttributes(BiobankSampleAttribute biobankSampleAttribute)
	{
		return tagGroupGenerator
				.generateTagGroups(biobankSampleAttribute.getLabel(), ontologyService.getAllOntologiesIds()).stream()
				.map(this::tagGroupToIdentifiableTagGroup).collect(Collectors.toList());
	}

	@Override
	public List<AttributeMappingCandidate> findCandidateMappings(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute target, SemanticSearchParam semanticSearchParam,
			List<OntologyBasedMatcher> ontologyBasedMatchers)
	{
		if (LOG.isTraceEnabled())
		{
			LOG.trace("Started matching the target attribute: (" + target.getName() + ":" + target.getLabel() + ")");
		}

		List<AttributeMappingCandidate> allCandidates = new ArrayList<>();

		for (OntologyBasedMatcher ontologyBasedMatcher : ontologyBasedMatchers)
		{
			List<BiobankSampleAttribute> sourceAttributes = ontologyBasedMatcher.match(semanticSearchParam);

			List<AttributeMappingCandidate> collect = ontologyBasedExplainService
					.explain(biobankUniverse, semanticSearchParam, target, newArrayList(sourceAttributes)).stream()
					.filter(candidate -> candidate.getExplanation().getNgramScore() > 0)
					.filter(candidate -> !candidate.getExplanation().getMatchedWords().isEmpty()).sorted()
					.limit(MAX_NUMBER_MATCHES).collect(toList());

			allCandidates.addAll(collect);
		}

		if (LOG.isTraceEnabled())
		{
			LOG.trace("Finished matching the target attribute: (" + target.getName() + ":" + target.getLabel() + ")");
		}

		return allCandidates;
	}

	@Override
	public List<AttributeMappingCandidate> getCandidateMappingsCandidates(Query query)
	{
		return biobankUniverseRepository.getAttributeMappingCandidates(query);
	}

	@RunAsSystem
	@Transactional
	@Override
	public void importSampleCollections(String sampleName, Stream<Entity> biobankSampleAttributeEntityStream)
	{
		BiobankSampleCollection biobankSampleCollection = BiobankSampleCollection.create(sampleName);
		biobankUniverseRepository.addBiobankSampleCollection(biobankSampleCollection);

		Stream<BiobankSampleAttribute> biobankSampleAttributeStream = biobankSampleAttributeEntityStream
				.map(entity -> importedAttributEntityToBiobankSampleAttribute(biobankSampleCollection, entity))
				.filter(Objects::nonNull);

		biobankUniverseRepository.addBiobankSampleAttributes(biobankSampleAttributeStream);
	}

	@RunAsSystem
	@Override
	public void addKeyConcepts(BiobankUniverse universe, List<String> semanticTypeNames)
	{
		List<SemanticType> semanticTypes = ontologyService.getSemanticTypesByNames(semanticTypeNames);
		biobankUniverseRepository.addKeyConcepts(universe, semanticTypes);
	}

	@RunAsSystem
	@Override
	public double computeSampleCollectionSimilarity(BiobankSampleCollection target, BiobankSampleCollection source)
	{
		List<OntologyTerm> targetOntologyTerms = getBiobankSampleAttributes(target).stream()
				.flatMap(attribute -> attribute.getTagGroups().stream())
				.flatMap(tag -> tag.getOntologyTerms().stream().distinct()).collect(Collectors.toList());

		List<OntologyTerm> sourceOntologyTerms = getBiobankSampleAttributes(source).stream()
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

				OntologyTermRelated ontologyTermRelated = OntologyTermRelated.create(targetOntologyTerm,
						sourceOntologyTerm, OntologyBasedMatcher.STOP_LEVEL);

				Double relatedNess = 0.0d;
				try
				{
					relatedNess = cachedOntologyTermSemanticRelateness.get(ontologyTermRelated);
				}
				catch (ExecutionException e)
				{
					LOG.error(e.getMessage());
				}

				similarity += relatedNess * targetFrequency * sourceFrequency;
			}
		}

		return similarity / base;
	}

	private Map<OntologyTerm, Integer> getOntologyTermFrequency(List<OntologyTerm> ontologyTerms)
	{
		Map<OntologyTerm, Integer> ontologyTermFrequency = new HashMap<>();

		for (OntologyTerm ot : ontologyTerms)
		{
			if (!ontologyTermFrequency.containsKey(ot))
			{
				ontologyTermFrequency.put(ot, 0);
			}

			ontologyTermFrequency.put(ot, ontologyTermFrequency.get(ot) + 1);
		}
		return ontologyTermFrequency;
	}

	private BiobankSampleAttribute importedAttributEntityToBiobankSampleAttribute(BiobankSampleCollection collection,
			Entity entity)
	{
		String identifier = idGenerator.generateId();
		String name = entity.getString(BiobankSampleAttributeMetaData.NAME);
		String label = entity.getString(BiobankSampleAttributeMetaData.LABEL);
		String description = entity.getString(BiobankSampleAttributeMetaData.DESCRIPTION);

		return isNotBlank(name)
				? BiobankSampleAttribute.create(identifier, name, label, description, collection, emptyList()) : null;
	}

	private IdentifiableTagGroup tagGroupToIdentifiableTagGroup(TagGroup tagGroup)
	{
		String identifier = idGenerator.generateId();
		String matchedWords = tagGroup.getMatchedWords();
		float score = tagGroup.getScore();

		List<OntologyTerm> ontologyTerms = ontologyService.getAtomicOntologyTerms(tagGroup.getOntologyTerm());

		List<SemanticType> semanticTypes = ontologyTerms.stream().flatMap(ot -> ot.getSemanticTypes().stream())
				.collect(toList());

		return IdentifiableTagGroup.create(identifier, ontologyTerms, semanticTypes, matchedWords, score);
	}
}