package org.molgenis.data.semanticsearch.service.bean;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_QueryExpansionParameter.class)
public abstract class QueryExpansionParam
{
	private final static int DEFAULT_SEARCH_LEVEL = 3;

	public static QueryExpansionParam create(boolean semanticSearchEnabled, boolean childExpansionEnabled)
	{
		return new AutoValue_QueryExpansionParameter(semanticSearchEnabled, childExpansionEnabled,
				DEFAULT_SEARCH_LEVEL);
	}

	public static QueryExpansionParam create(boolean semanticSearchEnabled, boolean childExpansionEnabled,
			int expansionLevel)
	{
		return new AutoValue_QueryExpansionParameter(semanticSearchEnabled, childExpansionEnabled, expansionLevel);
	}

	public abstract boolean isSemanticSearchEnabled();

	public abstract boolean isChildExpansionEnabled();

	public abstract int getExpansionLevel();
}
