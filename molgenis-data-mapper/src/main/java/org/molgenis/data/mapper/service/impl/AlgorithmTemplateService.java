package org.molgenis.data.mapper.service.impl;

import java.util.List;
import java.util.stream.Stream;

import org.molgenis.data.AttributeMetaData;

/**
 * Find suitable algorithm templates for provided attribute matches returned from {@see SemanticSearchService}.
 */
public interface AlgorithmTemplateService
{
	/**
	 * @param targetAttribute
	 * @param relevantAttributes
	 *            attribute matches returned from {@see SemanticSearchService}.
	 * @return algorithm templates that can be rendered using the given source and target
	 */
	Stream<AlgorithmTemplate> find(AttributeMetaData targetAttribute, List<AttributeMetaData> relevantAttributes);
}
