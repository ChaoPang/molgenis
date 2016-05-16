package org.molgenis.data.discovery.repo;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.molgenis.data.meta.EntityMetaDataMetaData.ATTRIBUTES;
import static org.molgenis.data.meta.EntityMetaDataMetaData.ENTITY_NAME;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM_IRI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.discovery.meta.AttributeMappingCandidateMetaData;
import org.molgenis.data.discovery.meta.BiobankUniverseMetaData;
import org.molgenis.data.discovery.meta.MappingExplanationMetaData;
import org.molgenis.data.discovery.meta.TaggedAttributeMetaData;
import org.molgenis.data.discovery.model.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.AttributeMappingDecision;
import org.molgenis.data.discovery.model.BiobankUniverse;
import org.molgenis.data.discovery.model.MappingExplanation;
import org.molgenis.data.discovery.model.TaggedAttribute;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.user.MolgenisUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.requireNonNull;

public class BiobankUniverseRepository
{
	private final DataService dataService;
	private final OntologyService ontologyService;
	private final MolgenisUserService molgenisUserService;

	@Autowired
	public BiobankUniverseRepository(DataService dataService, OntologyService ontologyService,
			MolgenisUserService molgenisUserService)
	{
		this.dataService = requireNonNull(dataService);
		this.ontologyService = requireNonNull(ontologyService);
		this.molgenisUserService = requireNonNull(molgenisUserService);
	}

	public List<BiobankUniverse> getAllUniverses()
	{
		return dataService.findAll(BiobankUniverseMetaData.ENTITY_NAME).map(this::entityToBiobankUniverse)
				.collect(Collectors.toList());
	}

	public BiobankUniverse getUniverse(String identifier)
	{
		Entity findOne = dataService.findOne(BiobankUniverseMetaData.ENTITY_NAME,
				QueryImpl.EQ(BiobankUniverseMetaData.IDENTIFIER, identifier));
		return findOne == null ? null : entityToBiobankUniverse(findOne);
	}

	private BiobankUniverse entityToBiobankUniverse(Entity entity)
	{
		String identifier = entity.getString(BiobankUniverseMetaData.IDENTIFIER);
		String name = entity.getString(BiobankUniverseMetaData.NAME);
		MolgenisUser owner = molgenisUserService
				.getUser(entity.getEntity(BiobankUniverseMetaData.OWNER).getString(MolgenisUser.USERNAME));

		List<EntityMetaData> members = new ArrayList<>();
		Iterable<Entity> memberIterable = entity.getEntities(BiobankUniverseMetaData.MEMBERS);
		if (memberIterable != null)
		{
			List<EntityMetaData> collect = StreamSupport.stream(memberIterable.spliterator(), false).map(
					entityMetaDataEntity -> dataService.getEntityMetaData(entityMetaDataEntity.getIdValue().toString()))
					.collect(toList());
			members.addAll(collect);
		}
		return BiobankUniverse.create(identifier, name, members, owner);
	}

	@Transactional
	public void createBiobankUniverse(BiobankUniverse biobankUniverse)
	{
		MapEntity mapEntity = new MapEntity(BiobankUniverseMetaData.INSTANCE);
		mapEntity.set(BiobankUniverseMetaData.IDENTIFIER, biobankUniverse.getIdentifier());
		mapEntity.set(BiobankUniverseMetaData.NAME, biobankUniverse.getName());
		mapEntity.set(BiobankUniverseMetaData.MEMBERS,
				biobankUniverse.getMembers().stream().map(EntityMetaData::getName).collect(Collectors.toList()));
		mapEntity.set(BiobankUniverseMetaData.OWNER, biobankUniverse.getOwner());
		dataService.add(BiobankUniverseMetaData.ENTITY_NAME, mapEntity);
	}

	@Transactional
	public void addUniverseMembers(BiobankUniverse biobankUniverse, List<EntityMetaData> entityMetaDatas)
	{
		MapEntity mapEntity = new MapEntity(BiobankUniverseMetaData.INSTANCE);
		mapEntity.set(BiobankUniverseMetaData.IDENTIFIER, biobankUniverse.getIdentifier());
		mapEntity.set(BiobankUniverseMetaData.NAME, biobankUniverse.getName());

		List<String> existingMembers = biobankUniverse.getMembers().stream().map(EntityMetaData::getName)
				.collect(Collectors.toList());

		List<String> newMemberNames = entityMetaDatas.stream().map(EntityMetaData::getName)
				.filter(name -> !existingMembers.contains(name)).collect(Collectors.toList());
		existingMembers.addAll(newMemberNames);

		mapEntity.set(BiobankUniverseMetaData.MEMBERS, existingMembers);
		mapEntity.set(BiobankUniverseMetaData.OWNER, biobankUniverse.getOwner());
		dataService.update(BiobankUniverseMetaData.ENTITY_NAME, mapEntity);
	}

