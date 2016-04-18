package org.molgenis.data.semanticsearch.explain.service.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData.create;
import static org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString.create;
import static org.molgenis.data.semanticsearch.semantic.Hit.create;
import static org.molgenis.data.semanticsearch.service.bean.OntologyTermHit.create;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermQueryExpansion;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceUtils;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
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
	SemanticSearchServiceUtils semanticSearchServiceUtils;

	@Autowired
	AttributeMappingExplainServiceImpl attributeMappingExplainServiceImpl;

	private Ontology ontology;
	private OntologyTerm hypertension;
	private OntologyTerm medication;
	private OntologyTerm systolicHypertension;
	private OntologyTerm distolicHypertension;
	private OntologyTerm hypertensionMedicationOntology;

	private DefaultAttributeMetaData targetAttribute;
	private DefaultAttributeMetaData matchedSourceAttribute;
	private DefaultEntityMetaData targetEntityMetaData;
	private List<String> allOntologyIds;
	private List<OntologyTerm> ontologyTerms;

	@BeforeMethod
	public void init()
	{
		ontology = Ontology.create("1", "iri", "name");
		hypertensionMedicationOntology = OntologyTerm.create("iri1,iri2", "hypertension,medication");
		hypertension = OntologyTerm.create("iri1", "hypertension", Lists.newArrayList("high blood pressure", "HBP"));
		medication = OntologyTerm.create("iri2", "medication");
		systolicHypertension = OntologyTerm.create("iri3", "systolic hypertension");
		distolicHypertension = OntologyTerm.create("iri4", "distolic hypertension");

		targetAttribute = new DefaultAttributeMetaData("hypertension medication");
		matchedSourceAttribute = new DefaultAttributeMetaData("high blood pressure medication");
		targetEntityMetaData = new DefaultEntityMetaData("target entity");

		allOntologyIds = Arrays.asList("1");
		ontologyTerms = asList(hypertension, systolicHypertension, distolicHypertension, medication);
		Set<String> sourceAttributeTerms = Sets.newLinkedHashSet(asList(matchedSourceAttribute.getName().split(" ")));

		when(ontologyService.getAtomicOntologyTerms(hypertensionMedicationOntology))
				.thenReturn(Arrays.asList(hypertension, medication));
		when(ontologyService.getAtomicOntologyTerms(hypertension)).thenReturn(Arrays.asList(hypertension));
		when(ontologyService.getAtomicOntologyTerms(medication)).thenReturn(Arrays.asList(medication));
		when(ontologyService.getLevelThreeChildren(hypertension))
				.thenReturn(asList(systolicHypertension, distolicHypertension));
		when(ontologyService.getLevelThreeChildren(medication)).thenReturn(emptyList());
		when(ontologyService.getOntologies()).thenReturn(Arrays.asList(ontology));
		when(ontologyService.getAllOntologiesIds()).thenReturn(allOntologyIds);

		when(semanticSearchServiceUtils.getQueryTermsFromAttribute(targetAttribute, emptySet()))
				.thenReturn(Sets.newHashSet(targetAttribute.getName()));
		when(semanticSearchServiceUtils.findOntologyTermsForAttr(targetAttribute, targetEntityMetaData, emptySet(),
				allOntologyIds)).thenReturn(ontologyTerms.stream().map(ot -> Hit.create(ot, 0.5f)).collect(toList()));
		when(semanticSearchServiceUtils.getQueryTermsFromAttribute(matchedSourceAttribute, null))
				.thenReturn(Sets.newHashSet(matchedSourceAttribute.getName()));

		when(semanticSearchServiceUtils.splitRemoveStopWords(matchedSourceAttribute.getName()))
				.thenReturn(sourceAttributeTerms);
		when(semanticSearchServiceUtils.splitIntoTerms(matchedSourceAttribute.getName()))
				.thenReturn(sourceAttributeTerms);

		when(ontologyService.fileterOntologyTerms(allOntologyIds, sourceAttributeTerms, 4, ontologyTerms))
				.thenReturn(Arrays.asList(hypertension, medication));
		when(ontologyService.fileterOntologyTerms(allOntologyIds, sourceAttributeTerms, 2,
				Arrays.asList(hypertension, medication))).thenReturn(Arrays.asList(hypertension, medication));

		when(semanticSearchServiceUtils.combineOntologyTerms(sourceAttributeTerms, asList(hypertension, medication)))
				.thenReturn(
						asList(create(create(hypertensionMedicationOntology, "high blood pressure medication"), 1.0f)));
	}

	@Test
	public void testExplainByAttribute()
	{
		ExplainedAttributeMetaData actual = attributeMappingExplainServiceImpl.explainAttributeMapping(targetAttribute,
				Collections.emptySet(), matchedSourceAttribute, targetEntityMetaData, false, false);

		ExplainedAttributeMetaData expected = create(matchedSourceAttribute,
				create("medication", "hypertension medication", "hypertension medication", 0.34146f), false);

		assertEquals(actual, expected);
	}

	@Test
	public void testExplainBySynonyms()
	{
		ExplainedAttributeMetaData actual = attributeMappingExplainServiceImpl.explainAttributeMapping(targetAttribute,
				Collections.emptySet(), matchedSourceAttribute, targetEntityMetaData, true, false);

		ExplainedAttributeMetaData expected = create(matchedSourceAttribute, create("high blood pressure medication",
				"high blood pressure medication", "hypertension,medication", 1.0f), true);
		assertEquals(actual, expected);
	}

	@Test
	public void testExplainByAll()
	{
		ExplainedAttributeMetaData actual = attributeMappingExplainServiceImpl.explainAttributeMapping(targetAttribute,
				emptySet(), matchedSourceAttribute, targetEntityMetaData, true, true);

		ExplainedAttributeMetaData expected = create(matchedSourceAttribute, create("high blood pressure medication",
				"high blood pressure medication", "hypertension,medication", 1.0f), true);
		assertEquals(actual, expected);
	}

	@Test
	public void testExplainAttributeMappingInternal()
	{
		List<OntologyTermQueryExpansion> collect = ontologyTerms.stream()
				.map(ot -> new OntologyTermQueryExpansion(ot, ontologyService, true)).collect(Collectors.toList());
		ExplainedAttributeMetaData actual = attributeMappingExplainServiceImpl.explainAttributeMappingInternal(
				Sets.newHashSet(targetAttribute.getName()), collect, matchedSourceAttribute);

		ExplainedAttributeMetaData expected = create(matchedSourceAttribute, create("high blood pressure medication",
				"high blood pressure medication", "hypertension,medication", 1.0f), true);
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
		public SemanticSearchServiceUtils semanticSearchServiceHelper()
		{
			return mock(SemanticSearchServiceUtils.class);
		}

		@Bean
		public AttributeMappingExplainServiceImpl attributeMappingExplainService()
		{
			return new AttributeMappingExplainServiceImpl(ontologyService(), semanticSearchServiceHelper());
		}
	}
}