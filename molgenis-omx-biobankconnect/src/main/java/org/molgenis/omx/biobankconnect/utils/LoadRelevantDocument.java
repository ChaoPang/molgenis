package org.molgenis.omx.biobankconnect.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.io.excel.ExcelReader;
import org.molgenis.io.excel.ExcelSheetReader;
import org.molgenis.util.tuple.Tuple;

public class LoadRelevantDocument
{
	public final static String FIELD_NAME = "Name";

	private final Map<String, StoreRelevantDocuments> map = new HashMap<String, StoreRelevantDocuments>();

	public Map<String, StoreRelevantDocuments> getMapForRelevantDocuments()
	{
		return map;
	}

	public LoadRelevantDocument(File folder) throws FileNotFoundException, IOException
	{
		if (folder.exists())
		{
			for (File file : ZipFileUtil.unzip(folder))
			{
				if (file.isDirectory() || file.getName().matches("^\\._.+")) continue;
				ExcelReader excelReader = null;
				try
				{
					excelReader = new ExcelReader(file);
					processEachExcelFile(removeExcelSuffix(file.getName()), excelReader);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					if (excelReader != null) excelReader.close();
				}
			}
		}
	}

	public void printResult()
	{
		for (Entry<String, StoreRelevantDocuments> entry : map.entrySet())
		{
			System.out.println("\t" + StringUtils.join(entry.getValue().getAllBiobankNames(), "\t"));
			StringBuilder output = new StringBuilder();
			output.append(entry.getKey());
			for (Entry<String, List<Object>> relevantDocuments : entry.getValue().getAllRelevantDocuments())
			{
				output.append("\t").append(StringUtils.join(relevantDocuments.getValue(), ','));
			}
			System.out.println(output.toString());
		}
	}

	private void processEachExcelFile(String variableName, ExcelReader excelReader) throws IOException
	{
		StoreRelevantDocuments storeRelevantDocuments = new StoreRelevantDocuments();
		for (String tableName : excelReader.getTableNames())
		{
			ExcelSheetReader excelSheetReader = excelReader.getSheet(tableName);
			Iterator<Tuple> iterator = excelSheetReader.iterator();
			List<Object> relevantDocuments = new ArrayList<Object>();
			while (iterator.hasNext())
			{
				Tuple row = iterator.next();
				if (row.isNull(FIELD_NAME)) continue;
				relevantDocuments.add(row.get(FIELD_NAME).toString());
			}
			storeRelevantDocuments.addAllRecords(tableName, relevantDocuments);
			excelSheetReader.close();
		}
		map.put(variableName, storeRelevantDocuments);
	}

	private String removeExcelSuffix(String variableName)
	{
		return variableName.split("\\.")[0];
	}
}