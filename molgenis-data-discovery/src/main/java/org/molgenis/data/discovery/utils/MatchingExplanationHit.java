package org.molgenis.data.discovery.utils;

import com.google.auto.value.AutoValue;
import org.molgenis.data.discovery.model.matching.MatchedOntologyTermHit;
import org.molgenis.gson.AutoGson;

import java.util.List;

import static java.lang.Double.compare;

@AutoValue
@AutoGson(autoValueClass = AutoValue_MatchingExplanationHit.class)
public abstract class MatchingExplanationHit implements Comparable<MatchingExplanationHit>
{
	public abstract String getMatchedWords();

	public abstract List<MatchedOntologyTermHit> getMatchedOntologyTermHits();

	public abstract float getVsmScore();

	public abstract float getNgramScore();

	public static MatchingExplanationHit create(String matchedWords,
			List<MatchedOntologyTermHit> matchedOntologyTermHits, float vsmScore, float ngramScore)
	{
		return new AutoValue_MatchingExplanationHit(matchedWords, matchedOntologyTermHits, vsmScore, ngramScore);
	}

	public int compareTo(MatchingExplanationHit o)
	{
		return compare(o.getVsmScore(), getVsmScore());
	}

	public float getScore()
	{
		return getVsmScore();
	}
}
