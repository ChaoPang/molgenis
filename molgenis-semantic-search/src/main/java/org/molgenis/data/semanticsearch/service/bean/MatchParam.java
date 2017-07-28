package org.molgenis.data.semanticsearch.service.bean;

import com.google.auto.value.AutoValue;
import org.molgenis.gson.AutoGson;

@AutoValue
@AutoGson(autoValueClass = AutoValue_MatchParam.class)
public abstract class MatchParam
{
	public final static float DEFAULT_HIGH_QUALITY_THRESHOLD = 0.8f;
	public final static boolean DEFAULT_HIGH_STRICT_MATCH = false;

	public abstract float getHighQualityThreshold();

	public abstract boolean isStrictMatch();

	public static MatchParam create()
	{
		return new AutoValue_MatchParam(DEFAULT_HIGH_QUALITY_THRESHOLD, DEFAULT_HIGH_STRICT_MATCH);
	}

	public static MatchParam create(boolean strictMatch)
	{
		return new AutoValue_MatchParam(DEFAULT_HIGH_QUALITY_THRESHOLD, strictMatch);
	}

	public static MatchParam create(float highQualityThreashold)
	{
		return new AutoValue_MatchParam(highQualityThreashold, DEFAULT_HIGH_STRICT_MATCH);
	}

	public static MatchParam create(float highQualityThreashold, boolean strictMatch)
	{
		return new AutoValue_MatchParam(highQualityThreashold, strictMatch);
	}
}
