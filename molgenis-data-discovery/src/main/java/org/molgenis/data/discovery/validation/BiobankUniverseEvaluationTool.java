package org.molgenis.data.discovery.validation;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.Entity;
import org.molgenis.data.MolgenisInvalidFormatException;
import org.molgenis.data.Repository;
import org.molgenis.data.csv.CsvRepository;
import org.molgenis.data.excel.ExcelRepositoryCollection;
import org.molgenis.data.semanticsearch.semantic.Hit;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class BiobankUniverseEvaluationTool
{
	private final static Pattern SINGLE_ATTRIBUTE_NAME_PATTERN = Pattern.compile("\\$\\('([a-zA-Z0-9_]+)'\\)");
	private final static Pattern ATTRIBUTE_GROUP_PATTERN = Pattern.compile("\\[('.+')\\]");
	private final static Pattern ATTRIBUTE_NAME_IN_GROUP_PATTERN = Pattern.compile("'([a-zA-Z0-9_&&[^,]]+)'");

	public static void main(String args[]) throws IOException, MolgenisInvalidFormatException
	{
		if (args.length == 2)
		{
			File manualMatchFile = new File(args[0]);
			File generatedMatchFile = new File(args[1]);

			if (generatedMatchFile.exists() && manualMatchFile.exists())
			{
				ExcelRepositoryCollection excelRepositoryCollection = new ExcelRepositoryCollection(manualMatchFile);
				Repository manualMatchRepository = excelRepositoryCollection.getSheet(0);
				// Repository manualMatchRepository = new CsvRepository(manualMatchFile, Arrays.asList(), ',');
				Repository generatedMatchRepository = new CsvRepository(generatedMatchFile, Arrays.asList(), ',');

				BiobankUniverseEvaluationTool tool = new BiobankUniverseEvaluationTool();

				tool.compare(manualMatchRepository, generatedMatchRepository);
			}
		}
	}

	private void compare(Repository manualMatchRepository, Repository generatedMatchRepository)
	{
		// Multimap<String, String> manualMatcheCollection = collectManualMatches(manualMatchRepository);
		Multimap<String, String> manualMatcheCollection = collectRelevantMatches(manualMatchRepository);
		Map<String, List<Hit<String>>> generatedCandidateMatchCollection = collectGeneratedMatches(
				generatedMatchRepository);

		generateRscriptInput(manualMatcheCollection, generatedCandidateMatchCollection);

		calculatePrecisionRecallForRanks(manualMatcheCollection, generatedCandidateMatchCollection);

		calculatePrecisionRecallForThresholds(manualMatcheCollection, generatedCandidateMatchCollection);
	}

	private List<Hit<String>> postProcessMatches(List<Hit<String>> candidateMatches)
	{
		if (candidateMatches.isEmpty()) return Collections.emptyList();

		List<Float> similarityTangentValues = new ArrayList<>();
		Hit<String> previousHit = candidateMatches.get(0);
		for (Hit<String> hit : candidateMatches.stream().skip(1).collect(Collectors.toList()))
		{
			float drop = previousHit.getScore() - hit.getScore();
			similarityTangentValues.add(drop);
			previousHit = hit;
		}

		List<Float> qualifiedTangentValues = similarityTangentValues.stream().filter(drop -> drop >= 0.1f)
				.collect(Collectors.toList());

		if (qualifiedTangentValues.size() > 0)
		{
			Float lastQualifedDropValue = qualifiedTangentValues.get(qualifiedTangentValues.size() - 1);
			int indexOf = similarityTangentValues.indexOf(lastQualifedDropValue);
			// 1,2,3,4,5,6,7
			for (int i = indexOf; i < similarityTangentValues.size() - 2; i++)
			{
				if (similarityTangentValues.get(i) <= 0.01 && similarityTangentValues.get(i + 1) <= 0.01
						&& similarityTangentValues.get(i + 2) <= 0.01)
				{
					return candidateMatches.subList(0, i);
				}
			}
		}

		return candidateMatches;
	}

	private void calculatePrecisionRecallForThresholds(Multimap<String, String> manualMatcheCollection,
			Map<String, List<Hit<String>>> generatedCandidateMatchCollection)
	{
		for (int i = 1; i < 101; i++)
		{
			float threshold = (float) i / 100;
			Map<String, List<Hit<String>>> tempCandidateMatchCollection = generatedCandidateMatchCollection.entrySet()
					.stream().collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().stream()
							.filter(hit -> hit.getScore() >= threshold).collect(toList())));

			int foundMatches = 0;
			int retrievedMatches = 0;
			int totalMatch = (int) manualMatcheCollection.values().stream().count();
			for (Entry<String, List<Hit<String>>> generatedMatchesEntry : tempCandidateMatchCollection.entrySet())
			{
				String target = generatedMatchesEntry.getKey();

				List<Hit<String>> generatedMatches = generatedMatchesEntry.getValue().stream().sorted()
						.collect(toList());

				List<String> manualMatches = manualMatcheCollection.containsKey(target)
						? Lists.newArrayList(manualMatcheCollection.get(target)) : emptyList();

				foundMatches += generatedMatches.stream().filter(hit -> manualMatches.contains(hit.getResult()))
						.count();
				retrievedMatches += generatedMatches.size();
			}

			double recall = (double) foundMatches / totalMatch;
			double precision = (double) foundMatches / retrievedMatches;
			double falseDiscoveryRate = (double) (retrievedMatches - foundMatches) / retrievedMatches;
			double fMeasure = 2 * recall * precision / (recall + precision);
			System.out.format(
					"Recall: %.3f; Precision: %.3f; FDR: %.3f; F-measure: %.3f; Threshold %.3f; Total matches: %d%n",
					recall, precision, falseDiscoveryRate, fMeasure, threshold, totalMatch);
		}
	}

	private void calculatePrecisionRecallForRanks(Multimap<String, String> manualMatcheCollection,
			Map<String, List<Hit<String>>> generatedCandidateMatchCollection)
	{
		List<Match> matchResult = new ArrayList<>();
		for (Entry<String, Collection<String>> entrySet : manualMatcheCollection.asMap().entrySet())
		{
			String target = entrySet.getKey();
			Collection<String> manualMatches = entrySet.getValue();
			List<Hit<String>> candidateMatches = generatedCandidateMatchCollection.containsKey(target)
					? generatedCandidateMatchCollection.get(target).stream().sorted(Comparator.reverseOrder())
							.collect(toList())
					: emptyList();
			List<Match> collect = manualMatches.stream()
					.map(manualMatch -> new Match(target, manualMatch, computeRank(manualMatch, candidateMatches)))
					.collect(Collectors.toList());
			matchResult.addAll(collect);
		}

		for (int i = 1; i < 51; i++)
		{
			int totalMatch = matchResult.size();
			int foundMatch = 0;
			for (Match match : matchResult)
			{
				if (match.getRank() != null && match.getRank() <= i)
				{
					foundMatch++;
				}
			}

			final int rank = i;
			int retrievedMatches = generatedCandidateMatchCollection.values().stream()
					.mapToInt(candidates -> candidates.size() > rank ? rank : candidates.size()).sum();

			double recall = (double) foundMatch / totalMatch;
			double precision = (double) foundMatch / retrievedMatches;
			System.out.format("Recall: %.3f; Precision: %.3f; Rank %d; Total matches: %d%n", recall, precision, i,
					totalMatch);
		}

		for (Match match : matchResult)
		{
			if (match.getRank() == null)
			{
				System.out.format("Target: %s; Manual matches: %s%n", match.getTarget(), match.getSource());
			}
		}
	}

	private void generateRscriptInput(Multimap<String, String> manualMatcheCollection,
			Map<String, List<Hit<String>>> generatedCandidateMatchCollection)
	{
		for (Entry<String, List<Hit<String>>> entry : generatedCandidateMatchCollection.entrySet())
		{
			String targetAttributeName = entry.getKey();
			Collection<String> manualMatch = manualMatcheCollection.containsKey(targetAttributeName)
					? manualMatcheCollection.get(targetAttributeName) : emptyList();
			List<Hit<String>> candidateMatches = entry.getValue();
			for (Hit<String> candidateMatch : candidateMatches)
			{
				String candidateMatchName = candidateMatch.getResult();
				int group = manualMatch.contains(candidateMatchName) ? 1 : 2;
				int rank = candidateMatches.indexOf(candidateMatch) + 1;
				System.out.format("%s,%s,%.3f,%d,%d%n", targetAttributeName, candidateMatchName,
						candidateMatch.getScore(), rank, group);
			}
		}
	}

	private Integer computeRank(String manualMatch, List<Hit<String>> candidateMatches)
	{
		Hit<String> orElse = candidateMatches.stream().filter(hit -> hit.getResult().equals(manualMatch)).findFirst()
				.orElse(null);
		return orElse == null ? null : (candidateMatches.indexOf(orElse) + 1);
	}

	private Map<String, List<Hit<String>>> collectGeneratedMatches(Repository generatedMatchRepository)
	{
		Map<String, List<Hit<String>>> generatedCandidateMatches = new LinkedHashMap<>();
		for (Entity entity : generatedMatchRepository)
		{
			String target = entity.getString("target");
			String source = entity.getString("source");
			Double ngramScore = entity.getDouble("ngramScore");

			if (!generatedCandidateMatches.containsKey(target))
			{
				generatedCandidateMatches.put(target, new ArrayList<>());
			}

			generatedCandidateMatches.get(target).add(Hit.create(source, (float) ngramScore.doubleValue()));
		}
		return generatedCandidateMatches;
	}

	private Multimap<String, String> collectRelevantMatches(Repository manualMatchRepository)
	{
		Multimap<String, String> manualMatches = LinkedHashMultimap.create();

		for (Entity entity : manualMatchRepository)
		{
			String attributeName = entity.getString("name");
			String matches = entity.getString("matches");

			if (StringUtils.isNotBlank(matches))
			{
				Set<String> matchedAttributeNames = Stream.of(matches.split(",")).map(StringUtils::trim)
						.collect(Collectors.toSet());
				manualMatches.putAll(attributeName, matchedAttributeNames);
				System.out
						.println("\"" + attributeName + "\",\"" + StringUtils.join(matchedAttributeNames, ',') + "\"");
			}
			else
			{
				System.out.println("\"" + attributeName + "\",\"\"");
			}
		}
		return manualMatches;
	}
	//
	// private Multimap<String, String> collectManualMatches(Repository manualMatchRepository)
	// {
	// Multimap<String, String> manualMatches = LinkedHashMultimap.create();
	//
	// for (Entity entity : manualMatchRepository)
	// {
	// String attributeName = entity.getString("name");
	// String algorithm = entity.getString("algorithm");
	//
	// if (!algorithm.equals("null"))
	// {
	// Set<String> matchedAttributeNames = extractAttributeNames(algorithm);
	// manualMatches.putAll(attributeName, matchedAttributeNames);
	// System.out
	// .println("\"" + attributeName + "\",\"" + StringUtils.join(matchedAttributeNames, ',') + "\"");
	// }
	// else
	// {
	// System.out.println("\"" + attributeName + "\",\"\"");
	// }
	// }
	// return manualMatches;
	// }

	private Set<String> extractAttributeNames(String algorithm)
	{
		Set<String> attributeNames = new LinkedHashSet<>();
		Matcher matcher = SINGLE_ATTRIBUTE_NAME_PATTERN.matcher(algorithm);
		while (matcher.find())
		{
			String attributeName = matcher.group(1);
			attributeNames.add(attributeName);
		}

		matcher = ATTRIBUTE_GROUP_PATTERN.matcher(algorithm);

		if (matcher.find())
		{
			String attributeGroup = matcher.group();
			Matcher matcher2 = ATTRIBUTE_NAME_IN_GROUP_PATTERN.matcher(attributeGroup);
			while (matcher2.find())
			{
				String attributName = matcher2.group(1);
				attributeNames.add(attributName);
			}
		}

		return attributeNames;
	}
}
