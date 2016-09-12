package org.molgenis.data.discovery.repo;

import java.util.List;
import java.util.stream.Stream;

import org.molgenis.data.Entity;
import org.molgenis.data.Query;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.AttributeMappingDecision;
import org.molgenis.data.discovery.model.matching.BiobankCollectionSimilarity;
import org.molgenis.data.semanticsearch.explain.bean.AttributeMatchExplanation;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.ontology.core.model.SemanticType;

public interface BiobankUniverseRepository
{
	public abstract void addKeyConcepts(BiobankUniverse biobankUniverse, List<SemanticType> semanticTypes);

	/**
	 * Get all {@link BiobankUniverse}s from the database
	 * 
	 * @return a list of {@link BiobankUniverse}
	 */
	public abstract List<BiobankUniverse> getAllUniverses();

	/**
	 * Get a specific {@link BiobankUniverse} by the identifier from the database
	 * 
	 * @param identifier
	 * @return a {@link BiobankUniverse}
	 */
	public abstract BiobankUniverse getUniverse(String identifier);

	/**
	 * Add a new {@link BiobankUniverse} with initial members {@link BiobankSampleCollection}s
	 * 
	 * @param biobankUniverse
	 */
	public abstract void addBiobankUniverse(BiobankUniverse biobankUniverse);

	/**
	 * Cascading delete the {@link BiobankUniverse} and its related entities including {@link AttributeMappingCandidate}
	 * s, {@link AttributeMappingDecision}s and {@link AttributeMatchExplanation}s
	 * 
	 * @param universe
	 */
	public abstract void removeBiobankUniverse(BiobankUniverse universe);

	/**
	 * Add new members {@link BiobankSampleCollection}s to the existing {@link BiobankUniverse}
	 * 
	 * @param biobankUniverse
	 * @param biobankSampleCollections
	 */
	public abstract void addUniverseMembers(BiobankUniverse biobankUniverse,
			List<BiobankSampleCollection> biobankSampleCollections);

	/**
	 * Remove the members {@link BiobankSampleCollection}s from the existing {@link BiobankUniverse}
	 * 
	 * @param biobankUniverse
	 * @param biobankSampleCollections
	 */
	public abstract void removeUniverseMembers(BiobankUniverse biobankUniverse,
			List<BiobankSampleCollection> biobankSampleCollections);

