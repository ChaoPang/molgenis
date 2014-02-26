package org.molgenis.omx.biobankconnect.utils;

import static org.testng.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.molgenis.search.Hit;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class LoadRelevantDocumentTest
{

	private Map<String, StoreRelevantDocuments> retrievedDocuments;

	private Map<String, StoreRelevantDocuments> relevantDocuments;

	private CalculateIRMetrics calculateIRMetrics;

	@BeforeTest
	public void init() throws FileNotFoundException, IOException
	{
		relevantDocuments = new HashMap<String, StoreRelevantDocuments>();
		retrievedDocuments = new HashMap<String, StoreRelevantDocuments>();
		calculateIRMetrics = new CalculateIRMetrics();

		List<Object> listOfHits_1 = new ArrayList<Object>();

		Map<String, Object> map0 = new HashMap<String, Object>();
		map0.put(LoadRelevantDocument.FIELD_IDENTIFIER, "noise0");
		listOfHits_1.add(new Hit("0", "type-1", "link0", map0));

		Map<String, Object> map1 = new HashMap<String, Object>();
		map1.put(LoadRelevantDocument.FIELD_IDENTIFIER, "TGL_1");
		listOfHits_1.add(new Hit("1", "type-1", "link1", map1));

		Map<String, Object> map2 = new HashMap<String, Object>();
		map2.put(LoadRelevantDocument.FIELD_IDENTIFIER, "LV_TR_3");
		listOfHits_1.add(new Hit("2", "type-1", "link2", map2));

		Map<String, Object> map3 = new HashMap<String, Object>();
		map3.put(LoadRelevantDocument.FIELD_IDENTIFIER, "LV_TR_8");
		listOfHits_1.add(new Hit("3", "type-1", "link3", map3));

		List<Object> listOfHits_2 = new ArrayList<Object>();

		Map<String, Object> map4 = new HashMap<String, Object>();
		map4.put(LoadRelevantDocument.FIELD_IDENTIFIER, "trig");
		listOfHits_2.add(new Hit("4", "type-1", "link4", map4));

		Map<String, Object> map5 = new HashMap<String, Object>();
		map5.put(LoadRelevantDocument.FIELD_IDENTIFIER, "noise2");
		listOfHits_2.add(new Hit("5", "type-1", "link5", map5));

		Map<String, Object> map6 = new HashMap<String, Object>();
		map6.put(LoadRelevantDocument.FIELD_IDENTIFIER, "noise3");
		listOfHits_2.add(new Hit("6", "type-1", "link6", map6));

		List<Object> listOfHits_3 = new ArrayList<Object>();

		Map<String, Object> map7 = new HashMap<String, Object>();
		map7.put(LoadRelevantDocument.FIELD_IDENTIFIER, "SeTrig@NT2BLM");
		listOfHits_3.add(new Hit("7", "type-1", "link7", map7));

		Map<String, Object> map8 = new HashMap<String, Object>();
		map8.put(LoadRelevantDocument.FIELD_IDENTIFIER, "SeTrig@NT3BLM");
		listOfHits_3.add(new Hit("8", "type-1", "link8", map8));

		StoreRelevantDocuments storeRelevantDocument_1 = new StoreRelevantDocuments();
		storeRelevantDocument_1.addAllRecords("Prevend", listOfHits_1);
		storeRelevantDocument_1.addAllRecords("NCDS", listOfHits_2);
		storeRelevantDocument_1.addAllRecords("HUNT", listOfHits_3);

		List<Object> listOfHits_4 = new ArrayList<Object>();

		Map<String, Object> map9 = new HashMap<String, Object>();
		map9.put(LoadRelevantDocument.FIELD_IDENTIFIER, "V57A_1");
		listOfHits_4.add(new Hit("9", "type-1", "link9", map9));

		Map<String, Object> map10 = new HashMap<String, Object>();
		map10.put(LoadRelevantDocument.FIELD_IDENTIFIER, "V57B_1");
		listOfHits_4.add(new Hit("10", "type-1", "link10", map10));

		StoreRelevantDocuments storeRelevantDocument_2 = new StoreRelevantDocuments();
		storeRelevantDocument_2.addAllRecords("Prevend", listOfHits_4);

		retrievedDocuments.put("LAB_TRIG", storeRelevantDocument_1);
		retrievedDocuments.put("PARENTAL_DIABETES", storeRelevantDocument_2);

		List<Object> documents = new ArrayList<Object>();
		documents.addAll(Arrays.asList("V57A_1", "V57B_1"));
		StoreRelevantDocuments storeRelevantDocument_3 = new StoreRelevantDocuments();
		storeRelevantDocument_3.addAllRecords("Prevend", documents);
		relevantDocuments.put("PARENTAL_DIABETES", storeRelevantDocument_3);

		List<Object> documents_1 = new ArrayList<Object>();
		documents_1.addAll(Arrays.asList("TGL_1", "LV_TR_3", "LV_TR_4", "LV_TR_5"));
		StoreRelevantDocuments storeRelevantDocument_4 = new StoreRelevantDocuments();
		storeRelevantDocument_4.addAllRecords("Prevend", documents_1);
		relevantDocuments.put("LAB_TRIG", storeRelevantDocument_4);

		calculateIRMetrics.processData(retrievedDocuments, relevantDocuments, 5);
	}

	@Test
	public void calculatePrecision()
	{
		assertEquals(new BigDecimal(new Double(4) / new Double(11)), calculateIRMetrics.calculatePrecision());
	}

	@Test
	public void calculateRecall()
	{
		assertEquals(new BigDecimal(new Double(4) / new Double(6)), calculateIRMetrics.calculateRecall());
	}
}
