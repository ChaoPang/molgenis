package org.molgenis.data.discovery.validation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.molgenis.data.MolgenisInvalidFormatException;
import org.molgenis.data.Repository;
import org.molgenis.data.csv.CsvRepository;
import org.molgenis.data.semanticsearch.semantic.Hit;

public class CalculateSimilarity
{
	public static void main(String args[]) throws IOException, MolgenisInvalidFormatException
	{
		if (args.length == 1)
		{
			File MatchFile = new File(args[0]);
			Repository generatedMatchesRepository = new CsvRepository(MatchFile, Arrays.asList(), ',');

			CalculateSimilarity tool = new CalculateSimilarity();
			tool.compute(generatedMatchesRepository);
		}
	}

	private void compute(Repository generatedMatchesRepository)
	{
		Map<String, List<Hit<String>>> collectGeneratedMatches = BiobankUniverseEvaluationTool
				.collectGeneratedMatches(generatedMatchesRepository, false);

		float similarity = 0.0f;

		for (Entry<String, List<Hit<String>>> entry : collectGeneratedMatches.entrySet())
		{
			List<Hit<String>> hits = entry.getValue();
			Collections.sort(hits, Comparator.reverseOrder());
			if (hits.size() > 0)
			{
				similarity += hits.get(0).getScore();
			}
		}

		similarity = similarity / collectGeneratedMatches.size();

		System.out.format("The average similarity with the best candidate match is %.2f%n", similarity);

		double totalSimilarity = collectGeneratedMatches.values().stream().flatMap(hits -> hits.stream().limit(10))
				.mapToDouble(hit -> hit.getScore()).sum();

		System.out.format("The average similarity with the total candidate match is %.2f%n", totalSimilarity);
	}
}
