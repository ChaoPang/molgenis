package org.molgenis.data.discovery.model.network;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_VisNode.class)
public abstract class VisNode
{
	public abstract String getId();

	public abstract String getLabel();

	public abstract int getSize();

	public static VisNode create(String id, String label, int size)
	{
		return new AutoValue_VisNode(id, label, size);
	}
}
