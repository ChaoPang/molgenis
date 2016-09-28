package org.molgenis.data.discovery.scoring.attributes;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.molgenis.ontology.ic.TermFrequencyService;
import org.molgenis.ontology.utils.Stemmer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@ContextConfiguration(classes = VectorSpaceModelAttributeSimilarityTest.Config.class)
public class VectorSpaceModelAttributeSimilarityTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	TermFrequencyService termFrequencyService;

	@Autowired
	VectorSpaceModelAttributeSimilarity vectorSpaceModelAttributeSimilarity;

	@BeforeMethod
	public void setup()
	{
		when(termFrequencyService.getTermFrequency(Stemmer.stem("history"))).thenReturn(1.0f);

		when(termFrequencyService.getTermFrequency(Stemmer.stem("of"))).thenReturn(1.0f);

		when(termFrequencyService.getTermFrequency(Stemmer.stem("hypertension"))).thenReturn(3.0f);

		when(termFrequencyService.getTermFrequency(Stemmer.stem("medication"))).thenReturn(1.5f);
	}

	@Test
	public void testScore()
	{
		assertEquals(0.1754116f,
				vectorSpaceModelAttributeSimilarity.score("history of hypertension", "history of medication", false));

		assertEquals(0.29250896f,
				vectorSpaceModelAttributeSimilarity.score("history of hypertension", "history of medication", true));

		assertEquals(0.90453404f,
				vectorSpaceModelAttributeSimilarity.score("history of hypertension", "hypertension", true));

		assertEquals(0.97332853f, vectorSpaceModelAttributeSimilarity.score("history of hypertension hypertension",
				"hypertension", true));

		assertEquals(0.0f, vectorSpaceModelAttributeSimilarity.score("history of", "hypertension", false));
	}

	@Configuration
	public static class Config
	{
		@Bean
		public TermFrequencyService termFrequencyService()
		{
			return mock(TermFrequencyService.class);
		}

		@Bean
		public VectorSpaceModelAttributeSimilarity vectorSpaceModelAttributeSimilarity()
		{
			return new VectorSpaceModelAttributeSimilarity(termFrequencyService());
		}
	}
}
