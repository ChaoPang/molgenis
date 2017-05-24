package org.molgenis.data.discovery.service.impl;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import org.molgenis.auth.User;
import org.molgenis.data.Entity;
import org.molgenis.data.aggregation.AggregateResult;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData.BiobankAttributeDataType;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.biobank.BiobankUniverseMemberVector;
import org.molgenis.data.discovery.model.matching.*;
import org.molgenis.data.discovery.model.network.VisNetworkRequest.NetworkType;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.scoring.collections.VectorSpaceModelCollectionSimilarity;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.discovery.service.OntologyBasedExplainService;
import org.molgenis.data.populate.IdGenerator;
import org.molgenis.data.semanticsearch.explain.service.ExplainMappingService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.SearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.ic.TermFrequencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData.BiobankAttributeDataType.toEnum;
import static org.molgenis.data.discovery.meta.matching.AttributeMappingDecisionMetaData.DecisionOptions.NO;
import static org.molgenis.data.discovery.meta.matching.AttributeMappingDecisionMetaData.DecisionOptions.YES;
import static org.molgenis.data.discovery.service.BiobankUniverseService.AttributeMatchStatus.*;

public class BiobankUniverseServiceImpl implements BiobankUniverseService
{
	private static final Logger LOG = LoggerFactory.getLogger(BiobankUniverseServiceImpl.class);
	private final static int MAX_NUMBER_MATCHES = 50;

	private final IdGenerator idGenerator;
	private final BiobankUniverseRepository biobankUniverseRepository;
	private final OntologyService ontologyService;
	private final TagGroupGenerator tagGroupGenerator;
	private final OntologyBasedExplainService ontologyBasedExplainService;
	private final AttributeCandidateScoringImpl biobankUniverseScore;
	private final VectorSpaceModelCollectionSimilarity vectorSpaceModelCollectionSimilarity;

	@Autowired
	public BiobankUniverseServiceImpl(IdGenerator idGenerator, BiobankUniverseRepository biobankUniverseRepository,
			OntologyService ontologyService, TagGroupGenerator tagGroupGenerator,
			ExplainMappingService explainMappingService, OntologyBasedExplainService ontologyBasedExplainService,
			TermFrequencyService termFrequencyService)
	{
		this.idGenerator = requireNonNull(idGenerator);
		this.biobankUniverseRepository = biobankUniverseRepository;
		this.ontologyService = requireNonNull(ontologyService);
		this.tagGroupGenerator = requireNonNull(tagGroupGenerator);
		this.ontologyBasedExplainService = requireNonNull(ontologyBasedExplainService);
		this.biobankUniverseScore = new AttributeCandidateScoringImpl(ontologyService, termFrequencyService);
		this.vectorSpaceModelCollectionSimilarity = new VectorSpaceModelCollectionSimilarity(biobankUniverseRepository,
				ontologyService, idGenerator);
	}

	@Override
	public BiobankUniverse addBiobankUniverse(String universeName, List<String> semanticTypeNames, User owner)
	{
		List<SemanticType> semanticTypes = ontologyService.getSemanticTypesByNames(semanticTypeNames);

		BiobankUniverse biobankUniverse = BiobankUniverse
				.create(idGenerator.generateId(), universeName, emptyList(), owner, semanticTypes, emptyList());

		biobankUniverseRepository.addBiobankUniverse(biobankUniverse);

		return biobankUniverseRepository.getUniverse(biobankUniverse.getIdentifier());
	}

	@Override
	public void deleteBiobankUniverse(String identifier)
	{
		biobankUniverseRepository.removeBiobankUniverse(biobankUniverseRepository.getUniverse(identifier));
	}

	@Override
	public BiobankUniverse getBiobankUniverse(String identifier)
	{
		return biobankUniverseRepository.getUniverse(identifier);
	}

	@Override
	public void addBiobankUniverseMember(BiobankUniverse biobankUniverse,
			List<BiobankSampleCollection> biobankSampleCollections)
	{
		biobankUniverseRepository.addUniverseMembers(biobankUniverse, biobankSampleCollections);
	}

