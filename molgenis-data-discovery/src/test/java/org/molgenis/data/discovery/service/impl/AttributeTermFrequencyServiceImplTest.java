package org.molgenis.data.discovery.service.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@ContextConfiguration(classes = AttributeTermFrequencyServiceImplTest.Config.class)
public class AttributeTermFrequencyServiceImplTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	DataService dataService;

	@Autowired
	AttributeTermFrequencyServiceImpl attributeTermFrequencyServiceImpl;

	@BeforeMethod
	public void setup()
	{
		MapEntity attr1 = new MapEntity();
		attr1.set(BiobankSampleAttributeMetaData.IDENTIFIER, "1");
		attr1.set(BiobankSampleAttributeMetaData.NAME, "attr1");
		attr1.set(BiobankSampleAttributeMetaData.LABEL, "history of hypertension");

		MapEntity attr2 = new MapEntity();
		attr2.set(BiobankSampleAttributeMetaData.IDENTIFIER, "2");
		attr2.set(BiobankSampleAttributeMetaData.NAME, "attr2");
		attr2.set(BiobankSampleAttributeMetaData.LABEL, "history of heart attack");

		MapEntity attr3 = new MapEntity();
		attr3.set(BiobankSampleAttributeMetaData.IDENTIFIER, "3");
		attr3.set(BiobankSampleAttributeMetaData.NAME, "attr3");
		attr3.set(BiobankSampleAttributeMetaData.LABEL, "history of diabetes");

		List<Entity> biobankAttributeEntities = Arrays.asList(attr1, attr2, attr3);

		when(dataService.findAll(BiobankSampleAttributeMetaData.ENTITY_NAME))
				.thenReturn(biobankAttributeEntities.stream());

		when(dataService.count(BiobankSampleAttributeMetaData.ENTITY_NAME,
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

	@Configuration
	public static class Config
	{
		@Bean
		public DataService dataService()
		{
			return mock(DataService.class);
		}

		@Bean
		public AttributeTermFrequencyServiceImpl attributeTermFrequencyServiceImpl()
		{
			return new AttributeTermFrequencyServiceImpl(dataService());
		}
	}
}
