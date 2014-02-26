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

import com.google.common.collect.Lists;

public class LoadRelevantDocument
{
	public final static String FIELD_NAME = "Name";
	public final static String FIELD_IDENTIFIER = "Identifier";

	private final Map<String, StoreRelevantDocuments> map = new HashMap<String, StoreRelevantDocuments>();
	private final List<String> involvedDataItems = new ArrayList<String>();

	public Map<String, StoreRelevantDocuments> getMapForRelevantDocuments()
	{
		return map;
	}

	public LoadRelevantDocument(List<File> files) throws FileNotFoundException, IOException
	{
		ExcelReader excelReader = null;
		try
		{
			for (File file : files)
			{
				excelReader = new ExcelReader(file);
				processEachExcelFile(removeExcelSuffix(file.getName()), excelReader);
			}
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

	private void processEachExcelFile(String biobankName, ExcelReader excelReader) throws IOException
	{
		ExcelSheetReader excelSheetReader = excelReader.getSheet(0);
		List<String> columnHeaders = Lists.newArrayList(excelSheetReader.colNamesIterator());
		Iterator<Tuple> iterator = excelSheetReader.iterator();
		while (iterator.hasNext())
		{
			Tuple row = iterator.next();
			if (row.isNull(FIELD_NAME)) continue;
			for (String eachHeader : columnHeaders)
			{
				if (eachHeader.equalsIgnoreCase(FIELD_NAME) || eachHeader.equalsIgnoreCase(FIELD_IDENTIFIER)) continue;
				Integer isRevelant = row.getInt(eachHeader);
				String desiredDataElement = extractVariableName(eachHeader);
				if (isRevelant == 1)
				{
					String relevantDataElement = extractVariableName(row.getString(FIELD_NAME));

					if (!map.containsKey(desiredDataElement)) map.put(desiredDataElement, new StoreRelevantDocuments());
					map.get(desiredDataElement).addSingleRecord(biobankName, relevantDataElement);
				}
				if (!involvedDataItems.contains(desiredDataElement)) involvedDataItems.add(desiredDataElement);
			}
		}
		excelSheetReader.close();
	}

	private String extractVariableName(String variableNameDescription)
	{
		return variableNameDescription.split(":")[0].trim();
	}

	private String removeExcelSuffix(String variableName)
	{
		String biobankNameWithSuffix = variableName.split("\\.")[0];
		return biobankNameWithSuffix.split("_")[0];
	}

	public List<String> getAllInvolvedBiobankNames()
	{
		List<String> biobankNames = new ArrayList<String>();
		for (StoreRelevantDocuments document : map.values())
		{
			biobankNames.addAll(document.getAllBiobankNames());
			break;
		}
		return biobankNames;
	}

	public List<String> getInvolvedDataItems()
	{
		return involvedDataItems;
	}
}