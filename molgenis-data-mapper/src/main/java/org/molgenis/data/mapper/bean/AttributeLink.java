package org.molgenis.data.mapper.bean;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_AttributeLink.class)
public abstract class AttributeLink
{
	public static AttributeLink create(Integer source, Integer target, Integer value)
	{
		return new AutoValue_AttributeLink(source, target, value);
	}

	public abstract Integer getSource();

	public abstract Integer getTarget();

	public abstract Integer getValue();
}
