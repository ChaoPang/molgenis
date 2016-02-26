package org.molgenis.data.semanticsearch.service.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.data.semanticsearch.service.impl.OntologyTermBasedSemanticSearchImpl.PSEUDO_ONTOLOGY_TERM;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@ContextConfiguration(classes = OntologyTermBasedSemanticSearchImplTest.Config.class)
public class OntologyTermBasedSemanticSearchImplTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	private SemanticSearchService semanticSearchService;

	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private OntologyTagServiceImpl tagService;

	private OntologyTermBasedSemanticSearchImpl ontologyTermBasedSemanticSearchImpl;

	private Ontology ontology;

	@BeforeMethod
	public void setup()
	{
		ontologyTermBasedSemanticSearchImpl = new OntologyTermBasedSemanticSearchImpl(semanticSearchService,
				ontologyService, tagService);
		ontology = Ontology.create("1", "UMLS", "UMLS");
		when(ontologyService.getOntology("UMLS")).thenReturn(ontology);
	}

	@Test
	public void calculateAverageDistance() throws ExecutionException
	{
		OntologyTerm ot1 = OntologyTerm.create("iri1", StringUtils.EMPTY);
		OntologyTerm ot2 = OntologyTerm.create("iri2", StringUtils.EMPTY);
		OntologyTerm ot3 = OntologyTerm.create("iri3", StringUtils.EMPTY);

		when(ontologyService.getOntologyTerm("iri1")).thenReturn(ot1);
		when(ontologyService.getOntologyTerm("iri2")).thenReturn(ot2);
		when(ontologyService.getOntologyTerm("iri3")).thenReturn(ot3);

		when(ontologyService.getOntologyTermSemanticRelatedness(ot1, ot3)).thenReturn(0.8);
		when(ontologyService.getOntologyTermSemanticRelatedness(ot1, ot2)).thenReturn(0.2);
		when(ontologyService.getOntologyTermSemanticRelatedness(ot2, ot3)).thenReturn(0.4);
		when(ontologyService.getOntologyTermSemanticRelatedness(ot2, ot2)).thenReturn(1.0);
		when(ontologyService.getOntologyTermSemanticRelatedness(ot1, PSEUDO_ONTOLOGY_TERM)).thenReturn(0.1);
		when(ontologyService.getOntologyTermSemanticRelatedness(ot2, PSEUDO_ONTOLOGY_TERM)).thenReturn(0.2);

		// When attributes have the same number of ontology terms
		List<OntologyTerm> ontologyTermsForAttr1 = Arrays.asList(ot1, ot2);
		List<OntologyTerm> ontologyTermsForAttr2 = Arrays.asList(ot3, ot2);

		double actualValue = ontologyTermBasedSemanticSearchImpl.calculateAverageDistance(ontologyTermsForAttr1,
				ontologyTermsForAttr2);
		double[] distances =
		{ 0.8, 1.0 };
		assertEquals(actualValue, StatUtils.sum(distances) / 2);

		// When attributes do not have the same number of ontology terms
		List<OntologyTerm> ontologyTermsForAttr3 = Arrays.asList(ot1, ot2);
		List<OntologyTerm> ontologyTermsForAttr4 = Arrays.asList(ot3);

		double actualValue2 = ontologyTermBasedSemanticSearchImpl.calculateAverageDistance(ontologyTermsForAttr3,
				ontologyTermsForAttr4);
		double[] distances2 =
		{ 0.8, 0.2 };
		assertEquals(actualValue2, StatUtils.sum(distances2) / 2);
	}

	@Test
	public void resolveOntologyTerms()
	{
		OntologyTerm ontologyTerm1 = OntologyTerm.create("iri1", "hypertension");
		OntologyTerm ontologyTerm2 = OntologyTerm.create("iri2", "medication");
		OntologyTerm ontologyTerm3 = OntologyTerm.create("iri1,iri2", "hypertension,medication");

		when(ontologyService.getOntologyTerm("iri1")).thenReturn(ontologyTerm1);
		when(ontologyService.getOntologyTerm("iri2")).thenReturn(ontologyTerm2);

		assertEquals(ontologyTermBasedSemanticSearchImpl.resolveOntologyTerms(ontologyTerm1),
				Arrays.asList(ontologyTerm1));

		assertEquals(ontologyTermBasedSemanticSearchImpl.resolveOntologyTerms(ontologyTerm3),
				Arrays.asList(ontologyTerm1, ontologyTerm2));
	}

	@Configuration
	public static class Config
	{
		@Bean
		OntologyService ontologyService()
		{
			return mock(OntologyService.class);
		}

		@Bean
		SemanticSearchService semanticSearchService()
		{
			return mock(SemanticSearchService.class);
		}

		@Bean
		OntologyTagServiceImpl tagService()
		{
			return mock(OntologyTagServiceImpl.class);
		}
	}
}
