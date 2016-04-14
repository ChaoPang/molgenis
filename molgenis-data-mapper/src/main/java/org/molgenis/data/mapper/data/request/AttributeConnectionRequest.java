package org.molgenis.data.mapper.data.request;

import java.util.List;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_AttributeConnectionRequest.class)
public abstract class AttributeConnectionRequest
{
	public abstract List<String> getEntityNames();
}
