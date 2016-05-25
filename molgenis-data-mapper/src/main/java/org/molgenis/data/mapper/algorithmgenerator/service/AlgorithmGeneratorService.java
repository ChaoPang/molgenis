package org.molgenis.data.mapper.algorithmgenerator.service;

import java.util.List;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.mapper.algorithmgenerator.bean.GeneratedAlgorithm;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;

public interface AlgorithmGeneratorService
{
	public abstract String generate(AttributeMetaData targetAttribute, List<AttributeMetaData> sourceAttributes,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData);

	public abstract GeneratedAlgorithm autoGenerate(SemanticSearchParam semanticSearchParam,
			AttributeMetaData targetAttribute, EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData,
			List<AttributeMetaData> sourceAttributes);
}
