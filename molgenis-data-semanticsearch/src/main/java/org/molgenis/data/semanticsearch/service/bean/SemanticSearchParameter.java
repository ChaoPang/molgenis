package org.molgenis.data.semanticsearch.service.bean;

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
				sourceEntityMetaData, exactMatch, QueryExpansionParameter.create(false, false));
	}

	public static SemanticSearchParameter create(AttributeMetaData targetAttribute, Set<String> userQueries,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData, boolean exactMatch,
			boolean semanticSearchEnabled, boolean childOntologyTermExpansionEnabled)
	{
		return new AutoValue_SemanticSearchParameter(targetAttribute, userQueries, targetEntityMetaData,
				sourceEntityMetaData, exactMatch,
				QueryExpansionParameter.create(semanticSearchEnabled, childOntologyTermExpansionEnabled));
	}

	public static SemanticSearchParameter create(AttributeMetaData targetAttribute, Set<String> userQueries,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData, boolean exactMatch,
			QueryExpansionParameter ontologyExpansionParameters)
	{
		return new AutoValue_SemanticSearchParameter(targetAttribute, userQueries, targetEntityMetaData,
				sourceEntityMetaData, exactMatch, ontologyExpansionParameters);
	}

	public static SemanticSearchParameter create(SemanticSearchParameter semanticSearchParameters,
			QueryExpansionParameter ontologyExpansionParameters)
	{
		return new AutoValue_SemanticSearchParameter(semanticSearchParameters.getTargetAttribute(),
				semanticSearchParameters.getUserQueries(), semanticSearchParameters.getTargetEntityMetaData(),
				semanticSearchParameters.getSourceEntityMetaData(), semanticSearchParameters.isExactMatch(),
				ontologyExpansionParameters);
	}

	public static SemanticSearchParameter create(SemanticSearchParameter semanticSearchParameters,
			boolean semanticSearchEnabled, boolean childOntologyTermExpansionEnabled)
	{
		return new AutoValue_SemanticSearchParameter(semanticSearchParameters.getTargetAttribute(),
				semanticSearchParameters.getUserQueries(), semanticSearchParameters.getTargetEntityMetaData(),
				semanticSearchParameters.getSourceEntityMetaData(), semanticSearchParameters.isExactMatch(),
				QueryExpansionParameter.create(semanticSearchEnabled, childOntologyTermExpansionEnabled));
	}

	public abstract AttributeMetaData getTargetAttribute();

	@Nullable
	public abstract Set<String> getUserQueries();

	@Nullable
	public abstract EntityMetaData getTargetEntityMetaData();

	public abstract EntityMetaData getSourceEntityMetaData();

	public abstract boolean isExactMatch();

	public abstract QueryExpansionParameter getOntologyExpansionParameters();
}
