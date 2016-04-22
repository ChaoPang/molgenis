package org.molgenis.data.semanticsearch.service.bean;

import java.util.Set;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_SemanticSearchParameters.class)
public abstract class SemanticSearchParameters
{
	public static SemanticSearchParameters create(AttributeMetaData targetAttribute, Set<String> userQueries,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData, boolean semanticSearchEnabled,
			boolean childOntologyTermExpansionEnabled, boolean exactMatch)
	{
		return new AutoValue_SemanticSearchParameters(targetAttribute, userQueries, targetEntityMetaData,
				sourceEntityMetaData, semanticSearchEnabled, childOntologyTermExpansionEnabled, exactMatch);
	}

	public static SemanticSearchParameters create(SemanticSearchParameters semanticSearchParameters,
			boolean semanticSearchEnabled, boolean childOntologyTermExpansionEnabled, boolean exactMatch)
	{
		return new AutoValue_SemanticSearchParameters(semanticSearchParameters.getTargetAttribute(),
				semanticSearchParameters.getUserQueries(), semanticSearchParameters.getTargetEntityMetaData(),
				semanticSearchParameters.getSourceEntityMetaData(), semanticSearchEnabled,
				childOntologyTermExpansionEnabled, exactMatch);
	}

	public abstract AttributeMetaData getTargetAttribute();

	public abstract Set<String> getUserQueries();

	public abstract EntityMetaData getTargetEntityMetaData();

	public abstract EntityMetaData getSourceEntityMetaData();

	public abstract boolean isSemanticSearchEnabled();

	public abstract boolean isChildOntologyTermExpansionEnabled();

	public abstract boolean isExactMatch();

}
