package org.molgenis.data.discovery.repo.impl;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.StreamSupport.stream;
import static org.molgenis.data.QueryRule.Operator.AND;
import static org.molgenis.data.QueryRule.Operator.EQUALS;
import static org.molgenis.data.QueryRule.Operator.IN;
import static org.molgenis.data.QueryRule.Operator.OR;
import static org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData.TAG_GROUPS;
import static org.molgenis.data.discovery.meta.matching.AttributeMappingCandidateMetaData.BIOBANK_UNIVERSE;
import static org.molgenis.data.discovery.meta.matching.AttributeMappingCandidateMetaData.SOURCE;
import static org.molgenis.data.discovery.meta.matching.AttributeMappingCandidateMetaData.TARGET;
import static org.molgenis.data.support.QueryImpl.EQ;
import static org.molgenis.data.support.QueryImpl.IN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Iterables;
import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityManager;
import org.molgenis.data.Fetch;
import org.molgenis.data.Query;
import org.molgenis.data.QueryRule;
import org.molgenis.data.discovery.job.BiobankUniverseJobExecutionMetaData;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleCollectionMetaData;
import org.molgenis.data.discovery.meta.biobank.BiobankUniverseMetaData;
import org.molgenis.data.discovery.meta.matching.AttributeMappingCandidateMetaData;
import org.molgenis.data.discovery.meta.matching.AttributeMappingDecisionMetaData;
import org.molgenis.data.discovery.meta.matching.AttributeMappingDecisionMetaData.DecisionOptions;
import org.molgenis.data.discovery.meta.matching.MatchingExplanationMetaData;
import org.molgenis.data.discovery.meta.matching.TagGroupMetaData;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.biobank.BiobankUniverseMemberVector;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.AttributeMappingDecision;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.model.matching.MatchingExplanation;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.support.DefaultEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.molgenis.ontology.core.meta.SemanticTypeMetaData;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.repository.OntologyTermRepository;
import org.molgenis.security.user.MolgenisUserService;
import org.molgenis.security.user.UserAccountService;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class BiobankUniverseRepositoryImpl implements BiobankUniverseRepository
{
	private final DataService dataService;
	private final MolgenisUserService molgenisUserService;
	private final UserAccountService userAcountService;
	private final EntityManager entityManager;

	public BiobankUniverseRepositoryImpl(DataService dataService, MolgenisUserService molgenisUserService,
			UserAccountService userAcountService, EntityManager entityManager)
	{
		this.dataService = requireNonNull(dataService);
		this.molgenisUserService = requireNonNull(molgenisUserService);
		this.userAcountService = requireNonNull(userAcountService);
		this.entityManager = requireNonNull(entityManager);
	}

	@Override
	public void addKeyConcepts(BiobankUniverse biobankUniverse, List<SemanticType> semanticTypes)
	{
		List<SemanticType> keyConcepts = Lists.newArrayList(biobankUniverse.getKeyConcepts());
		List<SemanticType> newKeyConcepts = semanticTypes.stream().filter(type -> !keyConcepts.contains(type))
				.collect(toList());

		if (newKeyConcepts.size() > 0)
		{
			keyConcepts.addAll(newKeyConcepts);
			BiobankUniverse newBiobankUniverse = BiobankUniverse.create(biobankUniverse.getIdentifier(),
					biobankUniverse.getName(), biobankUniverse.getMembers(), biobankUniverse.getOwner(), keyConcepts,
					biobankUniverse.getVectors());
			dataService.update(BiobankUniverseMetaData.ENTITY_NAME, biobankUniverseToEntity(newBiobankUniverse));
		}
	}

	@Override
	public List<BiobankUniverse> getAllUniverses()
	{
		List<BiobankUniverse> universes = dataService.findAll(BiobankUniverseMetaData.ENTITY_NAME)
				.map(this::entityToBiobankUniverse).collect(Collectors.toList());
		return universes;
	}

	@Override
	public BiobankUniverse getUniverse(String identifier)
	{
		Fetch fetch = new Fetch();
		BiobankUniverseMetaData.INSTANCE.getAtomicAttributes().forEach(attr -> fetch.field(attr.getName()));
		Entity findOne = dataService.findOne(BiobankUniverseMetaData.ENTITY_NAME,
				QueryImpl.EQ(BiobankUniverseMetaData.IDENTIFIER, identifier).fetch(fetch));
		return findOne == null ? null : entityToBiobankUniverse(findOne);
	}

	@Override
	public void addBiobankUniverse(BiobankUniverse biobankUniverse)
	{
		Entity entity = biobankUniverseToEntity(biobankUniverse);
		dataService.add(BiobankUniverseMetaData.ENTITY_NAME, entity);
	}

	@Override
	public void removeBiobankUniverse(BiobankUniverse biobankUniverse)
	{
		List<String> attributeIdentifiers = biobankUniverse.getMembers().stream()
				.flatMap(member -> getBiobankSampleAttributeIdentifiers(member).stream()).collect(Collectors.toList());

		Fetch fetchOntologyTerm = new Fetch();
		OntologyTermMetaData.INSTANCE.getAtomicAttributes().forEach(field -> fetchOntologyTerm.field(field.getName()));

		Fetch fetchSemanticType = new Fetch();
		SemanticTypeMetaData.INSTANCE.getAtomicAttributes().forEach(field -> fetchSemanticType.field(field.getName()));

		Fetch fetchTagGroupFields = new Fetch();
		TagGroupMetaData.INSTANCE.getAtomicAttributes()
				.forEach(attribute -> fetchTagGroupFields.field(attribute.getName()));
		fetchTagGroupFields.field(TagGroupMetaData.ONTOLOGY_TERMS, fetchOntologyTerm);
		fetchTagGroupFields.field(TagGroupMetaData.SEMANTIC_TYPES, fetchSemanticType);

		Fetch fetchBiobankSampleAttribute = new Fetch();
		fetchBiobankSampleAttribute.field(BiobankSampleAttributeMetaData.COLLECTION);
		fetchBiobankSampleAttribute.field(BiobankSampleAttributeMetaData.TAG_GROUPS, fetchTagGroupFields);

		Fetch fetchExplanation = new Fetch();
		fetchExplanation.field(MatchingExplanationMetaData.ONTOLOGY_TERMS, fetchOntologyTerm);

		Fetch attributeMappingCandidateFetch = new Fetch();
		attributeMappingCandidateFetch.field(AttributeMappingCandidateMetaData.TARGET, fetchBiobankSampleAttribute);
		attributeMappingCandidateFetch.field(AttributeMappingCandidateMetaData.SOURCE, fetchBiobankSampleAttribute);
		attributeMappingCandidateFetch.field(AttributeMappingCandidateMetaData.EXPLANATION, fetchExplanation);
		attributeMappingCandidateFetch.field(AttributeMappingCandidateMetaData.DECISIONS);

		List<QueryRule> innerQueryRules = newArrayList(new QueryRule(TARGET, IN, attributeIdentifiers),
				new QueryRule(OR), new QueryRule(SOURCE, IN, attributeIdentifiers));

		List<QueryRule> nestedQueryRules = newArrayList(
				new QueryRule(BIOBANK_UNIVERSE, EQUALS, biobankUniverse.getIdentifier()), new QueryRule(AND),
				new QueryRule(innerQueryRules));

		List<Entity> attributeMappingCandidateEntities = dataService
				.findAll(AttributeMappingCandidateMetaData.ENTITY_NAME,
						new QueryImpl(nestedQueryRules).fetch(attributeMappingCandidateFetch))
				.collect(toList());

		// Remove attributeMappingCandidates, explanations and decisions
		removeAttributeMappingCandidates(attributeMappingCandidateEntities);

		// Remove the BiobankUniverseJobExecutions in which the universe is involved
		Stream<Entity> biobankUniverseJobEntityStream = dataService.findAll(
				BiobankUniverseJobExecutionMetaData.ENTITY_NAME,
				QueryImpl.EQ(BiobankUniverseJobExecutionMetaData.UNIVERSE, biobankUniverse.getIdentifier()));

		dataService.delete(BiobankUniverseJobExecutionMetaData.ENTITY_NAME, biobankUniverseJobEntityStream);

		// Remove the BiobankUniverse itself
		dataService.delete(BiobankUniverseMetaData.ENTITY_NAME, biobankUniverseToEntity(biobankUniverse));
	}

	@Override
	public void addUniverseMembers(BiobankUniverse biobankUniverse, List<BiobankSampleCollection> members)
	{
		List<BiobankSampleCollection> allMembers = Stream
				.concat(biobankUniverse.getMembers().stream(), members.stream()).distinct()
				.collect(Collectors.toList());

		List<BiobankUniverseMemberVector> allVectors = concat(
				members.stream().filter(member -> !biobankUniverse.getMembers().contains(member))
						.map(member -> BiobankUniverseMemberVector.create(member, new double[0])),
				biobankUniverse.getVectors().stream()).collect(Collectors.toList());

		Entity biobankUniverseToEntity = biobankUniverseToEntity(
				BiobankUniverse.create(biobankUniverse.getIdentifier(), biobankUniverse.getName(), allMembers,
						biobankUniverse.getOwner(), biobankUniverse.getKeyConcepts(), allVectors));

		dataService.update(BiobankUniverseMetaData.ENTITY_NAME, biobankUniverseToEntity);
	}

	@Override
	public void updateBiobankUniverseMemberVectors(BiobankUniverse biobankUniverse,
			List<BiobankUniverseMemberVector> biobankUniverseMemberVectors)
	{
		BiobankUniverse updatedBiobankUniverse = BiobankUniverse.create(biobankUniverse.getIdentifier(),
				biobankUniverse.getName(), biobankUniverse.getMembers(), biobankUniverse.getOwner(),
				biobankUniverse.getKeyConcepts(), biobankUniverseMemberVectors);

		dataService.update(BiobankUniverseMetaData.ENTITY_NAME, biobankUniverseToEntity(updatedBiobankUniverse));
	}

	@Override
	public void removeUniverseMembers(BiobankUniverse biobankUniverse, List<BiobankSampleCollection> members)
	{
		List<BiobankSampleCollection> remainingMembers = biobankUniverse.getMembers().stream()
				.filter(member -> !members.contains(member)).collect(Collectors.toList());

		List<BiobankUniverseMemberVector> remainingVectors = biobankUniverse.getVectors().stream()
				.filter(vector -> !members.contains(vector.getBiobankSampleCollection())).collect(Collectors.toList());

		Entity biobankUniverseToEntity = biobankUniverseToEntity(
				BiobankUniverse.create(biobankUniverse.getIdentifier(), biobankUniverse.getName(), remainingMembers,
						biobankUniverse.getOwner(), biobankUniverse.getKeyConcepts(), remainingVectors));

		dataService.update(BiobankUniverseMetaData.ENTITY_NAME, biobankUniverseToEntity);
	}

	@Override
	public void addBiobankSampleCollection(BiobankSampleCollection biobankSampleCollection)
	{
		dataService.add(BiobankSampleCollectionMetaData.ENTITY_NAME,
				biobankSampleCollectionToEntity(biobankSampleCollection));
	}

	@Override
	public void removeBiobankSampleCollection(BiobankSampleCollection biobankSampleCollection)
	{
		Entity biobankSampleCollectionToEntity = biobankSampleCollectionToEntity(biobankSampleCollection);

		// Remove bioibankSampleAttribute, attributeMappingCandidates, Explanations and Decisions
		removeBiobankSampleAttributes(getBiobankSampleAttributes(biobankSampleCollection));

		// Remove the biobankSampleColleciton membership from all BiobankUniverses
		dataService
				.findAll(BiobankUniverseMetaData.ENTITY_NAME,
						QueryImpl.EQ(BiobankUniverseMetaData.MEMBERS, biobankSampleCollectionToEntity))
				.map(this::entityToBiobankUniverse)
				.forEach(universe -> removeUniverseMembers(universe, Arrays.asList(biobankSampleCollection)));

		// Remove the biobankSampleCollection itself
		dataService.delete(BiobankSampleCollectionMetaData.ENTITY_NAME, biobankSampleCollectionToEntity);
	}

	@Override
	public List<BiobankSampleCollection> getAllBiobankSampleCollections()
	{
		return dataService.findAll(BiobankSampleCollectionMetaData.ENTITY_NAME)
				.map(this::entityToBiobankSampleCollection).collect(Collectors.toList());
	}

	@Override
	public BiobankSampleCollection getBiobankSampleCollection(String name)
	{
		Entity entity = dataService.findOne(BiobankSampleCollectionMetaData.ENTITY_NAME, name);
		return entity == null ? null : entityToBiobankSampleCollection(entity);
	}

	@Override
	public boolean isBiobankSampleCollectionTagged(BiobankSampleCollection biobankSampleCollection)
	{
		Fetch fetchOntologyTerm = new Fetch();
		OntologyTermMetaData.INSTANCE.getAtomicAttributes().forEach(field -> fetchOntologyTerm.field(field.getName()));

		Fetch fetchSemanticType = new Fetch();
		SemanticTypeMetaData.INSTANCE.getAtomicAttributes().forEach(field -> fetchSemanticType.field(field.getName()));

		Fetch fetchTagGroupFields = new Fetch();
		TagGroupMetaData.INSTANCE.getAtomicAttributes()
				.forEach(attribute -> fetchTagGroupFields.field(attribute.getName()));
		fetchTagGroupFields.field(TagGroupMetaData.ONTOLOGY_TERMS, fetchOntologyTerm);
		fetchTagGroupFields.field(TagGroupMetaData.SEMANTIC_TYPES, fetchSemanticType);

		Fetch fetch = new Fetch();
		fetch.field(BiobankSampleAttributeMetaData.COLLECTION);
		fetch.field(BiobankSampleAttributeMetaData.TAG_GROUPS, fetchTagGroupFields);

		// Check if the first 100 biobankSampleAttributes have been tagged
		boolean anyMatch = dataService
				.findAll(BiobankSampleAttributeMetaData.ENTITY_NAME,
						EQ(BiobankSampleAttributeMetaData.COLLECTION, biobankSampleCollection.getName()).pageSize(100)
								.fetch(fetch))
				.anyMatch(entity -> Iterables.size((Iterable<?>) entity.get(TAG_GROUPS)) != 0);

		return anyMatch;
	}

	@Override
	public List<BiobankSampleAttribute> getBiobankSampleAttributes(BiobankSampleCollection biobankSampleCollection)
	{
		Fetch fetchOntologyTerm = new Fetch();
		OntologyTermMetaData.INSTANCE.getAtomicAttributes().forEach(field -> fetchOntologyTerm.field(field.getName()));

		Fetch fetchSemanticType = new Fetch();
		SemanticTypeMetaData.INSTANCE.getAtomicAttributes().forEach(field -> fetchSemanticType.field(field.getName()));

		Fetch fetchTagGroupFields = new Fetch();
		TagGroupMetaData.INSTANCE.getAtomicAttributes()
				.forEach(attribute -> fetchTagGroupFields.field(attribute.getName()));
		fetchTagGroupFields.field(TagGroupMetaData.ONTOLOGY_TERMS, fetchOntologyTerm);
		fetchTagGroupFields.field(TagGroupMetaData.SEMANTIC_TYPES, fetchSemanticType);

		Fetch fetch = new Fetch();
		fetch.field(BiobankSampleAttributeMetaData.COLLECTION);
		fetch.field(BiobankSampleAttributeMetaData.TAG_GROUPS, fetchTagGroupFields);

		List<BiobankSampleAttribute> biobankSampleAttributes = dataService
				.findAll(BiobankSampleAttributeMetaData.ENTITY_NAME,
						EQ(BiobankSampleAttributeMetaData.COLLECTION, biobankSampleCollection.getName()).fetch(fetch))
				.map(this::entityToBiobankSampleAttribute).collect(toList());

		return biobankSampleAttributes;
	}

	@Override
	public int countBiobankSampleAttributes(BiobankSampleCollection biobankSampleCollection)
	{
		int count = (int) dataService.count(BiobankSampleAttributeMetaData.ENTITY_NAME,
				QueryImpl.EQ(BiobankSampleAttributeMetaData.COLLECTION, biobankSampleCollection.getName()));
		return count;
	}

	@Override
	public List<String> getBiobankSampleAttributeIdentifiers(BiobankSampleCollection biobankSampleCollection)
	{
		List<String> biobankSampleAttributeIdentifiers = dataService
				.findAll(BiobankSampleAttributeMetaData.ENTITY_NAME,
						EQ(BiobankSampleAttributeMetaData.COLLECTION, biobankSampleCollection.getName()))
				.map(entity -> entity.getIdValue().toString()).collect(Collectors.toList());

		return biobankSampleAttributeIdentifiers;
	}

	@Override
	public void addBiobankSampleAttributes(Stream<BiobankSampleAttribute> biobankSampleAttributeStream)
	{
		Stream<Entity> biobankSampleAttributeEntityStream = biobankSampleAttributeStream
				.map(this::biobankSampleAttributeToEntity);

		dataService.add(BiobankSampleAttributeMetaData.ENTITY_NAME, biobankSampleAttributeEntityStream);
	}

	@Override
	public void removeBiobankSampleAttributes(Iterable<BiobankSampleAttribute> biobankSampleAttributes)
	{
		// Remove all associated candidate matches
		removeAttributeMappingCandidates(getAttributeMappingCandidateEntities(biobankSampleAttributes));

		// Remove all associated tag groups
		removeTagGroupsForAttributes(biobankSampleAttributes);

		Stream<Entity> biobankSampleAttributeEntityStream = stream(biobankSampleAttributes.spliterator(), false)
				.map(this::biobankSampleAttributeToEntity);

		dataService.delete(BiobankSampleAttributeMetaData.ENTITY_NAME, biobankSampleAttributeEntityStream);
	}

	@Override
	public Stream<BiobankSampleAttribute> queryBiobankSampleAttribute(Query query)
	{
		Fetch fetchOntologyTerm = new Fetch();
		OntologyTermMetaData.INSTANCE.getAtomicAttributes().forEach(field -> fetchOntologyTerm.field(field.getName()));

		Fetch fetchSemanticType = new Fetch();
		SemanticTypeMetaData.INSTANCE.getAtomicAttributes().forEach(field -> fetchSemanticType.field(field.getName()));

		Fetch fetchTagGroupFields = new Fetch();
		TagGroupMetaData.INSTANCE.getAtomicAttributes()
				.forEach(attribute -> fetchTagGroupFields.field(attribute.getName()));
		fetchTagGroupFields.field(TagGroupMetaData.ONTOLOGY_TERMS, fetchOntologyTerm);
		fetchTagGroupFields.field(TagGroupMetaData.SEMANTIC_TYPES, fetchSemanticType);

		Fetch fetch = new Fetch();
		fetch.field(BiobankSampleAttributeMetaData.COLLECTION);
		fetch.field(BiobankSampleAttributeMetaData.TAG_GROUPS, fetchTagGroupFields);

		return dataService.findAll(BiobankSampleAttributeMetaData.ENTITY_NAME, query.fetch(fetch))
				.map(this::entityToBiobankSampleAttribute);
	}

	@Override
	public void addTagGroupsForAttributes(Iterable<BiobankSampleAttribute> biobankSampleAttributes)
	{
		Stream<Entity> tagGroupEntityStream = stream(biobankSampleAttributes.spliterator(), false)
				.flatMap(attribute -> attribute.getTagGroups().stream()).map(this::identifiableTagGroupToEntity);
		dataService.add(TagGroupMetaData.ENTITY_NAME, tagGroupEntityStream);

		Stream<Entity> attributeEntityStream = stream(biobankSampleAttributes.spliterator(), false)
				.map(this::biobankSampleAttributeToEntity);

		dataService.update(BiobankSampleAttributeMetaData.ENTITY_NAME, attributeEntityStream);
	}

	@Override
	public void removeTagGroupsForAttributes(Iterable<BiobankSampleAttribute> biobankSampleAttributes)
	{
		Stream<Entity> identifiableTagGroupEntityStream = stream(biobankSampleAttributes.spliterator(), false)
				.flatMap(biobankSampleAttribute -> biobankSampleAttribute.getTagGroups().stream())
				.map(this::identifiableTagGroupToEntity);

		Stream<Entity> biobankSampleAttributeEntityStream = stream(biobankSampleAttributes.spliterator(), false)
				.map(biobankSampleAttribute -> BiobankSampleAttribute.create(biobankSampleAttribute.getIdentifier(),
						biobankSampleAttribute.getName(), biobankSampleAttribute.getLabel(),
						biobankSampleAttribute.getDescription(), biobankSampleAttribute.getCollection(), emptyList()))
				.map(this::biobankSampleAttributeToEntity);

		// Remove the TagGroup references from BiobankSampleAttributes
		dataService.update(BiobankSampleAttributeMetaData.ENTITY_NAME, biobankSampleAttributeEntityStream);

		// Remove the TagGroups
		dataService.delete(TagGroupMetaData.ENTITY_NAME, identifiableTagGroupEntityStream);
	}

	@Override
	public void addAttributeMappingCandidates(List<AttributeMappingCandidate> biobankSampleAttributes)
	{
		Stream<Entity> explanationStream = biobankSampleAttributes.stream()
				.map(AttributeMappingCandidate::getExplanation).map(this::mappingExplanationToEntity);
		dataService.add(MatchingExplanationMetaData.ENTITY_NAME, explanationStream);

		Stream<Entity> attributeMappingCandidateStream = biobankSampleAttributes.stream()
				.map(this::attributeMappingCandidateToEntity);
		dataService.add(AttributeMappingCandidateMetaData.ENTITY_NAME, attributeMappingCandidateStream);
	}

	@Override
	public Iterable<AttributeMappingCandidate> getAttributeMappingCandidates(BiobankUniverse biobankUniverse)
	{
		return getAttributeMappingCandidates(biobankUniverse, null);
	}

	@Override
	public Iterable<AttributeMappingCandidate> getAttributeMappingCandidates(BiobankUniverse biobankUniverse,
			BiobankSampleCollection target)
	{
		Fetch fetchOntologyTerm = new Fetch();
		OntologyTermMetaData.INSTANCE.getAtomicAttributes().forEach(field -> fetchOntologyTerm.field(field.getName()));

		Fetch fetchSemanticType = new Fetch();
		SemanticTypeMetaData.INSTANCE.getAtomicAttributes().forEach(field -> fetchSemanticType.field(field.getName()));

		Fetch fetchTagGroupFields = new Fetch();
		TagGroupMetaData.INSTANCE.getAtomicAttributes()
				.forEach(attribute -> fetchTagGroupFields.field(attribute.getName()));
		fetchTagGroupFields.field(TagGroupMetaData.ONTOLOGY_TERMS, fetchOntologyTerm);
		fetchTagGroupFields.field(TagGroupMetaData.SEMANTIC_TYPES, fetchSemanticType);

		Fetch fetchBiobankSampleAttribute = new Fetch();
		fetchBiobankSampleAttribute.field(BiobankSampleAttributeMetaData.COLLECTION);
		fetchBiobankSampleAttribute.field(BiobankSampleAttributeMetaData.TAG_GROUPS, fetchTagGroupFields);

		Fetch fetchExplanation = new Fetch();
		fetchExplanation.field(MatchingExplanationMetaData.ONTOLOGY_TERMS, fetchOntologyTerm);

		Fetch fetch = new Fetch();
		fetch.field(AttributeMappingCandidateMetaData.TARGET, fetchBiobankSampleAttribute);
		fetch.field(AttributeMappingCandidateMetaData.SOURCE, fetchBiobankSampleAttribute);
		fetch.field(AttributeMappingCandidateMetaData.EXPLANATION, fetchExplanation);
		fetch.field(AttributeMappingCandidateMetaData.DECISIONS);

		List<QueryRule> nestedQueryRules = Lists
				.newArrayList(new QueryRule(BIOBANK_UNIVERSE, EQUALS, biobankUniverse.getIdentifier()));

		if (Objects.nonNull(target))
		{
			List<String> attributeIdentifiers = getBiobankSampleAttributeIdentifiers(target);

			nestedQueryRules.addAll(Arrays.asList(new QueryRule(AND), new QueryRule(TARGET, IN, attributeIdentifiers)));
		}

		List<AttributeMappingCandidate> attributeMappingCandidates = dataService
				.findAll(AttributeMappingCandidateMetaData.ENTITY_NAME, new QueryImpl(nestedQueryRules).fetch(fetch))
				.map(this::entityToAttributeMappingCandidate).collect(Collectors.toList());

		return attributeMappingCandidates;
	}

	@Override
	public List<AttributeMappingCandidate> getAttributeMappingCandidates(
			List<BiobankSampleAttribute> biobankSampleAttributes)
	{
		return StreamSupport.stream(getAttributeMappingCandidateEntities(biobankSampleAttributes).spliterator(), false)
				.map(this::entityToAttributeMappingCandidate).collect(toList());
	}

	@Override
	public List<AttributeMappingCandidate> getAttributeMappingCandidates(Query query)
	{
		Fetch fetchOntologyTerm = new Fetch();
		OntologyTermMetaData.INSTANCE.getAtomicAttributes().forEach(field -> fetchOntologyTerm.field(field.getName()));

		Fetch fetchSemanticType = new Fetch();
		SemanticTypeMetaData.INSTANCE.getAtomicAttributes().forEach(field -> fetchSemanticType.field(field.getName()));

		Fetch fetchTagGroupFields = new Fetch();
		TagGroupMetaData.INSTANCE.getAtomicAttributes()
				.forEach(attribute -> fetchTagGroupFields.field(attribute.getName()));
		fetchTagGroupFields.field(TagGroupMetaData.ONTOLOGY_TERMS, fetchOntologyTerm);
		fetchTagGroupFields.field(TagGroupMetaData.SEMANTIC_TYPES, fetchSemanticType);

		Fetch fetchBiobankSampleAttribute = new Fetch();
		fetchBiobankSampleAttribute.field(BiobankSampleAttributeMetaData.COLLECTION);
		fetchBiobankSampleAttribute.field(BiobankSampleAttributeMetaData.TAG_GROUPS, fetchTagGroupFields);

		Fetch fetchExplanation = new Fetch();
		fetchExplanation.field(MatchingExplanationMetaData.ONTOLOGY_TERMS, fetchOntologyTerm);

		Fetch fetch = new Fetch();
		fetch.field(AttributeMappingCandidateMetaData.TARGET, fetchBiobankSampleAttribute);
		fetch.field(AttributeMappingCandidateMetaData.SOURCE, fetchBiobankSampleAttribute);
		fetch.field(AttributeMappingCandidateMetaData.EXPLANATION, fetchExplanation);
		fetch.field(AttributeMappingCandidateMetaData.DECISIONS);

		return dataService.findAll(AttributeMappingCandidateMetaData.ENTITY_NAME, query.fetch(fetch))
				.map(this::entityToAttributeMappingCandidate).collect(Collectors.toList());
	}

	private List<Entity> getAttributeMappingCandidateEntities(Iterable<BiobankSampleAttribute> biobankSampleAttributes)
	{
		List<String> attributeIdentifiers = stream(biobankSampleAttributes.spliterator(), false)
				.map(BiobankSampleAttribute::getIdentifier).collect(Collectors.toList());

		Fetch fetch = new Fetch();
		AttributeMappingCandidateMetaData.INSTANCE.getAtomicAttributes().forEach(attr -> fetch.field(attr.getName()));

		List<Entity> attributeMappingCandidateEntities = dataService
				.findAll(AttributeMappingCandidateMetaData.ENTITY_NAME,
						IN(TARGET, attributeIdentifiers).or().in(SOURCE, attributeIdentifiers).fetch(fetch))
				.collect(Collectors.toList());

		return attributeMappingCandidateEntities;
	}

	@Override
	public void removeAttributeMappingCandidates(List<Entity> attributeMappingCandidateEntities)
	{
		Stream<Entity> mappingExplanationStream = attributeMappingCandidateEntities.stream()
				.map(entity -> entity.getEntity(AttributeMappingCandidateMetaData.EXPLANATION));

		Stream<Entity> attributeMappingDecisionStream = attributeMappingCandidateEntities.stream()
				.flatMap(entity -> StreamSupport
						.stream(entity.getEntities(AttributeMappingCandidateMetaData.DECISIONS).spliterator(), false));

		dataService.delete(AttributeMappingCandidateMetaData.ENTITY_NAME, attributeMappingCandidateEntities.stream());
		dataService.delete(MatchingExplanationMetaData.ENTITY_NAME, mappingExplanationStream);
		dataService.delete(AttributeMappingDecisionMetaData.ENTITY_NAME, attributeMappingDecisionStream);
	}

	private Entity biobankUniverseToEntity(BiobankUniverse biobankUniverse)
	{
		Iterable<Entity> semanticTypeEntities = entityManager.getReferences(SemanticTypeMetaData.INSTANCE,
				biobankUniverse.getKeyConcepts().stream().map(SemanticType::getIdentifier).collect(toList()));

		DefaultEntity entity = new DefaultEntity(BiobankUniverseMetaData.INSTANCE, dataService);
		entity.set(BiobankUniverseMetaData.IDENTIFIER, biobankUniverse.getIdentifier());
		entity.set(BiobankUniverseMetaData.NAME, biobankUniverse.getName());
		entity.set(BiobankUniverseMetaData.MEMBERS,
				biobankUniverse.getMembers().stream().map(BiobankSampleCollection::getName).collect(toList()));
		entity.set(BiobankUniverseMetaData.OWNER, biobankUniverse.getOwner());
		entity.set(BiobankUniverseMetaData.KEY_CONCEPTS, semanticTypeEntities);
		entity.set(BiobankUniverseMetaData.VECTORS, vectorsToJsonString(biobankUniverse.getVectors()));

		return entity;
	}

	private String vectorsToJsonString(List<BiobankUniverseMemberVector> vectors)
	{
		Map<String, String> collect = vectors.stream().collect(Collectors.toMap(
				vector -> vector.getBiobankSampleCollection().getName(), vector -> Arrays.toString(vector.getPoint())));

		return new Gson().toJson(collect);
	}

	private List<BiobankUniverseMemberVector> jsonStringToVectors(String json)
	{
		Map<String, String> fromJson = new Gson().fromJson(json, new TypeToken<Map<String, String>>()
		{
		}.getType());

		List<BiobankUniverseMemberVector> vectors = new ArrayList<>();

		for (Entry<String, String> entry : fromJson.entrySet())
		{
			BiobankSampleCollection biobankSampleCollection = getBiobankSampleCollection(entry.getKey());
			String vectorString = entry.getValue();
			String[] split = vectorString.replaceAll("[\\[\\]]", StringUtils.EMPTY).split(", ");
			double[] vector = Stream.of(split).filter(StringUtils::isNotBlank).mapToDouble(Double::valueOf).toArray();
			vectors.add(BiobankUniverseMemberVector.create(biobankSampleCollection, vector));
		}

		return vectors;
	}

	private BiobankUniverse entityToBiobankUniverse(Entity entity)
	{
		String identifier = entity.getString(BiobankUniverseMetaData.IDENTIFIER);
		String name = entity.getString(BiobankUniverseMetaData.NAME);
		MolgenisUser owner = molgenisUserService
				.getUser(entity.getEntity(BiobankUniverseMetaData.OWNER).getString(MolgenisUser.USERNAME));

		List<BiobankSampleCollection> members = new ArrayList<>();
		Iterable<Entity> memberIterable = entity.getEntities(BiobankUniverseMetaData.MEMBERS);
		if (memberIterable != null)
		{
			List<BiobankSampleCollection> collect = StreamSupport.stream(memberIterable.spliterator(), false)
					.map(this::entityToBiobankSampleCollection).collect(toList());
			members.addAll(collect);
		}

		List<SemanticType> keyConcepts = new ArrayList<>();
		Iterable<Entity> keyConceptIterable = entity.getEntities(BiobankUniverseMetaData.KEY_CONCEPTS);
		if (keyConceptIterable != null)
		{
			List<SemanticType> collect = StreamSupport.stream(keyConceptIterable.spliterator(), false)
					.map(OntologyTermRepository::entityToSemanticType).collect(Collectors.toList());
			keyConcepts.addAll(collect);
		}

		List<BiobankUniverseMemberVector> vectors = new ArrayList<>();
		String vectorJson = entity.getString(BiobankUniverseMetaData.VECTORS);
		if (StringUtils.isNotBlank(vectorJson))
		{
			vectors.addAll(jsonStringToVectors(vectorJson));
		}

		return BiobankUniverse.create(identifier, name, members, owner, keyConcepts, vectors);
	}

	private BiobankSampleCollection entityToBiobankSampleCollection(Entity entity)
	{
		String name = entity.getString(BiobankSampleCollectionMetaData.NAME);
		return BiobankSampleCollection.create(name);
	}

	private Entity biobankSampleCollectionToEntity(BiobankSampleCollection biobankSampleCollection)
	{
		DefaultEntity entity = new DefaultEntity(BiobankSampleCollectionMetaData.INSTANCE, dataService);
		entity.set(BiobankSampleCollectionMetaData.NAME, biobankSampleCollection.getName());
		return entity;
	}

	private BiobankSampleAttribute entityToBiobankSampleAttribute(Entity entity)
	{
		String identifier = entity.getString(BiobankSampleAttributeMetaData.IDENTIFIER);
		String name = entity.getString(BiobankSampleAttributeMetaData.NAME);
		String label = entity.getString(BiobankSampleAttributeMetaData.LABEL);
		String description = entity.getString(BiobankSampleAttributeMetaData.DESCRIPTION);

		BiobankSampleCollection biobankSampleCollection = entityToBiobankSampleCollection(
				entity.getEntity(BiobankSampleAttributeMetaData.COLLECTION));

		Iterable<Entity> entities = entity.getEntities(BiobankSampleAttributeMetaData.TAG_GROUPS);
		List<IdentifiableTagGroup> tagGroups = StreamSupport.stream(entities.spliterator(), false)
				.map(this::entityToIdentifiableTagGroup).collect(Collectors.toList());

		return BiobankSampleAttribute.create(identifier, name, label, description, biobankSampleCollection, tagGroups);
	}

	private Entity biobankSampleAttributeToEntity(BiobankSampleAttribute biobankSampleAttribute)
	{
		Iterable<Entity> tagGroupEntities = entityManager.getReferences(TagGroupMetaData.INSTANCE,
				biobankSampleAttribute.getTagGroups().stream().map(IdentifiableTagGroup::getIdentifier)
						.collect(toList()));

		DefaultEntity entity = new DefaultEntity(BiobankSampleAttributeMetaData.INSTANCE, dataService);
		entity.set(BiobankSampleAttributeMetaData.IDENTIFIER, biobankSampleAttribute.getIdentifier());
		entity.set(BiobankSampleAttributeMetaData.NAME, biobankSampleAttribute.getName());
		entity.set(BiobankSampleAttributeMetaData.LABEL, biobankSampleAttribute.getLabel());
		entity.set(BiobankSampleAttributeMetaData.DESCRIPTION, biobankSampleAttribute.getDescription());
		entity.set(BiobankSampleAttributeMetaData.COLLECTION, biobankSampleAttribute.getCollection().getName());
		entity.set(BiobankSampleAttributeMetaData.TAG_GROUPS, tagGroupEntities);

		return entity;
	}

	private Entity identifiableTagGroupToEntity(IdentifiableTagGroup tagGroup)
	{
		Iterable<Entity> ontologyTermEntities = entityManager.getReferences(OntologyTermMetaData.INSTANCE,
				tagGroup.getOntologyTerms().stream().map(OntologyTerm::getId).collect(toList()));
		Iterable<Entity> semanticTypeEntities = entityManager.getReferences(SemanticTypeMetaData.INSTANCE,
				tagGroup.getSemanticTypes().stream().map(SemanticType::getIdentifier).collect(toList()));
		DefaultEntity entity = new DefaultEntity(TagGroupMetaData.INSTANCE, dataService);
		entity.set(TagGroupMetaData.IDENTIFIER, tagGroup.getIdentifier());
		entity.set(TagGroupMetaData.ONTOLOGY_TERMS, ontologyTermEntities);
		entity.set(TagGroupMetaData.SEMANTIC_TYPES, semanticTypeEntities);
		entity.set(TagGroupMetaData.MATCHED_WORDS, tagGroup.getMatchedWords());
		entity.set(TagGroupMetaData.NGRAM_SCORE, (double) tagGroup.getScore());
		return entity;
	}

	private IdentifiableTagGroup entityToIdentifiableTagGroup(Entity entity)
	{
		String identifier = entity.getString(TagGroupMetaData.IDENTIFIER);
		String matchedWords = entity.getString(TagGroupMetaData.MATCHED_WORDS);
		Double ngramScore = entity.getDouble(TagGroupMetaData.NGRAM_SCORE);

		List<OntologyTerm> ontologyTerms = stream(entity.getEntities(TagGroupMetaData.ONTOLOGY_TERMS).spliterator(),
				false).map(OntologyTermRepository::toOntologyTerm).collect(toList());

		List<SemanticType> semanticTypes = stream(entity.getEntities(TagGroupMetaData.SEMANTIC_TYPES).spliterator(),
				false).map(OntologyTermRepository::entityToSemanticType).collect(toList());

		return IdentifiableTagGroup.create(identifier, ontologyTerms, semanticTypes, matchedWords,
				ngramScore.floatValue());
	}

	private AttributeMappingCandidate entityToAttributeMappingCandidate(Entity entity)
	{
		String identifier = entity.getString(AttributeMappingCandidateMetaData.IDENTIFIER);
		BiobankUniverse biobankUniverse = entityToBiobankUniverse(
				entity.getEntity(AttributeMappingCandidateMetaData.BIOBANK_UNIVERSE));
		BiobankSampleAttribute target = entityToBiobankSampleAttribute(
				entity.getEntity(AttributeMappingCandidateMetaData.TARGET));
		BiobankSampleAttribute source = entityToBiobankSampleAttribute(
				entity.getEntity(AttributeMappingCandidateMetaData.SOURCE));
		MatchingExplanation explanation = entityToMappingExplanation(
				entity.getEntity(AttributeMappingCandidateMetaData.EXPLANATION));

		List<AttributeMappingDecision> decisions = StreamSupport
				.stream(entity.getEntities(AttributeMappingCandidateMetaData.DECISIONS).spliterator(), false)
				.map(this::entityToAttributeMappingDecision)
				.filter(decistion -> decistion.getOwner().equals(userAcountService.getCurrentUser().getUsername()))
				.collect(Collectors.toList());

		return AttributeMappingCandidate.create(identifier, biobankUniverse, target, source, explanation, decisions);
	}

	private Entity attributeMappingCandidateToEntity(AttributeMappingCandidate attributeMappingCandidate)
	{
		String identifier = attributeMappingCandidate.getIdentifier();
		BiobankUniverse biobankUniverse = attributeMappingCandidate.getBiobankUniverse();
		BiobankSampleAttribute target = attributeMappingCandidate.getTarget();
		BiobankSampleAttribute source = attributeMappingCandidate.getSource();
		MatchingExplanation explanation = attributeMappingCandidate.getExplanation();

		Iterable<Entity> decisionEntities = entityManager.getReferences(AttributeMappingDecisionMetaData.INSTANCE,
				attributeMappingCandidate.getDecisions().stream().map(AttributeMappingDecision::getIdentifier)
						.collect(toList()));

		DefaultEntity entity = new DefaultEntity(AttributeMappingCandidateMetaData.INSTANCE, dataService);
		entity.set(AttributeMappingCandidateMetaData.IDENTIFIER, identifier);
		entity.set(AttributeMappingCandidateMetaData.BIOBANK_UNIVERSE, biobankUniverse.getIdentifier());
		entity.set(AttributeMappingCandidateMetaData.TARGET, target.getIdentifier());
		entity.set(AttributeMappingCandidateMetaData.SOURCE, source.getIdentifier());
		entity.set(AttributeMappingCandidateMetaData.EXPLANATION, explanation.getIdentifier());
		entity.set(AttributeMappingCandidateMetaData.DECISIONS, decisionEntities);

		return entity;
	}

	private Entity mappingExplanationToEntity(MatchingExplanation mappingExplanation)
	{
		String identifier = mappingExplanation.getIdentifier();
		String queryString = mappingExplanation.getQueryString();
		String matchedWords = mappingExplanation.getMatchedWords();
		double ngramScore = mappingExplanation.getNgramScore();

		Iterable<Entity> ontologyTermEntities = entityManager.getReferences(OntologyTermMetaData.INSTANCE,
				mappingExplanation.getOntologyTerms().stream().map(OntologyTerm::getId).collect(toList()));

		DefaultEntity entity = new DefaultEntity(MatchingExplanationMetaData.INSTANCE, dataService);
		entity.set(MatchingExplanationMetaData.IDENTIFIER, identifier);
		entity.set(MatchingExplanationMetaData.MATCHED_QUERY_STRING, queryString);
		entity.set(MatchingExplanationMetaData.MATCHED_WORDS, matchedWords);
		entity.set(MatchingExplanationMetaData.ONTOLOGY_TERMS, ontologyTermEntities);
		entity.set(MatchingExplanationMetaData.N_GRAM_SCORE, ngramScore);

		return entity;
	}

	private MatchingExplanation entityToMappingExplanation(Entity mappingExplanationEntity)
	{
		String identifier = mappingExplanationEntity.getString(MatchingExplanationMetaData.IDENTIFIER);
		String queryString = mappingExplanationEntity.getString(MatchingExplanationMetaData.MATCHED_QUERY_STRING);
		String matchedWords = mappingExplanationEntity.getString(MatchingExplanationMetaData.MATCHED_WORDS);
		Double ngramScore = mappingExplanationEntity.getDouble(MatchingExplanationMetaData.N_GRAM_SCORE);

		List<OntologyTerm> ontologyTerms = new ArrayList<>();
		Iterable<Entity> ontologyTermEntities = mappingExplanationEntity
				.getEntities(MatchingExplanationMetaData.ONTOLOGY_TERMS);
		if (ontologyTermEntities != null)
		{
			List<OntologyTerm> collect = stream(ontologyTermEntities.spliterator(), false)
					.map(OntologyTermRepository::toOntologyTerm).collect(toList());
			ontologyTerms.addAll(collect);
		}

		return MatchingExplanation.create(identifier, ontologyTerms, queryString, matchedWords, ngramScore);
	}

	private AttributeMappingDecision entityToAttributeMappingDecision(Entity entity)
	{
		String identifier = entity.getString(AttributeMappingDecisionMetaData.IDENTIFIER);
		String owner = entity.getString(AttributeMappingDecisionMetaData.OWNER);
		DecisionOptions decision = DecisionOptions.valueOf(entity.getString(AttributeMappingDecisionMetaData.DECISION));
		String comment = entity.getString(AttributeMappingDecisionMetaData.COMMENT);
		return AttributeMappingDecision.create(identifier, decision, comment, owner);
	}

	private Entity attributeMappingDecisionToEntity(AttributeMappingDecision attributeMappingDecision)
	{
		String identifier = attributeMappingDecision.getIdentifier();
		String comment = attributeMappingDecision.getComment();
		DecisionOptions decision = attributeMappingDecision.getDecision();
		String owner = attributeMappingDecision.getOwner();

		DefaultEntity entity = new DefaultEntity(AttributeMappingDecisionMetaData.INSTANCE, dataService);
		entity.set(AttributeMappingDecisionMetaData.IDENTIFIER, identifier);
		entity.set(AttributeMappingDecisionMetaData.OWNER, owner);
		entity.set(AttributeMappingDecisionMetaData.DECISION, decision);
		entity.set(AttributeMappingDecisionMetaData.COMMENT, comment);

		return entity;
	}
}