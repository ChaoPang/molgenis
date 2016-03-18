package org.molgenis.data.semanticsearch.explain.service;

import java.util.Set;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;

public interface AttributeMappingExplainService
{
	/**
	 * This method is used to explain why the given source attribute is matched to the target attribute
	 * 
	 * @param userQueries
	 * @param targetAttribute
	 * @param matchedSourceAttribute
	 * @param targetEntityMetaData
	 * @param sourceEntityMetaData
	 * @return
	 */
	public abstract ExplainedAttributeMetaData explainAttributeMapping(Set<String> userQueries,
			AttributeMetaData targetAttribute, AttributeMetaData matchedSourceAttribute,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData);

	public abstract ExplainedAttributeMetaData explainAttributeMapping(AttributeMetaData targetAttribute,
			AttributeMetaData matchedSourceAttribute, EntityMetaData targetEntityMetaData,
			EntityMetaData sourceEntityMetaData);
}
