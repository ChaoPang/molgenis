package org.molgenis.data.discovery.scoring.attributes;

import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.ontology.ic.TermFrequencyService;

public class NgramAttributeSimilarity extends AttributeSimilarity
{
	public NgramAttributeSimilarity(TermFrequencyService termFrequencyService)
	{
		super(SimilarityFunctionName.NGRAM, termFrequencyService);
	}

	@Override
	public float score(String document1, String document2, boolean strictMatch)
	{
		boolean removeStopWords = !strictMatch;

		List<String> termTokens1 = createTermTokens(document1, removeStopWords);

		List<String> termTokens2 = createTermTokens(document2, removeStopWords);

		Map<String, Double> weightedWordSimilarity = redistributedNGramScore(termTokens1);

		double calibratedScore = termTokens1.stream()
				.filter(originalWord -> termTokens2.contains(originalWord)
						&& weightedWordSimilarity.containsKey(originalWord))
				.map(word -> weightedWordSimilarity.get(word)).mapToDouble(Double::doubleValue).sum();

		double score = stringMatching(document1, document2, removeStopWords) + calibratedScore;

		return (float) Math.round(score * 10) / 1000.0f;
	}

	public Map<String, Double> redistributedNGramScore(List<String> terms)
	{
		Map<String, Double> wordWeightedSimilarity = new HashMap<>();

		Map<String, Float> termIDF = terms.stream().distinct()
				.collect(Collectors.toMap(term -> term, term -> termFrequencyService.getTermFrequency(term)));

		if (termIDF.size() > 0)
		{
			double averageIDFValue = termIDF.values().stream().mapToDouble(Double::valueOf).average().getAsDouble();

			double queryStringLength = StringUtils.join(terms, " ").trim().length();

			double totalContribution = 0;

			double totalDenominator = 0;

			for (Entry<String, Float> entry : termIDF.entrySet())
			{
				double diff = entry.getValue() - averageIDFValue;
				if (diff < 0)
				{
					Double contributedSimilarity = (entry.getKey().length() / queryStringLength)
							* (diff / averageIDFValue);
					totalContribution += Math.abs(contributedSimilarity);
					wordWeightedSimilarity.put(entry.getKey(), contributedSimilarity);
				}
				else
				{
					totalDenominator += diff;
				}
			}

			for (Entry<String, Float> entry : termIDF.entrySet())
			{
				double diff = entry.getValue() - averageIDFValue;
				if (diff > 0)
				{
					wordWeightedSimilarity.put(entry.getKey(), ((diff / totalDenominator) * totalContribution));
				}
			}
		}

		return wordWeightedSimilarity;
	}
}
