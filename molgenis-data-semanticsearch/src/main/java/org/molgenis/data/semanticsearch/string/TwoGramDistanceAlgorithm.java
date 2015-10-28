package org.molgenis.data.semanticsearch.string;

public class TwoGramDistanceAlgorithm
{
	private final static Integer TWO_GRAMS = 2;

	public static double stringMatching(String queryOne, String queryTwo)
	{
		return NGramDistanceAlgorithm.stringMatching(TWO_GRAMS, queryOne, queryTwo);
	}

	public static double stringMatching(String queryOne, String queryTwo, boolean removeStopWords)
	{
		return NGramDistanceAlgorithm.stringMatching(TWO_GRAMS, queryOne, queryTwo, removeStopWords);
	}
}