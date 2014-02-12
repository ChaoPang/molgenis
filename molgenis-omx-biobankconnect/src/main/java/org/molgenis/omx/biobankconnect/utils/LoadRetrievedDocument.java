package org.molgenis.omx.biobankconnect.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.molgenis.io.excel.ExcelReader;
import org.molgenis.io.excel.ExcelSheetReader;
import org.molgenis.util.tuple.Tuple;

public class LoadRetrievedDocument
{
	private Map<String, String> featureMapping;
	private Map<String, StoreRelevantDocuments> retrievedDocuments;

	public LoadRetrievedDocument(File featureFile, File mappingFile) throws IOException
	{
		featureMapping = new HashMap<String, String>();
		retrievedDocuments = new HashMap<String, StoreRelevantDocuments>();

		if (mappingFile.exists() && featureFile.exists())
		{
			processFeature(featureFile);
			processRetrievedDocument(mappingFile);
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

	private void processRetrievedDocument(File file) throws IOException
	{
		ExcelReader excelReader = null;

		try
		{
			excelReader = new ExcelReader(file);

			for (String tableName : excelReader.getTableNames())
			{
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

	public void setRetrievedDocuments(Map<String, StoreRelevantDocuments> retrievedDocuments)
	{
		this.retrievedDocuments = retrievedDocuments;
	}
}
