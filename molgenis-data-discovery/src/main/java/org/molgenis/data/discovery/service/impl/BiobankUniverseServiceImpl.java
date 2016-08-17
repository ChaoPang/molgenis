package org.molgenis.data.discovery.service.impl;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.data.QueryRule.Operator.AND;
import static org.molgenis.data.QueryRule.Operator.IN;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.IDENTIFIER;

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
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.discovery.service.OntologyBasedExplainService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

public class BiobankUniverseServiceImpl implements BiobankUniverseService
{
	private static final Logger LOG = LoggerFactory.getLogger(BiobankUniverseServiceImpl.class);
	private final static int MAX_NUMBER_MATCHES = 50;

	private final IdGenerator idGenerator;
	private final BiobankUniverseRepository biobankUniverseRepository;
	private final OntologyService ontologyService;
	private final TagGroupGenerator tagGroupGenerator;
	private final QueryExpansionService queryExpansionService;
	private final OntologyBasedExplainService ontologyBasedExplainService;

	@Autowired
	public BiobankUniverseServiceImpl(IdGenerator idGenerator, BiobankUniverseRepository biobankUniverseRepository,
			OntologyService ontologyService, TagGroupGenerator tagGroupGenerator,
			QueryExpansionService queryExpansionService, ExplainMappingService explainMappingService,
			OntologyBasedExplainService ontologyBasedExplainService)
	{
		this.idGenerator = requireNonNull(idGenerator);
		this.biobankUniverseRepository = biobankUniverseRepository;
		this.ontologyService = requireNonNull(ontologyService);
		this.tagGroupGenerator = requireNonNull(tagGroupGenerator);
		this.queryExpansionService = requireNonNull(queryExpansionService);
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
	public List<AttributeMappingCandidate> findCandidateMappingsOntologyBased(BiobankUniverse biobankUniverse,
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

			allCandidates.addAll(ontologyBasedExplainService.explain(biobankUniverse, semanticSearchParam, target,
					newArrayList(sourceAttributes)));
		}

		allCandidates = allCandidates.stream().filter(candidate -> candidate.getExplanation().getNgramScore() > 0)
				.filter(candidate -> !candidate.getExplanation().getMatchedWords().isEmpty()).sorted()
				.limit(MAX_NUMBER_MATCHES).collect(toList());

		if (LOG.isTraceEnabled())
		{
			LOG.trace("Finished matching the target attribute: (" + target.getName() + ":" + target.getLabel() + ")");
		}

		return allCandidates;
	}

	@RunAsSystem
	@Override
	public List<AttributeMappingCandidate> findCandidateMappings(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute target, SemanticSearchParam semanticSearchParam,
			List<BiobankSampleCollection> biobankSampleCollections)
	{
		if (LOG.isTraceEnabled())
		{
			LOG.trace("Started matching the target attribute: (" + target.getName() + ":" + target.getLabel() + ")");
		}

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

				List<BiobankSampleAttribute> sourceAttributes = biobankUniverseRepository
						.queryBiobankSampleAttribute(new QueryImpl(finalQueryRules).pageSize(MAX_NUMBER_MATCHES))
						.collect(Collectors.toList());

				allCandidates.addAll(ontologyBasedExplainService.explain(biobankUniverse, semanticSearchParam, target,
						sourceAttributes));
			}
		}

		allCandidates = allCandidates.stream().filter(candidate -> candidate.getExplanation().getNgramScore() > 0)
				.filter(candidate -> !candidate.getExplanation().getMatchedWords().isEmpty()).collect(toList());

		if (LOG.isTraceEnabled())
		{
			LOG.trace("Finished matching the target attribute: (" + target.getName() + ":" + target.getLabel() + ")");
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
		// If ontology terms are not annotated with semantic types, we therefore cannot conclude whether or not they are
		// ket concepts
		if (ontologyTerm.getSemanticTypes().isEmpty()) return true;
		List<SemanticType> keyConcepts = biobankUniverse.getKeyConcepts();
		boolean anyMatch = !ontologyTerm.getSemanticTypes().stream().allMatch(keyConcepts::contains);
		return anyMatch;
	}

	@RunAsSystem
	@Override
	public void addKeyConcepts(BiobankUniverse universe, List<String> semanticTypeNames)
	{
		List<SemanticType> semanticTypes = ontologyService.getSemanticTypesByNames(semanticTypeNames);
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