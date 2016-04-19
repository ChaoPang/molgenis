package org.molgenis.data.semanticsearch.service.bean;

import static java.util.Collections.emptySet;

import java.util.Set;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;

import static java.util.Objects.requireNonNull;

public class QueryExpansion
{
	private final AttributeMetaData targetAttribute;
	private final Set<String> userQueries;
	private final EntityMetaData targetEntityMetaData;
	private final EntityMetaData sourceEntityMetaData;
	private boolean semanticSearchEnabled;
	private boolean childOntologyTermExpansionEnabled;

	public QueryExpansion(AttributeMetaData targetAttribute, Set<String> userQueries,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData, boolean semanticSearchEnabled,
			boolean childOntologyTermExpansionEnabled)
	{
		this.targetAttribute = requireNonNull(targetAttribute);
		this.userQueries = userQueries == null ? emptySet() : userQueries;
		this.targetEntityMetaData = requireNonNull(targetEntityMetaData);
		this.sourceEntityMetaData = requireNonNull(sourceEntityMetaData);
		this.semanticSearchEnabled = semanticSearchEnabled;
		this.childOntologyTermExpansionEnabled = childOntologyTermExpansionEnabled;
	}

	public boolean isSemanticSearchEnabled()
	{
		return semanticSearchEnabled;
	}

	public void enableSemanticSearch()
	{
		this.semanticSearchEnabled = true;
	}

	public void disableSemanticSearch()
	{
		this.semanticSearchEnabled = false;
	}

	public boolean isChildOntologyTermExpansionEnabled()
	{
		return childOntologyTermExpansionEnabled;
	}

	public void enableChildOntologyTermExpansion()
	{
		this.childOntologyTermExpansionEnabled = true;
	}

	public void disableChildOntologyTermExpansion()
	{
		this.childOntologyTermExpansionEnabled = true;
	}

	public AttributeMetaData getTargetAttribute()
	{
		return targetAttribute;
	}

	public Set<String> getUserQueries()
	{
		return userQueries;
	}

	public EntityMetaData getTargetEntityMetaData()
	{
		return targetEntityMetaData;
	}

	public EntityMetaData getSourceEntityMetaData()
	{
		return sourceEntityMetaData;
	}
}