	public List<TaggedAttribute> getTaggedAttributes(EntityMetaData entityMetaData)
	{
		Map<String, String> attributeNameToIdentifierMap = createAttributeNameToIdentifierMap(entityMetaData);
		List<TaggedAttribute> collect = dataService
				.findAll(TaggedAttributeMetaData.ENTITY_NAME,
						QueryImpl.IN(TaggedAttributeMetaData.ATTRIBUTE, attributeNameToIdentifierMap.values()))
				.map(entity -> entityToTaggedAttribute(entityMetaData, entity)).collect(Collectors.toList());
		return collect;
	}

	@Transactional
	public void addTaggedAttributes(EntityMetaData entityMetaData, List<TaggedAttribute> taggedAttributes)
	{
		Map<String, String> attributeNameToEntityIdentifier = createAttributeNameToIdentifierMap(entityMetaData);

		Stream<Entity> mappingExplanationEntityStream = taggedAttributes.stream()
				.flatMap(taggedAttribute -> taggedAttribute.getMappedOntologyTerms().stream())
				.map(this::mappingExplanationToEntity);
		dataService.add(MappingExplanationMetaData.ENTITY_NAME, mappingExplanationEntityStream);

		Stream<Entity> taggedAttributeEntityStream = taggedAttributes.stream()
				.map(taggedAttribute -> taggedAttributeToEntity(attributeNameToEntityIdentifier, taggedAttribute));
		dataService.add(TaggedAttributeMetaData.ENTITY_NAME, taggedAttributeEntityStream);
	}

	@Transactional
	public void addAttributeMappingCandidates(List<AttributeMappingCandidate> candidates)
	{
		Stream<Entity> explanationStream = candidates.stream().map(AttributeMappingCandidate::getExplanation)
				.map(this::mappingExplanationToEntity);
		dataService.add(MappingExplanationMetaData.ENTITY_NAME, explanationStream);

		Stream<Entity> attributeMappingCandidateStream = candidates.stream()
				.map(this::attributeMappingCandidateToEntity);
		dataService.add(AttributeMappingCandidateMetaData.ENTITY_NAME, attributeMappingCandidateStream);
	}

	private Entity attributeMappingCandidateToEntity(AttributeMappingCandidate attributeMappingCandidate)
	{
		String identifier = attributeMappingCandidate.getIdentifier();
		String target = attributeMappingCandidate.getTarget();
		String source = attributeMappingCandidate.getSource();
		EntityMetaData targetEntity = attributeMappingCandidate.getTargetEntity();
		EntityMetaData sourceEntity = attributeMappingCandidate.getSourceEntity();
		MappingExplanation explanation = attributeMappingCandidate.getExplanation();
		List<AttributeMappingDecision> decisions = attributeMappingCandidate.getDecisions();

		MapEntity mapEntity = new MapEntity(AttributeMappingCandidateMetaData.ENTITY_NAME);
		mapEntity.set(AttributeMappingCandidateMetaData.IDENTIFIER, identifier);
		mapEntity.set(AttributeMappingCandidateMetaData.TARGET, target);
		mapEntity.set(AttributeMappingCandidateMetaData.SOURCE, source);
		mapEntity.set(AttributeMappingCandidateMetaData.TARGET_ENTITY, targetEntity.getName());
		mapEntity.set(AttributeMappingCandidateMetaData.SOURCE_ENTITY, sourceEntity.getName());
		mapEntity.set(AttributeMappingCandidateMetaData.EXPLANATION, explanation.getIdentifier());
		mapEntity.set(AttributeMappingCandidateMetaData.DECISIONS,
				decisions.stream().map(AttributeMappingDecision::getIdentifier).collect(Collectors.toList()));

		return mapEntity;
	}

