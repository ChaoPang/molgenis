package org.molgenis.data.discovery.repo;

import org.molgenis.auth.User;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.Query;
import org.molgenis.data.aggregation.AggregateResult;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.biobank.BiobankUniverseMemberVector;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.AttributeMappingDecision;
import org.molgenis.data.discovery.model.matching.AttributeMappingTablePager;
import org.molgenis.data.discovery.model.matching.MatchingExplanation;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;

import java.util.List;
import java.util.stream.Stream;

/**
 * This repository communicates with the {@link DataService} to manipulate data related to {@link BiobankUniverse}
 *
 * @author chaopang
 */
public interface BiobankUniverseRepository
{
	void addKeyConcepts(BiobankUniverse biobankUniverse, List<SemanticType> semanticTypes);

	/**
	 * Get all {@link BiobankUniverse}s from the database
	 *
	 * @return a list of {@link BiobankUniverse}
	 */
	List<BiobankUniverse> getAllUniverses();

	/**
	 * Get a specific {@link BiobankUniverse} by the identifier from the database
	 *
	 * @param identifier
	 * @return a {@link BiobankUniverse}
	 */
	BiobankUniverse getUniverse(String identifier);

	/**
	 * Add a new {@link BiobankUniverse} with initial members {@link BiobankSampleCollection}s
	 *
	 * @param biobankUniverse
	 */
	void addBiobankUniverse(BiobankUniverse biobankUniverse);

	/**
	 * Cascading delete the {@link BiobankUniverse} and its related entities including {@link AttributeMappingCandidate}
	 * s, {@link AttributeMappingDecision}s and {@link MatchingExplanation}s
	 *
	 * @param universe
	 */
	void removeBiobankUniverse(BiobankUniverse universe);

	/**
	 * Add new members {@link BiobankSampleCollection}s to the existing {@link BiobankUniverse}
	 *
	 * @param biobankUniverse
	 * @param biobankSampleCollections
	 */
	void addUniverseMembers(BiobankUniverse biobankUniverse, List<BiobankSampleCollection> biobankSampleCollections);

	/**
	 * Remove the members {@link BiobankSampleCollection}s from the existing {@link BiobankUniverse}
	 *
	 * @param biobankUniverse
	 * @param biobankSampleCollections
	 */
	void removeUniverseMembers(BiobankUniverse biobankUniverse, List<BiobankSampleCollection> biobankSampleCollections);

