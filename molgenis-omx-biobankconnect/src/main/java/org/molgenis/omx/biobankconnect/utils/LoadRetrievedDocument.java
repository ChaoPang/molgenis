package org.molgenis.omx.biobankconnect.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
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
	private Map<String, FeatureEntity> detailedFeatureInfo;
	private Map<String, StoreRelevantDocuments> retrievedDocuments;
	private final List<Integer> excludedFeatures = Arrays.asList(86, 102, 104, 112, 115, 116, 117, 124);

	public LoadRetrievedDocument(File featureFile, File mappingFile, LoadRelevantDocument loadRelevantDocument)
			throws IOException
	{
		featureMapping = new HashMap<String, String>();
		detailedFeatureInfo = new HashMap<String, FeatureEntity>();
		retrievedDocuments = new HashMap<String, StoreRelevantDocuments>();

		if (mappingFile.exists() && featureFile.exists())
		{
			processFeature(featureFile);
			processRetrievedDocument(mappingFile, loadRelevantDocument);
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
				FeatureEntity feature = new FeatureEntity();
				feature.setId(Integer.parseInt(row.getString("id")));
				feature.setName(row.getString("Name"));
				feature.setDescription(row.getString("description"));
				detailedFeatureInfo.put(row.getString("id"), feature);
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

	private void processRetrievedDocument(File file, LoadRelevantDocument loadRelevantDocument) throws IOException
	{
		ExcelReader excelReader = null;
		ExcelWriter writer = null;
		List<String> biobankNames = loadRelevantDocument == null ? null : loadRelevantDocument
				.getAllInvolvedBiobankNames();
		List<String> dataItems = loadRelevantDocument == null ? null : loadRelevantDocument.getInvolvedDataItems();
		try
		{
			excelReader = new ExcelReader(file);
			writer = new ExcelWriter(new File(file.getAbsolutePath() + "_featureDetail.xlsx"), FileFormat.XLSX);

			for (String tableName : excelReader.getTableNames())
			{
				if (biobankNames != null && !biobankNames.contains(tableName)) continue;
				ExcelSheetReader excelSheetReader = excelReader.getSheet(tableName);
				writeOutFeatureDetails(proceessEachExcelSheet(tableName, dataItems, excelSheetReader),
						(ExcelSheetWriter) writer.createTupleWriter(tableName));
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
			if (writer != null) writer.close();
		}
	}

	private void writeOutFeatureDetails(Map<FeatureEntity, List<FeatureEntity>> mappingCollections,
			ExcelSheetWriter outputSheet) throws IOException
	{
		outputSheet.writeColNames(Arrays.asList("Desired_Element", "Desired_Element_Description", "Data_Element",
				"Data_Element_Description", "Rank"));
		for (Entry<FeatureEntity, List<FeatureEntity>> entry : mappingCollections.entrySet())
		{
			FeatureEntity feature = entry.getKey();
			if (feature == null) continue;
			int count = 1;
			for (FeatureEntity mappedFeature : entry.getValue())
			{
				KeyValueTuple tuple = new KeyValueTuple();
				tuple.set("Desired_Element", feature.getName());
				tuple.set("Desired_Element_Description", feature.getDescription());
				tuple.set("Data_Element", mappedFeature.getName());
				tuple.set("Data_Element_Description", mappedFeature.getDescription());
				tuple.set("Rank", count++);
				outputSheet.write(tuple);
			}
		}
		outputSheet.close();
	}

	private Map<FeatureEntity, List<FeatureEntity>> proceessEachExcelSheet(String biobankName,
			List<String> involvedDataItems, ExcelSheetReader excelSheetReader)
	{
		Map<FeatureEntity, List<FeatureEntity>> mappingCollections = new HashMap<FeatureEntity, List<FeatureEntity>>();
		Iterator<Tuple> iterator = excelSheetReader.iterator();
		while (iterator.hasNext())
		{
			Tuple row = iterator.next();
			if (excludedFeatures.contains(row.getInt("Features"))) continue;
			String featureName = featureMapping.get(row.getString("Features"));
			String mappedFeatureName = featureMapping.get(row.getString("Mapped features"));
			if (involvedDataItems != null && !involvedDataItems.contains(featureName))
			{
				System.out.println("WARNING: " + featureName + " is excluded!");
				continue;
			}
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

			if (!mappingCollections.containsKey(detailedFeatureInfo.get(row.getString("Features"))))
			{
				mappingCollections.put(detailedFeatureInfo.get(row.getString("Features")),
						new ArrayList<FeatureEntity>());
			}
			mappingCollections.get(detailedFeatureInfo.get(row.getString("Features"))).add(
					detailedFeatureInfo.get(row.getString("Mapped features")));
		}

		return mappingCollections;
	}

	public Map<String, StoreRelevantDocuments> getRetrievedDocuments()
	{
		return retrievedDocuments;
	}

	public static void main(String args[]) throws FileNotFoundException, IOException
	{
		if (args.length == 2)
		{
			new LoadRetrievedDocument(new File(args[0]), new File(args[1]), null);
		}
		else if (args.length == 3)
		{
			LoadRetrievedDocument loadRetrievedDocument = new LoadRetrievedDocument(new File(args[0]),
					new File(args[1]), null);
			for (Entry<String, StoreRelevantDocuments> entry : loadRetrievedDocument.getRetrievedDocuments().entrySet())
			{
				String featureName = entry.getKey();
				StoreRelevantDocuments storeRelevantDocuments = entry.getValue();
				ExcelWriter excelWriter = new ExcelWriter(new File(args[2] + featureName + ".xlsx"), FileFormat.XLSX);

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
}
