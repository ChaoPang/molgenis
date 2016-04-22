package org.molgenis.data.mapper.algorithmgenerator.service;

import java.util.List;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.mapper.algorithmgenerator.bean.GeneratedAlgorithm;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParameters;

public interface AlgorithmGeneratorService
{
	public abstract String generate(AttributeMetaData targetAttribute, List<AttributeMetaData> sourceAttributes,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData);

	public abstract GeneratedAlgorithm autoGenerate(SemanticSearchParameters semanticSearchParameters,
			List<AttributeMetaData> sourceAttributes);
}
