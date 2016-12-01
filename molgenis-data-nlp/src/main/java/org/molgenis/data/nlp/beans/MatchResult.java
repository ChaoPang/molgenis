package org.molgenis.data.nlp.beans;

import com.google.auto.value.AutoValue;
import org.molgenis.gson.AutoGson;

import static org.molgenis.data.nlp.beans.MatchResult.Result.*;

@AutoValue
@AutoGson(autoValueClass = AutoValue_MatchResult.class)
public abstract class MatchResult
{
	public enum Result
	{
		FULL_MATCH, PARTIAL_MATCH, NO_MATCH, UNCERTAIN;
	}

	public abstract String getIdentifier();

	public abstract Result getResult();

	public static MatchResult create(String identifier, Result result)
	{
		return new AutoValue_MatchResult(identifier, result);
	}

	public boolean isFullMatch()
	{
		return getResult().equals(FULL_MATCH);
	}

	public boolean isParitialMatch()
	{
		return getResult().equals(FULL_MATCH) || getResult().equals(PARTIAL_MATCH);
	}

	public boolean isNoMatch()
	{
		return getResult().equals(NO_MATCH);
	}

	public boolean isUncertain()
	{
		return getResult().equals(UNCERTAIN);
	}

	public static MatchResult combine(MatchResult first, MatchResult second)
	{
		Result result;
		if (first.isFullMatch() && second.isFullMatch())
		{
			result = FULL_MATCH;
		}
		else if (first.isFullMatch() && second.isParitialMatch())
		{
			result = PARTIAL_MATCH;
		}
		else if (first.isParitialMatch() && second.isFullMatch())
		{
			result = PARTIAL_MATCH;
		}
		else if (first.isParitialMatch() && second.isParitialMatch())
		{
			result = PARTIAL_MATCH;
		}
		else if (first.isNoMatch() || second.isNoMatch())
		{
			result = NO_MATCH;

		}
		else
		{
			result = UNCERTAIN;
		}

		String combinedIdentifier = first.getIdentifier() + second.getIdentifier();

		return MatchResult.create(combinedIdentifier, result);
	}

}
