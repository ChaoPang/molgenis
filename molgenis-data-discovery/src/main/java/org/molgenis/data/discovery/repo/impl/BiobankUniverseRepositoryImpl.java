package org.molgenis.data.discovery.repo.impl;

import static java.util.stream.Collectors.toList;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM_IRI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.discovery.meta.AttributeMappingCandidateMetaData;
import org.molgenis.data.discovery.meta.AttributeMappingDecisionMetaData;
import org.molgenis.data.discovery.meta.AttributeMappingDecisionMetaData.DecisionOptions;
import org.molgenis.data.discovery.meta.BiobankSampleAttributeMetaData;
import org.molgenis.data.discovery.meta.BiobankSampleCollectionMetaData;
import org.molgenis.data.discovery.meta.BiobankUniverseMetaData;
import org.molgenis.data.discovery.meta.MatchingExplanationMetaData;
import org.molgenis.data.discovery.meta.TagGroupMetaData;
import org.molgenis.data.discovery.model.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.AttributeMappingDecision;
import org.molgenis.data.discovery.model.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.BiobankSampleCollection;
import org.molgenis.data.discovery.model.BiobankUniverse;
import org.molgenis.data.discovery.model.IdentifiableTagGroup;
import org.molgenis.data.discovery.model.MatchingExplanation;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.user.MolgenisUserService;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.requireNonNull;

public class BiobankUniverseRepositoryImpl implements BiobankUniverseRepository
{
	private final DataService dataService;
	private final OntologyService ontologyService;
	private final MolgenisUserService molgenisUserService;

	public BiobankUniverseRepositoryImpl(DataService dataService, OntologyService ontologyService,
			MolgenisUserService molgenisUserService)
	{
		this.dataService = requireNonNull(dataService);
		this.ontologyService = requireNonNull(ontologyService);
		this.molgenisUserService = requireNonNull(molgenisUserService);
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
		Entity findOne = dataService.findOne(BiobankUniverseMetaData.ENTITY_NAME,
				QueryImpl.EQ(BiobankUniverseMetaData.IDENTIFIER, identifier));
		return findOne == null ? null : entityToBiobankUniverse(findOne);
	}

	@Transactional
	@Override
	public void addUniverse(BiobankUniverse biobankUniverse)
	{
		MapEntity mapEntity = biobankUniverseToEntity(biobankUniverse);
		dataService.add(BiobankUniverseMetaData.ENTITY_NAME, mapEntity);
	}

	@Transactional
	@Override
	public void addUniverseMembers(BiobankUniverse biobankUniverse, List<BiobankSampleCollection> members)
	{
		List<BiobankSampleCollection> newMembers = biobankUniverse.getMembers().stream()
				.filter(member -> !members.contains(member)).collect(Collectors.toList());
		dataService.add(BiobankSampleCollectionMetaData.ENTITY_NAME,
				newMembers.stream().map(this::biobankSampleCollectionToEntity));

		List<BiobankSampleCollection> allMembers = Stream
				.concat(biobankUniverse.getMembers().stream(), newMembers.stream()).collect(Collectors.toList());

		MapEntity biobankUniverseToEntity = biobankUniverseToEntity(BiobankUniverse.create(
				biobankUniverse.getIdentifier(), biobankUniverse.getName(), allMembers, biobankUniverse.getOwner()));

		dataService.update(BiobankUniverseMetaData.ENTITY_NAME, biobankUniverseToEntity);
	}

	@Override
	public List<BiobankSampleCollection> getBiobankSampleCollections(List<String> collectionIdentifiers)
	{
		List<BiobankSampleCollection> collections = dataService
				.findAll(BiobankSampleCollectionMetaData.ENTITY_NAME,
						QueryImpl.IN(BiobankSampleCollectionMetaData.IDENTIFIER, collectionIdentifiers))
				.map(this::entityToBiobankSampleCollection).collect(Collectors.toList());

		return collections;
	}

	@Override
	public List<BiobankSampleAttribute> getAttributesFromCollection(BiobankSampleCollection collection)
	{
		List<BiobankSampleAttribute> biobankSampleAttributes = dataService
				.findAll(BiobankSampleAttributeMetaData.ENTITY_NAME,
						QueryImpl.EQ(BiobankSampleAttributeMetaData.COLLECTION, collection.getIdentifier()))
				.map(this::entityToBiobankSampleAttribute).collect(Collectors.toList());

		return biobankSampleAttributes;
	}

