package org.molgenis.data.discovery.service;

import java.util.List;
import java.util.stream.Stream;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.Entity;
import org.molgenis.data.discovery.model.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.BiobankSampleCollection;
import org.molgenis.data.discovery.model.BiobankUniverse;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;

public interface BiobankUniverseService
{
	/**
	 * Add a new {@link BiobankUniverse} with the initial members
	 * 
	 * @param universeName
	 * @param biobankSampleNames
	 * @param owner
	 * 
	 * @return a {@link BiobankUniverse}
	 */
	public abstract BiobankUniverse addBiobankUniverse(String universeName, MolgenisUser owner);

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
	 * Import sampleName as the {@link BiobankSampleCollection} and import the list of BiobankSampleAttributeEntities as
	 * the {@link BiobankSampleAttribute}s
	 * 
	 * @param sampleName
	 * @param BiobankSampleAttributeEntityStream
	 */
	public abstract void importSampleCollections(String sampleName, Stream<Entity> BiobankSampleAttributeEntityStream);

	public abstract List<BiobankSampleCollection> getAllBiobankSampleCollections();

	/**
	 * Generate a list of {@link AttributeMappingCandidate}s for all {@link BiobankSampleCollection}s based on the given
	 * parameter {@link SemanticSearchParam}
	 * 
	 * @param target
	 * @param semanticSearchParam
	 * @param existingMembers
	 * @return
	 */
	public abstract List<AttributeMappingCandidate> findCandidateMappings(BiobankSampleAttribute target,
			SemanticSearchParam semanticSearchParam, List<BiobankSampleCollection> existingMembers);

	public abstract BiobankSampleCollection getBiobankSampleCollection(String biobankSampleCollectionName);

	public abstract List<BiobankSampleCollection> getBiobankSampleCollections(
			List<String> biobankSampleCollectionNames);

	/**
	 * Generate a list of {@link TagGroup}s for the given {@link BiobankSampleAttribute}
	 * 
	 * @param biobankSampleAttribute
	 * @return a list of {@link TagGroup}
	 */
	public abstract List<TagGroup> findTagGroupsForAttributes(BiobankSampleAttribute biobankSampleAttribute);
}
