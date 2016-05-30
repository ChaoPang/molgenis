package org.molgenis.data.discovery.model;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_SemanticType.class)
public abstract class SemanticType
{
	public abstract String getIdentifier();

	public abstract String getName();

	public abstract String getGroup();

	public static SemanticType create(String identifier, String name, String group)
	{
		return new AutoValue_SemanticType(identifier, name, group);
	}
}