	@Override
	public void tagAttributesInBiobankSampleCollection(BiobankSampleCollection collection)
	{
		List<BiobankSampleAttribute> biobankSampleAttributes = getAttributesFromCollection(collection);

		Stream<Entity> tagGroupEntityStream = biobankSampleAttributes.stream()
				.flatMap(attribute -> attribute.getTagGroups().stream()).map(this::identifiableTagGroupToEntity);
		dataService.add(TagGroupMetaData.ENTITY_NAME, tagGroupEntityStream);

		Stream<Entity> attributeEntityStream = biobankSampleAttributes.stream()
				.map(this::biobankSampleAttributeToEntity);
		dataService.update(BiobankSampleAttributeMetaData.ENTITY_NAME, attributeEntityStream);
	}

	@Transactional
	@Override
	public void addAttributeMatchCandidates(List<AttributeMappingCandidate> candidates)
	{
		Stream<Entity> explanationStream = candidates.stream().map(AttributeMappingCandidate::getExplanation)
				.map(this::mappingExplanationToEntity);
		dataService.add(MatchingExplanationMetaData.ENTITY_NAME, explanationStream);

		Stream<Entity> attributeMappingCandidateStream = candidates.stream()
				.map(this::attributeMappingCandidateToEntity);
		dataService.add(AttributeMappingCandidateMetaData.ENTITY_NAME, attributeMappingCandidateStream);
	}

	@Override
	public List<AttributeMappingCandidate> getAttributeMatchCandidates(
			List<BiobankSampleAttribute> biobankSampleAttributes)
	{
		List<String> attributeIdentifiers = biobankSampleAttributes.stream().map(BiobankSampleAttribute::getIdentifier)
				.collect(Collectors.toList());

		List<AttributeMappingCandidate> attributeMappingCandidates = dataService
				.findAll(AttributeMappingCandidateMetaData.ENTITY_NAME,
						QueryImpl.IN(AttributeMappingCandidateMetaData.TARGET, attributeIdentifiers).or()
								.in(AttributeMappingCandidateMetaData.SOURCE, attributeIdentifiers))
				.map(this::entityToAttributeMappingCandidate).collect(Collectors.toList());
		return attributeMappingCandidates;
	}

	private MapEntity biobankUniverseToEntity(BiobankUniverse biobankUniverse)
	{
		MapEntity mapEntity = new MapEntity(BiobankUniverseMetaData.INSTANCE);
		mapEntity.set(BiobankUniverseMetaData.IDENTIFIER, biobankUniverse.getIdentifier());
		mapEntity.set(BiobankUniverseMetaData.NAME, biobankUniverse.getName());
		mapEntity.set(BiobankUniverseMetaData.MEMBERS, biobankUniverse.getMembers().stream()
				.map(BiobankSampleCollection::getIdentifier).collect(Collectors.toList()));
		mapEntity.set(BiobankUniverseMetaData.OWNER, biobankUniverse.getOwner());
		return mapEntity;
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
		return BiobankUniverse.create(identifier, name, members, owner);
	}

	private BiobankSampleCollection entityToBiobankSampleCollection(Entity entity)
	{
		String identifier = entity.getString(BiobankSampleCollectionMetaData.IDENTIFIER);
		String name = entity.getString(BiobankSampleCollectionMetaData.NAME);
		return BiobankSampleCollection.create(identifier, name);
	}

	private Entity biobankSampleCollectionToEntity(BiobankSampleCollection biobankSampleCollection)
	{
		MapEntity mapEntity = new MapEntity(BiobankSampleCollectionMetaData.INSTANCE);
		mapEntity.set(BiobankSampleCollectionMetaData.IDENTIFIER, biobankSampleCollection.getIdentifier());
		mapEntity.set(BiobankSampleCollectionMetaData.NAME, biobankSampleCollection.getName());
		return mapEntity;
	}

