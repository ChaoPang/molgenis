package org.molgenis.data.discovery.service;

import com.google.common.collect.Table;
import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.Entity;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.biobank.BiobankUniverseMemberVector;
import org.molgenis.data.discovery.model.matching.*;
import org.molgenis.data.discovery.model.network.VisNetworkRequest.NetworkType;
import org.molgenis.data.discovery.service.impl.OntologyBasedMatcher;
import org.molgenis.data.semanticsearch.service.bean.SearchParam;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;

import java.util.List;
import java.util.stream.Stream;

public interface BiobankUniverseService
{
	public enum AttributeMatchStatus
	{
		UN_DECIDED, CURATED_MATCHES, CURATED_NO_MATCHES, NO_CANDIDATES;
	}

	/**
	 * Add a new {@link BiobankUniverse} with the initial members
	 *
	 * @param universeName
	 * @param semanticTypeGroups
	 * @param owner
	 */
	BiobankUniverse addBiobankUniverse(String universeName, List<String> semanticTypeGroups, MolgenisUser owner);

	/**
	 * Delete a {@link BiobankUniverse} by Id
	 *
	 * @param biobankUniverseId
	 */
	void deleteBiobankUniverse(String biobankUniverseId);

	/**
	 * Get all {@link BiobankUniverse}s
	 *
	 * @return a list of {@link BiobankUniverse}s
	 */
	List<BiobankUniverse> getBiobankUniverses();

	/**
	 * Get a {@link BiobankUniverse} based on its identifier
	 *
	 * @param identifier
	 * @return {@link BiobankUniverse}
	 */
	BiobankUniverse getBiobankUniverse(String identifier);

	/**
	 * Add a list of {@link BiobankSampleCollection}s to a {@link BiobankUniverse}
	 *
	 * @param biobankUniverse
	 * @param biobankSampleCollections
	 */
	void addBiobankUniverseMember(BiobankUniverse biobankUniverse,
			List<BiobankSampleCollection> biobankSampleCollections);

	/**
	 * Import sampleName as the {@link BiobankSampleCollection} and import the list of BiobankSampleAttributeEntities as
	 * the {@link BiobankSampleAttribute}s
	 *
	 * @param sampleName
	 * @param BiobankSampleAttributeEntityStream
	 */
	void importSampleCollections(String sampleName, Stream<Entity> BiobankSampleAttributeEntityStream);

	/**
	 * Get all {@link BiobankSampleCollection}s
	 *
	 * @return a list of {@link BiobankSampleCollection}s
	 */
	List<BiobankSampleCollection> getAllBiobankSampleCollections();

	/**
	 * Get a {@link BiobankSampleCollection} by name
	 *
	 * @param biobankSampleCollectionName
	 * @return {@link BiobankSampleCollection}
	 */
	BiobankSampleCollection getBiobankSampleCollection(String biobankSampleCollectionName);

	/**
	 * Get a list of {@link BiobankSampleCollection}s by the given names
	 *
	 * @param biobankSampleCollectionNames
	 * @return a list of {@link BiobankSampleCollection}s
	 */
	List<BiobankSampleCollection> getBiobankSampleCollections(List<String> biobankSampleCollectionNames);

