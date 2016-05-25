package org.molgenis.data.semanticsearch.explain.service;

import org.molgenis.data.semanticsearch.explain.bean.AttributeMatchExplanation;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;

public interface ExplainMappingService
{
	/**
	 * Explains why the given source attribute is matched to the target attribute using all the information available
	 * including the query terms from the target attribute, user defined queries, ontology terms and their children.
	 * 
	 * @param semanticSearchParam
	 * @param matchedSource
	 * 
	 * @return {@link AttributeMatchExplanation}
	 */
	AttributeMatchExplanation explainMapping(SemanticSearchParam semanticSearchParam, String matchedSource);
}
