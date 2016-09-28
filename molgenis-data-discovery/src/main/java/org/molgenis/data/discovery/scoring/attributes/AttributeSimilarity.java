package org.molgenis.data.discovery.scoring.attributes;

import java.util.List;
import java.util.stream.Collectors;

import org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils;
import org.molgenis.ontology.ic.TermFrequencyService;
import org.molgenis.ontology.utils.NGramDistanceAlgorithm;
import org.molgenis.ontology.utils.Stemmer;

public abstract class AttributeSimilarity
{
	public enum SimilarityFunctionName
	{
		NGRAM("Ngram"), VSM("VSM");

		String label;

		SimilarityFunctionName(String label)
		{
			this.label = label;
		}
	}

	protected final TermFrequencyService termFrequencyService;

	private final SimilarityFunctionName similarityFunctionName;

	public AttributeSimilarity(SimilarityFunctionName similarityFunctionName, TermFrequencyService termFrequencyService)
	{
		this.similarityFunctionName = similarityFunctionName;
		this.termFrequencyService = termFrequencyService;
	}

	protected List<String> createTermTokens(String document, boolean removeStopWords)
	{
		List<String> terms = SemanticSearchServiceUtils.splitIntoTerms(document);
		if (removeStopWords) terms.removeAll(NGramDistanceAlgorithm.STOPWORDSLIST);
		return terms.stream().map(Stemmer::stem).collect(Collectors.toList());
	}

	public SimilarityFunctionName getSimilarityFunctionName()
	{
		return similarityFunctionName;
	}

	public abstract float score(String document1, String document2, boolean strictMatch);
}
