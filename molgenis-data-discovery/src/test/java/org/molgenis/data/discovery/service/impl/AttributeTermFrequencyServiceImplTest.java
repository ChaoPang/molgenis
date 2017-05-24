package org.molgenis.data.discovery.service.impl;

import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.support.QueryImpl;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class AttributeTermFrequencyServiceImplTest
{
	AttributeTermFrequencyServiceImpl attributeTermFrequencyServiceImpl;

	@BeforeMethod
	public void setup()
	{
		DataService dataService = mock(DataService.class);

		attributeTermFrequencyServiceImpl = new AttributeTermFrequencyServiceImpl(dataService);

		EntityType biobankSampleAttributeMetaData = mock(EntityType.class);

		Attribute identifier = mock(Attribute.class);
		Attribute name = mock(Attribute.class);
		Attribute label = mock(Attribute.class);

		when(biobankSampleAttributeMetaData.getAttribute(BiobankSampleAttributeMetaData.IDENTIFIER))
				.thenReturn(identifier);
		when(biobankSampleAttributeMetaData.getAttribute(BiobankSampleAttributeMetaData.NAME)).thenReturn(name);
		when(biobankSampleAttributeMetaData.getAttribute(BiobankSampleAttributeMetaData.LABEL)).thenReturn(label);

		Entity attr1 = mock(Entity.class);
		when(attr1.getString(BiobankSampleAttributeMetaData.LABEL)).thenReturn("history of hypertension");
		Entity attr2 = mock(Entity.class);
		when(attr2.getString(BiobankSampleAttributeMetaData.LABEL)).thenReturn("history of heart attack");
		Entity attr3 = mock(Entity.class);
		when(attr3.getString(BiobankSampleAttributeMetaData.LABEL)).thenReturn("history of diabetes");

		List<Entity> biobankAttributeEntities = Arrays.asList(attr1, attr2, attr3);

		when(dataService.findAll(BiobankSampleAttributeMetaData.BIOBANK_SAMPLE_ATTRIBUTE))
				.thenReturn(biobankAttributeEntities.stream());

		when(dataService.count(BiobankSampleAttributeMetaData.BIOBANK_SAMPLE_ATTRIBUTE,
				QueryImpl.query().pageSize(Integer.MAX_VALUE))).thenReturn((long) 3);
	}

	@Test
	public void getTermFrequency()
	{
		assertEquals(attributeTermFrequencyServiceImpl.getTermFrequency("history"), 0f);

		assertEquals(attributeTermFrequencyServiceImpl.getTermFrequency("heart"), 0.477121254719662f);
	}

	@Test
	public void getTermOccurrence()
	{
		Integer actual = attributeTermFrequencyServiceImpl.getTermOccurrence("history");
		assertEquals(3, actual.intValue());
	}
}
