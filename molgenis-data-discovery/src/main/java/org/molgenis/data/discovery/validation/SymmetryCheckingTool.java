package org.molgenis.data.discovery.validation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.molgenis.data.MolgenisInvalidFormatException;
import org.molgenis.data.Repository;
import org.molgenis.data.csv.CsvRepository;
import org.molgenis.data.semanticsearch.semantic.Hit;

public class SymmetryCheckingTool
{
	public static void main(String args[]) throws IOException, MolgenisInvalidFormatException
	{
		if (args.length == 2)
		{
			File forwardMatchFile = new File(args[0]);
			File reversedMatchFile = new File(args[1]);
			Repository forwardGeneratedMatchRepository = new CsvRepository(forwardMatchFile, Arrays.asList(), ',');
			Repository backwordGeneratedMatchRepository = new CsvRepository(reversedMatchFile, Arrays.asList(), ',');

			SymmetryCheckingTool tool = new SymmetryCheckingTool();
			tool.compare(forwardGeneratedMatchRepository, backwordGeneratedMatchRepository);
		}
	}

	private void compare(Repository forwardGeneratedMatchRepository, Repository backwordGeneratedMatchRepository)
	{
		Map<String, List<Hit<String>>> forwardGeneratedMatches = BiobankUniverseEvaluationTool
				.collectGeneratedMatches(forwardGeneratedMatchRepository, false);

		Map<String, List<Hit<String>>> backwardGeneratedMatches = BiobankUniverseEvaluationTool
				.collectGeneratedMatches(backwordGeneratedMatchRepository, true);

		Set<String> targetAttributes = new HashSet<>();
		targetAttributes.addAll(forwardGeneratedMatches.keySet());
		targetAttributes.addAll(backwardGeneratedMatches.keySet());

		for (String targetAttribute : targetAttributes)
		{
			List<String> forwardCandidateMatches = forwardGeneratedMatches.containsKey(targetAttribute)
					? forwardGeneratedMatches.get(targetAttribute).stream().map(Hit::getResult)
							.collect(Collectors.toList())
					: Collections.emptyList();

			List<String> backwardCandidateMatches = backwardGeneratedMatches.containsKey(targetAttribute)
					? backwardGeneratedMatches.get(targetAttribute).stream().map(Hit::getResult)
							.collect(Collectors.toList())
					: Collections.emptyList();

			List<String> overlapCandidateMatches = forwardCandidateMatches.stream()
					.filter(backwardCandidateMatches::contains).collect(Collectors.toList());

			forwardCandidateMatches.removeAll(overlapCandidateMatches);
			backwardCandidateMatches.removeAll(overlapCandidateMatches);

			if (!forwardCandidateMatches.isEmpty() || !backwardCandidateMatches.isEmpty())
			{
				System.out.format("The target: %s; The forward difference: %s; The backward difference: %s%n",
						targetAttribute, forwardCandidateMatches, backwardCandidateMatches);
			}
		}
	}
}
