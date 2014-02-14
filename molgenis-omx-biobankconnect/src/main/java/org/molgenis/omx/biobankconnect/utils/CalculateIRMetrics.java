package org.molgenis.omx.biobankconnect.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.molgenis.search.Hit;

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

	public void processData(Map<String, StoreRelevantDocuments> retrievedDocuments,
			Map<String, StoreRelevantDocuments> relevantDocuments, int threshold)
	{
		totalRelevantDocuments.set(0);
		totalRetrievedDocuments.set(0);
		retrievedRelevantDocuments.set(0);
		totalRelevantDocuments.addAndGet(countDocuments(relevantDocuments, null));
		totalRetrievedDocuments.addAndGet(countDocuments(retrievedDocuments, threshold));

		// Map<String, List<String>> uniqueVariableNames = new HashMap<String,
		// List<String>>();
		// for (Entry<String, StoreRelevantDocuments> entry :
		// relevantDocuments.entrySet())
		// {
		// String variableName = entry.getKey();
		// uniqueVariableNames.put(variableName, new ArrayList<String>());
		//
		// for (Object candidateVariable :
		// entry.getValue().getRelevantDocuments("FinRisk"))
		// {
		// uniqueVariableNames.get(variableName).add(candidateVariable.toString());
		// }
		// }

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
							.getRelevantDocuments(biobankName))) retrievedRelevantDocuments.incrementAndGet();
					// uniqueVariableNames.get(standardVariableName).remove(candidateMappings.get(i).toString());
				}
			}
		}
		//
		// for (Entry<String, List<String>> left :
		// uniqueVariableNames.entrySet())
		// {
		// System.out.println(left.getKey() + " : " + left.getValue());
		// }
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
		int count = 0;
		for (Entry<String, StoreRelevantDocuments> document : documentMap.entrySet())
		{
			for (Entry<String, List<Object>> entry : document.getValue().getAllRelevantDocuments())
			{
				if (threshold == null) count += entry.getValue().size();
				else count += (threshold < entry.getValue().size() ? threshold : entry.getValue().size());
			}
		}
		return count;
	}

	public static void main(String args[]) throws FileNotFoundException, IOException
	{
		String relevantDocumentZipFile = "/Users/chaopang/Desktop/Variables_result/Evaluation/test/Archive.zip";
		String featureFilePath = "/Users/chaopang/Desktop/Variables_result/Evaluation/Retrieved-Documents/observableFeature.xlsx";
		String retrievedDocumentsPath = "/Users/chaopang/Desktop/Variables_result/Evaluation/Retrieved-Documents/RetrievedMappings.xlsx";
		LoadRelevantDocument loadRelevantDocument = new LoadRelevantDocument(new File(relevantDocumentZipFile));
		LoadRetrievedDocument loadRetrievedDocument = new LoadRetrievedDocument(new File(featureFilePath), new File(
				retrievedDocumentsPath), loadRelevantDocument.getAllInvolvedBiobankNames());
		CalculateIRMetrics calculateIRMetrics = new CalculateIRMetrics();
		DecimalFormat df = new DecimalFormat("#.##");
		System.out.println("Threshold\tPrecision\tRecall");
		for (int i = 1; i < 11; i++)
		{
			calculateIRMetrics.processData(loadRetrievedDocument.getRetrievedDocuments(),
					loadRelevantDocument.getMapForRelevantDocuments(), i);
			System.out.print(i + "\t");
			System.out.print(df.format(calculateIRMetrics.calculatePrecision()) + "\t");
			System.out.print(df.format(calculateIRMetrics.calculateRecall()) + "\n");
		}
	}
}