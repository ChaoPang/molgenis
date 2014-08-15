package org.molgenis.omx.biobankconnect.analysis;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.molgenis.io.csv.CsvReader;
import org.molgenis.io.excel.ExcelReader;
import org.molgenis.io.excel.ExcelSheetReader;
import org.molgenis.io.excel.ExcelSheetWriter;
import org.molgenis.io.excel.ExcelWriter;
import org.molgenis.io.excel.ExcelWriter.FileFormat;
import org.molgenis.omx.biobankconnect.utils.HitEntity;
import org.molgenis.util.tuple.KeyValueTuple;
import org.molgenis.util.tuple.Tuple;

import com.google.common.collect.Lists;

public class CalculateROC
{
	private final static List<Integer> excludedFeatures = Arrays.asList(86, 102, 104, 112, 115, 116, 117, 124);
	private final static Map<String, Integer> biobankTotalNumber = new HashMap<String, Integer>();
	{
		biobankTotalNumber.put("ncds", 512);
		biobankTotalNumber.put("hunt", 353);
		biobankTotalNumber.put("finrisk", 223);
		biobankTotalNumber.put("kora", 75);
		biobankTotalNumber.put("micros", 119);
		biobankTotalNumber.put("total", 1282);
	}

	private final static char CSV_SEPERATOR = ',';

	// A map to store all variables per biobank
	private Map<String, List<String>> biobankAllDocuments = new HashMap<String, List<String>>();

	// A map to store id/name pair for all biobank data elements
	private Map<String, String> featureMapping = new HashMap<String, String>();

	// A map to store biobank/corresponding retrieved documents for all 32 core
	// variables
	private final Map<String, Map<String, RetrievedHitEntity>> biobankRetrievedDocuments = new HashMap<String, Map<String, RetrievedHitEntity>>();

	// A map to store biobank/corresponding relevant documents for all 32 core
	// variables
	private final Map<String, StoreRelevantDocuments> biobankRelevantDocuments = new HashMap<String, StoreRelevantDocuments>();

	private final List<String> allCoreVariables = new ArrayList<String>();

