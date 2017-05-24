package org.molgenis.data.discovery.scoring.attributes;

import org.molgenis.ontology.ic.TermFrequencyService;
import org.molgenis.ontology.utils.Stemmer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
		assertEquals(0.175f,
				vectorSpaceModelAttributeSimilarity.score("history of hypertension", "history of medication", false));

		assertEquals(0.293f,
				vectorSpaceModelAttributeSimilarity.score("history of hypertension", "history of medication", true));

		assertEquals(0.905f,
				vectorSpaceModelAttributeSimilarity.score("history of hypertension", "hypertension", true));

		assertEquals(0.973f, vectorSpaceModelAttributeSimilarity
				.score("history of hypertension hypertension", "hypertension", true));

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
