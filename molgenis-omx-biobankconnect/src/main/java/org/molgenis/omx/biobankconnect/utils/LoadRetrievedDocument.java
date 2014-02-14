package org.molgenis.omx.biobankconnect.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.molgenis.io.excel.ExcelReader;
import org.molgenis.io.excel.ExcelSheetReader;
import org.molgenis.io.excel.ExcelSheetWriter;
import org.molgenis.io.excel.ExcelWriter;
import org.molgenis.io.excel.ExcelWriter.FileFormat;
import org.molgenis.util.tuple.KeyValueTuple;
import org.molgenis.util.tuple.Tuple;

public class LoadRetrievedDocument
{
	private Map<String, String> featureMapping;
	private Map<String, StoreRelevantDocuments> retrievedDocuments;
	private final List<Integer> excludedFeatures = Arrays.asList(86, 102, 104, 112, 115, 116, 117, 118, 124);

	public LoadRetrievedDocument(File featureFile, File mappingFile, List<String> biobankNames) throws IOException
	{
		featureMapping = new HashMap<String, String>();
		retrievedDocuments = new HashMap<String, StoreRelevantDocuments>();

		if (mappingFile.exists() && featureFile.exists())
		{
			processFeature(featureFile);
			processRetrievedDocument(mappingFile, biobankNames);
		}
	}

	private void processFeature(File featureFile) throws IOException
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

	private void processRetrievedDocument(File file, List<String> biobankNames) throws IOException
	{
		ExcelReader excelReader = null;

		try
		{
			excelReader = new ExcelReader(file);

			for (String tableName : excelReader.getTableNames())
			{
				if (biobankNames != null && !biobankNames.contains(tableName)) continue;
				ExcelSheetReader excelSheetReader = excelReader.getSheet(tableName);
				proceessEachExcelSheet(tableName, excelSheetReader);
				excelSheetReader.close();
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
	}

	private void proceessEachExcelSheet(String biobankName, ExcelSheetReader excelSheetReader)
	{
		Iterator<Tuple> iterator = excelSheetReader.iterator();
		while (iterator.hasNext())
		{
			Tuple row = iterator.next();
			if (excludedFeatures.contains(row.getInt("Features"))) continue;
			String featureName = featureMapping.get(row.getString("Features"));
			String mappedFeatureName = featureMapping.get(row.getString("Mapped features"));
			if (featureName == null || mappedFeatureName == null)
			{
				System.out.println("Feature name is : " + featureName + "; The mapped feature name is : "
						+ mappedFeatureName);
			}
			if (!retrievedDocuments.containsKey(featureName))
			{
				retrievedDocuments.put(featureName, new StoreRelevantDocuments());
			}
			retrievedDocuments.get(featureName).addSingleRecord(biobankName, mappedFeatureName);
		}
	}

	public Map<String, StoreRelevantDocuments> getRetrievedDocuments()
	{
		return retrievedDocuments;
	}

	public static void main(String args[]) throws FileNotFoundException, IOException
	{
		String featureFilePath = "/Users/chaopang/Desktop/Variables_result/Evaluation/Retrieved-Documents/observableFeature.xlsx";
		String retrievedDocumentsPath = "/Users/chaopang/Desktop/Variables_result/Evaluation/Retrieved-Documents/RetrievedMappings.xlsx";
		LoadRetrievedDocument loadRetrievedDocument = new LoadRetrievedDocument(new File(featureFilePath), new File(
				retrievedDocumentsPath), null);
		String outputDirecotry = "/Users/chaopang/Desktop/Variables_result/Evaluation/output/";
		for (Entry<String, StoreRelevantDocuments> entry : loadRetrievedDocument.getRetrievedDocuments().entrySet())
		{
			String featureName = entry.getKey();
			StoreRelevantDocuments storeRelevantDocuments = entry.getValue();
			ExcelWriter excelWriter = new ExcelWriter(new File(outputDirecotry + featureName + ".xlsx"),
					FileFormat.XLSX);

			for (Entry<String, List<Object>> mappings : storeRelevantDocuments.getAllRelevantDocuments())
			{
				String biobankName = mappings.getKey();
				ExcelSheetWriter excelSheetWriter = (ExcelSheetWriter) excelWriter.createTupleWriter(biobankName);
				excelSheetWriter.writeColNames(Arrays.asList("Name"));
				for (Object eachMapping : mappings.getValue())
				{
					KeyValueTuple tuple = new KeyValueTuple();
					tuple.set("Name", eachMapping.toString());
					excelSheetWriter.write(tuple);
				}
				excelSheetWriter.close();
			}
			excelWriter.close();
		}
	}
}
