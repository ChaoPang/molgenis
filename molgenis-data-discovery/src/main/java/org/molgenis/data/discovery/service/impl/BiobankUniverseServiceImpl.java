package org.molgenis.data.discovery.service.impl;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.molgenis.data.QueryRule.Operator.IN;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.IDENTIFIER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.molgenis.data.discovery.model.MatchingExplanation;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.semanticsearch.explain.bean.AttributeMatchExplanation;
import org.molgenis.data.semanticsearch.explain.service.ExplainMappingService;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.core.runas.RunAsSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

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
	public BiobankUniverse addBiobankUniverse(String universeName, MolgenisUser owner)
	{
		BiobankUniverse biobankUniverse = BiobankUniverse.create(idGenerator.generateId(), universeName,
				Collections.emptyList(), owner);
		biobankUniverseRepository.addBiobankUniverse(biobankUniverse);

		return biobankUniverse;
	}

	@RunAsSystem
	@Override
	public BiobankUniverse getBiobankUniverse(String identifier)
	{
		return biobankUniverseRepository.getUniverse(identifier);
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

	@RunAsSystem
	@Override
	public List<TagGroup> findTagGroupsForAttributes(BiobankSampleAttribute biobankSampleAttribute)
	{
		return tagGroupGenerator.findTagGroups(biobankSampleAttribute.getLabel(),
				ontologyService.getAllOntologiesIds());
	}

	@RunAsSystem
	@Override
	public List<AttributeMappingCandidate> findCandidateMappings(BiobankSampleAttribute target,
			SemanticSearchParam semanticSearchParam, List<BiobankSampleCollection> biobankSampleCollections)
	{
		List<AttributeMappingCandidate> allCandidates = new ArrayList<>();

		QueryRule expandedQuery = queryExpansionService.expand(semanticSearchParam.getLexicalQueries(),
				semanticSearchParam.getTagGroups(), semanticSearchParam.getQueryExpansionParameter());

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

				MatchingExplanation mappingExplanation = MatchingExplanation.create(idGenerator.generateId(),
						ontologyService.getAtomicOntologyTerms(attributeMatchExplanation.getOntologyTerm()),
						attributeMatchExplanation.getQueryString(), attributeMatchExplanation.getMatchedWords(),
						attributeMatchExplanation.getScore());

				allCandidates.add(AttributeMappingCandidate.create(idGenerator.generateId(), target,
						biobankSampleAttribute, mappingExplanation));
			});
		}

		return allCandidates;
	}

	@RunAsSystem
	@Transactional
	@Override
	public void importSampleCollections(String sampleName, Stream<Entity> biobankSampleAttributeEntityStream)
	{
		BiobankSampleCollection biobankSampleCollection = BiobankSampleCollection.create(sampleName);
		biobankUniverseRepository.addBiobankSampleCollection(biobankSampleCollection);

		Stream<BiobankSampleAttribute> biobankSampleAttributeStream = biobankSampleAttributeEntityStream
				.map(entity -> importedAttributEntityToBiobankSampleAttribute(biobankSampleCollection, entity));

		biobankUniverseRepository.addBiobankSampleAttributes(biobankSampleAttributeStream);
	}

	private BiobankSampleAttribute importedAttributEntityToBiobankSampleAttribute(BiobankSampleCollection collection,
			Entity entity)
	{
		String identifier = idGenerator.generateId();
		String name = entity.getString(BiobankSampleAttributeMetaData.NAME);
		String label = entity.getString(BiobankSampleAttributeMetaData.LABEL);
		String description = entity.getString(BiobankSampleAttributeMetaData.DESCRIPTION);

		return BiobankSampleAttribute.create(identifier, name, label, description, collection, emptyList());
	}
}
