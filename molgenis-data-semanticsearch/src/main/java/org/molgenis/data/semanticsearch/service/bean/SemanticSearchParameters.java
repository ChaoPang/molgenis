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
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData, boolean exactMatch)
	{
		return new AutoValue_SemanticSearchParameters(targetAttribute, userQueries, targetEntityMetaData,
				sourceEntityMetaData, exactMatch, false, false);
	}

	public static SemanticSearchParameters create(AttributeMetaData targetAttribute, Set<String> userQueries,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData, boolean exactMatch,
			boolean semanticSearchEnabled, boolean childOntologyTermExpansionEnabled)
	{
		return new AutoValue_SemanticSearchParameters(targetAttribute, userQueries, targetEntityMetaData,
				sourceEntityMetaData, exactMatch, semanticSearchEnabled, childOntologyTermExpansionEnabled);
	}

	public static SemanticSearchParameters create(SemanticSearchParameters semanticSearchParameters,
			boolean semanticSearchEnabled, boolean childOntologyTermExpansionEnabled)
	{
		return new AutoValue_SemanticSearchParameters(semanticSearchParameters.getTargetAttribute(),
				semanticSearchParameters.getUserQueries(), semanticSearchParameters.getTargetEntityMetaData(),
				semanticSearchParameters.getSourceEntityMetaData(), semanticSearchParameters.isExactMatch(),
				semanticSearchEnabled, childOntologyTermExpansionEnabled);
	}

	public abstract AttributeMetaData getTargetAttribute();

	public abstract Set<String> getUserQueries();

	public abstract EntityMetaData getTargetEntityMetaData();

	public abstract EntityMetaData getSourceEntityMetaData();

	public abstract boolean isExactMatch();

	public abstract boolean isSemanticSearchEnabled();

	public abstract boolean isChildOntologyTermExpansionEnabled();
}
