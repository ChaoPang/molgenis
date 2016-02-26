package org.molgenis.data.semanticsearch.explain.bean;

import static org.molgenis.data.semanticsearch.string.AttributeToMapUtil.attributeToMap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Sets;

@AutoValue
@AutoGson(autoValueClass = AutoValue_ExplainedAttributeMetaData.class)
public abstract class ExplainedAttributeMetaData
{
	public static ExplainedAttributeMetaData create(AttributeMetaData attributeMetaData)
	{
		return new AutoValue_ExplainedAttributeMetaData(attributeToMap(attributeMetaData), Collections.emptySet(),
				false);
	}

	public static ExplainedAttributeMetaData create(AttributeMetaData attributeMetaData,
			Iterable<ExplainedQueryString> explainedQueryStrings, boolean highQuality)
	{
		return new AutoValue_ExplainedAttributeMetaData(attributeToMap(attributeMetaData),
				Sets.newHashSet(explainedQueryStrings), highQuality);
	}

	public abstract Map<String, Object> getAttributeMetaData();

	public abstract Set<ExplainedQueryString> getExplainedQueryStrings();

	public abstract boolean isHighQuality();

}