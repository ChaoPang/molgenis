package org.molgenis.data.discovery.scoring;

import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;

public class NGramScoringModel implements ScoringModel
{
	@Override
	public float score(String document1, String document2, boolean strictMatch)
	{
		boolean removeStopWords = !strictMatch;

		return (float) stringMatching(document1, document2, removeStopWords);
	}
}
