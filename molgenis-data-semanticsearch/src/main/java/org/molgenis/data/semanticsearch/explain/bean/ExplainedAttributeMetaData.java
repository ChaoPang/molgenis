package org.molgenis.data.semanticsearch.explain.bean;

import static org.molgenis.data.semanticsearch.utils.AttributeToMapUtil.attributeToMap;

import java.util.Map;

import javax.annotation.Nullable;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_ExplainedAttributeMetaData.class)
public abstract class ExplainedAttributeMetaData implements Comparable<ExplainedAttributeMetaData>
{
	public static ExplainedAttributeMetaData create(AttributeMetaData attributeMetaData)
	{
		return new AutoValue_ExplainedAttributeMetaData(attributeToMap(attributeMetaData), null, false);
	}

	public static ExplainedAttributeMetaData create(AttributeMetaData attributeMetaData,
			ExplainedQueryString explainedQueryString, boolean highQuality)
	{
		return new AutoValue_ExplainedAttributeMetaData(attributeToMap(attributeMetaData), explainedQueryString,
				highQuality);
	}

	public abstract Map<String, Object> getAttributeMetaData();

	@Nullable
	public abstract ExplainedQueryString getExplainedQueryString();

	public abstract boolean isHighQuality();

	public int compareTo(ExplainedAttributeMetaData other)
	{
		return Float.compare(other.getExplainedQueryString().getScore(), getExplainedQueryString().getScore());
	}

}