package org.molgenis.data.discovery.service.impl;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.data.QueryRule.Operator.IN;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.IDENTIFIER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.Entity;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.discovery.meta.BiobankSampleAttributeMetaData;
import org.molgenis.data.discovery.model.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.BiobankSampleCollection;
import org.molgenis.data.discovery.model.BiobankUniverse;
import org.molgenis.data.discovery.model.IdentifiableTagGroup;
import org.molgenis.data.discovery.model.MatchingExplanation;
import org.molgenis.data.discovery.model.SemanticType;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.semanticsearch.explain.bean.AttributeMatchExplanation;
import org.molgenis.data.semanticsearch.explain.service.ExplainMappingService;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.core.runas.RunAsSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public class BiobankUniverseServiceImpl implements BiobankUniverseService
{
	private final static int MAX_NUMBER_MATCHES = 50;

	private final IdGenerator idGenerator;
	private final BiobankUniverseRepository biobankUniverseRepository;
	private final OntologyService ontologyService;
	private final TagGroupGenerator tagGroupGenerator;
	private final QueryExpansionService queryExpansionService;
	private final ExplainMappingService explainMappingService;

	@Autowired
	public BiobankUniverseServiceImpl(IdGenerator idGenerator, BiobankUniverseRepository biobankUniverseRepository,
			OntologyService ontologyService, TagGroupGenerator tagGroupGenerator,
			QueryExpansionService queryExpansionService, ExplainMappingService explainMappingService)
	{
		this.idGenerator = requireNonNull(idGenerator);
		this.biobankUniverseRepository = biobankUniverseRepository;
		this.ontologyService = requireNonNull(ontologyService);
		this.tagGroupGenerator = requireNonNull(tagGroupGenerator);
		this.queryExpansionService = requireNonNull(queryExpansionService);
		this.explainMappingService = requireNonNull(explainMappingService);
	}

	@RunAsSystem
	@Override
	public BiobankUniverse addBiobankUniverse(String universeName, List<String> semanticTypeGroups, MolgenisUser owner)
	{
		List<SemanticType> semanticTypes = biobankUniverseRepository.getSemanticTypesByGroups(semanticTypeGroups);
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

	@Override
	public List<BiobankUniverse> getBiobankUniverses()
	{
		return biobankUniverseRepository.getAllUniverses();
	}

	@Override
	public boolean isBiobankSampleCollectionTagged(BiobankSampleCollection biobankSampleCollection)
	{
		return !biobankUniverseRepository.getBiobankSampleAttributes(biobankSampleCollection).stream()
				.map(BiobankSampleAttribute::getTagGroups).allMatch(List::isEmpty);
	}

	@RunAsSystem
	@Override
	public List<IdentifiableTagGroup> findTagGroupsForAttributes(BiobankSampleAttribute biobankSampleAttribute)
	{
		return tagGroupGenerator.findTagGroups(biobankSampleAttribute.getLabel(), ontologyService.getAllOntologiesIds())
				.stream().map(tag -> IdentifiableTagGroup.create(idGenerator.generateId(), tag))
				.collect(Collectors.toList());
	}

	@RunAsSystem
	@Override
	public List<AttributeMappingCandidate> findCandidateMappings(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute target, SemanticSearchParam semanticSearchParam,
			List<BiobankSampleCollection> biobankSampleCollections)
	{
		List<AttributeMappingCandidate> allCandidates = new ArrayList<>();

		QueryRule expandedQuery = queryExpansionService.expand(semanticSearchParam.getLexicalQueries(),
				semanticSearchParam.getTagGroups(), semanticSearchParam.getQueryExpansionParameter());

		if (expandedQuery != null)
		{
			for (BiobankSampleCollection biobankSampleCollection : biobankSampleCollections)
			{
				List<String> identifiers = biobankUniverseRepository.getBiobankSampleAttributes(biobankSampleCollection)
						.stream().map(BiobankSampleAttribute::getIdentifier).collect(Collectors.toList());

				List<QueryRule> finalQueryRules = Lists.newArrayList(new QueryRule(IDENTIFIER, IN, identifiers));

				if (expandedQuery.getNestedRules().size() > 0)
				{
					finalQueryRules.addAll(Arrays.asList(new QueryRule(Operator.AND), expandedQuery));
				}

				List<BiobankSampleAttribute> biobankSampleAttributes = biobankUniverseRepository
						.queryBiobankSampleAttribute(new QueryImpl(finalQueryRules).pageSize(MAX_NUMBER_MATCHES));

				biobankSampleAttributes.stream().limit(10).forEach(biobankSampleAttribute -> {

					AttributeMatchExplanation attributeMatchExplanation = explainMappingService
							.explainMapping(semanticSearchParam, biobankSampleAttribute.getLabel());

					if (applyKeyConceptFilter(biobankUniverse, target, attributeMatchExplanation, semanticSearchParam))
					{
						MatchingExplanation mappingExplanation = MatchingExplanation.create(idGenerator.generateId(),
								ontologyService.getAtomicOntologyTerms(attributeMatchExplanation.getOntologyTerm()),
								attributeMatchExplanation.getQueryString(), attributeMatchExplanation.getMatchedWords(),
								attributeMatchExplanation.getScore());

						allCandidates.add(AttributeMappingCandidate.create(idGenerator.generateId(), target,
								biobankSampleAttribute, mappingExplanation));
					}
				});
			}
		}

		return allCandidates;
	}

	private boolean applyKeyConceptFilter(BiobankUniverse biobankUniverse, BiobankSampleAttribute target,
			AttributeMatchExplanation attributeMatchExplanation, SemanticSearchParam semanticSearchParam)
	{
		// If match is not generated using key concepts, then the similarity score needs to be higher than the
		// high quality threshold
		if (attributeMatchExplanation.getScore() > semanticSearchParam.getHighQualityThreshold())
		{
			return true;
		}

		// If the match is not generated based on the ontology term information, we cannot filter it using key concepts
		// and hence always return true.
		if (nonNull(attributeMatchExplanation.getOntologyTerm()))
		{
			List<String> matchedOntologyTermNames = ontologyService
					.getAtomicOntologyTerms(attributeMatchExplanation.getOntologyTerm()).stream()
					.map(OntologyTerm::getLabel).map(StringUtils::lowerCase).collect(toList());

			List<OntologyTerm> ontologyTerms = target.getTagGroups().stream()
					.flatMap(tag -> ontologyService.getAtomicOntologyTerms(tag.getOntologyTerm()).stream())
					.filter(ot -> matchedOntologyTermNames.contains(ot.getLabel().toLowerCase())).collect(toList());

			// Stream<IdentifiableTagGroup> involvedTagGroupStream = target.getTagGroups().stream()
			// .filter(tag -> ontologyService.getAtomicOntologyTerms(tag.getOntologyTerm()).stream()
			// .anyMatch(atomicOntologyTerms::contains));
			//
			// boolean keyConceptMatchEnabled = involvedTagGroupStream
			// .flatMap(tag -> ontologyService.getAtomicOntologyTerms(tag.getOntologyTerm()).stream())
			// .anyMatch(ot -> isOntologyTermKeyConcept(biobankUniverse, ot));
			//
			// if (keyConceptMatchEnabled)
			// {
			// return atomicOntologyTerms.stream().anyMatch(ot -> isOntologyTermKeyConcept(biobankUniverse, ot));
			// }
			return ontologyTerms.stream().anyMatch(ot -> isOntologyTermKeyConcept(biobankUniverse, ot));
		}

		return false;
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
	public boolean isOntologyTermKeyConcept(BiobankUniverse biobankUniverse, OntologyTerm ontologyTerm)
	{
		List<SemanticType> keyConcepts = biobankUniverse.getKeyConcepts();
		boolean anyMatch = biobankUniverseRepository.getSemanticTypes(ontologyTerm).stream()
				.anyMatch(keyConcepts::contains);
		return anyMatch;
	}

	@RunAsSystem
	@Override
	public List<SemanticType> getAllSemanticType()
	{
		return biobankUniverseRepository.getAllSemanticType();
	}

	@RunAsSystem
	@Override
	public void addKeyConcepts(BiobankUniverse universe, List<String> semanticTypeGroups)
	{
		List<SemanticType> semanticTypes = biobankUniverseRepository.getSemanticTypesByGroups(semanticTypeGroups);
		biobankUniverseRepository.addKeyConcepts(universe, semanticTypes);
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
}