	private BiobankSampleAttribute entityToBiobankSampleAttribute(Entity entity)
	{
		String identifier = entity.getString(BiobankSampleAttributeMetaData.IDENTIFIER);
		String name = entity.getString(BiobankSampleAttributeMetaData.NAME);
		String label = entity.getString(BiobankSampleAttributeMetaData.LABEL);
		String description = entity.getString(BiobankSampleAttributeMetaData.DESCRIPTION);
		List<BiobankSampleCollection> biobankCollectionMembers = getBiobankSampleCollections(
				Arrays.asList(entity.getString(BiobankSampleAttributeMetaData.COLLECTION)));
		BiobankSampleCollection collection = biobankCollectionMembers.get(0);

		Iterable<Entity> entities = entity.getEntities(BiobankSampleAttributeMetaData.TAG_GROUPS);
		List<IdentifiableTagGroup> tagGroups = StreamSupport.stream(entities.spliterator(), false)
				.map(this::entityToIdentifiableTagGroup).collect(Collectors.toList());
		return BiobankSampleAttribute.create(identifier, name, label, description, collection, tagGroups);
	}

	private Entity biobankSampleAttributeToEntity(BiobankSampleAttribute attribute)
	{
		MapEntity mapEntity = new MapEntity(BiobankSampleAttributeMetaData.INSTANCE);
		mapEntity.set(BiobankSampleAttributeMetaData.IDENTIFIER, attribute.getIdentifier());
		mapEntity.set(BiobankSampleAttributeMetaData.NAME, attribute.getName());
		mapEntity.set(BiobankSampleAttributeMetaData.LABEL, attribute.getLabel());
		mapEntity.set(BiobankSampleAttributeMetaData.DESCRIPTION, attribute.getDescription());
		mapEntity.set(BiobankSampleAttributeMetaData.COLLECTION, attribute.getCollection().getIdentifier());
		mapEntity.set(BiobankSampleAttributeMetaData.TAG_GROUPS, attribute.getTagGroups().stream()
				.map(IdentifiableTagGroup::getIdentifier).collect(Collectors.toList()));
		return mapEntity;
	}

	private Entity identifiableTagGroupToEntity(IdentifiableTagGroup tagGroup)
	{
		List<OntologyTerm> atomicOntologyTerms = ontologyService.getAtomicOntologyTerms(tagGroup.getOntologyTerm());

		List<String> ontologyTermIris = atomicOntologyTerms.stream().map(OntologyTerm::getIRI)
				.collect(Collectors.toList());
		List<String> ontologyTermIds = dataService
				.findAll(OntologyTermMetaData.ENTITY_NAME,
						QueryImpl.IN(OntologyTermMetaData.ONTOLOGY_TERM_IRI, ontologyTermIris))
				.map(entity -> entity.getString(OntologyTermMetaData.ID)).collect(Collectors.toList());

		MapEntity mapEntity = new MapEntity(TagGroupMetaData.INSTANCE);
		mapEntity.set(TagGroupMetaData.IDENTIFIER, tagGroup.getIdentifier());
		mapEntity.set(TagGroupMetaData.ONTOLOGY_TERMS, ontologyTermIds);
		mapEntity.set(TagGroupMetaData.MATCHED_WORDS, tagGroup.getMatchedWords());
		mapEntity.set(TagGroupMetaData.NGRAM_SCORE, tagGroup.getScore());

		return mapEntity;
	}

	private IdentifiableTagGroup entityToIdentifiableTagGroup(Entity entity)
	{
		MapEntity mapEntity = new MapEntity(TagGroupMetaData.INSTANCE);

		String identifier = mapEntity.getString(TagGroupMetaData.IDENTIFIER);
		String matchedWords = mapEntity.getString(TagGroupMetaData.MATCHED_WORDS);
		Double ngramScore = mapEntity.getDouble(TagGroupMetaData.NGRAM_SCORE);
		OntologyTerm ontologyTerm = OntologyTerm
				.and(StreamSupport.stream(mapEntity.getEntities(TagGroupMetaData.ONTOLOGY_TERMS).spliterator(), false)
						.map(ot -> ot.getString(OntologyTermMetaData.ONTOLOGY_TERM_IRI))
						.map(ontologyService::getOntologyTerm).toArray(OntologyTerm[]::new));

		return IdentifiableTagGroup.create(identifier, ontologyTerm, matchedWords, ngramScore.floatValue());
	}

