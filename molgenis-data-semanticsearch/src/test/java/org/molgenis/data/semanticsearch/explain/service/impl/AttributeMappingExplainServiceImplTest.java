package org.molgenis.data.semanticsearch.explain.service.impl;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData.create;
import static org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString.create;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceHelper;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ContextConfiguration(classes = AttributeMappingExplainServiceImplTest.Config.class)
public class AttributeMappingExplainServiceImplTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	SemanticSearchService semanticSearchService;

	@Autowired
	OntologyService ontologyService;

	@Autowired
	SemanticSearchServiceHelper semanticSearchServiceHelper;

	@Autowired
	AttributeMappingExplainServiceImpl attributeMappingExplainServiceImpl;

	@BeforeMethod
	public void init()
	{
		OntologyTerm ot = OntologyTerm.create("iri1,iri2", "hypertension,medication");
		OntologyTerm atomic_ot1 = OntologyTerm.create("iri1", "hypertension",
				Lists.newArrayList("high blood pressure", "HBP"));
		OntologyTerm atomic_ot2 = OntologyTerm.create("iri2", "medication");
		when(ontologyService.getAtomicOntologyTerms(ot)).thenReturn(Arrays.asList(atomic_ot1, atomic_ot2));

		OntologyTerm atomic_ot1_child1 = OntologyTerm.create("iri3", "systolic hypertension");
		OntologyTerm atomic_ot1_child2 = OntologyTerm.create("iri4", "distolic hypertension");

		when(ontologyService.getChildren(atomic_ot1)).thenReturn(Arrays.asList(atomic_ot1_child1, atomic_ot1_child2));
		when(ontologyService.getChildren(atomic_ot2)).thenReturn(Collections.emptyList());
	}

	@Test
	public void explainAttributeMappingAttributeMetaDataAttributeMetaDataEntityMetaDataEntityMetaData()
	{
		Set<String> userQueries = Collections.emptySet();

		DefaultAttributeMetaData targetAttribute = new DefaultAttributeMetaData("hypertension medication");
		DefaultAttributeMetaData matchedSourceAttribute = new DefaultAttributeMetaData(
				"high blood pressure medication");
		DefaultEntityMetaData targetEntityMetaData = new DefaultEntityMetaData("target entity");
		DefaultEntityMetaData sourceEntityMetaData = new DefaultEntityMetaData("source entity");

		List<OntologyTerm> ontologyTerms = Arrays.asList(
				OntologyTerm.create("iri1", "hypertension", Lists.newArrayList("high blood pressure", "HBP")),
				OntologyTerm.create("iri3", "systolic hypertension"),
				OntologyTerm.create("iri4", "distolic hypertension"), OntologyTerm.create("iri2", "medication"));

		List<String> allOntologyIds = Arrays.asList("1");

		when(semanticSearchServiceHelper.createLexicalSearchQueryTerms(targetAttribute, userQueries))
				.thenReturn(Sets.newHashSet("hypertension medication"));

		when(semanticSearchServiceHelper.createLexicalSearchQueryTerms(matchedSourceAttribute, null))
				.thenReturn(Sets.newHashSet("high blood pressure medication"));

		when(semanticSearchService.findOntologyTermsForAttr(targetAttribute, targetEntityMetaData, userQueries))
				.thenReturn(Arrays.asList(OntologyTerm.create("iri1,iri2", "hypertension,medication")));

		when(ontologyService.getAllOntologiesIds()).thenReturn(allOntologyIds);

		OntologyTermHit ontologyTermHit = OntologyTermHit
				.create(OntologyTerm.create("iri1,iri2", "hypertension,medication"), "high blood pressure medication");

		when(semanticSearchService.findAllTagsForAttribute(matchedSourceAttribute, allOntologyIds, ontologyTerms))
				.thenReturn(Arrays.asList(Hit.<OntologyTermHit> create(ontologyTermHit, (float) 1)));

		ExplainedAttributeMetaData actual = attributeMappingExplainServiceImpl.explainAttributeMapping(targetAttribute,
				matchedSourceAttribute, targetEntityMetaData, sourceEntityMetaData);

		ExplainedAttributeMetaData expected = create(matchedSourceAttribute, asList(create("high pressur medic blood",
				"high blood pressure medication", "hypertension,medication", (float) 100.0)), true);

		assertEquals(actual, expected);
	}

	@Test
	public void findBestQueryTerm()
	{
		Hit<String> findBestQueryTerm = attributeMappingExplainServiceImpl.findBestQueryTerm(
				Sets.newHashSet("high blood pressure", "hypertension"), Sets.newHashSet("Hypertensive", "HBP"));
		assertEquals(findBestQueryTerm,
				Hit.<String> create("hypertension", (float) stringMatching("hypertension", "Hypertensive") / 100));
	}

	@Test
	public void getExpandedOntologyTerms()
	{
		OntologyTerm ot = OntologyTerm.create("iri1,iri2", "hypertension,medication");
		List<OntologyTerm> actual = attributeMappingExplainServiceImpl.getExpandedOntologyTerms(Arrays.asList(ot));
		List<OntologyTerm> expected = Arrays.asList(
				OntologyTerm.create("iri1", "hypertension", Lists.newArrayList("high blood pressure", "HBP")),
				OntologyTerm.create("iri3", "systolic hypertension"),
				OntologyTerm.create("iri4", "distolic hypertension"), OntologyTerm.create("iri2", "medication"));
		assertEquals(actual, expected);
	}

	@Configuration
	public static class Config
	{
		@Bean
		public SemanticSearchService semanticSearchService()
		{
			return mock(SemanticSearchService.class);
		}

		@Bean
		public OntologyService ontologyService()
		{
			return mock(OntologyService.class);
		}

		@Bean
		public SemanticSearchServiceHelper semanticSearchServiceHelper()
		{
			return mock(SemanticSearchServiceHelper.class);
		}

		@Bean
		public AttributeMappingExplainServiceImpl attributeMappingExplainService()
		{
			return new AttributeMappingExplainServiceImpl(semanticSearchService(), ontologyService(),
					semanticSearchServiceHelper());
		}
	}
}