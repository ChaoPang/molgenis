package org.molgenis.data.semanticsearch.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.ontology.core.model.OntologyTerm;

public interface SemanticSearchService
{
	/**
	 * Find all relevant source attributes
	 * 
	 * @param source
	 * @param attributeMetaData
	 * @param ontologyTerms
	 * 
	 * @return AttributeMetaData of resembling attributes, sorted by relevance
	 */
	List<AttributeMetaData> findAttributes(AttributeMetaData targetAttribute, EntityMetaData targetEntityMetaData,
			EntityMetaData sourceEntityMetaData, Set<String> searchTerms);

	List<OntologyTerm> findOntologyTermsForAttr(AttributeMetaData attribute, EntityMetaData entityMetadata,
			Set<String> searchTerms);

	/**
	 * Finds {@link OntologyTerm}s that can be used to tag an attribute.
	 * 
	 * @param entity
	 *            name of the entity
	 * @param ontologies
	 *            IDs of ontologies to take the {@link OntologyTerm}s from.
	 * @return {@link Map} of {@link Hit}s for {@link OntologyTerm} results
	 */
	Map<AttributeMetaData, Hit<OntologyTerm>> findTagsForEntity(String entity, List<String> ontologyIDs);

	/**
	 * Finds {@link OntologyTerm}s for an attribute.
	 * 
	 * @param attribute
	 *            AttributeMetaData to tag
	 * @param ontologyIds
	 *            IDs of ontologies to take the {@link OntologyTerm}s from.
	 * @return {@link List} of {@link Hit}s for {@link OntologyTerm}s found, most relevant first
	 */
	Hit<OntologyTerm> findTagForAttr(AttributeMetaData attribute, List<String> ontologyIds);

	Hit<OntologyTerm> findTag(String description, List<String> ontologyIds);

	/**
	 * Finds all {@link OntologyTermHit}s for an attribute with in all ontology terms for the specified ontologies.
	 * {@link OntologyTermHit} contains the best combination of ontology terms that yields the highest lexical
	 * similarity score.
	 * 
	 * @param attribute
	 * @param ontologyIds
	 * @return
	 */
	List<Hit<OntologyTermHit>> findAllTagsForAttr(AttributeMetaData attribute, List<String> ontologyIds);

	List<Hit<OntologyTermHit>> findAllTags(String description, List<String> ontologyIds);

	/**
	 * Filters all the relevant {@link OntologyTerm}s and creates the {@link OntologyTermHit}s for the attribute.
	 * {@link OntologyTermHit} contains the best combination of ontology terms that yields the highest lexical
	 * similarity score.
	 * 
	 * @param attribute
	 * @param ontologyIds
	 * @param scope
	 *            defines a scope of ontology terms in which the search is performed.
	 * @return
	 */
	List<Hit<OntologyTermHit>> filterTagsForAttr(AttributeMetaData attribute, List<String> ontologyIds,
			List<OntologyTerm> scope);
}