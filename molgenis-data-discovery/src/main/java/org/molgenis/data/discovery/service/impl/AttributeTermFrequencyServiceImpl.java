package org.molgenis.data.discovery.service.impl;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.molgenis.data.DataService;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.ic.TermFrequencyService;
import org.molgenis.ontology.utils.Stemmer;

public class AttributeTermFrequencyServiceImpl implements TermFrequencyService
{
	private final DataService dataService;
	private final Map<String, Integer> termFrequencyMap;
	private final AtomicInteger totalDocument = new AtomicInteger(0);

	public AttributeTermFrequencyServiceImpl(DataService dataService)
	{
		this.dataService = requireNonNull(dataService);
		this.termFrequencyMap = Collections.synchronizedMap(new HashMap<>());
	}

	public float getTermFrequency(String term)
	{
		if (termFrequencyMap.isEmpty()) updateTermFrequency();

		String stemmedTerm = Stemmer.stem(term);
		return termFrequencyMap.containsKey(stemmedTerm)
				? (float) Math.log10(totalDocument.get() / termFrequencyMap.get(stemmedTerm)) : 1;
	}

	public Integer getTermOccurrence(String term)
	{
		if (termFrequencyMap.isEmpty()) updateTermFrequency();

		String stemmedTerm = Stemmer.stem(term);
		return termFrequencyMap.containsKey(stemmedTerm) ? termFrequencyMap.get(stemmedTerm) : 1;
	}

	public void updateTermFrequency()
	{
		Map<String, Integer> termFrequency = new HashMap<>();

		dataService.findAll(BiobankSampleAttributeMetaData.ENTITY_NAME).forEach(entity -> {

			String label = entity.getString(BiobankSampleAttributeMetaData.LABEL);

			for (String word : Stemmer.splitAndStem(label))
			{
				if (!termFrequency.containsKey(word))
				{
					termFrequency.put(word, 0);
				}
				termFrequency.put(word, termFrequency.get(word) + 1);
			}
		});

		int count = (int) dataService.count(BiobankSampleAttributeMetaData.ENTITY_NAME,
				QueryImpl.query().pageSize(Integer.MAX_VALUE));

		termFrequencyMap.putAll(termFrequency);

		totalDocument.set(count);
	}
}
