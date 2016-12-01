package org.molgenis.data.discovery.repo.impl;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.auth.MolgenisUser;
import org.molgenis.auth.MolgenisUserMetaData;
import org.molgenis.data.*;
import org.molgenis.data.discovery.job.BiobankUniverseJobExecutionMetaData;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData.BiobankAttributeDataType;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleCollectionMetaData;
import org.molgenis.data.discovery.meta.biobank.BiobankUniverseMemberVectorMetaData;
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
import org.molgenis.data.discovery.model.matching.*;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.meta.model.AttributeMetaData;
import org.molgenis.data.support.AggregateQueryImpl;
import org.molgenis.data.support.DynamicEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.meta.OntologyTermEntity;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.molgenis.ontology.core.meta.OntologyTermNodePathMetaData;
import org.molgenis.ontology.core.meta.SemanticTypeMetaData;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.repository.OntologyTermRepository;
import org.molgenis.security.user.MolgenisUserService;
import org.molgenis.security.user.UserAccountService;

import java.util.*;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.molgenis.data.QueryRule.Operator.*;
import static org.molgenis.data.discovery.job.BiobankUniverseJobExecutionMetaData.BIOBANK_UNIVERSE_JOB_EXECUTION;
import static org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData.BIOBANK_SAMPLE_ATTRIBUTE;
import static org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData.BiobankAttributeDataType.toEnum;
import static org.molgenis.data.discovery.meta.biobank.BiobankSampleCollectionMetaData.BIOBANK_SAMPLE_COLLECTION;
import static org.molgenis.data.discovery.meta.biobank.BiobankUniverseMemberVectorMetaData.BIOBANK_UNIVERSE_MEMBER_VECTOR;
import static org.molgenis.data.discovery.meta.biobank.BiobankUniverseMetaData.BIOBANK_UNIVERSE;
import static org.molgenis.data.discovery.meta.matching.AttributeMappingCandidateMetaData.*;
import static org.molgenis.data.discovery.meta.matching.AttributeMappingDecisionMetaData.ATTRIBUTE_MAPPING_DECISION;
import static org.molgenis.data.discovery.meta.matching.AttributeMappingDecisionMetaData.DecisionOptions.YES;
import static org.molgenis.data.discovery.meta.matching.MatchingExplanationMetaData.MATCHING_EXPLANATION;
import static org.molgenis.data.discovery.meta.matching.TagGroupMetaData.TAG_GROUP;
import static org.molgenis.data.support.QueryImpl.EQ;
import static org.molgenis.data.support.QueryImpl.IN;

public class BiobankUniverseRepositoryImpl implements BiobankUniverseRepository
{
	private final DataService dataService;
	private final MolgenisUserService molgenisUserService;
	private final UserAccountService userAcountService;
	private final EntityManager entityManager;
	private final BiobankUniverseMetaData biobankUniverseMetaData;
	private final BiobankUniverseMemberVectorMetaData biobankUniverseMemberVectorMetaData;
	private final BiobankSampleCollectionMetaData biobankSampleCollectionMetaData;
	private final BiobankSampleAttributeMetaData biobankSampleAttributeMetaData;
	private final AttributeMappingCandidateMetaData attributeMappingCandidateMetaData;
	private final MatchingExplanationMetaData matchingExplanationMetaData;
	private final AttributeMappingDecisionMetaData attributeMappingDecisionMetaData;
	private final TagGroupMetaData tagGroupMetaData;
	private final OntologyTermMetaData ontologyTermMetaData;
	private final SemanticTypeMetaData semanticTypeMetaData;
	private final OntologyTermNodePathMetaData ontologyTermNodePathMetaData;