	@Override
	public List<BiobankSampleCollection> getAllBiobankSampleCollections()
	{
		return biobankUniverseRepository.getAllBiobankSampleCollections();
	}

	@Override
	public List<BiobankSampleCollection> getBiobankSampleCollections(List<String> biobankSampleCollectionNames)
	{
		return biobankSampleCollectionNames.stream().map(biobankUniverseRepository::getBiobankSampleCollection)
				.collect(toList());
	}

	@Override
	public BiobankSampleCollection getBiobankSampleCollection(String biobankSampleCollectionName)
	{
		return biobankUniverseRepository.getBiobankSampleCollection(biobankSampleCollectionName);
	}

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
	public BiobankSampleAttribute getBiobankSampleAttribute(String attributeIdentifier)
	{
		return biobankUniverseRepository.getBiobankSampleAttributes(attributeIdentifier);
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

	@Override
	public void removeAllTagGroups(BiobankSampleCollection biobankSampleCollection)
	{
		List<BiobankSampleAttribute> biobankSampleAttributes = biobankUniverseRepository
				.getBiobankSampleAttributes(biobankSampleCollection);

		biobankUniverseRepository.removeTagGroupsForAttributes(biobankSampleAttributes);
	}

	@Override
	public List<IdentifiableTagGroup> findTagGroupsForAttributes(BiobankSampleAttribute biobankSampleAttribute)
	{
		return tagGroupGenerator
				.generateTagGroups(biobankSampleAttribute.getLabel(), ontologyService.getAllOntologyIds()).stream()
				.map(this::tagGroupToIdentifiableTagGroup).collect(Collectors.toList());
	}

	@Override
	public List<AttributeMappingCandidate> generateAttributeCandidateMappings(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute target, SearchParam searchParam, List<OntologyBasedMatcher> ontologyBasedMatchers)
	{
		if (LOG.isTraceEnabled())
		{
			LOG.trace("Started matching the target attribute: (" + target.getName() + ":" + target.getLabel() + ")");
		}

		List<AttributeMappingCandidate> allCandidates = new ArrayList<>();

		for (OntologyBasedMatcher ontologyBasedMatcher : ontologyBasedMatchers)
		{
			List<BiobankSampleAttribute> sourceAttributes = ontologyBasedMatcher.match(searchParam);

			List<AttributeMappingCandidate> collect = ontologyBasedExplainService
					.explain(biobankUniverse, searchParam, target, newArrayList(sourceAttributes), biobankUniverseScore)
					.stream().filter(candidate -> candidate.getExplanation().getNgramScore() > 0)
					.filter(candidate -> !candidate.getExplanation().getMatchedSourceWords().isEmpty()).sorted()
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
	public Table<BiobankSampleAttribute, BiobankSampleCollection, AttributeMatchStatus> getAttributeMatchStatus(
			BiobankUniverse biobankUniverse, BiobankSampleCollection targetBiobankSampleCollection,
			AttributeMappingTablePager pager, User owner)
	{
		List<BiobankSampleAttribute> targetAttributes = biobankUniverseRepository
				.getBiobankSampleAttributes(targetBiobankSampleCollection, pager);

		List<BiobankSampleCollection> sourceBiobankSampleCollections = biobankUniverse.getMembers().stream()
				.filter(member -> !member.equals(targetBiobankSampleCollection)).collect(toList());

		Iterable<AttributeMappingCandidate> curatedAttributeMatches = biobankUniverseRepository
				.getCuratedAttributeMatches(biobankUniverse, targetAttributes, owner);

		Table<BiobankSampleAttribute, BiobankSampleCollection, AttributeMatchStatus> table = HashBasedTable.create();

		for (AttributeMappingCandidate curatedAttributeMatch : curatedAttributeMatches)
		{
			BiobankSampleAttribute rowKey = curatedAttributeMatch.getTarget();
			BiobankSampleCollection columnKey = curatedAttributeMatch.getSource().getCollection();
			AttributeMappingDecision attributeMappingDecision = curatedAttributeMatch.getDecisions().get(0);

			AttributeMatchStatus attributeMatchStatus;
			switch (attributeMappingDecision.getDecision())
			{
				case YES:
					attributeMatchStatus = CURATED_MATCHES;
					break;
				case NO:
					attributeMatchStatus = CURATED_NO_MATCHES;
					break;
				default:
					attributeMatchStatus = UN_DECIDED;
			}

			if (table.contains(rowKey, columnKey))
			{
				attributeMatchStatus = combineAttributeMatchStatus(table.get(rowKey, columnKey), attributeMatchStatus);
			}

			table.put(rowKey, columnKey, attributeMatchStatus);
		}

		for (BiobankSampleAttribute targetAttribute : targetAttributes)
		{
			for (BiobankSampleCollection sourceBiobankSampleCollection : sourceBiobankSampleCollections)
			{
				if (!table.contains(targetAttribute, sourceBiobankSampleCollection))
				{
					table.put(targetAttribute, sourceBiobankSampleCollection, UN_DECIDED);
				}
			}
		}

		return table;
	}

	private AttributeMatchStatus combineAttributeMatchStatus(AttributeMatchStatus attributeMatchStatus1,
			AttributeMatchStatus attributeMatchStatus2)
	{
		if (attributeMatchStatus1.equals(CURATED_MATCHES) || attributeMatchStatus2.equals(CURATED_MATCHES))
		{
			return CURATED_MATCHES;
		}

		if (attributeMatchStatus1.equals(CURATED_NO_MATCHES) && attributeMatchStatus2.equals(CURATED_NO_MATCHES))
		{
			return CURATED_NO_MATCHES;
		}

		return UN_DECIDED;
	}

	@Override
	public Table<BiobankSampleAttribute, BiobankSampleCollection, List<AttributeMappingCandidate>> getCuratedAttributeMatchCandidates(
			BiobankUniverse biobankUniverse, BiobankSampleCollection targetBiobankSampleCollection,
			AttributeMappingTablePager pager, User owner)
	{
		List<BiobankSampleAttribute> targetAttributes = biobankUniverseRepository
				.getBiobankSampleAttributes(targetBiobankSampleCollection, pager);

		List<BiobankSampleCollection> sourceBiobankSampleCollections = biobankUniverse.getMembers().stream()
				.filter(member -> !member.equals(targetBiobankSampleCollection)).collect(toList());

		Iterable<AttributeMappingCandidate> curatedAttributeMappings = biobankUniverseRepository
				.getCuratedAttributeMatches(biobankUniverse, targetAttributes, owner);

		Table<BiobankSampleAttribute, BiobankSampleCollection, List<AttributeMappingCandidate>> table = HashBasedTable
				.create();

		for (AttributeMappingCandidate attributeMappingCandidate : curatedAttributeMappings)
		{
			BiobankSampleAttribute rowKey = attributeMappingCandidate.getTarget();
			BiobankSampleCollection columnKey = attributeMappingCandidate.getSource().getCollection();

			List<AttributeMappingDecision> decisions = attributeMappingCandidate.getDecisions();

			if (!decisions.isEmpty() && decisions.get(0).getDecision().equals(YES))
			{
				if (!table.contains(rowKey, columnKey))
				{
					table.put(rowKey, columnKey, new ArrayList<>());
				}

				table.get(rowKey, columnKey).add(attributeMappingCandidate);
			}
		}

		//Sort the candidate mappings based on similarity scores and fill in an empty list for the non-existent row/column combiniation
		for (BiobankSampleAttribute row : targetAttributes)
		{
			for (BiobankSampleCollection column : sourceBiobankSampleCollections)
			{
				if (table.contains(row, column))
				{
					sort(table.get(row, column));
				}
				else
				{
					table.put(row, column, emptyList());
				}
			}
		}

		return table;
	}

	@Override
	public List<AttributeMappingCandidate> getCuratedAttributeMatchCandidates(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute targetAttribute, BiobankSampleCollection sourceBiobankSampleCollection)
	{
		List<AttributeMappingCandidate> attributeMappingCandidates = stream(biobankUniverseRepository
				.getAttributeMappingCandidates(biobankUniverse, targetAttribute, sourceBiobankSampleCollection)
				.spliterator(), false).collect(toList());

		sort(attributeMappingCandidates);

		return attributeMappingCandidates;
	}

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

	@Override
	public void addKeyConcepts(BiobankUniverse universe, List<String> semanticTypeNames)
	{
		List<SemanticType> semanticTypes = ontologyService.getSemanticTypesByNames(semanticTypeNames);
		biobankUniverseRepository.addKeyConcepts(universe, semanticTypes);
	}

	@Override
	public void updateBiobankUniverseMemberVectors(BiobankUniverse biobankUniverse)
	{
		List<BiobankUniverseMemberVector> biobankUniverseMemberVectors = vectorSpaceModelCollectionSimilarity
				.createBiobankUniverseMemberVectors(biobankUniverse);

		biobankUniverseRepository.updateBiobankUniverseMemberVectors(biobankUniverse, biobankUniverseMemberVectors);
	}

	private BiobankSampleAttribute importedAttributEntityToBiobankSampleAttribute(BiobankSampleCollection collection,
			Entity entity)
	{
		String identifier = idGenerator.generateId();
		String name = entity.getString(BiobankSampleAttributeMetaData.NAME);
		String label = entity.getString(BiobankSampleAttributeMetaData.LABEL);
		BiobankAttributeDataType biobankAttributeDataType = toEnum(
				entity.getString(BiobankSampleAttributeMetaData.DATA_TYPE));
		String description = entity.getString(BiobankSampleAttributeMetaData.DESCRIPTION);

		return isNotBlank(name) ? BiobankSampleAttribute
				.create(identifier, name, label, description, biobankAttributeDataType, collection, emptyList()) : null;
	}

	private IdentifiableTagGroup tagGroupToIdentifiableTagGroup(TagGroup tagGroup)
	{
		String identifier = idGenerator.generateId();
		String matchedWords = tagGroup.getMatchedWords();
		float score = tagGroup.getScore();

		List<SemanticType> semanticTypes = tagGroup.getOntologyTerms().stream()
				.flatMap(ot -> ot.getSemanticTypes().stream()).collect(toList());

		return IdentifiableTagGroup.create(identifier, tagGroup.getOntologyTerms(), semanticTypes, matchedWords, score);
	}

	@Override
	public List<BiobankSampleCollectionSimilarity> getCollectionSimilarities(BiobankUniverse biobankUniverse,
			NetworkType networkType, List<OntologyTerm> ontologyTermTopics)
	{
		switch (networkType)
		{
			case CANDIDATE_MATCHES:
				return computeAttributeMatchesBasedNetwork(biobankUniverse, ontologyTermTopics, false);
			case CURATED_MATCHES:
				return computeAttributeMatchesBasedNetwork(biobankUniverse, ontologyTermTopics, true);
			case SEMANTIC_SIMILARITY:
			default:
				return computeSemanticSimilarityBasedNetwork(biobankUniverse);
		}

	}

	@Override
	public void curateAttributeMappingCandidates(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute targetAttrinute, List<BiobankSampleAttribute> sourceAttributes,
			BiobankSampleCollection sourceSampleCollection, User molgenisUser)
	{
		List<AttributeMappingCandidate> attributeMappingCandidates = stream(biobankUniverseRepository
						.getAttributeMappingCandidates(biobankUniverse, targetAttrinute, sourceSampleCollection).spliterator(),
				false).collect(toList());

		if (!attributeMappingCandidates.isEmpty())
		{
			List<AttributeMappingDecision> attributeMappingDecisions = new ArrayList<>();

			List<AttributeMappingCandidate> attributeMappingCandidatesToUpdate = new ArrayList<>();

			for (AttributeMappingCandidate attributeMappingCandidate : attributeMappingCandidates)
			{
				BiobankSampleAttribute target = attributeMappingCandidate.getTarget();
				BiobankSampleAttribute source = attributeMappingCandidate.getSource();

				String identifier = attributeMappingCandidate.getDecisions().isEmpty() ? idGenerator
						.generateId() : attributeMappingCandidate.getDecisions().get(0).getIdentifier();

				AttributeMappingDecision attributeMappingDecision = AttributeMappingDecision
						.create(identifier, sourceAttributes.contains(source) ? YES : NO, EMPTY,
								molgenisUser.getUsername(), biobankUniverse);

				attributeMappingDecisions.add(attributeMappingDecision);

				AttributeMappingCandidate attributeMappingCandidateToUpdate = AttributeMappingCandidate
						.create(attributeMappingCandidate.getIdentifier(), biobankUniverse, target, source,
								attributeMappingCandidate.getExplanation(), asList(attributeMappingDecision));

				attributeMappingCandidatesToUpdate.add(attributeMappingCandidateToUpdate);
			}

			boolean add = attributeMappingCandidates.stream().map(AttributeMappingCandidate::getDecisions)
					.allMatch(List::isEmpty);

			biobankUniverseRepository.addAttributeMappingDecisions(attributeMappingDecisions, add);

			biobankUniverseRepository
					.updateAttributeMappingCandidateDecisions(attributeMappingCandidatesToUpdate, molgenisUser);
		}
	}

	private List<BiobankSampleCollectionSimilarity> computeAttributeMatchesBasedNetwork(BiobankUniverse biobankUniverse,
			List<OntologyTerm> ontologyTermTopics, boolean curated)
	{
		List<BiobankSampleCollectionSimilarity> collectionSimilarities = new ArrayList<>();

		AggregateResult aggregateResult = biobankUniverseRepository
				.aggregateAttributeMatches(biobankUniverse, ontologyTermTopics, curated);

		List<List<Long>> matrix = aggregateResult.getMatrix();

		List<Object> xAxisObjects = aggregateResult.getxLabels().stream().filter(Objects::nonNull).collect(toList());

		List<Object> yAxisObjects = aggregateResult.getyLabels().stream().filter(Objects::nonNull).collect(toList());

		long maxCount = matrix.stream().flatMap(List::stream).mapToLong(Long::valueOf).max().orElse(0);

		for (int i = 0; i < xAxisObjects.size(); i++)
		{
			BiobankSampleCollection target = getBiobankSampleCollection(xAxisObjects.get(i).toString());

			for (int j = 0; j < yAxisObjects.size(); j++)
			{
				BiobankSampleCollection source = getBiobankSampleCollection(yAxisObjects.get(j).toString());
				if (matrix.get(i).get(j) != 0)
				{
					float similarity = (float) Math.sqrt((float) matrix.get(i).get(j) / maxCount) / 2;
					String label = Long.toString(matrix.get(i).get(j));
					collectionSimilarities
							.add(BiobankSampleCollectionSimilarity.create(target, source, similarity, label));
				}
			}
		}

		return collectionSimilarities;
	}

	private List<BiobankSampleCollectionSimilarity> computeSemanticSimilarityBasedNetwork(
			BiobankUniverse biobankUniverse)
	{
		BiobankUniverseMemberVector[] array = biobankUniverse.getVectors().stream()
				.toArray(BiobankUniverseMemberVector[]::new);

		List<BiobankSampleCollectionSimilarity> collectionSimilarities = new ArrayList<>();
		for (int i = 0; i < array.length; i++)
		{
			BiobankUniverseMemberVector target = array[i];
			for (int j = i + 1; j < array.length; j++)
			{
				BiobankUniverseMemberVector source = array[j];
				collectionSimilarities.add(vectorSpaceModelCollectionSimilarity.cosineValue(target, source));
			}
		}

		return collectionSimilarities;
	}
}