package org.molgenis.data.discovery.service;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.Entity;
import org.molgenis.data.Query;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.biobank.BiobankUniverseMemberVector;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.AttributeMappingDecision;
import org.molgenis.data.discovery.model.matching.BiobankSampleCollectionSimilarity;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.service.impl.OntologyBasedMatcher;
import org.molgenis.data.semanticsearch.service.bean.SearchParam;
import org.molgenis.ontology.core.model.SemanticType;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface BiobankUniverseService
{
	/**
	 * Add a new {@link BiobankUniverse} with the initial members
	 * 
	 * @param universeName
	 * @param semanticTypeGroups
	 * @param owner
	 * 
	 */
	public abstract BiobankUniverse addBiobankUniverse(String universeName, List<String> semanticTypeGroups,
			MolgenisUser owner);

	/**
	 * Delete a {@link BiobankUniverse} by Id
	 * 
	 * @param biobankUniverseId
	 */
	public abstract void deleteBiobankUniverse(String biobankUniverseId);

	/**
	 * Get all {@link BiobankUniverse}s
	 * 
	 * @return a list of {@link BiobankUniverse}s
	 */
	public abstract List<BiobankUniverse> getBiobankUniverses();

	/**
	 * Get a {@link BiobankUniverse} based on its identifier
	 * 
	 * @param identifier
	 * @return {@link BiobankUniverse}
	 */
	public abstract BiobankUniverse getBiobankUniverse(String identifier);

	/**
	 * Add a list of {@link BiobankSampleCollection}s to a {@link BiobankUniverse}
	 * 
	 * @param biobankUniverse
	 * @param biobankSampleCollections
	 */
	public abstract void addBiobankUniverseMember(BiobankUniverse biobankUniverse,
			List<BiobankSampleCollection> biobankSampleCollections);

	/**
	 * Import sampleName as the {@link BiobankSampleCollection} and import the list of BiobankSampleAttributeEntities as
	 * the {@link BiobankSampleAttribute}s
	 * 
	 * @param sampleName
	 * @param BiobankSampleAttributeEntityStream
	 */
	public abstract void importSampleCollections(String sampleName, Stream<Entity> BiobankSampleAttributeEntityStream);

	/**
	 * Get all {@link BiobankSampleCollection}s
	 * 
	 * @return a list of {@link BiobankSampleCollection}s
	 */
	public abstract List<BiobankSampleCollection> getAllBiobankSampleCollections();

	/**
	 * Get a {@link BiobankSampleCollection} by name
	 * 
	 * @param biobankSampleCollectionName
	 * @return {@link BiobankSampleCollection}
	 */
	public abstract BiobankSampleCollection getBiobankSampleCollection(String biobankSampleCollectionName);

	/**
	 * Get a list of {@link BiobankSampleCollection}s by the given names
	 * 
	 * @param biobankSampleCollectionNames
	 * @return a list of {@link BiobankSampleCollection}s
	 */
	public abstract List<BiobankSampleCollection> getBiobankSampleCollections(
			List<String> biobankSampleCollectionNames);

	/**
	 * Cascading delete the given {@link BiobankSampleCollection} and its related entities including
	 * {@link BiobankSampleAttribute}s, {@link AttributeMappingCandidate}s, {@link AttributeMappingDecision}s and
	 * {@link org.molgenis.data.discovery.model.matching.MatchingExplanation}s
	 * 
	 * @param biobankSampleCollection
	 */
	public abstract void removeBiobankSampleCollection(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Check if any of the {@link BiobankSampleAttribute}s in the {@link BiobankSampleCollection} has been tagged
	 * 
	 * @param biobankSampleCollection
	 * @return
	 */
	public abstract boolean isBiobankSampleCollectionTagged(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Delete all {@link IdentifiableTagGroup}s associated with {@link BiobankSampleAttribute}s in the given
	 * {@link BiobankSampleCollection}
	 * 
	 * @param biobankSampleCollection
	 */
	public abstract void removeAllTagGroups(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Generate a list of {@link IdentifiableTagGroup}s for the given {@link BiobankSampleAttribute}
	 * 
	 * @param biobankSampleAttribute
	 * @return a list of {@link IdentifiableTagGroup}
	 */
	public abstract List<IdentifiableTagGroup> findTagGroupsForAttributes(
			BiobankSampleAttribute biobankSampleAttribute);

	/**
	 * Add a list of {@link SemanticType} groups to the {@link BiobankUniverse} to add the associated semantic types as
	 * key concepts
	 * 
	 * @param universe
	 * @param semanticTypeGroups
	 */
	public abstract void addKeyConcepts(BiobankUniverse universe, List<String> semanticTypeGroups);

	/**
	 * Get all {@link AttributeMappingCandidate}s for the given target {@link BiobankSampleCollection}
	 * 
	 * @param biobankUniverse
	 * @param target
	 * @return
	 */
	Map<BiobankSampleCollection, List<AttributeMappingCandidate>> getAttributeCandidateMappings(
			BiobankUniverse biobankUniverse, BiobankSampleCollection target);

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
	public abstract List<AttributeMappingCandidate> generateAttributeCandidateMappings(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute target, SearchParam searchParam, List<OntologyBasedMatcher> ontologyBasedInputData);

	/**
	 * Get a list of {@link AttributeMappingCandidate}s based on the given {@link Query}
	 * 
	 * @param query
	 * @return a list of {@link AttributeMappingCandidate}s
	 */
	public abstract List<AttributeMappingCandidate> getCandidateMappingsCandidates(Query<Entity> query);

	/**
	 * Get a list of {@link BiobankSampleAttribute}s for the given {@link BiobankSampleCollection}
	 * 
	 * @param biobankSampleAttribute
	 * @return a list of {@link BiobankSampleAttribute}s
	 */
	public abstract List<BiobankSampleAttribute> getBiobankSampleAttributes(
			BiobankSampleCollection biobankSampleAttribute);

	public abstract int countBiobankSampleAttributes(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Update the {@link BiobankUniverseMemberVector}s
	 * 
	 * @param biobankUniverse
	 */
	public abstract void updateBiobankUniverseMemberVectors(BiobankUniverse biobankUniverse);

	/**
	 * Compute the pairwise cosine similarities between {@link BiobankSampleCollection}s in the {@link BiobankUniverse}
	 * 
	 * @param biobankUniverse
	 * @return
	 */
	public abstract List<BiobankSampleCollectionSimilarity> getCollectionSimilarities(BiobankUniverse biobankUniverse);
}
