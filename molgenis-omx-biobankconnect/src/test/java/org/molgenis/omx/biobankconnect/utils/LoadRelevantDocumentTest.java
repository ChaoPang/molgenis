package org.molgenis.omx.biobankconnect.utils;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.molgenis.search.Hit;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class LoadRelevantDocumentTest
{

	private Map<String, StoreRelevantDocuments> retrievedDocuments;

	private LoadRelevantDocument loadRelevantDocument;

	private CalculateIRMetrics calculateIRMetrics;

	@BeforeTest
	public void init() throws FileNotFoundException, IOException
	{
		String folderPath = "/Users/chaopang/Desktop/Variables_result/Relevant-Documents/test/Archive.zip";
		loadRelevantDocument = new LoadRelevantDocument(new File(folderPath));
		calculateIRMetrics = new CalculateIRMetrics();
		retrievedDocuments = new HashMap<String, StoreRelevantDocuments>();

		List<Object> listOfHits_1 = new ArrayList<Object>();

		Map<String, Object> map0 = new HashMap<String, Object>();
		map0.put(LoadRelevantDocument.FIELD_NAME, "noise0");
		listOfHits_1.add(new Hit("0", "type-1", "link0", map0));

		Map<String, Object> map1 = new HashMap<String, Object>();
		map1.put(LoadRelevantDocument.FIELD_NAME, "TGL_1");
		listOfHits_1.add(new Hit("1", "type-1", "link1", map1));

		Map<String, Object> map2 = new HashMap<String, Object>();
		map2.put(LoadRelevantDocument.FIELD_NAME, "LV_TR_3");
		listOfHits_1.add(new Hit("2", "type-1", "link2", map2));

		Map<String, Object> map3 = new HashMap<String, Object>();
		map3.put(LoadRelevantDocument.FIELD_NAME, "LV_TR_8");
		listOfHits_1.add(new Hit("3", "type-1", "link3", map3));

		List<Object> listOfHits_2 = new ArrayList<Object>();

		Map<String, Object> map4 = new HashMap<String, Object>();
		map4.put(LoadRelevantDocument.FIELD_NAME, "trig");
		listOfHits_2.add(new Hit("4", "type-1", "link4", map4));

		Map<String, Object> map5 = new HashMap<String, Object>();
		map5.put(LoadRelevantDocument.FIELD_NAME, "noise2");
		listOfHits_2.add(new Hit("5", "type-1", "link5", map5));

		Map<String, Object> map6 = new HashMap<String, Object>();
		map6.put(LoadRelevantDocument.FIELD_NAME, "noise3");
		listOfHits_2.add(new Hit("6", "type-1", "link6", map6));

		List<Object> listOfHits_3 = new ArrayList<Object>();

		Map<String, Object> map7 = new HashMap<String, Object>();
		map7.put(LoadRelevantDocument.FIELD_NAME, "SeTrig@NT2BLM");
		listOfHits_3.add(new Hit("7", "type-1", "link7", map7));

		Map<String, Object> map8 = new HashMap<String, Object>();
		map8.put(LoadRelevantDocument.FIELD_NAME, "SeTrig@NT3BLM");
		listOfHits_3.add(new Hit("8", "type-1", "link8", map8));

		StoreRelevantDocuments storeRelevantDocument_1 = new StoreRelevantDocuments();
		storeRelevantDocument_1.addAllRecords("Prevend", listOfHits_1);
		storeRelevantDocument_1.addAllRecords("NCDS", listOfHits_2);
		storeRelevantDocument_1.addAllRecords("HUNT", listOfHits_3);

		List<Object> listOfHits_4 = new ArrayList<Object>();

		Map<String, Object> map9 = new HashMap<String, Object>();
		map9.put(LoadRelevantDocument.FIELD_NAME, "V57A_1");
		listOfHits_4.add(new Hit("9", "type-1", "link9", map9));

		Map<String, Object> map10 = new HashMap<String, Object>();
		map10.put(LoadRelevantDocument.FIELD_NAME, "V57B_1");
		listOfHits_4.add(new Hit("10", "type-1", "link10", map10));

		StoreRelevantDocuments storeRelevantDocument_2 = new StoreRelevantDocuments();
		storeRelevantDocument_2.addAllRecords("Prevend", listOfHits_4);

		retrievedDocuments.put("LAB_TRIG", storeRelevantDocument_1);
		retrievedDocuments.put("PARENTAL_DIABETES", storeRelevantDocument_2);

		calculateIRMetrics.processRetrievedData(retrievedDocuments, loadRelevantDocument.getMapForRelevantDocuments(),
				5);
	}

	@Test
	public void calculatePrecision()
	{
		assertEquals(new BigDecimal(new Double(8) / new Double(11)), calculateIRMetrics.calculatePrecision());
	}

	@Test
	public void calculateRecall()
	{
		assertEquals(new BigDecimal(new Double(8) / new Double(19)), calculateIRMetrics.calculateRecall());
	}
}
