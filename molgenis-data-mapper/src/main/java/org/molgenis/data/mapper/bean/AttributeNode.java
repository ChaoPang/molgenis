package org.molgenis.data.mapper.bean;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_AttributeNode.class)
public abstract class AttributeNode
{
	public static AttributeNode create(String name, String label, int group)
	{
		return new AutoValue_AttributeNode(name, label, group);
	}

	public abstract String getName();

	public abstract String getLabel();

	public abstract int getGroup();
}
