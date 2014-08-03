package org.molgenis.omx.biobankconnect.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.molgenis.io.excel.ExcelSheetWriter;
import org.molgenis.io.excel.ExcelWriter;
import org.molgenis.io.excel.ExcelWriter.FileFormat;
import org.molgenis.search.Hit;
import org.molgenis.util.tuple.KeyValueTuple;

public class CalculateIRMetrics
{
	private final AtomicInteger totalRelevantDocuments = new AtomicInteger();
	private final AtomicInteger totalRetrievedDocuments = new AtomicInteger();
	private final AtomicInteger retrievedRelevantDocuments = new AtomicInteger();

	public BigDecimal calculatePrecision()
	{
		return new BigDecimal(retrievedRelevantDocuments.doubleValue() / totalRetrievedDocuments.doubleValue());
	}

	public BigDecimal calculateRecall()
	{
		return new BigDecimal(retrievedRelevantDocuments.doubleValue() / totalRelevantDocuments.doubleValue());
	}

	private BigDecimal calculateFMeasure()
	{
		BigDecimal precision = calculatePrecision();
		BigDecimal recall = calculateRecall();
		return new BigDecimal((2 * recall.doubleValue() * precision.doubleValue())
				/ (recall.doubleValue() + precision.doubleValue()));
	}

	public void processData(Map<String, StoreRelevantDocuments> retrievedDocuments,
			Map<String, StoreRelevantDocuments> relevantDocuments, int threshold)
	{
		totalRelevantDocuments.set(0);
		totalRetrievedDocuments.set(0);
		retrievedRelevantDocuments.set(0);
		totalRelevantDocuments.addAndGet(countDocuments(relevantDocuments, null));
		totalRetrievedDocuments.addAndGet(countDocuments(retrievedDocuments, threshold));

		Set<String> uniqueSet = new HashSet<String>();

		for (Entry<String, StoreRelevantDocuments> retrievedDocument : retrievedDocuments.entrySet())
		{
			String standardVariableName = retrievedDocument.getKey();
			if (!relevantDocuments.containsKey(standardVariableName)) continue;
			for (Entry<String, List<Object>> entry : retrievedDocument.getValue().getAllRelevantDocuments())
			{
				String biobankName = entry.getKey();
				List<Object> candidateMappings = entry.getValue();

				for (int i = 0; i < (threshold < candidateMappings.size() ? threshold : candidateMappings.size()); i++)
				{
					if (isRetrieved(candidateMappings.get(i), relevantDocuments.get(standardVariableName)
							.getRelevantDocuments(biobankName)))
					{
						uniqueSet.add(candidateMappings.get(i).toString());
						// retrievedRelevantDocuments.incrementAndGet();
					}
				}
			}
		}
		retrievedRelevantDocuments.set(uniqueSet.size());
	}

	private boolean isRetrieved(Object retrievedDocument, List<Object> relevantDocuments)
	{
		if (relevantDocuments == null) return false;
		if (retrievedDocument instanceof Hit)
		{
			Hit hit = (Hit) retrievedDocument;
			String mappedFeatureName = hit.getColumnValueMap().get(LoadRelevantDocument.FIELD_IDENTIFIER).toString();
			for (Object relevantDocument : relevantDocuments)
			{
				if (mappedFeatureName.equalsIgnoreCase(relevantDocument.toString())) return true;
			}
		}
		else if (retrievedDocument instanceof String)
		{
			for (Object relevantDocument : relevantDocuments)
			{
				if (retrievedDocument.toString().equalsIgnoreCase(relevantDocument.toString())) return true;
			}
		}
		return false;
	}

	private int countDocuments(Map<String, StoreRelevantDocuments> documentMap, Integer threshold)
	{
		Set<String> uniqueSet = new HashSet<String>();
		// int count = 0;
		for (Entry<String, StoreRelevantDocuments> document : documentMap.entrySet())
		{
			for (Entry<String, List<Object>> entry : document.getValue().getAllRelevantDocuments())
			{
				// if (threshold == null) count += entry.getValue().size();
				// else count += (threshold < entry.getValue().size() ?
				// threshold : entry.getValue().size());
				int number = 0;
				for (Object term : entry.getValue())
				{
					if (threshold != null && number == threshold)
					{
						break;
					}
					uniqueSet.add(term.toString());
					number++;
				}
			}
		}
		// return count;
		return uniqueSet.size();
	}

