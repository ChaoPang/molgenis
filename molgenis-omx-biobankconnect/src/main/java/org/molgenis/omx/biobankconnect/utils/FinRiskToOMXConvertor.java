//package org.molgenis.omx.biobankconnect.utils;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Set;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import org.apache.commons.lang3.StringUtils;
//import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
//import org.molgenis.data.Entity;
//import org.molgenis.data.excel.ExcelRepository;
//import org.molgenis.data.excel.ExcelRepositorySource;
//import org.molgenis.data.excel.ExcelSheetWriter;
//import org.molgenis.data.excel.ExcelWriter;
//import org.molgenis.data.excel.ExcelWriter.FileFormat;
//import org.molgenis.data.processor.LowerCaseProcessor;
//import org.molgenis.data.processor.TrimProcessor;
//import org.molgenis.data.support.MapEntity;
//
//import com.google.common.base.Function;
//import com.google.common.collect.Lists;
//
//public class FinRiskToOMXConvertor
//{
//	private final Set<String> topProtocolIdentifiers = new HashSet<String>();
//	private final Map<String, Set<String>> nestedProtocolsRelationMap = new HashMap<String, Set<String>>();
//	private final Map<String, Set<String>> protocolFeaturesRelationMap = new HashMap<String, Set<String>>();
//
//	public FinRiskToOMXConvertor(String dataSetName, File finRiskFile) throws IOException, InvalidFormatException
//	{
//		convertFinRiskToOmx(dataSetName, finRiskFile);
//	}
//
//	private void convertFinRiskToOmx(String dataSetName, File finRiskFile) throws IOException, InvalidFormatException
//	{
//		ExcelWriter excelWriter = null;
//		ExcelSheetWriter excelFeatureSheetWriter = null;
//		ExcelSheetWriter excelProtocolSheetWriter = null;
//		ExcelSheetWriter excelDataSetSheetWriter = null;
//		ExcelSheetWriter excelCategorySheetWriter = null;
//		ExcelRepositorySource excelRepositorySource = null;
//		try
//		{
//			excelWriter = new ExcelWriter(new File(finRiskFile.getAbsolutePath() + ".OMX.xls"), FileFormat.XLS);
//			excelDataSetSheetWriter = excelWriter.createWritable("dataset",
//					Arrays.asList("identifier", "name", "protocolused_identifier"));
//			excelProtocolSheetWriter = excelWriter.createWritable("protocol", Arrays.asList("identifier", "name",
//					"description", "subprotocols_identifier", "features_identifier", "root", "active"));
//			excelFeatureSheetWriter = excelWriter.createWritable("observablefeature",
//					Arrays.asList("identifier", "name", "description", "dataType"));
//			excelCategorySheetWriter = excelWriter.createWritable("category",
//					Arrays.asList("identifier", "name", "description", "valueCode", "observablefeature_identifier"));
//			excelRepositorySource = new ExcelRepositorySource(finRiskFile);
//			excelRepositorySource.addCellProcessor(new LowerCaseProcessor(true, false));
//			excelRepositorySource.addCellProcessor(new TrimProcessor(true, true));
//			ExcelRepository sheet = excelRepositorySource.getSheet(0);
//			Iterator<Entity> iterator = sheet.iterator();
//
//			while (iterator.hasNext())
//			{
//				Entity excelInputEntity = iterator.next();
//				if (excelInputEntity.get("label") == null || excelInputEntity.getString("label").isEmpty()) continue;
//				Entity outputFeatureEntity = processFeature(dataSetName,
//						processNestedProtocol(dataSetName, excelInputEntity), excelInputEntity);
//				processCategory(outputFeatureEntity.getString("identifier"), excelInputEntity, excelCategorySheetWriter);
//				excelFeatureSheetWriter.add(outputFeatureEntity);
//			}
//			addProtocolSheet(dataSetName, excelProtocolSheetWriter);
//			addDataSetSheet(dataSetName, excelDataSetSheetWriter);
//			excelFeatureSheetWriter.flush();
//			excelCategorySheetWriter.flush();
//			excelProtocolSheetWriter.flush();
//			excelDataSetSheetWriter.flush();
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//		finally
//		{
//			if (excelFeatureSheetWriter != null) excelFeatureSheetWriter.clearCache();
//			if (excelCategorySheetWriter != null) excelCategorySheetWriter.clearCache();
//			if (excelProtocolSheetWriter != null) excelProtocolSheetWriter.clearCache();
//			if (excelDataSetSheetWriter != null) excelDataSetSheetWriter.clearCache();
//			if (excelWriter != null) excelWriter.close();
//		}
//	}
//
//	private void addDataSetSheet(String dataSetName, ExcelSheetWriter excelDataSetSheetWriter)
//	{
//		Entity outputEntity = new MapEntity();
//		outputEntity.set("identifier", "dataSet-identifier-" + dataSetName.replaceAll(" ", "-"));
//		outputEntity.set("name", dataSetName + " dataset");
//		outputEntity.set("protocolused_identifier", "protocol-identifier-" + dataSetName.replaceAll(" ", "-"));
//		excelDataSetSheetWriter.add(outputEntity);
//	}
//
//	private void addProtocolSheet(final String dataSetName, ExcelSheetWriter excelProtocolSheetWriter)
//	{
//		Entity rootProtocol = new MapEntity();
//		rootProtocol.set("identifier", "protocol-identifier-" + dataSetName.replaceAll(" ", "-"));
//		rootProtocol.set("name", dataSetName);
//		rootProtocol.set("root", "true");
//		rootProtocol.set("active", "true");
//		rootProtocol.set("subprotocols_identifier", StringUtils.join(topProtocolIdentifiers, ","));
//		excelProtocolSheetWriter.add(rootProtocol);
//
//		for (Entry<String, Set<String>> entry : nestedProtocolsRelationMap.entrySet())
//		{
//			String protocolName = entry.getKey();
//			String protocolIdentifier = "protocol-identifier-" + dataSetName.replaceAll(" ", "-") + "-"
//					+ protocolName.replaceAll(" ", "-");
//			List<String> subProtocolIdentifiers = Lists.transform(new ArrayList<String>(entry.getValue()),
//					new Function<String, String>()
//					{
//						@Override
//						public String apply(String protocolName)
//						{
//							return "protocol-identifier-" + dataSetName.replaceAll(" ", "-") + "-"
//									+ protocolName.replaceAll(" ", "-");
//						}
//					});
//			Entity outputEntity = new MapEntity();
//			outputEntity.set("identifier", protocolIdentifier);
//			outputEntity.set("name", protocolName);
//			outputEntity.set("subprotocols_identifier", StringUtils.join(subProtocolIdentifiers, ","));
//			outputEntity.set(
//					"features_identifier",
//					protocolFeaturesRelationMap.containsKey(protocolName) ? StringUtils.join(
//							protocolFeaturesRelationMap.get(protocolName), ",") : StringUtils.EMPTY);
//			excelProtocolSheetWriter.add(outputEntity);
//		}
//	}
//
//	private String processNestedProtocol(String dataSetName, Entity excelInputEntity)
//	{
//		String nestedProtocolString = excelInputEntity.getString("section");
//		if (nestedProtocolString.isEmpty()) new RuntimeException("The cell is entity");
//
//		String[] protocols = nestedProtocolString.split("::");
//		for (int i = 0; i < protocols.length - 1; i++)
//		{
//			String protocolName = protocols[i];
//			if (!nestedProtocolsRelationMap.containsKey(protocolName)) nestedProtocolsRelationMap.put(protocolName,
//					new HashSet<String>());
//			nestedProtocolsRelationMap.get(protocolName).add(protocols[i + 1]);
//		}
//		topProtocolIdentifiers.add("protocol-identifier-" + dataSetName.replaceAll(" ", "-") + "-"
//				+ protocols[0].replaceAll(" ", "-"));
//		if (!nestedProtocolsRelationMap.containsKey(protocols[protocols.length - 1])) nestedProtocolsRelationMap.put(
//				protocols[protocols.length - 1], new HashSet<String>());
//		return protocols[protocols.length - 1];
//	}
//
//	private Entity processFeature(String dataSetName, String parentProtocol, Entity excelInputEntity)
//	{
//		String featureIdentifier = "feature-identifier-" + dataSetName.replaceAll(" ", "-") + "-"
//				+ excelInputEntity.getString("label");
//		String featureName = excelInputEntity.getString("display name");
//		String description = excelInputEntity.getString("description");
//		Entity outputEntity = new MapEntity();
//		outputEntity.set("identifier", featureIdentifier);
//		outputEntity.set("name", featureName);
//		outputEntity.set("description", description);
//
//		if (!protocolFeaturesRelationMap.containsKey(parentProtocol)) protocolFeaturesRelationMap.put(parentProtocol,
//				new HashSet<String>());
//		protocolFeaturesRelationMap.get(parentProtocol).add(featureIdentifier);
//
//		return outputEntity;
//	}
//
//	private void processCategory(String featureIdentifier, Entity excelInputEntity,
//			ExcelSheetWriter excelCategorySheetWriter)
//	{
//		String categoriesInfo = excelInputEntity.getString("enums");
//		if (categoriesInfo != null)
//		{
//			Pattern pattern = Pattern.compile("\\((\\d*)\\s*\\[([^\\\\(\\\\)]*)\\]\\)");
//			Matcher matcher = pattern.matcher(categoriesInfo.toString());
//
//			while (matcher.find())
//			{
//				if (matcher.group(1).isEmpty() || matcher.group(2).isEmpty()) continue;
//				Integer categoryCode = Integer.parseInt(matcher.group(1));
//				String categoryValue = matcher.group(2);
//
//				if (categoryValue != null && categoryCode != null)
//				{
//					Entity entityOutput = new MapEntity();
//					entityOutput.set("identifier",
//							featureIdentifier + "-category-" + categoryValue.replaceAll(" ", "-"));
//					entityOutput.set("name", categoryValue);
//					entityOutput.set("valueCode", categoryCode);
//					entityOutput.set("observablefeature_identifier", featureIdentifier);
//					excelCategorySheetWriter.add(entityOutput);
//				}
//			}
//		}
//	}
//
//	public static void main(String[] args) throws IOException
//	{
//		String test = "1.56";
//		if (test.matches("[\\.\\d]*")) System.out.println("Hello!");
//
//		// if (args.length == 0)
//		// {
//		// System.out.println("Please provide the FinRisk data dictionary and the name of dataset");
//		// }
//		// else if (args.length > 1)
//		// {
//		// File finRiskFile = new File(args[0]);
//		//
//		// if (!finRiskFile.exists())
//		// {
//		// System.out.println("Could not locate the file with the provided file path!");
//		// }
//		// else
//		// {
//		// new FinRiskToOMXConvertor(args[1], finRiskFile);
//		// }
//		// }
//	}
// }