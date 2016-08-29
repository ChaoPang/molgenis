package org.molgenis.data.semanticsearch.service.bean;

import java.util.List;
import java.util.Set;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_SemanticSearchParam.class)
public abstract class SemanticSearchParam
{
	public final static float DEFAULT_HIGH_QUALITY_THRESHOLD = 0.8f;

	public abstract Set<String> getLexicalQueries();

	public abstract List<TagGroup> getTagGroups();

	public abstract QueryExpansionParam getQueryExpansionParameter();

	public abstract float getHighQualityThreshold();

	public abstract boolean isStrictMatch();

	public static SemanticSearchParam create(Set<String> lexicalQueries, List<TagGroup> tagGroups,
			QueryExpansionParam queryExpansionParameter)
	{
		return new AutoValue_SemanticSearchParam(lexicalQueries, tagGroups, queryExpansionParameter,
				DEFAULT_HIGH_QUALITY_THRESHOLD, false);
	}

	public static SemanticSearchParam create(Set<String> lexicalQueries, List<TagGroup> tagGroups,
			QueryExpansionParam queryExpansionParameter, boolean strictMatch)
	{
		return new AutoValue_SemanticSearchParam(lexicalQueries, tagGroups, queryExpansionParameter,
				DEFAULT_HIGH_QUALITY_THRESHOLD, strictMatch);
	}

	public static SemanticSearchParam create(Set<String> lexicalQueries, List<TagGroup> tagGroups,
			QueryExpansionParam queryExpansionParameter, float highQualityThreshold)
	{
		return new AutoValue_SemanticSearchParam(lexicalQueries, tagGroups, queryExpansionParameter,
				highQualityThreshold, false);
	}
}
