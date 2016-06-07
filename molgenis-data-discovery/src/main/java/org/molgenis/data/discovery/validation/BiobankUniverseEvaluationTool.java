package org.molgenis.data.discovery.validation;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.molgenis.data.Entity;
import org.molgenis.data.MolgenisInvalidFormatException;
import org.molgenis.data.Repository;
import org.molgenis.data.csv.CsvRepository;
import org.molgenis.data.excel.ExcelRepositoryCollection;
import org.molgenis.data.semanticsearch.semantic.Hit;

import com.google.common.collect.LinkedHashMultimap;
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
				Repository generatedMatchRepository = new CsvRepository(generatedMatchFile, Arrays.asList(), ',');

				BiobankUniverseEvaluationTool tool = new BiobankUniverseEvaluationTool();

				tool.compare(manualMatchRepository, generatedMatchRepository);
			}
		}
	}

	private void compare(Repository manualMatchRepository, Repository generatedMatchRepository)
	{
		Multimap<String, String> manualMatcheCollection = collectManualMatches(manualMatchRepository);
		Map<String, List<Hit<String>>> generatedCandidateMatchCollection = collectGeneratedMatches(
				generatedMatchRepository);

		List<Match> matchResult = new ArrayList<>();
		for (Entry<String, Collection<String>> entrySet : manualMatcheCollection.asMap().entrySet())
		{
			String target = entrySet.getKey();
			Collection<String> manualMatches = entrySet.getValue();
			List<Hit<String>> candidateMatches = generatedCandidateMatchCollection.containsKey(target)
					? generatedCandidateMatchCollection.get(target).stream().sorted().collect(toList()) : emptyList();
			List<Match> collect = manualMatches.stream()
					.map(manualMatch -> new Match(target, manualMatch, computeRank(manualMatch, candidateMatches)))
					.collect(Collectors.toList());
			matchResult.addAll(collect);
		}

		for (int i = 1; i < 21; i++)
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
			System.out.format("%.3f of matches have been found within rank %d%n", (double) foundMatch / totalMatch, i);
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

			generatedCandidateMatches.get(target).add(Hit.create(source, (float) ngramScore.doubleValue() / 100000));
		}
		return generatedCandidateMatches;
	}

	private Multimap<String, String> collectManualMatches(Repository manualMatchRepository)
	{
		Multimap<String, String> manualMatches = LinkedHashMultimap.create();

		for (Entity entity : manualMatchRepository)
		{
			String attributeName = entity.getString("name");
			String algorithm = entity.getString("algorithm");

			if (!algorithm.equals("null"))
			{
				manualMatches.putAll(attributeName, extractAttributeNames(algorithm));
			}
		}
		return manualMatches;
	}

	Set<String> extractAttributeNames(String algorithm)
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
