package org.molgenis.data.semanticsearch.explain.service;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParameters;

public interface AttributeMappingExplainService
{
	/**
	 * Explains why the given source attribute is matched to the target attribute using all the information available
	 * including the query terms from the target attribute, user defined queries, ontology terms and their children.
	 * 
	 * @param semanticSearchParameters
	 * @param matchedSourceAttribute
	 * @return
	 */
	ExplainedAttributeMetaData explainAttributeMapping(SemanticSearchParameters semanticSearchParameters,
			AttributeMetaData matchedSourceAttribute);
}