	private AttributeMappingCandidate entityToAttributeMappingCandidate(Entity entity)
	{
		String identifier = entity.getString(AttributeMappingCandidateMetaData.IDENTIFIER);
		BiobankSampleAttribute target = entityToBiobankSampleAttribute(
				entity.getEntity(AttributeMappingCandidateMetaData.TARGET));
		BiobankSampleAttribute source = entityToBiobankSampleAttribute(
				entity.getEntity(AttributeMappingCandidateMetaData.SOURCE));
		MatchingExplanation explanation = entityToMappingExplanation(
				entity.getEntity(AttributeMappingCandidateMetaData.EXPLANATION));
		// TODO: add the real decisions
		List<AttributeMappingDecision> decisions = Arrays.asList();
		return AttributeMappingCandidate.create(identifier, target, source, explanation, decisions);
	}

	private Entity attributeMappingCandidateToEntity(AttributeMappingCandidate attributeMappingCandidate)
	{
		String identifier = attributeMappingCandidate.getIdentifier();
		BiobankSampleAttribute target = attributeMappingCandidate.getTarget();
		BiobankSampleAttribute source = attributeMappingCandidate.getSource();
		MatchingExplanation explanation = attributeMappingCandidate.getExplanation();
		List<AttributeMappingDecision> decisions = attributeMappingCandidate.getDecisions();

		MapEntity mapEntity = new MapEntity(AttributeMappingCandidateMetaData.ENTITY_NAME);
		mapEntity.set(AttributeMappingCandidateMetaData.IDENTIFIER, identifier);
		mapEntity.set(AttributeMappingCandidateMetaData.TARGET, target.getIdentifier());
		mapEntity.set(AttributeMappingCandidateMetaData.SOURCE, source.getIdentifier());
		mapEntity.set(AttributeMappingCandidateMetaData.EXPLANATION, explanation.getIdentifier());
		mapEntity.set(AttributeMappingCandidateMetaData.DECISIONS,
				decisions.stream().map(AttributeMappingDecision::getIdentifier).collect(Collectors.toList()));

		return mapEntity;
	}

	private Entity mappingExplanationToEntity(MatchingExplanation mappingExplanation)
	{
		String identifier = mappingExplanation.getIdentifier();
		List<String> ontologyTermIdentifiers = new ArrayList<>();
		String queryString = mappingExplanation.getQueryString();
		String matchedWords = mappingExplanation.getMatchedWords();
		double ngramScore = mappingExplanation.getNgramScore();

		if (mappingExplanation.getOntologyTerms().size() > 0)
		{
			List<String> ontologyTermIris = mappingExplanation.getOntologyTerms().stream().map(OntologyTerm::getIRI)
					.collect(Collectors.toList());
			ontologyTermIdentifiers
					.addAll(dataService
							.findAll(OntologyTermMetaData.ENTITY_NAME,
									QueryImpl.IN(ONTOLOGY_TERM_IRI, ontologyTermIris))
							.map(entity -> entity.getString(OntologyTermMetaData.ID)).collect(toList()));
		}

		MapEntity mapEntity = new MapEntity(MatchingExplanationMetaData.INSTANCE);
		mapEntity.set(MatchingExplanationMetaData.IDENTIFIER, identifier);
		mapEntity.set(MatchingExplanationMetaData.MATCHED_QUERY_STRING, queryString);
		mapEntity.set(MatchingExplanationMetaData.MATCHED_WORDS, matchedWords);
		mapEntity.set(MatchingExplanationMetaData.ONTOLOGY_TERMS, ontologyTermIdentifiers);
		mapEntity.set(MatchingExplanationMetaData.N_GRAM_SCORE, ngramScore);

		return mapEntity;
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
			List<OntologyTerm> collect = StreamSupport.stream(ontologyTermEntities.spliterator(), false)
					.map(entity -> entity.getString(OntologyTermMetaData.ONTOLOGY_TERM_IRI))
					.map(ontologyService::getOntologyTerm).collect(Collectors.toList());
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
}
