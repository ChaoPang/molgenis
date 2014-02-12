package org.molgenis.omx.biobankconnect.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class StoreRelevantDocuments
{
	// Store biobank/documents pair
	private final Map<String, List<Object>> biobankRelevantDocuments;

	public StoreRelevantDocuments()
	{
		biobankRelevantDocuments = new HashMap<String, List<Object>>();
	}

	public void addRecord(String biobankName, List<Object> relevantDocuments)
	{
		biobankRelevantDocuments.put(biobankName, relevantDocuments);
	}

	public Set<Entry<String, List<Object>>> getAllRelevantDocuments()
	{
		return biobankRelevantDocuments.entrySet();
	}

	public List<Object> getRelevantDocuments(String biobankName)
	{
		if (!biobankRelevantDocuments.containsKey(biobankName)) return Collections.emptyList();
		return biobankRelevantDocuments.get(biobankName);
	}

	public Set<String> getAllBiobankNames()
	{
		return biobankRelevantDocuments.keySet();
	}
}