	/**
	 * Cascading delete the given {@link BiobankSampleCollection} and its related entities including
	 * {@link BiobankSampleAttribute}s, {@link AttributeMappingCandidate}s, {@link AttributeMappingDecision}s and
	 * {@link org.molgenis.data.discovery.model.matching.MatchingExplanation}s
	 *
	 * @param biobankSampleCollection
	 */
	void removeBiobankSampleCollection(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Check if any of the {@link BiobankSampleAttribute}s in the {@link BiobankSampleCollection} has been tagged
	 *
	 * @param biobankSampleCollection
	 * @return
	 */
	boolean isBiobankSampleCollectionTagged(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Delete all {@link IdentifiableTagGroup}s associated with {@link BiobankSampleAttribute}s in the given
	 * {@link BiobankSampleCollection}
	 *
	 * @param biobankSampleCollection
	 */
	void removeAllTagGroups(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Generate a list of {@link IdentifiableTagGroup}s for the given {@link BiobankSampleAttribute}
	 *
	 * @param biobankSampleAttribute
	 * @return a list of {@link IdentifiableTagGroup}
	 */
	List<IdentifiableTagGroup> findTagGroupsForAttributes(BiobankSampleAttribute biobankSampleAttribute);

	/**
	 * Add a list of {@link SemanticType} groups to the {@link BiobankUniverse} to add the associated semantic types as
	 * key concepts
	 *
	 * @param universe
	 * @param semanticTypeGroups
	 */
	void addKeyConcepts(BiobankUniverse universe, List<String> semanticTypeGroups);

	/**
	 * Generate a list of {@link AttributeMappingCandidate}s for all {@link BiobankSampleCollection}s based on the given
	 * parameter {@link SearchParam}
	 *
	 * @param biobankUniverse
	 * @param target
	 * @param searchParam
	 * @param ontologyBasedInputData
	 * @return
	 */
	List<AttributeMappingCandidate> generateAttributeCandidateMappings(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute target, SearchParam searchParam, List<OntologyBasedMatcher> ontologyBasedInputData);

	/**
	 * Get all {@link AttributeMatchStatus}s for target {@link BiobankSampleCollection} and source {@link BiobankSampleCollection}s from the current {@link BiobankUniverse}
	 *
	 * @param biobankUniverse
	 * @param targetBiobankSampleCollection
	 * @param pager
	 * @param owner
	 * @return
	 */
	Table<BiobankSampleAttribute, BiobankSampleCollection, AttributeMatchStatus> getAttributeMatchStatus(
			BiobankUniverse biobankUniverse, BiobankSampleCollection targetBiobankSampleCollection,
			AttributeMappingTablePager pager, MolgenisUser owner);

	/**
	 * Get a {@link List} of {@link AttributeMappingCandidate}s from the given {@link BiobankUniverse} for the given {@link BiobankSampleCollection} as the target
	 *
	 * @param biobankUniverse
	 * @param targetBiobankSampleCollection
	 * @param pager
	 * @param owner
	 * @return
	 */
	Table<BiobankSampleAttribute, BiobankSampleCollection, List<AttributeMappingCandidate>> getCuratedAttributeMatchCandidates(
			BiobankUniverse biobankUniverse, BiobankSampleCollection targetBiobankSampleCollection,
			AttributeMappingTablePager pager, MolgenisUser owner);

	/**
	 * Get a {@link List} of {@link AttributeMappingCandidate}s from the given {@link BiobankUniverse} for the target {@link BiobankSampleAttribute} and the source {@link BiobankSampleCollection}
	 *
	 * @param biobankUniverse
	 * @param targetAttribute
	 * @param sourceBiobankSampleCollection
	 * @return
	 */
	List<AttributeMappingCandidate> getCuratedAttributeMatchCandidates(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute targetAttribute, BiobankSampleCollection sourceBiobankSampleCollection);

	/**
	 * Get a list of {@link BiobankSampleAttribute}s for the given {@link BiobankSampleCollection}
	 *
	 * @param biobankSampleAttribute
	 * @return a list of {@link BiobankSampleAttribute}s
	 */
	List<BiobankSampleAttribute> getBiobankSampleAttributes(BiobankSampleCollection biobankSampleAttribute);

	/**
	 * Get the {@link BiobankSampleAttribute} based on the given identifier
	 *
	 * @param attributeIdentifier
	 * @return
	 */
	BiobankSampleAttribute getBiobankSampleAttribute(String attributeIdentifier);

	int countBiobankSampleAttributes(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Update the {@link BiobankUniverseMemberVector}s
	 *
	 * @param biobankUniverse
	 */
	void updateBiobankUniverseMemberVectors(BiobankUniverse biobankUniverse);

	/**
	 * Compute the pairwise cosine similarities between {@link BiobankSampleCollection}s in the {@link BiobankUniverse} depending on the {@link NetworkType}
	 *
	 * @param biobankUniverse
	 * @param networkType
	 * @param ontologyTermTopics a list of {@link OntologyTerm}s to filter on
	 * @return
	 */
	List<BiobankSampleCollectionSimilarity> getCollectionSimilarities(BiobankUniverse biobankUniverse,
			NetworkType networkType, List<OntologyTerm> ontologyTermTopics);

	/**
	 * Make the decisions on the candidate matches. The provided source {@link BiobankSampleAttribute}s are the final matches for the given target {@link BiobankSampleAttribute} in the current {@link BiobankUniverse}
	 *
	 * @param biobankUniverse
	 * @param targetAttrinute
	 * @param sourceAttributes
	 * @param sourceSampleCollection
	 * @param currentUser
	 */
	void curateAttributeMappingCandidates(BiobankUniverse biobankUniverse, BiobankSampleAttribute targetAttrinute,
			List<BiobankSampleAttribute> sourceAttributes, BiobankSampleCollection sourceSampleCollection,
			MolgenisUser currentUser);
}
