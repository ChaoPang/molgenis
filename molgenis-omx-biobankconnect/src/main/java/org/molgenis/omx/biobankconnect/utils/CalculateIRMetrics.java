package org.molgenis.omx.biobankconnect.utils;

import java.math.BigDecimal;
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

	public void processRetrievedData(Map<String, StoreRelevantDocuments> retrievedDocuments,
			Map<String, StoreRelevantDocuments> relevantDocuments, int threshold)
	{
		totalRelevantDocuments.addAndGet(countDocuments(relevantDocuments, null));
		totalRetrievedDocuments.addAndGet(countDocuments(retrievedDocuments, threshold));

		for (Entry<String, StoreRelevantDocuments> retrievedDocument : retrievedDocuments.entrySet())
		{
			String standardVariableName = retrievedDocument.getKey();
			for (Entry<String, List<Object>> entry : retrievedDocument.getValue().getAllRelevantDocuments())
			{
				String biobankName = entry.getKey();
				List<Object> candidateMappings = entry.getValue();
				for (int i = 0; i < (threshold < candidateMappings.size() ? threshold : candidateMappings.size()); i++)
				{
					if (isRetrieved(candidateMappings.get(i), relevantDocuments.get(standardVariableName)
							.getRelevantDocuments(biobankName))) retrievedRelevantDocuments.incrementAndGet();
				}
			}
		}
	}

	private boolean isRetrieved(Object object, List<Object> relevantDocuments)
	{
		if (relevantDocuments == null) return false;
		if (object instanceof Hit)
		{
			Hit hit = (Hit) object;
			String mappedFeatureName = hit.getColumnValueMap().get(LoadRelevantDocument.FIELD_NAME).toString();
			for (Object relevantDocument : relevantDocuments)
			{
				if (mappedFeatureName.equalsIgnoreCase(relevantDocument.toString())) return true;
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
}