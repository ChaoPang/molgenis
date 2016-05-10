package org.molgenis.data.semanticsearch.service.bean;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_SemanticSearchParameter.class)
public abstract class SemanticSearchParameter
{
	public static SemanticSearchParameter create(AttributeMetaData targetAttribute, Set<String> userQueries,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData, boolean exactMatch)
	{
		return new AutoValue_SemanticSearchParameter(targetAttribute, userQueries, targetEntityMetaData,
				asList(sourceEntityMetaData), exactMatch, QueryExpansionParameter.create(false, false));
	}

	public static SemanticSearchParameter create(AttributeMetaData targetAttribute, Set<String> userQueries,
			EntityMetaData targetEntityMetaData, List<EntityMetaData> sourceEntityMetaDatas, boolean exactMatch)
	{
		return new AutoValue_SemanticSearchParameter(targetAttribute, userQueries, targetEntityMetaData,
				sourceEntityMetaDatas, exactMatch, QueryExpansionParameter.create(false, false));
	}

	public static SemanticSearchParameter create(AttributeMetaData targetAttribute, Set<String> userQueries,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData, boolean exactMatch,
			boolean semanticSearchEnabled, boolean childOntologyTermExpansionEnabled)
	{
		return new AutoValue_SemanticSearchParameter(targetAttribute, userQueries, targetEntityMetaData,
				asList(sourceEntityMetaData), exactMatch,
				QueryExpansionParameter.create(semanticSearchEnabled, childOntologyTermExpansionEnabled));
	}

	public static SemanticSearchParameter create(AttributeMetaData targetAttribute, Set<String> userQueries,
			EntityMetaData targetEntityMetaData, List<EntityMetaData> sourceEntityMetaDatas, boolean exactMatch,
			boolean semanticSearchEnabled, boolean childOntologyTermExpansionEnabled)
	{
		return new AutoValue_SemanticSearchParameter(targetAttribute, userQueries, targetEntityMetaData,
				sourceEntityMetaDatas, exactMatch,
				QueryExpansionParameter.create(semanticSearchEnabled, childOntologyTermExpansionEnabled));
	}

	public static SemanticSearchParameter create(AttributeMetaData targetAttribute, Set<String> userQueries,
			EntityMetaData targetEntityMetaData, List<EntityMetaData> sourceEntityMetaDatas, boolean exactMatch,
			QueryExpansionParameter ontologyExpansionParameter)
	{
		return new AutoValue_SemanticSearchParameter(targetAttribute, userQueries, targetEntityMetaData,
				sourceEntityMetaDatas, exactMatch, ontologyExpansionParameter);
	}

	public static SemanticSearchParameter create(SemanticSearchParameter semanticSearchParameter,
			QueryExpansionParameter ontologyExpansionParameter)
	{
		return new AutoValue_SemanticSearchParameter(semanticSearchParameter.getTargetAttribute(),
				semanticSearchParameter.getUserQueries(), semanticSearchParameter.getTargetEntityMetaData(),
				semanticSearchParameter.getSourceEntityMetaDatas(), semanticSearchParameter.isExactMatch(),
				ontologyExpansionParameter);
	}

	public static SemanticSearchParameter create(SemanticSearchParameter semanticSearchParameter,
			boolean semanticSearchEnabled, boolean childOntologyTermExpansionEnabled)
	{
		return new AutoValue_SemanticSearchParameter(semanticSearchParameter.getTargetAttribute(),
				semanticSearchParameter.getUserQueries(), semanticSearchParameter.getTargetEntityMetaData(),
				semanticSearchParameter.getSourceEntityMetaDatas(), semanticSearchParameter.isExactMatch(),
				QueryExpansionParameter.create(semanticSearchEnabled, childOntologyTermExpansionEnabled));
	}

	public abstract AttributeMetaData getTargetAttribute();

	@Nullable
	public abstract Set<String> getUserQueries();

	@Nullable
	public abstract EntityMetaData getTargetEntityMetaData();

	public abstract List<EntityMetaData> getSourceEntityMetaDatas();

	public abstract boolean isExactMatch();

	public abstract QueryExpansionParameter getExpansionParameter();
}
