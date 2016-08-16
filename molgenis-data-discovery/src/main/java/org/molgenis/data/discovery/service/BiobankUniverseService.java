package org.molgenis.data.discovery.service;

import java.util.List;
import java.util.stream.Stream;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.Entity;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.AttributeMappingDecision;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.service.impl.OntologyBasedMatcher;
import org.molgenis.data.semanticsearch.explain.bean.AttributeMatchExplanation;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;

public interface BiobankUniverseService
{
	/**
	 * Add a new {@link BiobankUniverse} with the initial members
	 * 
	 * @param universeName
	 * @param biobankSampleNames
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
	 * @param string
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
	 * {@link AttributeMatchExplanation}s
	 * 
	 * @param biobankSampleCollection
	 */
	public abstract void removeBiobankSampleCollection(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Generate a list of {@link AttributeMappingCandidate}s for all {@link BiobankSampleCollection}s based on the given
	 * parameter {@link SemanticSearchParam}
	 * 
	 * @param target
	 * @param semanticSearchParam
	 * @param existingMembers
	 * @return
	 */
	public abstract List<AttributeMappingCandidate> findCandidateMappings(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute target, SemanticSearchParam semanticSearchParam,
			List<BiobankSampleCollection> existingMembers);

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
	 * Check if a particular {@link OntologyTerm} is a key concept
	 * 
	 * @param ontologyTerm
	 * @return
	 */
	public abstract boolean isOntologyTermKeyConcept(BiobankUniverse biobankUniverse, OntologyTerm ontologyTerm);

	/**
	 * Add a list of {@link SemanticType} groups to the {@link BiobankUniverse} to add the associated semantic types as
	 * key concepts
	 * 
	 * @param universe
	 * @param semanticTypeGroups
	 */
	public abstract void addKeyConcepts(BiobankUniverse universe, List<String> semanticTypeGroups);

	List<AttributeMappingCandidate> findCandidateMappingsOntologyBased(BiobankUniverse biobankUniverse,
			BiobankSampleAttribute target, SemanticSearchParam semanticSearchParam,
			List<OntologyBasedMatcher> ontologyBasedInputData);
}
