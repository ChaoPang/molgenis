package org.molgenis.data.semanticsearch.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParameter;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParameter;
import org.molgenis.ontology.core.model.OntologyTerm;

public interface SemanticSearchService
{
	/**
	 * Finds all relevant source {@link AttributeMetaData}s. Due to the large number of queries that might be generated
	 * from ontology terms, this method will search for attributes using the information in such an order, 1. only the
	 * terms collected from the target @{link AttributeMetaData}; 2. synonyms collected from @{link OntologyTerm}s; 3.
	 * all terms including children terms from @{link OntologyTerm}s. At any of the stage, it the match result passes a
	 * certain threshold the search will stop.
	 * 
	 * @param targetAttribute
	 * @param targetEntityMetaData
	 * @param sourceEntityMetaData
	 * @param searchTerms
	 * @return
	 */
	List<AttributeMetaData> findAttributesLazy(SemanticSearchParameter semanticSearchParameters);

	List<ExplainedAttributeMetaData> findAttributesLazyWithExplanations(
			SemanticSearchParameter semanticSearchParameters);

	List<AttributeMetaData> findAttributes(SemanticSearchParameter semanticSearchParameters);

	List<ExplainedAttributeMetaData> findAttributesWithExplanation(SemanticSearchParameter semanticSearchParameters);

	Map<EntityMetaData, List<AttributeMetaData>> findMultiEntityAttributes(
			SemanticSearchParameter semanticSearchParameters);

	Map<EntityMetaData, List<AttributeMetaData>> findAttributes(Set<String> queryTerms,
			List<OntologyTermHit> ontologyTermHits, QueryExpansionParameter ontologyExpansionParameters,
			List<EntityMetaData> sourceEntityMetaDatas);

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
	List<OntologyTermHit> findAllTagsForAttr(AttributeMetaData attribute, List<String> ontologyIds);

	List<OntologyTermHit> findAllTags(String description, List<String> ontologyIds);

}