	private void calculateROC(ExcelWriter excelWriter) throws IOException
	{
		List<String> outputColHeaders = new ArrayList<String>();
		outputColHeaders.add("Ranks");
		outputColHeaders.addAll(allCoreVariables);
		outputColHeaders.addAll(Arrays.asList("totalElements", "totalPositives", "totalNegatives", "totalTruePositive",
				"totalFalsePositive", "TPR", "FPR", "Precision", "Retrieved"));
		Set<String> biobankNames = biobankRetrievedDocuments.keySet();

		Map<Integer, Integer> totalTruePositves = new HashMap<Integer, Integer>();
		Map<Integer, Integer> totalRetrieved = new HashMap<Integer, Integer>();

		for (String biobankName : biobankNames)
		{
			int size = biobankTotalNumber.get(biobankName);
			Map<String, RetrievedHitEntity> retrievedDocuments = biobankRetrievedDocuments.get(biobankName);
			StoreRelevantDocuments relevantDocuments = biobankRelevantDocuments.get(biobankName);

			System.out.println("The biobank is : " + biobankName);
			System.out.println("total\tpositives\tnegatives\ttruePositive\tfalsePositive");
			// go through all possible ranks from 1st rank to last rank, which
			// is the size of biobank
			ExcelSheetWriter excelSheetWriter = (ExcelSheetWriter) excelWriter.createTupleWriter(biobankName);
			excelSheetWriter.writeColNames(outputColHeaders);

			int totalPositives = 0;
			int totalNegatives = 0;
			KeyValueTuple firstRow = new KeyValueTuple();
			firstRow.set("Ranks", "Total relevant");
			for (String coreVariable : allCoreVariables)
			{
				int totalNumOfRelevantDocuments = relevantDocuments.getRelevantDocuments(coreVariable).size();
				firstRow.set(coreVariable, totalNumOfRelevantDocuments);
				totalPositives += totalNumOfRelevantDocuments;
				totalNegatives += (biobankTotalNumber.get(biobankName) - totalNumOfRelevantDocuments);
			}
			firstRow.set("totalElements", biobankTotalNumber.get(biobankName));
			firstRow.set("totalPositives", totalPositives);
			firstRow.set("totalNegatives", totalNegatives);
			excelSheetWriter.write(firstRow);

			List<KeyValueTuple> listOfTuples = new ArrayList<KeyValueTuple>();
			List<Integer> recordingTP = new ArrayList<Integer>();
			for (int i = 0; i <= size; i++)
			{
				KeyValueTuple row = calculateROCperBiobankperRank(biobankName, relevantDocuments, retrievedDocuments, i);
				listOfTuples.add(row);
				recordingTP.add(0, row.getInt("totalTruePositive"));

				if (!totalTruePositves.containsKey(i))
				{
					totalTruePositves.put(i, 0);
				}

				totalTruePositves.put(i, totalTruePositves.get(i) + row.getInt("totalTruePositive"));

				if (!totalRetrieved.containsKey(i))
				{
					totalRetrieved.put(i, 0);
				}

				totalRetrieved.put(i, totalRetrieved.get(i) + row.getInt("Retrieved"));
			}

			// Integer previousTP = recordingTP.get(0);
			// for (int i = 1; i < recordingTP.size(); i++)
			// {
			// if (previousTP == recordingTP.get(i))
			// {
			// int indexToRemove = (recordingTP.size() - i);
			// listOfTuples.remove(indexToRemove);
			// }
			// else
			// {
			// break;
			// }
			// }
			int limit = 0;
			for (String variableName : allCoreVariables)
			{
				List<HitEntity> retrievedHits = retrievedDocuments.get(variableName) != null ? retrievedDocuments.get(
						variableName).getHits() : Collections.<HitEntity> emptyList();
				if (limit < retrievedHits.size())
				{
					limit = retrievedHits.size();
				}
			}

			for (int i = 0; i < limit; i++)
			{
				excelSheetWriter.write(listOfTuples.get(i));
			}

			// for (KeyValueTuple keyValueTuple : listOfTuples)
			// {
			// excelSheetWriter.write(keyValueTuple);
			// }

			System.out.println();
			System.out.println();

			excelSheetWriter.close();
		}

		System.out.println("Ranks\tRecall\tPrecision");
		DecimalFormat formatter = new DecimalFormat("#.##");
		for (Entry<Integer, Integer> entry : totalTruePositves.entrySet())
		{
			Integer rank = entry.getKey();
			Integer totalTruePositives = entry.getValue();
			Integer totalRetrievedDocuments = totalRetrieved.get(rank);
			System.out.println(rank + "\t" + formatter.format((double) totalTruePositives / 420) + "\t"
					+ formatter.format((double) totalTruePositives / totalRetrievedDocuments));
		}

	}

	private List<String> rearrangeOrder(List<HitEntity> retrievedHits, List<String> allDataElements)
	{
		List<String> retrievedDataElements = new ArrayList<String>();
		for (HitEntity entity : retrievedHits)
		{
			retrievedDataElements.add(entity.getVariableName());
			allDataElements.remove(entity.getVariableName());
		}
		retrievedDataElements.addAll(allDataElements);
		return retrievedDataElements;
	}

