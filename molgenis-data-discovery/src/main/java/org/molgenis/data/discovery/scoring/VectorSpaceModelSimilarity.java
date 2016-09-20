package org.molgenis.data.discovery.scoring;

import static java.util.stream.Collectors.toList;
import static org.molgenis.data.discovery.scoring.Similarity.SimilarityFunctionName.VSM;

import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.molgenis.ontology.ic.TermFrequencyService;

public class VectorSpaceModelSimilarity extends Similarity
{
	public VectorSpaceModelSimilarity(TermFrequencyService termFrequencyService)
	{
		super(VSM, termFrequencyService);
	}

	@Override
	public float score(String document1, String document2, boolean strictMatch)
	{
		boolean removeStopWords = true;

		List<String> terms1 = createTermTokens(document1, removeStopWords);

		List<String> terms2 = createTermTokens(document2, removeStopWords);

		List<String> totalUniqueTerms = Stream.concat(terms1.stream(), terms2.stream()).distinct().collect(toList());

		double[] vector1 = createVector(terms1, totalUniqueTerms);

		double[] vector2 = createVector(terms2, totalUniqueTerms);

		double docProduct = 0.0;

		for (int i = 0; i < totalUniqueTerms.size(); i++)
		{
			docProduct += vector1[i] * vector2[i];
		}

		double euclideanNorm = euclideanNorms(vector1) * euclideanNorms(vector2);

		docProduct = docProduct / euclideanNorm;

		return (float) docProduct;
	}

	private double euclideanNorms(double[] vector)
	{
		double sum = DoubleStream.of(vector).map(f -> Math.pow(f, 2.0)).sum();
		return Math.sqrt(sum);
	}

	private double[] createVector(List<String> terms, List<String> totalUniqueTerms)
	{
		double[] vector = new double[totalUniqueTerms.size()];
		for (String term : terms)
		{
			int indexOf = totalUniqueTerms.indexOf(term);
			vector[indexOf] += 1 * termFrequencyService.getTermFrequency(term);
		}
		return vector;
	}
}
