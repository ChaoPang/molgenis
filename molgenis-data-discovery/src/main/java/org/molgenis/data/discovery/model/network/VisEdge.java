package org.molgenis.data.discovery.model.network;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_VisEdge.class)
public abstract class VisEdge
{
	public abstract String getId();

	public abstract String getLabel();

	public abstract double getLength();

	public abstract String getFrom();

	public abstract String getTo();

	public static VisEdge create(String id, String label, double length, String from, String to)
	{
		return new AutoValue_VisEdge(id, label, length, from, to);
	}
}