	private KeyValueTuple calculateROCperBiobankperRank(String biobankName, StoreRelevantDocuments relevantDocuments,
			Map<String, RetrievedHitEntity> retrievedDocuments, int threshold)
	{
		if (biobankName.equals("hunt"))
		{
			System.out.println();
		}
		KeyValueTuple tuple = new KeyValueTuple();
		tuple.set("Ranks", threshold);
		int size = biobankTotalNumber.get(biobankName.toLowerCase());
		int totalRetrieved = 0;
		int totalPositives = 0;
		int totalNegatives = 0;
		int totalTruePositive = 0;
		int totalFalsePositive = 0;
		for (String variableName : allCoreVariables)
		{
			List<HitEntity> retrievedHits = retrievedDocuments.get(variableName) != null ? retrievedDocuments.get(
					variableName).getHits() : Collections.<HitEntity> emptyList();

			List<String> orderDataElements = rearrangeOrder(retrievedHits,
					new ArrayList<String>(biobankAllDocuments.get(biobankName)));
			List<String> relevantHits = new ArrayList<String>(relevantDocuments.getRelevantDocuments(variableName));
			int positivesForOneVariable = relevantHits.size();
			int negativesForOneVariable = size - positivesForOneVariable;
			int truePositiveForOneVariable = 0;
			int falsePositiveForOneVariable = 0;
			int count = 0;
			for (String dataElement : orderDataElements)
			{
				if (count >= threshold) break;
				// If the retrieved hit is relevant, truePositive is
				// increased
				// by one otherwise the falsePositive is increased by one
				if (count < retrievedHits.size())
				{
					if (relevantHits.contains(dataElement))
					{
						relevantHits.remove(dataElement);
						truePositiveForOneVariable++;
					}
					else
					{
						falsePositiveForOneVariable++;
					}
				}
				count++;
			}

			totalPositives += positivesForOneVariable;
			totalNegatives += negativesForOneVariable;

			totalTruePositive += truePositiveForOneVariable;
			totalFalsePositive += falsePositiveForOneVariable;

			totalRetrieved += threshold < retrievedHits.size() ? threshold : retrievedHits.size();

			if (threshold == retrievedHits.size())
			{
				tuple.set(variableName, truePositiveForOneVariable + "|" + falsePositiveForOneVariable + "|end");
			}
			else
			{
				tuple.set(variableName, truePositiveForOneVariable + "|" + falsePositiveForOneVariable);
			}

			if (threshold == 20 && relevantHits.size() > 0)
			{
				System.out.println("threshold : " + threshold + "\tcore variable : " + variableName + "\tmissed hit : "
						+ relevantHits);
			}
		}

		DecimalFormat dataFormat = new DecimalFormat("#.##");

		tuple.set("totalElements", size);
		tuple.set("totalPositives", totalPositives);
		tuple.set("totalNegatives", totalNegatives);
		tuple.set("totalTruePositive", totalTruePositive);
		tuple.set("totalFalsePositive", totalFalsePositive);
		tuple.set("TPR", dataFormat.format((double) totalTruePositive / totalPositives));
		tuple.set("FPR", dataFormat.format((double) totalFalsePositive / totalNegatives));
		tuple.set("Precision", totalRetrieved != 0 ? dataFormat.format((double) totalTruePositive / totalRetrieved) : 0);
		tuple.set("Retrieved", totalRetrieved);

		if (threshold == 1 || threshold == 20) System.out.println(size + "\t" + totalPositives + "\t" + totalNegatives
				+ "\t" + totalTruePositive + "\t" + totalFalsePositive + "\t"
				+ dataFormat.format((double) totalTruePositive / totalPositives) + "\t"
				+ dataFormat.format((double) totalFalsePositive / totalNegatives) + "\t"
				+ dataFormat.format((double) totalTruePositive / totalRetrieved));
		return tuple;
	}

	private void loadRelevantDocuments(String fileName, ExcelReader excelReader) throws IOException
	{
		String biobankName = removeExcelSuffix(fileName).toLowerCase();
		biobankAllDocuments.put(biobankName, new ArrayList<String>());
		ExcelSheetReader excelSheetReader = excelReader.getSheet(0);
		List<String> columnHeaders = Lists.newArrayList(excelSheetReader.colNamesIterator());
		Iterator<Tuple> iterator = excelSheetReader.iterator();
		while (iterator.hasNext())
		{
			Tuple row = iterator.next();
			if (row.isNull("Name")) continue;
			for (String eachHeader : columnHeaders)
			{
				if (eachHeader.equalsIgnoreCase("Name") || eachHeader.equalsIgnoreCase("Identifier")) continue;
				Integer isRevelant = row.getInt(eachHeader);
				String desiredDataElement = extractVariableName(eachHeader);
				String biobankDataElement = extractVariableName(row.getString("Name"));
				if (isRevelant == 1)
				{

					if (!biobankRelevantDocuments.containsKey(biobankName))
					{
						biobankRelevantDocuments.put(biobankName, new StoreRelevantDocuments());
					}
					biobankRelevantDocuments.get(biobankName).addSingleRecord(desiredDataElement, biobankDataElement);
				}
				if (!allCoreVariables.contains(desiredDataElement))
				{
					allCoreVariables.add(desiredDataElement);
				}

				if (!biobankAllDocuments.get(biobankName).contains(biobankDataElement))
				{
					biobankAllDocuments.get(biobankName).add(biobankDataElement);
				}
			}
		}
		Collections.shuffle(biobankAllDocuments.get(biobankName));
		excelSheetReader.close();
	}

