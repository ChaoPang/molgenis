package org.molgenis.data.discovery.scoring.collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.data.discovery.scoring.collections.VectorSpaceModelCollectionSimilarity.DISTANCE;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

import org.junit.Assert;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverseMemberVector;
import org.molgenis.data.discovery.model.matching.BiobankSampleCollectionSimilarity;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

@ContextConfiguration(classes = VectorSpaceModelCollectionSimilarityTest.Config.class)
public class VectorSpaceModelCollectionSimilarityTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	OntologyService ontologyService;

	@Autowired
	BiobankUniverseRepository biobankUniverseRepository;

	@Autowired
	VectorSpaceModelCollectionSimilarity vectorSpaceModelCollectionSimilarity;

	OntologyTerm vegetables;

	OntologyTerm beans;

	OntologyTerm tomatoes;

	OntologyTerm consumption;

	@BeforeMethod
	public void setup()
	{
		vegetables = OntologyTerm.create("1", "iri1", "Vegetables");
		beans = OntologyTerm.create("2", "iri2", "Beans");
		tomatoes = OntologyTerm.create("3", "iri3", "Tomatoes");
		consumption = OntologyTerm.create("4", "iri4", "Consumption");

		when(ontologyService.related(vegetables, vegetables, DISTANCE)).thenReturn(true);
		when(ontologyService.related(consumption, consumption, DISTANCE)).thenReturn(true);
		when(ontologyService.related(beans, beans, DISTANCE)).thenReturn(true);
		when(ontologyService.related(tomatoes, tomatoes, DISTANCE)).thenReturn(true);
		when(ontologyService.related(vegetables, beans, DISTANCE)).thenReturn(true);
		when(ontologyService.related(vegetables, tomatoes, DISTANCE)).thenReturn(true);
		when(ontologyService.related(beans, vegetables, DISTANCE)).thenReturn(true);
		when(ontologyService.related(tomatoes, vegetables, DISTANCE)).thenReturn(true);
		when(ontologyService.getOntologyTermSemanticRelatedness(vegetables, vegetables)).thenReturn(1.0);
		when(ontologyService.getOntologyTermSemanticRelatedness(consumption, consumption)).thenReturn(1.0);
		when(ontologyService.getOntologyTermSemanticRelatedness(vegetables, beans)).thenReturn(0.8);
		when(ontologyService.getOntologyTermSemanticRelatedness(vegetables, tomatoes)).thenReturn(0.8);
		when(ontologyService.getOntologyTermSemanticRelatedness(beans, vegetables)).thenReturn(0.8);
		when(ontologyService.getOntologyTermSemanticRelatedness(tomatoes, vegetables)).thenReturn(0.8);
		when(ontologyService.getOntologyTermSemanticRelatedness(beans, beans)).thenReturn(1.0);
		when(ontologyService.getOntologyTermSemanticRelatedness(tomatoes, tomatoes)).thenReturn(1.0);
	}

	@Test
	public void testCosineValue()
	{
		BiobankSampleCollection biobankSampleCollection1 = BiobankSampleCollection.create("test1");
		double[] vector1 = new double[]
		{ 1.0, 0.8, 0.8, 1.0 };

		BiobankSampleCollection biobankSampleCollection2 = BiobankSampleCollection.create("test2");
		double[] vector2 = new double[]
		{ 1.6, 1.0, 1.0, 1.0 };

		BiobankSampleCollectionSimilarity cosineValue = vectorSpaceModelCollectionSimilarity.cosineValue(
				BiobankUniverseMemberVector.create(biobankSampleCollection1, vector1),
				BiobankUniverseMemberVector.create(biobankSampleCollection2, vector2));

		BiobankSampleCollectionSimilarity expected = BiobankSampleCollectionSimilarity.create(biobankSampleCollection1,
				biobankSampleCollection2, 0.9835013f);

		Assert.assertEquals(expected, cosineValue);
	}

	@Test
	public void testCreateVector()
	{
		List<OntologyTerm> uniqueOntologyTermList = Arrays.asList(vegetables, beans, tomatoes, consumption);

		Map<OntologyTerm, Integer> ontologyTermFrequency1 = ImmutableMap.of(vegetables, 1, consumption, 2);

		double[] actual1 = DoubleStream
				.of(vectorSpaceModelCollectionSimilarity.createVector(ontologyTermFrequency1, uniqueOntologyTermList))
				.map(d -> Math.round(d * 10000) / 10000.0d).toArray();

		double[] expected1 = new double[]
		{ 1.0, 0.8, 0.8, 1.0 };
		Assert.assertEquals(Arrays.toString(expected1), Arrays.toString(actual1));

		Map<OntologyTerm, Integer> ontologyTermFrequency2 = ImmutableMap.of(beans, 1, tomatoes, 1, consumption, 2);

		double[] actual2 = DoubleStream
				.of(vectorSpaceModelCollectionSimilarity.createVector(ontologyTermFrequency2, uniqueOntologyTermList))
				.map(d -> Math.round(d * 10000) / 10000.0d).toArray();

		double[] expected2 = new double[]
		{ 1.6, 1.0, 1.0, 1.0 };
		Assert.assertEquals(Arrays.toString(expected2), Arrays.toString(actual2));
	}

	@Configuration
	public static class Config
	{
		@Bean
		public OntologyService ontologyService()
		{
			return mock(OntologyService.class);
		}

		@Bean
		public BiobankUniverseRepository biobankUniverseRepository()
		{
			return mock(BiobankUniverseRepository.class);
		}

		@Bean
		public VectorSpaceModelCollectionSimilarity vectorSpaceModelCollectionSimilarity()
		{
			return new VectorSpaceModelCollectionSimilarity(biobankUniverseRepository(), ontologyService());
		}
	}
}