	/**
	 * Add a new {@link BiobankSampleCollection} to the database
	 * 
	 * @param biobankSampleCollection
	 */
	public abstract void addBiobankSampleCollection(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Cascading delete the given {@link BiobankSampleCollection} and its related entities including
	 * {@link BiobankSampleAttribute}s, {@link AttributeMappingCandidate}s, {@link AttributeMappingDecision}s and
	 * {@link AttributeMatchExplanation}s
	 * 
	 * @param biobankSampleCollection
	 */
	public abstract void removeBiobankSampleCollection(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Get all {@link BiobankSampleCollection}s in the database
	 * 
	 * @return a list of {@link BiobankSampleCollection}s
	 */
	public abstract List<BiobankSampleCollection> getAllBiobankSampleCollections();

	/**
	 * Get a {@link BiobankSampleCollection} by the name
	 * 
	 * @param name
	 * @return a {@link BiobankSampleCollection}
	 */
	public abstract BiobankSampleCollection getBiobankSampleCollection(String name);

	/**
	 * Get all {@link BiobankSampleAttribute}s from the given {@link BiobankSampleCollection}
	 * 
	 * @param biobankSampleCollection
	 * @return a {@link List} of {@link BiobankSampleAttribute}s
	 */
	public abstract Iterable<BiobankSampleAttribute> getBiobankSampleAttributes(
			BiobankSampleCollection biobankSampleCollection);

	/**
	 * Count the number of {@link BiobankSampleAttribute}s associated with the {@link BiobankSampleCollection}
	 * 
	 * @param biobankSampleCollection
	 * @return
	 */
	public abstract int countBiobankSampleAttributes(BiobankSampleCollection biobankSampleCollection);

	/**
	 * Add a list of {@link BiobankSampleAttribute}s to the database
	 * 
	 * @param biobankSampleAttributes
	 */
	public abstract void addBiobankSampleAttributes(Stream<BiobankSampleAttribute> biobankSampleAttributes);

	/**
	 * Cascading delete the given list of {@link BiobankSampleAttribute}s and their related entities including
	 * {@link AttributeMappingCandidate}s, {@link AttributeMappingDecision}s and {@link AttributeMatchExplanation}s
	 * 
	 * @param biobankSampleAttributes
	 */
	public abstract void removeBiobankSampleAttributes(Iterable<BiobankSampleAttribute> biobankSampleAttributes);

	/**
	 * Retrieve a list of {@link BiobankSampleAttribute}s based on the given {@link Query}
	 * 
	 * @param query
	 * @return a list of {@link BiobankSampleAttribute}s
	 */
	public abstract Stream<BiobankSampleAttribute> queryBiobankSampleAttribute(Query query);

	/**
	 * Store all {@link TagGroup}s and update all {@link BiobankSampleAttribute}s
	 * 
	 * @param biobankSampleAttributes
	 */
	public abstract void addTagGroupsForAttributes(Iterable<BiobankSampleAttribute> biobankSampleAttributes);

	/**
	 * Delete all {@link TagGroup}s
	 * 
	 * @param tagGroupStream
	 */
	public abstract void removeTagGroupsForAttributes(Iterable<BiobankSampleAttribute> biobankSampleAttributes);

	/**
	 * Add a list of {@link AttributeMappingCandidate}s to the database
	 * 
	 * @param attributeMappingCandidates
	 */
	public abstract void addAttributeMappingCandidates(List<AttributeMappingCandidate> attributeMappingCandidates);

	/**
	 * Get all the {@link AttributeMappingCandidate}s from the given {@link BiobankUniverse}
	 * 
	 * @param biobankUniverse
	 * @return a list of {@link AttributeMappingCandidate}s
	 */
	public abstract List<AttributeMappingCandidate> getAttributeMappingCandidatesFromUniverse(
			BiobankUniverse biobankUniverse);

	/**
	 * Get all the {@link AttributeMappingCandidate}s, in which either the target or the source is present in the given
	 * list of {@link BiobankSampleAttribute}s
	 * 
	 * @param biobankUniverse
	 * @return a list of {@link AttributeMappingCandidate}s
	 */
	public abstract List<AttributeMappingCandidate> getAttributeMappingCandidates(
			List<BiobankSampleAttribute> biobankSampleAttributes);

	/**
	 * Cascading delete the given list of {@link Entity}s and their related entities including
	 * {@link AttributeMappingDecision}s and {@link AttributeMatchExplanation}s
	 * 
	 * @param attributeMappingCandidates
	 */
	public abstract void removeAttributeMappingCandidates(List<Entity> attributeMappingCandidates);

	public abstract List<String> getBiobankSampleAttributeIdentifiers(BiobankSampleCollection biobankSampleCollection);

	public abstract boolean isBiobankSampleCollectionTagged(BiobankSampleCollection biobankSampleCollection);

	public abstract List<AttributeMappingCandidate> getAttributeMappingCandidates(Query query);

	/**
	 * Add a list of {@link BiobankCollectionSimilarity}s
	 * 
	 * @param biobankCollectionSimilarities
	 */
	public abstract void addCollectionSimilarities(List<BiobankCollectionSimilarity> biobankCollectionSimilarities);

	/**
	 * Remove a list of {@link BiobankCollectionSimilarity}s from the {@link BiobankUniverse}
	 * 
	 * @param biobankCollectionSimilarities
	 */
	public abstract void removeCollectionSimilaritiesFromUniverse(BiobankUniverse biobankUniverse);

	/**
	 * Get a lit of {@link BiobankCollectionSimilarity}s from the {@link BiobankUniverse}
	 * 
	 * @param biobankUniverse
	 * @return
	 */
	public abstract List<BiobankCollectionSimilarity> getCollectionSimilaritiesFromUniverse(
			BiobankUniverse biobankUniverse);
}