	/**
	 * Add a new {@link BiobankSampleCollection} to the database
	 *
	 * @param biobankSampleCollection
	 */
	void addBiobankSampleCollection(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Cascading delete the given {@link BiobankSampleCollection} and its related entities including
	 * {@link BiobankSampleAttribute}s, {@link AttributeMappingCandidate}s, {@link AttributeMappingDecision}s and
	 * {@link MatchingExplanation}s
	 *
	 * @param biobankSampleCollection
	 */
	void removeBiobankSampleCollection(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Get all {@link BiobankSampleCollection}s in the database
	 *
	 * @return a list of {@link BiobankSampleCollection}s
	 */
	List<BiobankSampleCollection> getAllBiobankSampleCollections();

	/**
	 * Get a {@link BiobankSampleCollection} by the name
	 *
	 * @param name
	 * @return a {@link BiobankSampleCollection}
	 */
	BiobankSampleCollection getBiobankSampleCollection(String name);

	/**
	 * Get all {@link BiobankSampleAttribute}s from the given {@link BiobankSampleCollection}
	 *
	 * @param biobankSampleCollection
	 * @return a {@link List} of {@link BiobankSampleAttribute}s
	 */
	List<BiobankSampleAttribute> getBiobankSampleAttributes(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Get a list of {@link BiobankSampleAttribute}s from the given {@link BiobankSampleCollection} based on the {@link AttributeMappingTablePager}
	 *
	 * @param biobankSampleCollection
	 * @param pager
	 * @return
	 */
	List<BiobankSampleAttribute> getBiobankSampleAttributes(BiobankSampleCollection biobankSampleCollection,
			AttributeMappingTablePager pager);

	/**
	 * Get the {@link BiobankSampleAttribute} based on the given identifier
	 *
	 * @param attributeIdentifier
	 * @return
	 */
	BiobankSampleAttribute getBiobankSampleAttributes(String attributeIdentifier);

	/**
	 * Count the number of {@link BiobankSampleAttribute}s associated with the {@link BiobankSampleCollection}
	 *
	 * @param biobankSampleCollection
	 * @return
	 */
	int countBiobankSampleAttributes(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Add a list of {@link BiobankSampleAttribute}s to the database
	 *
	 * @param biobankSampleAttributes
	 */
	void addBiobankSampleAttributes(Stream<BiobankSampleAttribute> biobankSampleAttributes);

	/**
	 * Cascading delete the given list of {@link BiobankSampleAttribute}s and their related entities including
	 * {@link AttributeMappingCandidate}s, {@link AttributeMappingDecision}s and {@link MatchingExplanation}s
	 *
	 * @param biobankSampleAttributes
	 */
	void removeBiobankSampleAttributes(List<BiobankSampleAttribute> biobankSampleAttributes);

	/**
	 * Retrieve a list of {@link BiobankSampleAttribute}s based on the given {@link Query}
	 *
	 * @param query
	 * @return a list of {@link BiobankSampleAttribute}s
	 */
	List<BiobankSampleAttribute> getBiobankSampleAttributes(Query<Entity> query);

	/**
	 * Store all {@link TagGroup}s and update all {@link BiobankSampleAttribute}s
	 *
	 * @param biobankSampleAttributes
	 */
	void addTagGroupsForAttributes(List<BiobankSampleAttribute> biobankSampleAttributes);

	/**
	 * Delete all {@link TagGroup}s that are associated with the given {@link BiobankSampleAttribute}s
	 *
	 * @param biobankSampleAttributes
	 */
	void removeTagGroupsForAttributes(List<BiobankSampleAttribute> biobankSampleAttributes);

	/**
	 * Add a list of {@link AttributeMappingCandidate}s to the database
	 *
	 * @param attributeMappingCandidates
	 */
	void addAttributeMappingCandidates(List<AttributeMappingCandidate> attributeMappingCandidates);

	/**
	 * Count the number of attribute matches based on the target {@link BiobankSampleCollection} and source {@link BiobankSampleCollection} in the current {@link BiobankUniverse}
	 *
	 * @param biobankUniverse
	 * @param ontologyTermTopics a {@link List} of {@link OntologyTerm} topic to filter the aggregate result
	 * @param curated            a flag indicating whether to aggregate curated matches or canddiate matches
	 * @return
	 */
	AggregateResult aggregateAttributeMatches(BiobankUniverse biobankUniverse, List<OntologyTerm> ontologyTermTopics,
			boolean curated);

	/**
	 * Get an {@link Iterable} (iterated over one time) of the curated {@link AttributeMappingCandidate}s for the current {@link MolgenisUser} from the current {@link BiobankUniverse} for target {@link BiobankSampleAttribute}s.
	 *
	 * @param biobankUniverse
	 * @param targetAttributes
	 * @param owner
	 * @return
	 */
	Iterable<AttributeMappingCandidate> getCuratedAttributeMatches(BiobankUniverse biobankUniverse,
			List<BiobankSampleAttribute> targetAttributes, User owner);

	/**
	 * Get an {@link Iterable} (iterated over one time) of the {@link AttributeMappingCandidate}s from the current {@link BiobankUniverse} for target {@link BiobankSampleAttribute}s
	 *
	 * @param biobankUniverse
	 * @param targetAttributes
	 * @return
	 */
	Iterable<AttributeMappingCandidate> getAttributeMappingCandidates(BiobankUniverse biobankUniverse,
			List<BiobankSampleAttribute> targetAttributes);

	/**
	 * Get an {@link Iterable} (iterated over one time) of the {@link AttributeMappingCandidate}s for the target {@link BiobankSampleAttribute} and the source {@link BiobankSampleCollection}s from the current {@link BiobankUniverse}
	 *
	 * @param biobankUniverse
	 * @param targetAttrinute
	 * @param sourceSampleCollection
	 * @return
	 */
	Iterable<AttributeMappingCandidate> getAttributeMappingCandidates(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute targetAttrinute, BiobankSampleCollection sourceSampleCollection);

	/**
	 * Update the {@link AttributeMappingDecision}s in the {@link AttributeMappingCandidate}s in the database for the given {@link MolgenisUser}
	 *
	 * @param attributeMappingCandidatesToUpdate
	 * @param user                               the user who makes the curation decisions
	 */
	void updateAttributeMappingCandidateDecisions(List<AttributeMappingCandidate> attributeMappingCandidatesToUpdate,
			User user);

	/**
	 * Cascading delete the given list of {@link Entity}s and their related entities including
	 * {@link AttributeMappingDecision}s and {@link MatchingExplanation}s
	 *
	 * @param attributeMappingCandidates
	 */
	void removeAttributeMappingCandidates(List<Entity> attributeMappingCandidates);

	List<String> getBiobankSampleAttributeIdentifiers(BiobankSampleCollection biobankSampleCollection);

	boolean isBiobankSampleCollectionTagged(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Update {@link BiobankUniverse} with the new list of {@link BiobankUniverseMemberVector}s
	 *
	 * @param biobankUniverse
	 * @param biobankUniverseMemberVectors
	 */
	void updateBiobankUniverseMemberVectors(BiobankUniverse biobankUniverse,
			List<BiobankUniverseMemberVector> biobankUniverseMemberVectors);

	/**
	 * Add a list of {@link AttributeMappingDecision}s
	 *
	 * @param attributeMappingDecisions
	 * @param toAdd
	 */
	void addAttributeMappingDecisions(List<AttributeMappingDecision> attributeMappingDecisions, boolean toAdd);
}