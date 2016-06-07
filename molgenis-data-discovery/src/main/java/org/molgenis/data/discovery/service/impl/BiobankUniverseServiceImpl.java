package org.molgenis.data.discovery.service.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.data.QueryRule.Operator.AND;
import static org.molgenis.data.QueryRule.Operator.IN;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.IDENTIFIER;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.getLowerCaseTerms;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.Entity;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.QueryRule;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.model.matching.MatchingExplanation;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.semanticsearch.explain.bean.AttributeMatchExplanation;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.explain.service.ExplainMappingService;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.core.runas.RunAsSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

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
	public boolean isBiobankSampleCollectionTagged(BiobankSampleCollection biobankSampleCollection)
	{
		return biobankUniverseRepository.getBiobankSampleAttributes(biobankSampleCollection).stream().limit(10)
				.map(BiobankSampleAttribute::getTagGroups).anyMatch(list -> !list.isEmpty());
	}

	@RunAsSystem
	@Override
	public void removeAllTagGroups(BiobankSampleCollection biobankSampleCollection)
	{
		List<BiobankSampleAttribute> biobankSampleAttributes = biobankUniverseRepository
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

	@RunAsSystem
	@Override
	public List<AttributeMappingCandidate> findCandidateMappings(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute target, SemanticSearchParam semanticSearchParam,
			List<BiobankSampleCollection> biobankSampleCollections)
	{
		List<AttributeMappingCandidate> allCandidates = new ArrayList<>();

		List<TagGroup> tagGroups = semanticSearchParam.getTagGroups();

		QueryRule expandedQuery = queryExpansionService.expand(semanticSearchParam.getLexicalQueries(), tagGroups,
				semanticSearchParam.getQueryExpansionParameter());

		if (expandedQuery != null)
		{
			for (BiobankSampleCollection biobankSampleCollection : biobankSampleCollections)
			{
				List<String> identifiers = biobankUniverseRepository
						.getBiobankSampleAttributeIdentifiers(biobankSampleCollection);

				List<QueryRule> finalQueryRules = Lists.newArrayList(new QueryRule(IDENTIFIER, IN, identifiers));

				if (expandedQuery.getNestedRules().size() > 0)
				{
					finalQueryRules.addAll(asList(new QueryRule(AND), expandedQuery));
				}

				biobankUniverseRepository
						.queryBiobankSampleAttribute(new QueryImpl(finalQueryRules).pageSize(MAX_NUMBER_MATCHES))
						.limit(10).forEach(biobankSampleAttribute -> {

							AttributeMatchExplanation attributeMatchExplanation = explainMappingService
									.explainMapping(semanticSearchParam, biobankSampleAttribute.getLabel());

							if (isMatchHighQuality(biobankUniverse, target, attributeMatchExplanation,
									semanticSearchParam))
							{
								OntologyTermHit ontologyTermHit = attributeMatchExplanation.getOntologyTermHit();

								List<OntologyTerm> ontologyTerms = ontologyTermHit != null
										? ontologyService.getAtomicOntologyTerms(ontologyTermHit.getOntologyTerm())
										: emptyList();

								MatchingExplanation mappingExplanation = MatchingExplanation.create(
										idGenerator.generateId(), ontologyTerms,
										attributeMatchExplanation.getQueryString(),
										attributeMatchExplanation.getMatchedWords(),
										attributeMatchExplanation.getScore());

								allCandidates.add(AttributeMappingCandidate.create(idGenerator.generateId(),
										biobankUniverse, target, biobankSampleAttribute, mappingExplanation));
							}
						});
			}
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
				.map(entity -> importedAttributEntityToBiobankSampleAttribute(biobankSampleCollection, entity))
				.filter(Objects::nonNull);

		biobankUniverseRepository.addBiobankSampleAttributes(biobankSampleAttributeStream);
	}

	@RunAsSystem
	@Override
	public boolean isOntologyTermKeyConcept(BiobankUniverse biobankUniverse, OntologyTerm ontologyTerm)
	{
		List<SemanticType> keyConcepts = biobankUniverse.getKeyConcepts();
		boolean anyMatch = ontologyService.getSemanticTypes(ontologyTerm).stream().anyMatch(keyConcepts::contains);
		return anyMatch;
	}

	@RunAsSystem
	@Override
	public void addKeyConcepts(BiobankUniverse universe, List<String> semanticTypeGroups)
	{
		List<SemanticType> semanticTypes = ontologyService.getSemanticTypesByGroups(semanticTypeGroups);
		biobankUniverseRepository.addKeyConcepts(universe, semanticTypes);
	}

	/**
	 * A key concept filter to determined whether or not the match is generated using key concepts
	 * 
	 * @param biobankUniverse
	 * @param target
	 * @param attributeMatchExplanation
	 * @return
	 */
	private boolean applyKeyConceptFilter(BiobankUniverse biobankUniverse, BiobankSampleAttribute target,
			AttributeMatchExplanation attributeMatchExplanation)
	{
		// If the match is not generated based on the ontology term information, we cannot filter it using key concepts
		// and hence always return false.
		if (nonNull(attributeMatchExplanation.getOntologyTermHit()))
		{
			// we need to first of all check if any of ontology terms used to tag the target are key concepts. If there
			// is no key concept involved, it's useless to check if the expanded ontology terms are key concepts
			boolean keyConceptMatchEnabled = target.getTagGroups().stream()
					.flatMap(tag -> tag.getOntologyTerms().stream())
					.anyMatch(ot -> isOntologyTermKeyConcept(biobankUniverse, ot));

			if (keyConceptMatchEnabled)
			{
				OntologyTerm origin = attributeMatchExplanation.getOntologyTermHit().getOrigin();

				List<String> ontologyTermNames = ontologyService.getAtomicOntologyTerms(origin).stream()
						.flatMap(ot -> getLowerCaseTerms(ot).stream()).collect(toList());

				List<OntologyTerm> ontologyTerms = target.getTagGroups().stream()
						.flatMap(tag -> tag.getOntologyTerms().stream())
						.filter(ot -> ontologyTermNames.contains(ot.getLabel().toLowerCase())).collect(toList());

				return ontologyTerms.stream().anyMatch(ot -> isOntologyTermKeyConcept(biobankUniverse, ot));
			}
		}

		return nonNull(attributeMatchExplanation.getMatchedWords());
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

		List<SemanticType> semanticTypes = ontologyTerms.stream()
				.flatMap(ot -> ontologyService.getSemanticTypes(ot).stream()).collect(toList());

		return IdentifiableTagGroup.create(identifier, ontologyTerms, semanticTypes, matchedWords, score);
	}

	// private AttributeMatchExplanation explainAttributeMatchLazy(BiobankUniverse biobankUniverse,
	// BiobankSampleAttribute target, BiobankSampleAttribute biobankSampleAttribute,
	// SemanticSearchParam semanticSearchParam)
	// {
	// // Explain the match with lexical queries
	// SemanticSearchParam explainSemanticSearchParam = SemanticSearchParam.create(
	// semanticSearchParam.getLexicalQueries(), semanticSearchParam.getTagGroups(),
	// QueryExpansionParam.create(false, false));
	// AttributeMatchExplanation attributeMatchExplanation = explainMappingService
	// .explainMapping(explainSemanticSearchParam, biobankSampleAttribute.getLabel());
	// if (isMatchHighQuality(biobankUniverse, target, attributeMatchExplanation, explainSemanticSearchParam))
	// return attributeMatchExplanation;
	//
	// // Explain the match with lexical queries and ontology term synonyms
	// explainSemanticSearchParam = SemanticSearchParam.create(semanticSearchParam.getLexicalQueries(),
	// semanticSearchParam.getTagGroups(), QueryExpansionParam.create(true, false));
	// attributeMatchExplanation = explainMappingService.explainMapping(explainSemanticSearchParam,
	// biobankSampleAttribute.getLabel());
	// if (isMatchHighQuality(biobankUniverse, target, attributeMatchExplanation, explainSemanticSearchParam))
	// return attributeMatchExplanation;
	//
	// // Explain the match with lexical queries, ontology term synonyms, ontology term children and parents
	// explainSemanticSearchParam = SemanticSearchParam.create(semanticSearchParam.getLexicalQueries(),
	// semanticSearchParam.getTagGroups(), QueryExpansionParam.create(true, true));
	// attributeMatchExplanation = explainMappingService.explainMapping(explainSemanticSearchParam,
	// biobankSampleAttribute.getLabel());
	// if (isMatchHighQuality(biobankUniverse, target, attributeMatchExplanation, explainSemanticSearchParam))
	// return attributeMatchExplanation;
	//
	// return null;
	// }

	private boolean isMatchHighQuality(BiobankUniverse biobankUniverse, BiobankSampleAttribute target,
			AttributeMatchExplanation attributeMatchExplanation, SemanticSearchParam semanticSearchParam)
	{
		boolean isHighQuality = attributeMatchExplanation.getScore() > semanticSearchParam.getHighQualityThreshold();

		// A high quality match is defined as either being generated with a high similarity score or being
		// matched with key concepts
		return isHighQuality || applyKeyConceptFilter(biobankUniverse, target, attributeMatchExplanation);
	}
}