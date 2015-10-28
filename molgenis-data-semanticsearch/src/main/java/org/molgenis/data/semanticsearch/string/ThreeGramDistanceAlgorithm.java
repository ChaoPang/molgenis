package org.molgenis.data.semanticsearch.string;

public class ThreeGramDistanceAlgorithm
{
	private final static Integer THREE_GRAMS = 3;

	public static double stringMatching(String queryOne, String queryTwo)
	{
		return NGramDistanceAlgorithm.stringMatching(THREE_GRAMS, queryOne, queryTwo);
	}

	public static double stringMatching(String queryOne, String queryTwo, boolean removeStopWords)
	{
		return NGramDistanceAlgorithm.stringMatching(THREE_GRAMS, queryOne, queryTwo, removeStopWords);
	}
}