package org.molgenis.omx.biobankconnect.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.io.excel.ExcelReader;
import org.molgenis.io.excel.ExcelSheetReader;
import org.molgenis.io.excel.ExcelSheetWriter;
import org.molgenis.io.excel.ExcelWriter;
import org.molgenis.io.excel.ExcelWriter.FileFormat;
import org.molgenis.util.tuple.KeyValueTuple;
import org.molgenis.util.tuple.Tuple;

public class PerformanceComparison
{
	private static List<String> biobankNames = Arrays.asList("finrisk", "ncds", "prevend", "hunt", "kora", "micros");

	public static Map<String, Map<String, List<String>>> collectRankInfo(File rankInfoFile) throws IOException
	{
		Map<String, Map<String, List<String>>> rankInfoMap = new HashMap<String, Map<String, List<String>>>();

		ExcelReader excelReader = null;
		try
		{
			excelReader = new ExcelReader(rankInfoFile);
			ExcelSheetReader excelSheetReader = excelReader.getSheet(0);
			List<String> colNames = new ArrayList<String>();
			Iterator<String> colNamesIterator = excelSheetReader.colNamesIterator();
			while (colNamesIterator.hasNext())
			{
				colNames.add(colNamesIterator.next());
			}

			Iterator<Tuple> iterator = excelSheetReader.iterator();
			while (iterator.hasNext())
			{
				Tuple row = iterator.next();
				String variableName = row.get("variable").toString();
				for (String biobankName : colNames)
				{
					biobankName = biobankName.toLowerCase().trim();
					if (!biobankName.equalsIgnoreCase("variable"))
					{
						if (!rankInfoMap.containsKey(biobankName))
						{
							rankInfoMap.put(biobankName, new HashMap<String, List<String>>());
						}

						Object object = row.get(biobankName);
						if (object != null && !StringUtils.isEmpty(object.toString()))
						{
							String manualMappings = object.toString();
							rankInfoMap.get(biobankName).put(variableName, Arrays.asList(manualMappings.split(",")));
						}
						else
						{
							rankInfoMap.get(biobankName).put(variableName, Collections.<String> emptyList());
						}
					}
				}
			}
			excelSheetReader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (excelReader != null) excelReader.close();
		}

		return rankInfoMap;
	}

