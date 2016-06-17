package org.molgenis.data.semanticsearch.service.bean;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_QueryExpansionParam.class)
public abstract class QueryExpansionParam
{
	private final static int DEFAULT_SEARCH_LEVEL = 4;

	public static QueryExpansionParam create(boolean semanticSearchEnabled, boolean childExpansionEnabled)
	{
		return new AutoValue_QueryExpansionParam(semanticSearchEnabled, childExpansionEnabled, DEFAULT_SEARCH_LEVEL);
	}

	public static QueryExpansionParam create(boolean semanticSearchEnabled, boolean childExpansionEnabled,
			int expansionLevel)
	{
		return new AutoValue_QueryExpansionParam(semanticSearchEnabled, childExpansionEnabled, expansionLevel);
	}

	public abstract boolean isSemanticSearchEnabled();

	public abstract boolean isChildExpansionEnabled();

	public abstract int getExpansionLevel();
}
