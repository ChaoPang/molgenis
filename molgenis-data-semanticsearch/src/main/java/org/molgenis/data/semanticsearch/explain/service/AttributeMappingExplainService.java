package org.molgenis.data.semanticsearch.explain.service;

import java.util.Set;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;

public interface AttributeMappingExplainService
{
	/**
	 * Explains why the given source attribute is matched to the target attribute using all the information available
	 * including the query terms from the target attribute, user defined queries, ontology terms and their children.
	 * 
	 * @param userQueries
	 * @param targetAttribute
	 * @param matchedSourceAttribute
	 * @return
	 */
	ExplainedAttributeMetaData explainByAll(Set<String> userQueries, AttributeMetaData targetAttribute,
			AttributeMetaData matchedSourceAttribute, EntityMetaData targetEntityMetaData);

	/**
	 * Explains why the given source attribute is matched to the target attribute using the information including query
	 * terms from the target attribute and user defined queries.
	 * 
	 * @param userQueries
	 * @param targetAttribute
	 * @param matchedSourceAttribute
	 * @return
	 */
	ExplainedAttributeMetaData explainByAttribute(Set<String> userQueries, AttributeMetaData targetAttribute,
			AttributeMetaData matchedSourceAttribute);

	/**
	 * Explains why the given source attribute is matched to the target attribute using the information including query
	 * terms the target attribute, user defined queries and the ontology term synonyms + label.
	 * 
	 * @param userQueries
	 * @param targetAttribute
	 * @param matchedSourceAttribute
	 * @param targetEntityMetaData
	 * @return
	 */
	ExplainedAttributeMetaData explainBySynonyms(Set<String> userQueries, AttributeMetaData targetAttribute,
			AttributeMetaData matchedSourceAttribute, EntityMetaData targetEntityMetaData);

	/**
	 * Explains why the given source attribute is matched to the target attribute using the information from a set of
	 * query terms and a set of ontology terms.
	 * 
	 * @param queriesFromTargetAttribute
	 * @param ontologyTerms
	 * @param matchedSourceAttribute
	 * @return
	 */
	ExplainedAttributeMetaData explainAttributeMapping(Set<String> userQueries, AttributeMetaData targetAttribute,
			AttributeMetaData matchedSourceAttribute, EntityMetaData targetEntityMetaData);
}