	private Map<String, String> createAttributeNameToIdentifierMap(EntityMetaData entityMetaData)
	{
		Entity entityMetaDataEntity = dataService.findOne(ENTITY_NAME, entityMetaData.getName());
		Map<String, String> attributeNameToEntityIdentifier = StreamSupport
				.stream(entityMetaDataEntity.getEntities(ATTRIBUTES).spliterator(), false)
				.collect(toMap(att -> att.getString(AttributeMetaDataMetaData.NAME),
						att -> att.getString(AttributeMetaDataMetaData.IDENTIFIER)));
		return attributeNameToEntityIdentifier;
	}

	private TaggedAttribute entityToTaggedAttribute(EntityMetaData entityMetaData, Entity taggedAttributeEntity)
	{
		String identifier = taggedAttributeEntity.getString(TaggedAttributeMetaData.IDENTIFIER);
		Entity attributeEntity = taggedAttributeEntity.getEntity(TaggedAttributeMetaData.ATTRIBUTE);
		AttributeMetaData attribute = entityMetaData
				.getAttribute(attributeEntity.getString(AttributeMetaDataMetaData.NAME));
		List<MappingExplanation> mappingExplanations = new ArrayList<>();
		Iterable<Entity> tagGroups = taggedAttributeEntity.getEntities(TaggedAttributeMetaData.TAG_GROUPS);
		if (tagGroups != null)
		{
			List<MappingExplanation> collect = StreamSupport.stream(tagGroups.spliterator(), false)
					.map(this::entityToMappingExplanation).collect(Collectors.toList());
			mappingExplanations.addAll(collect);
		}

		return TaggedAttribute.create(identifier, attribute, mappingExplanations);
	}

	private Entity taggedAttributeToEntity(Map<String, String> attributeNameToEntityIdentifier,
			TaggedAttribute taggedAttribute)
	{
		String attributeIdentifier = attributeNameToEntityIdentifier.get(taggedAttribute.getAttribute().getName());
		List<String> explanationsIdentifiers = taggedAttribute.getMappedOntologyTerms().stream()
				.map(MappingExplanation::getIdentifier).collect(Collectors.toList());

		MapEntity mapEntity = new MapEntity(TaggedAttributeMetaData.INSTANCE);
		mapEntity.set(TaggedAttributeMetaData.IDENTIFIER, taggedAttribute.getIdentifier());
		mapEntity.set(TaggedAttributeMetaData.ATTRIBUTE, attributeIdentifier);
		mapEntity.set(TaggedAttributeMetaData.TAG_GROUPS, explanationsIdentifiers);
		return mapEntity;
	}

	private Entity mappingExplanationToEntity(MappingExplanation mappingExplanation)
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

		MapEntity mapEntity = new MapEntity(MappingExplanationMetaData.INSTANCE);
		mapEntity.set(MappingExplanationMetaData.IDENTIFIER, identifier);
		mapEntity.set(MappingExplanationMetaData.MATCHED_QUERY_STRING, queryString);
		mapEntity.set(MappingExplanationMetaData.MATCHED_WORDS, matchedWords);
		mapEntity.set(MappingExplanationMetaData.ONTOLOGY_TERMS, ontologyTermIdentifiers);
		mapEntity.set(MappingExplanationMetaData.N_GRAM_SCORE, ngramScore);

		return mapEntity;
	}

	private MappingExplanation entityToMappingExplanation(Entity mappingExplanationEntity)
	{
		String identifier = mappingExplanationEntity.getString(MappingExplanationMetaData.IDENTIFIER);
		String queryString = mappingExplanationEntity.getString(MappingExplanationMetaData.MATCHED_QUERY_STRING);
		String matchedWords = mappingExplanationEntity.getString(MappingExplanationMetaData.MATCHED_WORDS);
		Double ngramScore = mappingExplanationEntity.getDouble(MappingExplanationMetaData.N_GRAM_SCORE);

		List<OntologyTerm> ontologyTerms = new ArrayList<>();
		Iterable<Entity> ontologyTermEntities = mappingExplanationEntity
				.getEntities(MappingExplanationMetaData.ONTOLOGY_TERMS);
		if (ontologyTermEntities != null)
		{
			List<OntologyTerm> collect = StreamSupport.stream(ontologyTermEntities.spliterator(), false)
					.map(entity -> entity.getString(OntologyTermMetaData.ONTOLOGY_TERM_IRI))
					.map(ontologyService::getOntologyTerm).collect(Collectors.toList());
			ontologyTerms.addAll(collect);
		}

		return MappingExplanation.create(identifier, ontologyTerms, queryString, matchedWords, ngramScore);
	}
}
