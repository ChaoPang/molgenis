package org.molgenis.data.discovery.scoring;

import static java.util.stream.Collectors.toList;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.splitIntoTerms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.ontology.utils.NGramDistanceAlgorithm;
import org.molgenis.ontology.utils.Stemmer;

public class VectorSpaceScoringModel implements ScoringModel
{
	private final Map<String, Float> stemmedIDF;
	private final BiobankUniverseRepository biobankUniverseRepository;

	public VectorSpaceScoringModel(BiobankUniverseRepository biobankUniverseRepository)
	{
		this.stemmedIDF = new HashMap<>();
		this.biobankUniverseRepository = Objects.requireNonNull(biobankUniverseRepository);
	}

	@Override
	public float score(String document1, String document2, boolean strictMatch)
	{
		if (stemmedIDF.isEmpty())
		{
			stemmedIDF.putAll(biobankUniverseRepository.getAttributeTermIDF());
		}

		boolean removeStopWords = true;

		List<String> terms1 = createTerms(document1, removeStopWords);

		List<String> terms2 = createTerms(document2, removeStopWords);

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

	private List<String> createTerms(String document, boolean removeStopWords)
	{
		List<String> terms = splitIntoTerms(document);

		if (removeStopWords) terms.removeAll(NGramDistanceAlgorithm.STOPWORDSLIST);

		return terms.stream().map(Stemmer::stem).collect(Collectors.toList());
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
			vector[indexOf] += 1 * getTermFrequency(term);
		}
		return vector;
	}

	private double getTermFrequency(String stemmedWord)
	{
		return stemmedIDF.containsKey(stemmedWord) ? stemmedIDF.get(stemmedWord) : 0.0f;
	}
}