	public BiobankUniverseRepositoryImpl(DataService dataService, MolgenisUserService molgenisUserService,
			UserAccountService userAcountService, EntityManager entityManager,
			BiobankUniverseMetaData biobankUniverseMetaData,
			BiobankUniverseMemberVectorMetaData biobankUniverseMemberVectorMetaData,
			BiobankSampleCollectionMetaData biobankSampleCollectionMetaData,
			BiobankSampleAttributeMetaData biobankSampleAttributeMetaData,
			MatchingExplanationMetaData matchingExplanationMetaData,
			AttributeMappingCandidateMetaData attributeMappingCandidateMetaData,
			AttributeMappingDecisionMetaData attributeMappingDecisionMetaData, TagGroupMetaData tagGroupMetaData,
			OntologyTermMetaData ontologyTermMetaData, SemanticTypeMetaData semanticTypeMetaData,
			OntologyTermNodePathMetaData ontologyTermNodePathMetaData)
	{
		this.dataService = requireNonNull(dataService);
		this.molgenisUserService = requireNonNull(molgenisUserService);
		this.userAcountService = requireNonNull(userAcountService);
		this.entityManager = requireNonNull(entityManager);
		this.biobankUniverseMetaData = requireNonNull(biobankUniverseMetaData);
		this.biobankUniverseMemberVectorMetaData = requireNonNull(biobankUniverseMemberVectorMetaData);
		this.biobankSampleCollectionMetaData = requireNonNull(biobankSampleCollectionMetaData);
		this.biobankSampleAttributeMetaData = requireNonNull(biobankSampleAttributeMetaData);
		this.attributeMappingCandidateMetaData = requireNonNull(attributeMappingCandidateMetaData);
		this.attributeMappingDecisionMetaData = requireNonNull(attributeMappingDecisionMetaData);
		this.matchingExplanationMetaData = requireNonNull(matchingExplanationMetaData);
		this.tagGroupMetaData = requireNonNull(tagGroupMetaData);
		this.ontologyTermMetaData = requireNonNull(ontologyTermMetaData);
		this.semanticTypeMetaData = requireNonNull(semanticTypeMetaData);
		this.ontologyTermNodePathMetaData = requireNonNull(ontologyTermNodePathMetaData);
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
			BiobankUniverse newBiobankUniverse = BiobankUniverse
					.create(biobankUniverse.getIdentifier(), biobankUniverse.getName(), biobankUniverse.getMembers(),
							biobankUniverse.getOwner(), keyConcepts, biobankUniverse.getVectors());
			dataService.update(BIOBANK_UNIVERSE, biobankUniverseToEntity(newBiobankUniverse));
		}
	}

	@Override
	public List<BiobankUniverse> getAllUniverses()
	{
		List<BiobankUniverse> universes = dataService.findAll(BIOBANK_UNIVERSE).map(this::entityToBiobankUniverse)
				.collect(toList());
		return universes;
	}

	@Override
	public BiobankUniverse getUniverse(String identifier)
	{
		Fetch fetch = new Fetch();
		biobankUniverseMetaData.getAtomicAttributes().forEach(attr -> fetch.field(attr.getName()));
		Entity findOne = dataService
				.findOne(BIOBANK_UNIVERSE, QueryImpl.EQ(BiobankUniverseMetaData.IDENTIFIER, identifier).fetch(fetch));
		return findOne == null ? null : entityToBiobankUniverse(findOne);
	}

	@Override
	public void addBiobankUniverse(BiobankUniverse biobankUniverse)
	{
		Entity entity = biobankUniverseToEntity(biobankUniverse);
		dataService.add(BIOBANK_UNIVERSE, entity);
	}

	@Override
	public void removeBiobankUniverse(BiobankUniverse biobankUniverse)
	{
		List<String> attributeIdentifiers = biobankUniverse.getMembers().stream()
				.flatMap(member -> getBiobankSampleAttributeIdentifiers(member).stream()).collect(toList());

		if (!attributeIdentifiers.isEmpty())
		{
			Fetch fetchOntologyTerm = new Fetch();
			ontologyTermMetaData.getAtomicAttributes().forEach(field -> fetchOntologyTerm.field(field.getName()));

			Fetch fetchSemanticType = new Fetch();
			semanticTypeMetaData.getAtomicAttributes().forEach(field -> fetchSemanticType.field(field.getName()));

			Fetch fetchTagGroupFields = new Fetch();
			tagGroupMetaData.getAtomicAttributes().forEach(attribute -> fetchTagGroupFields.field(attribute.getName()));
			fetchTagGroupFields.field(TagGroupMetaData.ONTOLOGY_TERMS, fetchOntologyTerm);
			fetchTagGroupFields.field(TagGroupMetaData.SEMANTIC_TYPES, fetchSemanticType);

			Fetch fetchBiobankSampleAttribute = new Fetch();
			biobankSampleAttributeMetaData.getAtomicAttributes()
					.forEach(attribute -> fetchBiobankSampleAttribute.field(attribute.getName()));
			fetchBiobankSampleAttribute.field(BiobankSampleAttributeMetaData.COLLECTION);
			fetchBiobankSampleAttribute.field(BiobankSampleAttributeMetaData.TAG_GROUPS, fetchTagGroupFields);

			Fetch fetchExplanation = new Fetch();
			matchingExplanationMetaData.getAtomicAttributes()
					.forEach(attribute -> fetchExplanation.field(attribute.getName()));
			fetchExplanation.field(MatchingExplanationMetaData.TARGET_ONTOLOGY_TERMS, fetchOntologyTerm);

			Fetch attributeMappingCandidateFetch = new Fetch();
			attributeMappingCandidateMetaData.getAtomicAttributes()
					.forEach(attribute -> attributeMappingCandidateFetch.field(attribute.getName()));
			attributeMappingCandidateFetch.field(AttributeMappingCandidateMetaData.TARGET, fetchBiobankSampleAttribute);
			attributeMappingCandidateFetch.field(AttributeMappingCandidateMetaData.SOURCE, fetchBiobankSampleAttribute);
			attributeMappingCandidateFetch.field(AttributeMappingCandidateMetaData.EXPLANATION, fetchExplanation);
			attributeMappingCandidateFetch.field(AttributeMappingCandidateMetaData.DECISIONS);

			List<QueryRule> innerQueryRules = newArrayList(new QueryRule(TARGET, IN, attributeIdentifiers),
					new QueryRule(OR), new QueryRule(SOURCE, IN, attributeIdentifiers));

			List<QueryRule> nestedQueryRules = newArrayList(
					new QueryRule(AttributeMappingCandidateMetaData.BIOBANK_UNIVERSE, EQUALS,
							biobankUniverse.getIdentifier()), new QueryRule(AND), new QueryRule(innerQueryRules));

			List<Entity> attributeMappingCandidateEntities = dataService.findAll(ATTRIBUTE_MAPPING_CANDIDATE,
					new QueryImpl<Entity>(nestedQueryRules).fetch(attributeMappingCandidateFetch)).collect(toList());

			// Remove attributeMappingCandidates, explanations and decisions
			removeAttributeMappingCandidates(attributeMappingCandidateEntities);
		}

		// Remove the BiobankUniverseJobExecutions in which the universe is involved
		Stream<Entity> biobankUniverseJobEntityStream = dataService.findAll(BIOBANK_UNIVERSE_JOB_EXECUTION,
				QueryImpl.EQ(BiobankUniverseJobExecutionMetaData.UNIVERSE, biobankUniverse.getIdentifier()));

		dataService.delete(BIOBANK_UNIVERSE_JOB_EXECUTION, biobankUniverseJobEntityStream);

		//Remove all the biobankSampleMemberVectors
		Stream<Entity> biobankSampleMemberVectorStream = biobankUniverse.getVectors().stream()
				.map(vector -> biobankUniverseMemberVectorToEntity(biobankUniverse, vector));

		dataService.delete(BIOBANK_UNIVERSE_MEMBER_VECTOR, biobankSampleMemberVectorStream);

		// Remove the BiobankUniverse itself
		dataService.delete(BIOBANK_UNIVERSE, biobankUniverseToEntity(biobankUniverse));

	}

	@Override
	public void addUniverseMembers(BiobankUniverse biobankUniverse, List<BiobankSampleCollection> members)
	{
		List<BiobankSampleCollection> allMembers = Stream
				.concat(biobankUniverse.getMembers().stream(), members.stream()).distinct().collect(toList());

		Entity biobankUniverseToEntity = biobankUniverseToEntity(BiobankUniverse
				.create(biobankUniverse.getIdentifier(), biobankUniverse.getName(), allMembers,
						biobankUniverse.getOwner(), biobankUniverse.getKeyConcepts(), biobankUniverse.getVectors()));

		dataService.update(BIOBANK_UNIVERSE, biobankUniverseToEntity);
	}

	@Override
	public void updateBiobankUniverseMemberVectors(BiobankUniverse biobankUniverse,
			List<BiobankUniverseMemberVector> biobankUniverseMemberVectors)
	{
		BiobankUniverse updatedBiobankUniverse = BiobankUniverse
				.create(biobankUniverse.getIdentifier(), biobankUniverse.getName(), biobankUniverse.getMembers(),
						biobankUniverse.getOwner(), biobankUniverse.getKeyConcepts(), biobankUniverseMemberVectors);

		Stream<Entity> existingVectorStream = dataService.findAll(BIOBANK_UNIVERSE_MEMBER_VECTOR, new QueryImpl<>()
				.eq(BiobankUniverseMemberVectorMetaData.BIOBANK_UNIVERSE, biobankUniverse.getIdentifier()));

		dataService.delete(BIOBANK_UNIVERSE_MEMBER_VECTOR, existingVectorStream);

		dataService.add(BIOBANK_UNIVERSE_MEMBER_VECTOR, biobankUniverseMemberVectors.stream()
				.map(vector -> biobankUniverseMemberVectorToEntity(biobankUniverse, vector)));
	}

	@Override
	public void removeUniverseMembers(BiobankUniverse biobankUniverse, List<BiobankSampleCollection> members)
	{
		List<BiobankSampleCollection> remainingMembers = biobankUniverse.getMembers().stream()
				.filter(member -> !members.contains(member)).collect(toList());

		List<BiobankUniverseMemberVector> remainingVectors = biobankUniverse.getVectors().stream()
				.filter(vector -> !members.contains(vector.getBiobankSampleCollection())).collect(toList());

		Entity biobankUniverseToEntity = biobankUniverseToEntity(BiobankUniverse
				.create(biobankUniverse.getIdentifier(), biobankUniverse.getName(), remainingMembers,
						biobankUniverse.getOwner(), biobankUniverse.getKeyConcepts(), remainingVectors));

		dataService.update(BIOBANK_UNIVERSE, biobankUniverseToEntity);
	}

	@Override
	public void addBiobankSampleCollection(BiobankSampleCollection biobankSampleCollection)
	{
		dataService.add(BIOBANK_SAMPLE_COLLECTION, biobankSampleCollectionToEntity(biobankSampleCollection));
	}

	@Override
	public void removeBiobankSampleCollection(BiobankSampleCollection biobankSampleCollection)
	{
		Entity biobankSampleCollectionToEntity = biobankSampleCollectionToEntity(biobankSampleCollection);

		// Remove bioibankSampleAttribute, attributeMappingCandidates, Explanations and Decisions
		removeBiobankSampleAttributes(getBiobankSampleAttributes(biobankSampleCollection));

		// Remove the biobankSampleColleciton membership from all BiobankUniverses
		dataService.findAll(BIOBANK_UNIVERSE,
				QueryImpl.EQ(BiobankUniverseMetaData.MEMBERS, biobankSampleCollectionToEntity))
				.map(this::entityToBiobankUniverse)
				.forEach(universe -> removeUniverseMembers(universe, asList(biobankSampleCollection)));

		// Remove the biobankSampleCollection itself
		dataService.delete(BIOBANK_SAMPLE_COLLECTION, biobankSampleCollectionToEntity);
	}

	@Override
	public List<BiobankSampleCollection> getAllBiobankSampleCollections()
	{
		return dataService.findAll(BIOBANK_SAMPLE_COLLECTION).map(this::entityToBiobankSampleCollection)
				.collect(toList());
	}

	@Override
	public BiobankSampleCollection getBiobankSampleCollection(String name)
	{
		Entity entity = dataService.findOneById(BIOBANK_SAMPLE_COLLECTION, name);
		return entity == null ? null : entityToBiobankSampleCollection(entity);
	}

	@Override
	public boolean isBiobankSampleCollectionTagged(BiobankSampleCollection biobankSampleCollection)
	{
		Query<Entity> query = new QueryImpl<Entity>()
				.eq(BiobankSampleAttributeMetaData.COLLECTION, biobankSampleCollection.getName()).pageSize(100);

		List<BiobankSampleAttribute> biobankSampleAttributes = getBiobankSampleAttributes(query);

		// Check if the first 100 biobankSampleAttributes have been tagged
		return biobankSampleAttributes.stream().map(BiobankSampleAttribute::getTagGroups)
				.anyMatch(list -> !list.isEmpty());
	}

	@Override
	public List<BiobankSampleAttribute> getBiobankSampleAttributes(BiobankSampleCollection biobankSampleCollection)
	{
		Query<Entity> query = new QueryImpl<Entity>()
				.eq(BiobankSampleAttributeMetaData.COLLECTION, biobankSampleCollection.getName());

		return getBiobankSampleAttributes(query);
	}

	@Override
	public BiobankSampleAttribute getBiobankSampleAttributes(String attributeIdentifier)
	{
		Query<Entity> query = new QueryImpl<Entity>()
				.eq(BiobankSampleAttributeMetaData.IDENTIFIER, attributeIdentifier);

		List<BiobankSampleAttribute> biobankSampleAttributes = getBiobankSampleAttributes(query);

		return biobankSampleAttributes.isEmpty() ? null : biobankSampleAttributes.get(0);
	}

	@Override
	public int countBiobankSampleAttributes(BiobankSampleCollection biobankSampleCollection)
	{
		int count = (int) dataService.count(BIOBANK_SAMPLE_ATTRIBUTE,
				QueryImpl.EQ(BiobankSampleAttributeMetaData.COLLECTION, biobankSampleCollection.getName()));
		return count;
	}

	@Override
	public List<String> getBiobankSampleAttributeIdentifiers(BiobankSampleCollection biobankSampleCollection)
	{
		List<String> biobankSampleAttributeIdentifiers = dataService.findAll(BIOBANK_SAMPLE_ATTRIBUTE,
				EQ(BiobankSampleAttributeMetaData.COLLECTION, biobankSampleCollection.getName()))
				.map(entity -> entity.getIdValue().toString()).collect(toList());

		return biobankSampleAttributeIdentifiers;
	}

	@Override
	public void addBiobankSampleAttributes(Stream<BiobankSampleAttribute> biobankSampleAttributeStream)
	{
		Stream<Entity> biobankSampleAttributeEntityStream = biobankSampleAttributeStream
				.map(this::biobankSampleAttributeToEntity);

		dataService.add(BIOBANK_SAMPLE_ATTRIBUTE, biobankSampleAttributeEntityStream);
	}

	@Override
	public void removeBiobankSampleAttributes(List<BiobankSampleAttribute> biobankSampleAttributes)
	{
		if (!biobankSampleAttributes.isEmpty())
		{
			// Remove all associated candidate matches
			removeAttributeMappingCandidates(getAttributeMappingCandidateEntities(biobankSampleAttributes));

			// Remove all associated tag groups
			removeTagGroupsForAttributes(biobankSampleAttributes);

			Stream<Entity> biobankSampleAttributeEntityStream = stream(biobankSampleAttributes.spliterator(), false)
					.map(this::biobankSampleAttributeToEntity);

			dataService.delete(BIOBANK_SAMPLE_ATTRIBUTE, biobankSampleAttributeEntityStream);
		}
	}

	@Override
	public List<BiobankSampleAttribute> getBiobankSampleAttributes(Query<Entity> query)
	{
		Fetch fetchOntologyTerm = new Fetch();
		ontologyTermMetaData.getAtomicAttributes().forEach(field -> fetchOntologyTerm.field(field.getName()));

		Fetch fetchSemanticType = new Fetch();
		semanticTypeMetaData.getAtomicAttributes().forEach(field -> fetchSemanticType.field(field.getName()));

		Fetch fetchTagGroupFields = new Fetch();
		tagGroupMetaData.getAtomicAttributes().forEach(attribute -> fetchTagGroupFields.field(attribute.getName()));
		fetchTagGroupFields.field(TagGroupMetaData.ONTOLOGY_TERMS, fetchOntologyTerm);
		fetchTagGroupFields.field(TagGroupMetaData.SEMANTIC_TYPES, fetchSemanticType);

		Fetch fetch = new Fetch();
		biobankSampleAttributeMetaData.getAtomicAttributes()
				.forEach(attributeMetaData -> fetch.field(attributeMetaData.getName()));
		fetch.field(BiobankSampleAttributeMetaData.COLLECTION);
		fetch.field(BiobankSampleAttributeMetaData.TAG_GROUPS, fetchTagGroupFields);

		return dataService.findAll(BIOBANK_SAMPLE_ATTRIBUTE, query.fetch(fetch))
				.map(this::entityToBiobankSampleAttribute).collect(toList());
	}

	@Override
	public void addTagGroupsForAttributes(List<BiobankSampleAttribute> biobankSampleAttributes)
	{
		Stream<Entity> tagGroupEntityStream = stream(biobankSampleAttributes.spliterator(), false)
				.flatMap(attribute -> attribute.getTagGroups().stream()).map(this::identifiableTagGroupToEntity);
		dataService.add(TagGroupMetaData.TAG_GROUP, tagGroupEntityStream);

		Stream<Entity> attributeEntityStream = stream(biobankSampleAttributes.spliterator(), false)
				.map(this::biobankSampleAttributeToEntity);

		dataService.update(BIOBANK_SAMPLE_ATTRIBUTE, attributeEntityStream);
	}

	@Override
	public void removeTagGroupsForAttributes(List<BiobankSampleAttribute> biobankSampleAttributes)
	{
		Stream<Entity> identifiableTagGroupEntityStream = stream(biobankSampleAttributes.spliterator(), false)
				.flatMap(biobankSampleAttribute -> biobankSampleAttribute.getTagGroups().stream())
				.map(this::identifiableTagGroupToEntity);

		Stream<Entity> biobankSampleAttributeEntityStream = stream(biobankSampleAttributes.spliterator(), false)
				.map(biobankSampleAttribute -> BiobankSampleAttribute.create(biobankSampleAttribute, emptyList()))
				.map(this::biobankSampleAttributeToEntity);

		// Remove the TagGroup references from BiobankSampleAttributes
		dataService.update(BIOBANK_SAMPLE_ATTRIBUTE, biobankSampleAttributeEntityStream);

		// Remove the TagGroups
		dataService.delete(TAG_GROUP, identifiableTagGroupEntityStream);
	}

	@Override
	public void addAttributeMappingCandidates(List<AttributeMappingCandidate> biobankSampleAttributes)
	{
		Stream<Entity> explanationStream = biobankSampleAttributes.stream()
				.map(AttributeMappingCandidate::getExplanation).map(this::mappingExplanationToEntity);
		dataService.add(MATCHING_EXPLANATION, explanationStream);

		Stream<Entity> attributeMappingCandidateStream = biobankSampleAttributes.stream()
				.map(this::attributeMappingCandidateToEntity);
		dataService.add(ATTRIBUTE_MAPPING_CANDIDATE, attributeMappingCandidateStream);
	}

	@Override
	public AggregateResult aggregateAttributeMatches(BiobankUniverse biobankUniverse,
			List<OntologyTerm> ontologyTermTopics, boolean curated)
	{
		AttributeMetaData targetCollection = attributeMappingCandidateMetaData
				.getAttribute(AttributeMappingCandidateMetaData.TARGET_COLLECTION);
		AttributeMetaData sourceCollection = attributeMappingCandidateMetaData
				.getAttribute(AttributeMappingCandidateMetaData.SOURCE_COLLECTION);
		AttributeMetaData identifier = attributeMappingCandidateMetaData
				.getAttribute(AttributeMappingCandidateMetaData.IDENTIFIER);

		Query<Entity> finalQuery = new QueryImpl<>()
				.eq(AttributeMappingCandidateMetaData.BIOBANK_UNIVERSE, biobankUniverse.getIdentifier());

		//First of all, retrieve all the decisions that belong to the current user, which indicate that associated candidates have been curated
		List<String> qualifiedDecisionIentifiers = getRelevantDecisionIdentifiers(biobankUniverse, curated);

		if (!qualifiedDecisionIentifiers.isEmpty())
		{
			finalQuery.and().in(AttributeMappingCandidateMetaData.DECISIONS, qualifiedDecisionIentifiers);
		}
		else if (curated)
		{
			return new AggregateResult(emptyList(), emptyList(), emptyList());
		}

		//Retrieve the BiobankSampleAttributes that are related to the given ontologyTermTopics
		List<String> relatedAttributeIdentifiers = findRelatedAttributeIdentifiers(biobankUniverse, ontologyTermTopics);

		if (!relatedAttributeIdentifiers.isEmpty())
		{
			finalQuery.and().nest().in(AttributeMappingCandidateMetaData.TARGET, relatedAttributeIdentifiers).and()
					.in(AttributeMappingCandidateMetaData.SOURCE, relatedAttributeIdentifiers).unnest();
		}
		else if (!ontologyTermTopics.isEmpty())
		{
			return new AggregateResult(emptyList(), emptyList(), emptyList());
		}

		AggregateQueryImpl aggregateQuery = new AggregateQueryImpl(targetCollection, sourceCollection, identifier,
				finalQuery);

		AggregateResult aggregate = dataService
				.aggregate(AttributeMappingCandidateMetaData.ATTRIBUTE_MAPPING_CANDIDATE, aggregateQuery);

		return aggregate;
	}

	private List<String> findRelatedAttributeIdentifiers(BiobankUniverse biobankUniverse,
			List<OntologyTerm> ontologyTerms)
	{
		List<String> nodePaths = ontologyTerms.stream().map(OntologyTerm::getNodePaths).flatMap(List::stream)
				.collect(toList());

		//Check if any ontology terms have any nodepaths
		if (!nodePaths.isEmpty())
		{
			List<String> members = biobankUniverse.getMembers().stream().map(BiobankSampleCollection::getName)
					.collect(toList());

			Query<Entity> query = new QueryImpl<>().in(BiobankSampleAttributeMetaData.COLLECTION, members).and().nest();

			for (String nodePath : nodePaths)
			{
				query.search(nodePath);
				if (nodePaths.indexOf(nodePath) != nodePaths.size() - 1)
				{
					query.or();
				}
			}

			query.unnest();

			return dataService
					.findAll(BiobankSampleAttributeMetaData.BIOBANK_SAMPLE_ATTRIBUTE, query.pageSize(Integer.MAX_VALUE))
					.map(Entity::getIdValue).map(Objects::toString).collect(toList());
		}

		return emptyList();
	}

	private List<String> getRelevantDecisionIdentifiers(BiobankUniverse biobankUniverse, boolean curated)
	{
		if (curated)
		{
			Query<Entity> queryOne = new QueryImpl<Entity>()
					.eq(AttributeMappingDecisionMetaData.UNIVERSE, biobankUniverse.getIdentifier()).and()
					.eq(AttributeMappingDecisionMetaData.OWNER, userAcountService.getCurrentUser().getUsername()).and()
					.eq(AttributeMappingDecisionMetaData.DECISION, YES.toString());

			return dataService.findAll(AttributeMappingDecisionMetaData.ATTRIBUTE_MAPPING_DECISION, queryOne)
					.map(Entity::getIdValue).map(Objects::toString).collect(toList());
		}

		return emptyList();
	}

	@Override
	public List<AttributeMappingCandidate> getAttributeMappingCandidates(BiobankUniverse biobankUniverse,
			BiobankSampleCollection target, AttributeMappingTablePager pager)
	{
		List<String> attributeIdentifiers = getBiobankSampleAttributeIdentifiers(target);

		int lowerBound = (pager.getCurrentPage() - 1) * pager.getPageSize();
		int upperBound = pager.getCurrentPage() * pager.getPageSize();

		//Make sure the lowerBound and upperBound don't exceed the size of the targetAttributes
		lowerBound = lowerBound > attributeIdentifiers.size() ? attributeIdentifiers.size() : lowerBound;
		upperBound = upperBound > attributeIdentifiers.size() ? attributeIdentifiers.size() : upperBound;

		//Only retrieve the AttributeMappingCandidates that are generated for the subset of targetAttributes
		List<String> visiableTargetAttributeIdentifiers = attributeIdentifiers.subList(lowerBound, upperBound);

		// FIXME: 25/10/16
		Query<Entity> query = new QueryImpl<Entity>()
				.eq(AttributeMappingCandidateMetaData.BIOBANK_UNIVERSE, biobankUniverse.getIdentifier()).and()
				.in(AttributeMappingCandidateMetaData.TARGET, visiableTargetAttributeIdentifiers);

		return getAttributeMappingCandidates(query).stream().map(this::entityToAttributeMappingCandidate)
				.collect(toList());
	}

	@Override
	public List<AttributeMappingCandidate> getAttributeMappingCandidates(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute targetAttrinute, BiobankSampleCollection targetSampleCollection,
			BiobankSampleCollection sourceSampleCollection)
	{
		Query<Entity> query = new QueryImpl<Entity>()
				.eq(AttributeMappingCandidateMetaData.BIOBANK_UNIVERSE, biobankUniverse.getIdentifier()).and()
				.eq(AttributeMappingCandidateMetaData.TARGET, targetAttrinute.getIdentifier()).and()
				.eq(AttributeMappingCandidateMetaData.TARGET_COLLECTION, targetSampleCollection.getName()).and()
				.eq(AttributeMappingCandidateMetaData.SOURCE_COLLECTION, sourceSampleCollection.getName());

		return getAttributeMappingCandidates(query).stream().map(this::entityToAttributeMappingCandidate)
				.collect(toList());
	}

	@Override
	public void updateAttributeMappingCandidateDecisions(
			List<AttributeMappingCandidate> attributeMappingCandidatesToUpdate, MolgenisUser molgenisUser)
	{
		//FIXME: to prevent the system to reindex the whole AttributeMappingCandidate table. Update the entities individually
		List<String> attributeMappingCandidateIdentifiers = attributeMappingCandidatesToUpdate.stream()
				.map(AttributeMappingCandidate::getIdentifier).collect(toList());

		List<Entity> attributeMappingCandidateEntities = getAttributeMappingCandidates(new QueryImpl<Entity>()
				.in(AttributeMappingCandidateMetaData.IDENTIFIER, attributeMappingCandidateIdentifiers));

		//Because AttributeMappingCandidate contains decisions by all the users. Therefore we need to get all
		//the existing decisions from the database and merge them with the new decision made by the current user
		Map<String, Set<String>> attributeCandidateToDecisionMap = new HashMap<>();
		for (Entity attributeMappingCandidateEntity : attributeMappingCandidateEntities)
		{
			String identifier = attributeMappingCandidateEntity.getString(AttributeMappingCandidateMetaData.IDENTIFIER);
			Iterable<Entity> decisionEntities = attributeMappingCandidateEntity
					.getEntities(AttributeMappingCandidateMetaData.DECISIONS);
			Set<String> existingDecisionIdentifiers = stream(decisionEntities.spliterator(), false)
					.map(Entity::getIdValue).map(Objects::toString).collect(toSet());
			attributeCandidateToDecisionMap.put(identifier, existingDecisionIdentifiers);
		}

		for (AttributeMappingCandidate attributeMappingCandidate : attributeMappingCandidatesToUpdate)
		{
			String identifier = attributeMappingCandidate.getIdentifier();

			List<String> decisionIdentifiers = Stream.concat(attributeCandidateToDecisionMap.get(identifier).stream(),
					attributeMappingCandidate.getDecisions().stream().map(AttributeMappingDecision::getIdentifier))
					.distinct().collect(toList());

			Entity biobankUniverseEntity = entityManager.getReference(biobankUniverseMetaData,
					attributeMappingCandidate.getBiobankUniverse().getIdentifier());

			Entity targetEntity = entityManager.getReference(biobankSampleAttributeMetaData,
					attributeMappingCandidate.getTarget().getIdentifier());

			Entity sourceEntity = entityManager.getReference(biobankSampleAttributeMetaData,
					attributeMappingCandidate.getSource().getIdentifier());

			Entity matchingExplanationEntity = entityManager.getReference(matchingExplanationMetaData,
					attributeMappingCandidate.getExplanation().getIdentifier());

			Iterable<Entity> decisionEntities = entityManager
					.getReferences(attributeMappingDecisionMetaData, decisionIdentifiers);

			Entity entity = new DynamicEntity(attributeMappingCandidateMetaData);
			entity.set(AttributeMappingCandidateMetaData.IDENTIFIER, identifier);
			entity.set(AttributeMappingCandidateMetaData.BIOBANK_UNIVERSE, biobankUniverseEntity);
			entity.set(AttributeMappingCandidateMetaData.TARGET, targetEntity);
			entity.set(AttributeMappingCandidateMetaData.SOURCE, sourceEntity);
			entity.set(AttributeMappingCandidateMetaData.TARGET_COLLECTION,
					attributeMappingCandidate.getTarget().getCollection().getName());
			entity.set(AttributeMappingCandidateMetaData.SOURCE_COLLECTION,
					attributeMappingCandidate.getSource().getCollection().getName());
			entity.set(AttributeMappingCandidateMetaData.EXPLANATION, matchingExplanationEntity);
			entity.set(AttributeMappingCandidateMetaData.DECISIONS, decisionEntities);

			dataService.update(attributeMappingCandidateMetaData.getName(), entity);
		}
	}

	@Override
	public void removeAttributeMappingCandidates(List<Entity> attributeMappingCandidateEntities)
	{
		Stream<Entity> mappingExplanationStream = attributeMappingCandidateEntities.stream()
				.map(entity -> entity.getEntity(AttributeMappingCandidateMetaData.EXPLANATION));

		Stream<Entity> attributeMappingDecisionStream = attributeMappingCandidateEntities.stream().flatMap(
				entity -> stream(entity.getEntities(AttributeMappingCandidateMetaData.DECISIONS).spliterator(), false));

		dataService.delete(ATTRIBUTE_MAPPING_CANDIDATE, attributeMappingCandidateEntities.stream());
		dataService.delete(MATCHING_EXPLANATION, mappingExplanationStream);
		dataService.delete(ATTRIBUTE_MAPPING_DECISION, attributeMappingDecisionStream);
	}

	@Override
	public void addAttributeMappingDecisions(List<AttributeMappingDecision> attributeMappingDecisions, boolean toAdd)
	{
		for (AttributeMappingDecision attributeMappingDecision : attributeMappingDecisions)
		{
			Entity attributeMappingDecisionEntity = attributeMappingDecisionToEntity(attributeMappingDecision);

			if (toAdd)
			{
				dataService.add(attributeMappingDecisionMetaData.getName(), attributeMappingDecisionEntity);
			}
			else
			{
				dataService.update(attributeMappingDecisionMetaData.getName(), attributeMappingDecisionEntity);
			}
		}
	}

	private List<Entity> getAttributeMappingCandidates(Query<Entity> query)
	{
		Fetch fetchOntologyTerm = new Fetch();
		ontologyTermMetaData.getAtomicAttributes().forEach(field -> fetchOntologyTerm.field(field.getName()));

		Fetch fetchSemanticType = new Fetch();
		semanticTypeMetaData.getAtomicAttributes().forEach(field -> fetchSemanticType.field(field.getName()));

		Fetch fetchTagGroupFields = new Fetch();
		tagGroupMetaData.getAtomicAttributes().forEach(attribute -> fetchTagGroupFields.field(attribute.getName()));
		fetchTagGroupFields.field(TagGroupMetaData.ONTOLOGY_TERMS, fetchOntologyTerm);
		fetchTagGroupFields.field(TagGroupMetaData.SEMANTIC_TYPES, fetchSemanticType);

		Fetch fetchBiobankSampleAttribute = new Fetch();
		biobankSampleAttributeMetaData.getAtomicAttributes()
				.forEach(attribute -> fetchBiobankSampleAttribute.field(attribute.getName()));
		fetchBiobankSampleAttribute.field(BiobankSampleAttributeMetaData.COLLECTION);
		fetchBiobankSampleAttribute.field(BiobankSampleAttributeMetaData.TAG_GROUPS, fetchTagGroupFields);

		Fetch fetchExplanation = new Fetch();
		matchingExplanationMetaData.getAtomicAttributes()
				.forEach(attribute -> fetchExplanation.field(attribute.getName()));
		fetchExplanation.field(MatchingExplanationMetaData.TARGET_ONTOLOGY_TERMS, fetchOntologyTerm);

		Fetch biobankUniverseFetch = new Fetch();
		biobankUniverseMetaData.getAtomicAttributes()
				.forEach(attribute -> biobankUniverseFetch.field(attribute.getName()));

		Fetch fetchDecision = new Fetch();
		attributeMappingCandidateMetaData.getAtomicAttributes()
				.forEach(attribute -> fetchDecision.field(attribute.getName()));
		fetchDecision.field(AttributeMappingDecisionMetaData.UNIVERSE, biobankUniverseFetch);

		Fetch fetch = new Fetch();
		attributeMappingCandidateMetaData.getAtomicAttributes().forEach(attribute -> fetch.field(attribute.getName()));
		fetch.field(AttributeMappingCandidateMetaData.BIOBANK_UNIVERSE, biobankUniverseFetch);
		fetch.field(AttributeMappingCandidateMetaData.TARGET, fetchBiobankSampleAttribute);
		fetch.field(AttributeMappingCandidateMetaData.SOURCE, fetchBiobankSampleAttribute);
		fetch.field(AttributeMappingCandidateMetaData.EXPLANATION, fetchExplanation);
		fetch.field(AttributeMappingCandidateMetaData.DECISIONS, fetchDecision);

		return dataService.findAll(ATTRIBUTE_MAPPING_CANDIDATE, query.fetch(fetch)).collect(toList());
	}

	private List<Entity> getAttributeMappingCandidateEntities(List<BiobankSampleAttribute> biobankSampleAttributes)
	{
		List<String> attributeIdentifiers = stream(biobankSampleAttributes.spliterator(), false)
				.map(BiobankSampleAttribute::getIdentifier).collect(toList());

		Fetch fetch = new Fetch();
		attributeMappingCandidateMetaData.getAtomicAttributes().forEach(attr -> fetch.field(attr.getName()));

		List<Entity> attributeMappingCandidateEntities = dataService.findAll(ATTRIBUTE_MAPPING_CANDIDATE,
				IN(TARGET, attributeIdentifiers).or().in(SOURCE, attributeIdentifiers).fetch(fetch)).collect(toList());

		return attributeMappingCandidateEntities;
	}

	private Entity biobankUniverseToEntity(BiobankUniverse biobankUniverse)
	{
		Iterable<Entity> semanticTypeEntities = entityManager.getReferences(semanticTypeMetaData,
				biobankUniverse.getKeyConcepts().stream().map(SemanticType::getIdentifier).collect(toList()));

		Iterable<Entity> biobankSampleCollectionEntities = entityManager.getReferences(biobankSampleCollectionMetaData,
				biobankUniverse.getMembers().stream().map(BiobankSampleCollection::getName).collect(toList()));

		Entity entity = new DynamicEntity(biobankUniverseMetaData);
		entity.set(BiobankUniverseMetaData.IDENTIFIER, biobankUniverse.getIdentifier());
		entity.set(BiobankUniverseMetaData.NAME, biobankUniverse.getName());
		entity.set(BiobankUniverseMetaData.MEMBERS, biobankSampleCollectionEntities);
		entity.set(BiobankUniverseMetaData.OWNER, biobankUniverse.getOwner());
		entity.set(BiobankUniverseMetaData.KEY_CONCEPTS, semanticTypeEntities);

		return entity;
	}

	private Entity biobankUniverseMemberVectorToEntity(BiobankUniverse biobankUniverse,
			BiobankUniverseMemberVector biobankUniverseMemberVector)
	{
		Entity biobankUniverseEntity = entityManager
				.getReference(biobankUniverseMetaData, biobankUniverse.getIdentifier());
		Entity biobankSampleCollectionEntity = entityManager.getReference(biobankSampleCollectionMetaData,
				biobankUniverseMemberVector.getBiobankSampleCollection().getName());

		Entity entity = new DynamicEntity(biobankUniverseMemberVectorMetaData);
		entity.set(BiobankUniverseMemberVectorMetaData.IDENTIFIER, biobankUniverseMemberVector.getIdentifier());
		entity.set(BiobankUniverseMemberVectorMetaData.MEMBER, biobankSampleCollectionEntity);
		entity.set(BiobankUniverseMemberVectorMetaData.BIOBANK_UNIVERSE, biobankUniverseEntity);
		entity.set(BiobankUniverseMemberVectorMetaData.VECTOR, Arrays.toString(biobankUniverseMemberVector.getPoint()));
		return entity;
	}

	private BiobankUniverseMemberVector entityToBiobankUniverseMemberVectors(Entity entity)
	{
		String identifier = entity.getString(BiobankUniverseMemberVectorMetaData.IDENTIFIER);
		BiobankSampleCollection biobankSampleCollection = entityToBiobankSampleCollection(
				entity.getEntity(BiobankUniverseMemberVectorMetaData.MEMBER));
		String vectorString = entity.getString(BiobankUniverseMemberVectorMetaData.VECTOR);
		String[] split = vectorString.replaceAll("[\\[\\]]", StringUtils.EMPTY).split(", ");
		double[] vector = Stream.of(split).filter(StringUtils::isNotBlank).mapToDouble(Double::valueOf).toArray();

		return BiobankUniverseMemberVector.create(identifier, biobankSampleCollection, vector);
	}

	private BiobankUniverse entityToBiobankUniverse(Entity entity)
	{
		String identifier = entity.getString(BiobankUniverseMetaData.IDENTIFIER);
		String name = entity.getString(BiobankUniverseMetaData.NAME);
		MolgenisUser owner = molgenisUserService
				.getUser(entity.getEntity(BiobankUniverseMetaData.OWNER).getString(MolgenisUserMetaData.USERNAME));

		List<BiobankSampleCollection> members = new ArrayList<>();
		Iterable<Entity> memberIterable = entity.getEntities(BiobankUniverseMetaData.MEMBERS);
		if (memberIterable != null)
		{
			List<BiobankSampleCollection> collect = stream(memberIterable.spliterator(), false)
					.map(this::entityToBiobankSampleCollection).collect(toList());
			members.addAll(collect);
		}

		List<SemanticType> keyConcepts = new ArrayList<>();
		Iterable<Entity> keyConceptIterable = entity.getEntities(BiobankUniverseMetaData.KEY_CONCEPTS);
		if (keyConceptIterable != null)
		{
			List<SemanticType> collect = stream(keyConceptIterable.spliterator(), false)
					.map(OntologyTermRepository::entityToSemanticType).collect(toList());
			keyConcepts.addAll(collect);
		}

		List<BiobankUniverseMemberVector> vectors = new ArrayList<>();

		Fetch biobankUniverseMemberVectorFetch = new Fetch();
		biobankUniverseMemberVectorFetch.field(BiobankUniverseMemberVectorMetaData.IDENTIFIER);
		biobankUniverseMemberVectorFetch.field(BiobankUniverseMemberVectorMetaData.MEMBER);
		biobankUniverseMemberVectorFetch.field(BiobankUniverseMemberVectorMetaData.VECTOR);

		List<BiobankUniverseMemberVector> biobankUniverseMemberVectors = dataService
				.findAll(BIOBANK_UNIVERSE_MEMBER_VECTOR,
						new QueryImpl<>().eq(BiobankUniverseMemberVectorMetaData.BIOBANK_UNIVERSE, identifier)
								.fetch(biobankUniverseMemberVectorFetch))
				.map(this::entityToBiobankUniverseMemberVectors).collect(toList());

		return BiobankUniverse.create(identifier, name, members, owner, keyConcepts, biobankUniverseMemberVectors);
	}

	private BiobankSampleCollection entityToBiobankSampleCollection(Entity entity)
	{
		String name = entity.getString(BiobankSampleCollectionMetaData.NAME);
		return BiobankSampleCollection.create(name);
	}

	private Entity biobankSampleCollectionToEntity(BiobankSampleCollection biobankSampleCollection)
	{
		Entity entity = new DynamicEntity(biobankSampleCollectionMetaData);
		entity.set(BiobankSampleCollectionMetaData.NAME, biobankSampleCollection.getName());
		return entity;
	}

	private BiobankSampleAttribute entityToBiobankSampleAttribute(Entity entity)
	{
		String identifier = entity.getString(BiobankSampleAttributeMetaData.IDENTIFIER);
		String name = entity.getString(BiobankSampleAttributeMetaData.NAME);
		String label = entity.getString(BiobankSampleAttributeMetaData.LABEL);
		String description = entity.getString(BiobankSampleAttributeMetaData.DESCRIPTION);
		BiobankAttributeDataType biobankAttributeDataType = toEnum(
				entity.getString(BiobankSampleAttributeMetaData.DATA_TYPE));

		BiobankSampleCollection biobankSampleCollection = entityToBiobankSampleCollection(
				entity.getEntity(BiobankSampleAttributeMetaData.COLLECTION));

		Iterable<Entity> entities = entity.getEntities(BiobankSampleAttributeMetaData.TAG_GROUPS);
		List<IdentifiableTagGroup> tagGroups = stream(entities.spliterator(), false)
				.map(this::entityToIdentifiableTagGroup).collect(toList());

		return BiobankSampleAttribute
				.create(identifier, name, label, description, biobankAttributeDataType, biobankSampleCollection,
						tagGroups);
	}

	private Entity biobankSampleAttributeToEntity(BiobankSampleAttribute biobankSampleAttribute)
	{
		Iterable<Entity> tagGroupEntities = entityManager.getReferences(tagGroupMetaData,
				biobankSampleAttribute.getTagGroups().stream().map(IdentifiableTagGroup::getIdentifier)
						.collect(toList()));

		Entity biobankSampleCollectionEntity = entityManager
				.getReference(biobankSampleCollectionMetaData, biobankSampleAttribute.getCollection().getName());

		Iterable<Entity> ontologyTermNodePathEntities = entityManager.getReferences(ontologyTermNodePathMetaData,
				getOntologyTermNodePathIdentifiers(biobankSampleAttribute.getTagGroups()));

		Entity entity = new DynamicEntity(biobankSampleAttributeMetaData);
		entity.set(BiobankSampleAttributeMetaData.IDENTIFIER, biobankSampleAttribute.getIdentifier());
		entity.set(BiobankSampleAttributeMetaData.NAME, biobankSampleAttribute.getName());
		entity.set(BiobankSampleAttributeMetaData.LABEL, biobankSampleAttribute.getLabel());
		entity.set(BiobankSampleAttributeMetaData.DESCRIPTION, biobankSampleAttribute.getDescription());
		entity.set(BiobankSampleAttributeMetaData.DATA_TYPE,
				biobankSampleAttribute.getBiobankAttributeDataType().toString());
		entity.set(BiobankSampleAttributeMetaData.COLLECTION, biobankSampleCollectionEntity);
		entity.set(BiobankSampleAttributeMetaData.TAG_GROUPS, tagGroupEntities);
		entity.set(BiobankSampleAttributeMetaData.ONTOLOGY_TERM_NODE_PATH, ontologyTermNodePathEntities);

		return entity;
	}

	private List<String> getOntologyTermNodePathIdentifiers(List<IdentifiableTagGroup> tagGroups)
	{
		List<String> ontologyTermIds = tagGroups.stream().flatMap(tagGroup -> tagGroup.getOntologyTerms().stream())
				.distinct().map(OntologyTerm::getId).collect(toList());

		if (!ontologyTermIds.isEmpty())
		{
			List<Entity> ontologyTermEntities = dataService.findAll(OntologyTermMetaData.ONTOLOGY_TERM,
					new QueryImpl<>().in(OntologyTermMetaData.ID, ontologyTermIds)).collect(toList());

			return ontologyTermEntities.stream().flatMap(
					entity -> stream(entity.getEntities(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH).spliterator(),
							false).map(Entity::getIdValue).map(Objects::toString)).collect(toList());
		}

		return emptyList();
	}

	private Entity identifiableTagGroupToEntity(IdentifiableTagGroup tagGroup)
	{
		Iterable<Entity> ontologyTermEntities = entityManager.getReferences(ontologyTermMetaData,
				tagGroup.getOntologyTerms().stream().map(OntologyTerm::getId).distinct().collect(toList()));

		Iterable<Entity> semanticTypeEntities = entityManager.getReferences(semanticTypeMetaData,
				tagGroup.getSemanticTypes().stream().map(SemanticType::getIdentifier).distinct().collect(toList()));

		Entity entity = new DynamicEntity(tagGroupMetaData);
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
				false).map(OntologyTermEntity::new).map(OntologyTermRepository::toOntologyTerm).collect(toList());

		List<SemanticType> semanticTypes = stream(entity.getEntities(TagGroupMetaData.SEMANTIC_TYPES).spliterator(),
				false).map(OntologyTermRepository::entityToSemanticType).collect(toList());

		return IdentifiableTagGroup
				.create(identifier, ontologyTerms, semanticTypes, matchedWords, ngramScore.floatValue());
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

		Iterable<Entity> entities = entity.getEntities(AttributeMappingCandidateMetaData.DECISIONS);
		List<AttributeMappingDecision> decisions = stream(entities.spliterator(), false)
				.map(this::entityToAttributeMappingDecision)
				.filter(decision -> decision.getOwner().equals(userAcountService.getCurrentUser().getUsername()))
				.collect(toList());

		return AttributeMappingCandidate.create(identifier, biobankUniverse, target, source, explanation, decisions);
	}

	private Entity attributeMappingCandidateToEntity(AttributeMappingCandidate attributeMappingCandidate)
	{
		String identifier = attributeMappingCandidate.getIdentifier();
		BiobankUniverse biobankUniverse = attributeMappingCandidate.getBiobankUniverse();
		BiobankSampleAttribute target = attributeMappingCandidate.getTarget();
		BiobankSampleAttribute source = attributeMappingCandidate.getSource();
		MatchingExplanation explanation = attributeMappingCandidate.getExplanation();
		String targetCollection = target.getCollection().getName();
		String sourceCollection = source.getCollection().getName();

		Entity biobankUniverseEntity = entityManager
				.getReference(biobankUniverseMetaData, biobankUniverse.getIdentifier());

		Entity targetEntity = entityManager.getReference(biobankSampleAttributeMetaData, target.getIdentifier());

		Entity sourceEntity = entityManager.getReference(biobankSampleAttributeMetaData, source.getIdentifier());

		Entity matchingExplanationEntity = entityManager
				.getReference(matchingExplanationMetaData, explanation.getIdentifier());

		Iterable<Entity> decisionEntities = entityManager.getReferences(attributeMappingDecisionMetaData,
				attributeMappingCandidate.getDecisions().stream().map(AttributeMappingDecision::getIdentifier)
						.collect(toList()));

		Entity entity = new DynamicEntity(attributeMappingCandidateMetaData);
		entity.set(AttributeMappingCandidateMetaData.IDENTIFIER, identifier);
		entity.set(AttributeMappingCandidateMetaData.BIOBANK_UNIVERSE, biobankUniverseEntity);
		entity.set(AttributeMappingCandidateMetaData.TARGET, targetEntity);
		entity.set(AttributeMappingCandidateMetaData.SOURCE, sourceEntity);
		entity.set(AttributeMappingCandidateMetaData.TARGET_COLLECTION, targetCollection);
		entity.set(AttributeMappingCandidateMetaData.SOURCE_COLLECTION, sourceCollection);
		entity.set(AttributeMappingCandidateMetaData.EXPLANATION, matchingExplanationEntity);
		entity.set(AttributeMappingCandidateMetaData.DECISIONS, decisionEntities);

		return entity;
	}

	private Entity mappingExplanationToEntity(MatchingExplanation mappingExplanation)
	{
		String identifier = mappingExplanation.getIdentifier();
		String queryString = mappingExplanation.getQueryString();
		String matchedTargetWords = mappingExplanation.getMatchedTargetWords();
		String matchedSourceWords = mappingExplanation.getMatchedSourceWords();
		double vsmScore = mappingExplanation.getVsmScore();
		double ngramScore = mappingExplanation.getNgramScore();

		Iterable<Entity> targetOntologyTermEntities = entityManager.getReferences(ontologyTermMetaData,
				mappingExplanation.getTargetOntologyTerms().stream().map(OntologyTerm::getId).collect(toList()));

		Iterable<Entity> sourceOntologyTermEntities = entityManager.getReferences(ontologyTermMetaData,
				mappingExplanation.getSourceOntologyTerms().stream().map(OntologyTerm::getId).collect(toList()));

		Entity entity = new DynamicEntity(matchingExplanationMetaData);
		entity.set(MatchingExplanationMetaData.IDENTIFIER, identifier);
		entity.set(MatchingExplanationMetaData.MATCHED_QUERY_STRING, queryString);
		entity.set(MatchingExplanationMetaData.MATCHED_TARGET_WORDS, matchedTargetWords);
		entity.set(MatchingExplanationMetaData.MATCHED_SOURCE_WORDS, matchedSourceWords);
		entity.set(MatchingExplanationMetaData.TARGET_ONTOLOGY_TERMS, targetOntologyTermEntities);
		entity.set(MatchingExplanationMetaData.SOURCE_ONTOLOGY_TERMS, sourceOntologyTermEntities);
		entity.set(MatchingExplanationMetaData.VSM_SCORE, vsmScore);
		entity.set(MatchingExplanationMetaData.N_GRAM_SCORE, ngramScore);

		return entity;
	}

	private MatchingExplanation entityToMappingExplanation(Entity mappingExplanationEntity)
	{
		String identifier = mappingExplanationEntity.getString(MatchingExplanationMetaData.IDENTIFIER);
		String queryString = mappingExplanationEntity.getString(MatchingExplanationMetaData.MATCHED_QUERY_STRING);
		String matchedTargetWords = mappingExplanationEntity
				.getString(MatchingExplanationMetaData.MATCHED_TARGET_WORDS);
		String matchedSourceWords = mappingExplanationEntity
				.getString(MatchingExplanationMetaData.MATCHED_SOURCE_WORDS);
		Double ngramScore = mappingExplanationEntity.getDouble(MatchingExplanationMetaData.N_GRAM_SCORE);
		Double vsmScore = mappingExplanationEntity.getDouble(MatchingExplanationMetaData.VSM_SCORE);

		List<OntologyTerm> targetOntologyTerms = new ArrayList<>();

		Iterable<Entity> targetOntologyTermEntities = mappingExplanationEntity
				.getEntities(MatchingExplanationMetaData.TARGET_ONTOLOGY_TERMS);

		if (targetOntologyTermEntities != null)
		{
			List<OntologyTerm> collect = stream(targetOntologyTermEntities.spliterator(), false)
					.map(OntologyTermEntity::new).map(OntologyTermRepository::toOntologyTerm).collect(toList());
			targetOntologyTerms.addAll(collect);
		}

		List<OntologyTerm> sourceOntologyTerms = new ArrayList<>();

		Iterable<Entity> sourceOntologyTermEntities = mappingExplanationEntity
				.getEntities(MatchingExplanationMetaData.SOURCE_ONTOLOGY_TERMS);

		if (sourceOntologyTermEntities != null)
		{
			List<OntologyTerm> collect = stream(sourceOntologyTermEntities.spliterator(), false)
					.map(OntologyTermEntity::new).map(OntologyTermRepository::toOntologyTerm).collect(toList());
			sourceOntologyTerms.addAll(collect);
		}

		return MatchingExplanation
				.create(identifier, targetOntologyTerms, sourceOntologyTerms, queryString, matchedTargetWords,
						matchedSourceWords, vsmScore, ngramScore);
	}

	private AttributeMappingDecision entityToAttributeMappingDecision(Entity entity)
	{
		String identifier = entity.getString(AttributeMappingDecisionMetaData.IDENTIFIER);
		String owner = entity.getString(AttributeMappingDecisionMetaData.OWNER);
		DecisionOptions decision = DecisionOptions.toEnum(entity.getString(AttributeMappingDecisionMetaData.DECISION));
		String comment = entity.getString(AttributeMappingDecisionMetaData.COMMENT);
		BiobankUniverse biobankUniverse = entityToBiobankUniverse(
				entity.getEntity(AttributeMappingDecisionMetaData.UNIVERSE));
		return AttributeMappingDecision.create(identifier, decision, comment, owner, biobankUniverse);
	}

	private Entity attributeMappingDecisionToEntity(AttributeMappingDecision attributeMappingDecision)
	{
		String identifier = attributeMappingDecision.getIdentifier();
		String comment = attributeMappingDecision.getComment();
		String decision = attributeMappingDecision.getDecision().toString();
		String owner = attributeMappingDecision.getOwner();
		Entity biobankUniverseEntity = entityManager
				.getReference(biobankUniverseMetaData, attributeMappingDecision.getBiobankUniverse().getIdentifier());

		Entity entity = new DynamicEntity(attributeMappingDecisionMetaData);
		entity.set(AttributeMappingDecisionMetaData.IDENTIFIER, identifier);
		entity.set(AttributeMappingDecisionMetaData.OWNER, owner);
		entity.set(AttributeMappingDecisionMetaData.DECISION, decision);
		entity.set(AttributeMappingDecisionMetaData.COMMENT, comment);
		entity.set(AttributeMappingDecisionMetaData.UNIVERSE, biobankUniverseEntity);

		return entity;
	}
}