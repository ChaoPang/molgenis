package org.molgenis.omx.biobankconnect.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class StoreRelevantDocuments
{
	// Store variable/documents pair
	private final Map<String, List<String>> biobankRelevantDocuments;

	public StoreRelevantDocuments()
	{
		biobankRelevantDocuments = new HashMap<String, List<String>>();
	}

	public void addSingleRecord(String variableName, String relevantDocument)
	{
		if (!biobankRelevantDocuments.containsKey(variableName))
		{
			biobankRelevantDocuments.put(variableName, new ArrayList<String>());
		}
		if (!biobankRelevantDocuments.get(variableName).contains(relevantDocument))
		{
			biobankRelevantDocuments.get(variableName).add(relevantDocument);
		}
	}

	public Set<Entry<String, List<String>>> getAllRelevantDocuments()
	{
		return biobankRelevantDocuments.entrySet();
	}

	public List<String> getRelevantDocuments(String variableName)
	{
		if (!biobankRelevantDocuments.containsKey(variableName)) return Collections.emptyList();
		return biobankRelevantDocuments.get(variableName);
	}

	public Set<String> getAllInvolvedVariables()
	{
		return biobankRelevantDocuments.keySet();
	}
}
