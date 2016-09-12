package org.molgenis.data.discovery.model.network;

import java.util.List;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_VisNetworkResponse.class)
public abstract class VisNetworkResponse
{
	public abstract List<VisNode> getNodes();

	public abstract List<VisEdge> getEdges();

	public static VisNetworkResponse create(List<VisNode> nodes, List<VisEdge> edges)
	{
		return new AutoValue_VisNetworkResponse(nodes, edges);
	}
}
