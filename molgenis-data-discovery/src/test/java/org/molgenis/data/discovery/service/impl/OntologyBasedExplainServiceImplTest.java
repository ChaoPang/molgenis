package org.molgenis.data.discovery.service.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.molgenis.data.IdGenerator;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.service.OntologyBasedExplainService;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ContextConfiguration(classes = OntologyBasedExplainServiceImplTest.Config.class)
public class OntologyBasedExplainServiceImplTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	OntologyService ontologyService;

	@Autowired
	OntologyBasedExplainService ontologyBasedExplainServiceImpl;

	@Test
	public void computeScoreForMatchedSource()
	{
		OntologyTerm ontologyTerm2 = OntologyTerm.create("C0302836", "C0302836", "Smoking tobacco",
				Lists.newArrayList("Smoking tobacco", "Smoked Tobacco"));

		OntologyTerm ontologyTerm1 = OntologyTerm.create("C0302836", "C0302836", "Cigar Smoker",
				Lists.newArrayList("Cigar Smoker"));

		when(ontologyService.isDescendant(ontologyTerm1, ontologyTerm2)).thenReturn(true);
		when(ontologyService.getOntologyTermSemanticRelatedness(ontologyTerm1, ontologyTerm2)).thenReturn(0.5);

		BiobankSampleCollection collection = BiobankSampleCollection.create("test collection");

		BiobankSampleAttribute target = BiobankSampleAttribute.create("1", "SMK_CIGAR_CURRENT", "Current Cigar Smoker",
				"", collection, Collections.emptyList());
		BiobankSampleAttribute source = BiobankSampleAttribute.create("2", "SMK121",
				"How many hours a day you are exposed to the tobacco smoke of others? (Repeat) (1)", "", collection,
				Collections.emptyList());
		Hit<String> computeScoreForMatchedSource = ((OntologyBasedExplainServiceImpl) ontologyBasedExplainServiceImpl)
				.computeScoreForMatchedSource(Sets.newHashSet(OntologyTermHit.create(ontologyTerm1, ontologyTerm2)),
						target, source);

		System.out.println(computeScoreForMatchedSource);
	}

	@Test
	public void test2()
	{
		OntologyTerm ontologyTerm2 = OntologyTerm.create("1", "1", "Diabetes Mellitus",
				Lists.newArrayList("Diabetes Mellitus"));

		OntologyTerm ontologyTerm1 = OntologyTerm.create("2", "2", "Diabetes type",
				Lists.newArrayList("Diabetes type"));

		OntologyTerm ontologyTerm3 = OntologyTerm.create("3", "3", "Diabetes type 2",
				Lists.newArrayList("Diabetes type 2"));

		BiobankSampleCollection collection = BiobankSampleCollection.create("test collection");

		BiobankSampleAttribute target = BiobankSampleAttribute.create("1", "DIS_DIAB_TYPE", "Type of Diabetes", "",
				collection, Collections.emptyList());
		BiobankSampleAttribute source = BiobankSampleAttribute.create("2", "dmtype2", "all type 2 diabetes", "",
				collection, Collections.emptyList());
		Hit<String> computeScoreForMatchedSource = ((OntologyBasedExplainServiceImpl) ontologyBasedExplainServiceImpl)
				.computeScoreForMatchedSource(Sets.newHashSet(OntologyTermHit.create(ontologyTerm1, ontologyTerm1),
						OntologyTermHit.create(ontologyTerm2, ontologyTerm2),
						OntologyTermHit.create(ontologyTerm3, ontologyTerm3)), target, source);

		System.out.println(computeScoreForMatchedSource);
	}
	//
	// @Test
	// public void explain()
	// {
	//
	// }
	//
	// @Test
	// public void findAllRelatedOntologyTerms()
	// {
	//
	// }
	//
	// @Test
	// public void findLeftUnmatchedWords()
	// {
	//
	// }
	//
	// @Test
	// public void findMatchedSynonymsInAttribute()
	// {
	//
	// }
	//
	// @Test
	// public void findMatchedWords()
	// {
	//
	// }

	@Configuration
	public static class Config
	{
		@Bean
		public IdGenerator idGenerator()
		{
			return mock(IdGenerator.class);
		}

		@Bean
		public OntologyService ontologyService()
		{
			return mock(OntologyService.class);
		}

		@Bean
		public OntologyBasedExplainService ontologyBasedExplainService()
		{
			return new OntologyBasedExplainServiceImpl(idGenerator(), ontologyService());
		}
	}
}
