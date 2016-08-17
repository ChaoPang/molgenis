package org.molgenis.data.discovery.service.impl;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.mockito.Mockito;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

@ContextConfiguration(classes = OntologyBasedMatcherTest.Config.class)
public class OntologyBasedMatcherTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	BiobankUniverseRepository biobankUniverseRepository;
	@Autowired
	QueryExpansionService queryExpansionService;
	@Autowired
	OntologyService ontologyService;

	BiobankSampleCollection biobankSampleCollection;

	OntologyBasedMatcher matcher;

	@BeforeMethod
	public void init()
	{
		biobankSampleCollection = BiobankSampleCollection.create("test");

		Mockito.when(biobankUniverseRepository.getBiobankSampleAttributes(biobankSampleCollection))
				.thenReturn(emptyList());

		matcher = new OntologyBasedMatcher(biobankSampleCollection, biobankUniverseRepository, queryExpansionService,
				ontologyService);
	}

	@Test
	public void getParent()
	{
		Assert.assertEquals(matcher.getParent("A1836683.A2656910.A2655472.A2656419.A2655207", 1),
				"A1836683.A2656910.A2655472.A2656419");
		Assert.assertEquals(matcher.getParent("A1836683.A2656910.A2655472.A2656419.A2655207", 3), "A1836683.A2656910");
		Assert.assertEquals(matcher.getParent("A1836683.A2656910.A2655472.A2656419.A2655207", 4), "A1836683");
		Assert.assertEquals(matcher.getParent("A1836683.A2656910.A2655472.A2656419.A2655207", 5), "A1836683");
		Assert.assertEquals(matcher.getParent("A1836683.A2656910.A2655472.A2656419.A2655207", 6), "A1836683");
	}

	@Test
	public void getAllParents()
	{
		List<String> actual = stream(
				matcher.getAllParents("A1836683.A2656910.A2655472.A2656419.A2655207").spliterator(), false)
						.collect(toList());

		List<String> expected = Lists.newArrayList("A1836683.A2656910.A2655472.A2656419", "A1836683.A2656910.A2655472",
				"A1836683.A2656910", "A1836683");

		Assert.assertEquals(actual, expected);
	}

	@Configuration
	public static class Config
	{
		@Bean
		public BiobankUniverseRepository biobankUniverseRepository()
		{
			return mock(BiobankUniverseRepository.class);
		}

		@Bean
		public OntologyService ontologyService()
		{
			return mock(OntologyService.class);
		}

		@Bean
		public QueryExpansionService queryExpansionService()
		{
			return mock(QueryExpansionService.class);
		}
	}
}