	private static Map<String, List<FeatureEntity>> collectFeatureInfo(File allFeaturesFile) throws IOException
	{
		Map<String, List<FeatureEntity>> featureInfoMap = new HashMap<String, List<FeatureEntity>>();
		ExcelReader excelReader = null;
		try
		{
			excelReader = new ExcelReader(allFeaturesFile);
			ExcelSheetReader excelSheetReader = excelReader.getSheet(0);
			List<String> colNames = new ArrayList<String>();
			Iterator<String> colNamesIterator = excelSheetReader.colNamesIterator();
			while (colNamesIterator.hasNext())
			{
				colNames.add(colNamesIterator.next());
			}

			Iterator<Tuple> iterator = excelSheetReader.iterator();

			while (iterator.hasNext())
			{
				Tuple row = iterator.next();
				String identifier = row.get("Identifier").toString();
				FeatureEntity feature = new FeatureEntity();
				feature.setId(Integer.parseInt(row.getString("id")));
				feature.setName(row.getString("Name"));
				String description = row.getString("description");
				Set<String> bagOfWords = new HashSet<String>(Arrays.asList(description.replaceAll("\\W", " ")
						.toLowerCase().trim().split(" ")));
				bagOfWords.removeAll(NGramMatchingModel.STOPWORDSLIST);
				feature.setDescription(StringUtils.join(bagOfWords, " "));
				for (String biobankName : biobankNames)
				{
					if (!featureInfoMap.containsKey(biobankName))
					{
						featureInfoMap.put(biobankName, new ArrayList<FeatureEntity>());
					}

					if (identifier.toLowerCase().startsWith(biobankName))
					{
						featureInfoMap.get(biobankName).add(feature);
						break;
					}
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (excelReader != null) excelReader.close();
		}

		return featureInfoMap;
	}

	private static Map<String, Map<String, List<Integer>>> compare(
			Map<String, Map<String, List<String>>> manualMappingInfoMap, Map<String, List<FeatureEntity>> featureInfoMap)
	{
		Map<String, Map<String, List<Integer>>> evaluation = new HashMap<String, Map<String, List<Integer>>>();
		for (Entry<String, Map<String, List<String>>> entrySet : manualMappingInfoMap.entrySet())
		{
			String biobankName = entrySet.getKey();

			Map<String, List<String>> value = entrySet.getValue();
			evaluation.put(biobankName, new HashMap<String, List<Integer>>());
			for (String variableName : value.keySet())
			{
				evaluation.get(biobankName).put(variableName, new ArrayList<Integer>());
				List<String> manualMappings = value.get(variableName);

				List<HitEntity> relevantElements = getRelevantElements(variableName, biobankName, featureInfoMap);
				for (String mapping : manualMappings)
				{
					boolean isFound = false;
					for (int i = 0; i < relevantElements.size(); i++)
					{
						HitEntity entity = relevantElements.get(i);
						if (entity.getVariableName().equalsIgnoreCase(mapping))
						{
							evaluation.get(biobankName).get(variableName).add((i + 1));
							isFound = true;
						}
					}
					if (!isFound)
					{
						evaluation.get(biobankName).get(variableName).add(featureInfoMap.get(biobankName).size() / 2);
					}
				}
			}
		}
		return evaluation;
	}

	private static List<HitEntity> getRelevantElements(String variableName, String biobankName,
			Map<String, List<FeatureEntity>> featureInfoMap)
	{
		List<HitEntity> relevantElements = new ArrayList<HitEntity>();
		String processDescription = variableName.toLowerCase().trim().replaceAll("\\W", " ");
		for (String word : processDescription.split(" "))
		{
			for (FeatureEntity featureEntity : featureInfoMap.get(biobankName.toLowerCase()))
			{
				if (featureEntity.getDescription().toLowerCase().contains(word))
				{
					HitEntity hitEntity = new HitEntity(featureEntity.getName(), 0);
					if (!relevantElements.contains(hitEntity))
					{
						double score = NGramMatchingModel.stringMatching(variableName, featureEntity.getDescription(),
								true);
						hitEntity.setScore((int) score);
						relevantElements.add(hitEntity);
					}
				}
			}
		}

		try
		{
			Collections.sort(relevantElements);

		}
		catch (Exception e)
		{
			System.out.println("biobank : " + biobankName + " ---- variable : " + variableName);
			// if (variableName.equals("Number of People in the Household") &&
			// biobankName.equals("prevend"))
			// {
			// for (HitEntity entity : relevantElements)
			// {
			// System.out.println(entity.getVariableName() + " ---- " +
			// entity.getScore());
			// }
			// }
		}
		return relevantElements;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException
	{
		if (args.length == 2)
		{
			File rankInfoFile = new File(args[0]);
			File allFeaturesFile = new File(args[1]);

			if (rankInfoFile.exists() && allFeaturesFile.exists())
			{
				ExcelWriter excelWriter = new ExcelWriter(new File("/Users/chaopang/Desktop/results"), FileFormat.XLSX);
				ExcelSheetWriter excelSheetWriter = (ExcelSheetWriter) excelWriter.createTupleWriter("results"
						.split("\\.")[0]);
				List<String> headers = new ArrayList<String>();
				headers.add("variable");
				headers.addAll(biobankNames);
				excelSheetWriter.writeColNames(headers);
				// Collect rank information per biobank and per variable
				Map<String, Map<String, List<Integer>>> evaluation = compare(collectRankInfo(rankInfoFile),
						collectFeatureInfo(allFeaturesFile));

				Map<String, KeyValueTuple> map = new HashMap<String, KeyValueTuple>();
				for (Entry<String, Map<String, List<Integer>>> entry : evaluation.entrySet())
				{
					String biobankName = entry.getKey();
					Map<String, List<Integer>> mappingResults = entry.getValue();
					for (String variableName : mappingResults.keySet())
					{
						if (!map.containsKey(variableName))
						{
							map.put(variableName, new KeyValueTuple());
						}
						List<Integer> ranks = mappingResults.get(variableName);
						map.get(variableName).set(biobankName, StringUtils.join(ranks, ","));
					}
				}

				for (Entry<String, KeyValueTuple> entry : map.entrySet())
				{
					entry.getValue().set("variable", entry.getKey());
					excelSheetWriter.write(entry.getValue());
				}
				excelSheetWriter.close();
				excelWriter.close();
			}
		}
	}
}
