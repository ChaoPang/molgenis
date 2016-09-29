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

@ContextConfiguration(classes = NgramAttributeSimilarityTest.Config.class)
public class NgramAttributeSimilarityTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	TermFrequencyService termFrequencyService;

	@Autowired
	NgramAttributeSimilarity ngramAttributeSimilarity;

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
		assertEquals(0.498f, ngramAttributeSimilarity.score("history of hypertension", "history of medication", false));

		assertEquals(0.577f, ngramAttributeSimilarity.score("history of hypertension", "history of medication", true));

		assertEquals(0.647f, ngramAttributeSimilarity.score("history of hypertension", "hypertension", true));

		assertEquals(0.490f,
				ngramAttributeSimilarity.score("history of hypertension hypertension", "hypertension", true));

		assertEquals(0.111f, ngramAttributeSimilarity.score("history of", "hypertension", false));
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
		public NgramAttributeSimilarity ngramAttributeSimilarity()
		{
			return new NgramAttributeSimilarity(termFrequencyService());
		}
	}
}