	private String removeExcelSuffix(String variableName)
	{
		String biobankNameWithSuffix = variableName.split("\\.")[0];
		return biobankNameWithSuffix.split("_")[0];
	}

	private String extractVariableName(String variableNameDescription)
	{
		return variableNameDescription.split(":")[0].trim();
	}

	private void loadFeatureInformation(File featureFile) throws IOException
	{
		ExcelReader excelReader = null;
		try
		{
			excelReader = new ExcelReader(featureFile);
			ExcelSheetReader excelSheetReader = excelReader.getSheet(0);
			Iterator<Tuple> iterator = excelSheetReader.iterator();
			while (iterator.hasNext())
			{
				Tuple row = iterator.next();
				featureMapping.put(row.getString("id"), row.getString("Name"));
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
	}

	private void loadRetrievedDocuments(String fileName, CsvReader csvReader) throws Exception
	{
		String biobankName = fileName.split("\\.")[0].toLowerCase();
		Map<String, RetrievedHitEntity> listOfRetrievedHits = new HashMap<String, RetrievedHitEntity>();
		// Iterables.toArray(, String.class);
		List<String> colNames = new ArrayList<String>();
		Iterator<String> colNamesIterator = csvReader.colNamesIterator();
		while (colNamesIterator.hasNext())
		{
			colNames.add(colNamesIterator.next());
		}
		Iterator<Tuple> iterator = csvReader.iterator();

		while (iterator.hasNext())
		{
			Tuple eachRow = iterator.next();
			String featureId = eachRow.getString("Features");
			// There are 40 variables in total, however only 32 were selected
			// for the analysis. So we need to exclude the core variables that
			// were not selected for the data analysis
			if (!excludedFeatures.contains(Integer.parseInt(featureId)))
			{
				String mappedFeatureId = eachRow.getString("Mapped features");
				Double score = eachRow.getDouble("store_mapping_score");
				String featureName = featureMapping.get(featureId);
				String mappedFeatureName = featureMapping.get(mappedFeatureId);
				if (featureName == null) throw new Exception("featureId : " + featureId + " is not valid!");
				if (mappedFeatureName == null) throw new Exception("mappedFeatureId : " + mappedFeatureId
						+ " is not valid!");

				// create a listOfHits entity for a new feature
				if (!listOfRetrievedHits.containsKey(featureName))
				{
					listOfRetrievedHits.put(featureName, new RetrievedHitEntity(featureName));
				}
				listOfRetrievedHits.get(featureName).addHit(mappedFeatureName, score);
			}
		}

		biobankRetrievedDocuments.put(biobankName, listOfRetrievedHits);
	}

	public static void main(String args[]) throws Exception
	{
		if (args.length == 4)
		{
			File relevantDocumentFolder = new File(args[0]);
			File featureInfoFile = new File(args[1]);
			File retrievedDocumentFolder = new File(args[2]);

			if (relevantDocumentFolder.exists() && featureInfoFile.exists() && retrievedDocumentFolder.exists())
			{
				File outputFile = new File(args[3]);
				ExcelWriter excelWriter = new ExcelWriter(outputFile, FileFormat.XLSX);

				CalculateROC instance = new CalculateROC();
				instance.loadFeatureInformation(featureInfoFile);
				for (File file : retrievedDocumentFolder.listFiles())
				{
					if (file.exists())
					{
						if (file.isDirectory() || file.getName().matches("^\\..+") || file.getName().matches("^\\W.+")) continue;
						instance.loadRetrievedDocuments(file.getName(), new CsvReader(file, CSV_SEPERATOR, true));
					}
				}

				for (File file : relevantDocumentFolder.listFiles())
				{
					if (file.exists())
					{
						if (file.isDirectory() || file.getName().matches("^\\..+") || file.getName().matches("^\\W.+")) continue;
						instance.loadRelevantDocuments(file.getName(), new ExcelReader(file));
					}
				}

				instance.calculateROC(excelWriter);
				excelWriter.close();
			}
		}
	}
}