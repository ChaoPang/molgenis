package org.molgenis.data.mapper.data.request;

import java.util.List;

import org.molgenis.data.mapper.bean.AttributeLink;
import org.molgenis.data.mapper.bean.AttributeNode;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_AttributeConnectionResponse.class)
public abstract class AttributeConnectionResponse
{
	public static AttributeConnectionResponse create(List<AttributeNode> nodes, List<AttributeLink> links)
	{
		return new AutoValue_AttributeConnectionResponse(nodes, links);
	}

	public abstract List<AttributeNode> getNodes();

	public abstract List<AttributeLink> getLinks();
}
