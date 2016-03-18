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

	List<OntologyTerm> getOntologyTermsForAttr(AttributeMetaData attribute, EntityMetaData entityMetadata,
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
	Map<AttributeMetaData, Hit<OntologyTerm>> findTags(String entity, List<String> ontologyIDs);

	/**
	 * Finds {@link OntologyTerm}s for an attribute.
	 * 
	 * @param attribute
	 *            AttributeMetaData to tag
	 * @param ontologyIds
	 *            IDs of ontologies to take the {@link OntologyTerm}s from.
	 * @return {@link List} of {@link Hit}s for {@link OntologyTerm}s found, most relevant first
	 */
	Hit<OntologyTerm> findTags(AttributeMetaData attribute, List<String> ontologyIds);

	Hit<OntologyTermHit> findAndFilterTags(AttributeMetaData attribute, List<String> ontologyIds,
			List<OntologyTerm> ontologyTerms);
}
