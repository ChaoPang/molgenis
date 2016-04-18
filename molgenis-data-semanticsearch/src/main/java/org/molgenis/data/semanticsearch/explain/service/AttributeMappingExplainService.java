package org.molgenis.data.semanticsearch.explain.service;

import java.util.Set;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;

public interface AttributeMappingExplainService
{
	/**
	 * Explains why the given source attribute is matched to the target attribute using the information from a set of
	 * query terms and a set of ontology terms.
	 * 
	 * @param queriesFromTargetAttribute
	 * @param ontologyTerms
	 * @param matchedSourceAttribute
	 * @return
	 */
	ExplainedAttributeMetaData explainAttributeMapping(AttributeMetaData targetAttribute, Set<String> userQueries,
			AttributeMetaData matchedSourceAttribute, EntityMetaData targetEntityMetaData);

	/**
	 * Explains why the given source attribute is matched to the target attribute using all the information available
	 * including the query terms from the target attribute, user defined queries, ontology terms and their children.
	 * 
	 * @param userQueries
	 * @param targetAttribute
	 * @param matchedSourceAttribute
	 * @param targetEntityMetaData
	 * @param semanticSearchEnabled
	 * @param childOntologyTermExpansionEnabled
	 * @return
	 */
	ExplainedAttributeMetaData explainAttributeMapping(AttributeMetaData targetAttribute, Set<String> userQueries,
			AttributeMetaData matchedSourceAttribute, EntityMetaData targetEntityMetaData,
			boolean semanticSearchEnabled, boolean childOntologyTermExpansionEnabled);
}
