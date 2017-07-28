package org.molgenis.data.semanticsearch.service.bean;

import com.google.auto.value.AutoValue;
import org.molgenis.gson.AutoGson;

import java.util.List;
import java.util.Set;

@AutoValue
@AutoGson(autoValueClass = AutoValue_SearchParam.class)
public abstract class SearchParam
{
	public abstract Set<String> getLexicalQueries();

	public abstract List<TagGroup> getTagGroups();

	public abstract boolean isSemanticSearchEnabled();

	public abstract MatchParam getMatchParam();

	public static SearchParam create(Set<String> lexicalQueries, List<TagGroup> tagGroups)
	{
		return new AutoValue_SearchParam(lexicalQueries, tagGroups, true, MatchParam.create());
	}

	public static SearchParam create(Set<String> lexicalQueries, List<TagGroup> tagGroups,
			boolean isSemanticSearchEnabled)
	{
		return new AutoValue_SearchParam(lexicalQueries, tagGroups, isSemanticSearchEnabled, MatchParam.create());
	}

	public static SearchParam create(Set<String> lexicalQueries, List<TagGroup> tagGroups,
			boolean isSemanticSearchEnabled, MatchParam matchParam)
	{
		return new AutoValue_SearchParam(lexicalQueries, tagGroups, isSemanticSearchEnabled, matchParam);
	}
}