	private static void writeTupleToSheet(String sheetName, List<KeyValueTuple> tuples, ExcelWriter excelWriter)
			throws IOException
	{
		ExcelSheetWriter excelSheetWriter = (ExcelSheetWriter) excelWriter.createTupleWriter(sheetName.split("\\.")[0]);
		excelSheetWriter.writeColNames(Arrays.asList("Precision", "Recall", "F-measure"));
		for (KeyValueTuple tuple : tuples)
		{
			excelSheetWriter.write(tuple);
		}
		excelSheetWriter.close();
	}

	private static List<KeyValueTuple> calculateMetrics(List<File> relevantDocumentFiles, File featureInfoFile,
			File retrievedDocumentFile) throws IOException
	{
		List<KeyValueTuple> tuples = new ArrayList<KeyValueTuple>();
		LoadRelevantDocument loadRelevantDocument = new LoadRelevantDocument(relevantDocumentFiles);
		LoadRetrievedDocument loadRetrievedDocument = new LoadRetrievedDocument(featureInfoFile, retrievedDocumentFile,
				loadRelevantDocument);
		CalculateIRMetrics calculateIRMetrics = new CalculateIRMetrics();
		DecimalFormat df = new DecimalFormat("#.##");
		System.out.print("Biobank : ");
		for (File file : relevantDocumentFiles)
		{
			System.out.print(file.getName() + "\t");
		}
		System.out.println();
		System.out
				.println("Threshold\tPrecision\tRecall\tRelevant_Documents\tRetrieved_Documents\tTrue_Positive\tF-measure");
		for (int i = 1; i < 51; i++)
		{
			calculateIRMetrics.processData(loadRetrievedDocument.getRetrievedDocuments(),
					loadRelevantDocument.getMapForRelevantDocuments(), i);
			System.out.print(i + "\t");
			System.out.print(df.format(calculateIRMetrics.calculatePrecision()) + "\t");
			System.out.print(df.format(calculateIRMetrics.calculateRecall()) + "\t");
			System.out.print(df.format(calculateIRMetrics.totalRelevantDocuments) + "\t");
			System.out.print(df.format(calculateIRMetrics.totalRetrievedDocuments) + "\t");
			System.out.print(df.format(calculateIRMetrics.retrievedRelevantDocuments) + "\t");
			System.out.print(df.format(calculateIRMetrics.calculateFMeasure()) + "\n");

			KeyValueTuple tuple = new KeyValueTuple();
			tuple.set("Precision", df.format(calculateIRMetrics.calculatePrecision()));
			tuple.set("Recall", df.format(calculateIRMetrics.calculateRecall()));
			tuple.set("F-measure", df.format(calculateIRMetrics.calculateFMeasure()));
			tuples.add(tuple);
		}
		System.out.println("\n");
		return tuples;
	}

	public static void main(String args[]) throws FileNotFoundException, IOException
	{
		if (args.length == 4)
		{
			File relevantDocumentFolder = new File(args[0]);
			File featureInfoFile = new File(args[1]);
			File retrievedDocumentFile = new File(args[2]);

			if (relevantDocumentFolder.exists() && featureInfoFile.exists() && retrievedDocumentFile.exists())
			{
				File outputFile = new File(args[3]);
				ExcelWriter excelWriter = new ExcelWriter(outputFile, FileFormat.XLSX);
				List<File> validFiles = new ArrayList<File>();
				for (File file : relevantDocumentFolder.listFiles())
				{
					if (file.isDirectory() || file.getName().matches("^\\..+") || file.getName().matches("^\\W.+")) continue;
					List<KeyValueTuple> tuples = calculateMetrics(Arrays.asList(file), featureInfoFile,
							retrievedDocumentFile);
					writeTupleToSheet(file.getName(), tuples, excelWriter);
					validFiles.add(file);
				}
				List<KeyValueTuple> tuples = calculateMetrics(validFiles, featureInfoFile, retrievedDocumentFile);
				writeTupleToSheet("total", tuples, excelWriter);
				excelWriter.close();
			}
		}
	}
}