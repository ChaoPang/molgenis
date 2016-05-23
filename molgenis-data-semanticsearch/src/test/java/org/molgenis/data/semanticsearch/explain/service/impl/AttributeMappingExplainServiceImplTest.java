package org.molgenis.data.semanticsearch.explain.service.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData.create;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString;
import org.molgenis.data.semanticsearch.explain.bean.QueryExpansion;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParameter;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParameter;
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
	AttributeMappingExplainServiceImpl attributeMappingExplainServiceImpl;

	@Autowired
	TagGroupGenerator tagGroupGenerator;

	private Ontology ontology;
	private OntologyTerm hypertension;
	private OntologyTerm medication;
	private OntologyTerm systolicHypertension;
	private OntologyTerm distolicHypertension;
	private OntologyTerm hypertensionMedication;

	// private OntologyTermHit hypertensionHit;
	// private OntologyTermHit medicationHit;
	// private OntologyTermHit systolicHypertensionHit;
	// private OntologyTermHit distolicHypertensionHit;
	private OntologyTermHit hypertensionMedicationHit;

	private DefaultAttributeMetaData targetAttribute;
	private DefaultAttributeMetaData matchedSourceAttribute;
	private DefaultEntityMetaData targetEntityMetaData;
	private DefaultEntityMetaData sourceEntityMetaData;
	private List<String> allOntologyIds;
	private List<OntologyTerm> ontologyTerms;

	@BeforeMethod
	public void init()
	{
		ontology = Ontology.create("1", "iri", "name");

		hypertensionMedication = OntologyTerm.create("iri1,iri2", "hypertension,medication");
		hypertension = OntologyTerm.create("iri1", "hypertension", Lists.newArrayList("high blood pressure", "HBP"));
		medication = OntologyTerm.create("iri2", "medication");
		systolicHypertension = OntologyTerm.create("iri3", "systolic hypertension");
		distolicHypertension = OntologyTerm.create("iri4", "distolic hypertension");

		targetAttribute = new DefaultAttributeMetaData("hypertension medication");
		matchedSourceAttribute = new DefaultAttributeMetaData("high blood pressure medication");
		targetEntityMetaData = new DefaultEntityMetaData("target entity");
		sourceEntityMetaData = new DefaultEntityMetaData("source entity");

		hypertensionMedicationHit = OntologyTermHit.create(hypertensionMedication, "high blood pressure medication",
				"high blood pressure medication", 1.0f);

		allOntologyIds = Arrays.asList("1");
		ontologyTerms = asList(hypertension, systolicHypertension, distolicHypertension, medication);
		Set<String> sourceAttributeTerms = Sets.newLinkedHashSet(asList(matchedSourceAttribute.getName().split(" ")));

		when(ontologyService.getOntologies()).thenReturn(Arrays.asList(ontology));

		when(ontologyService.getAllOntologiesIds()).thenReturn(allOntologyIds);

		when(ontologyService.getAtomicOntologyTerms(hypertensionMedication))
				.thenReturn(Arrays.asList(hypertension, medication));

		when(ontologyService.getAtomicOntologyTerms(hypertension)).thenReturn(Arrays.asList(hypertension));

		when(ontologyService.getAtomicOntologyTerms(medication)).thenReturn(Arrays.asList(medication));

		when(ontologyService.getLevelThreeChildren(hypertension))
				.thenReturn(asList(systolicHypertension, distolicHypertension));

		when(ontologyService.getLevelThreeChildren(medication)).thenReturn(emptyList());

		when(ontologyService.fileterOntologyTerms(allOntologyIds, sourceAttributeTerms, 4, ontologyTerms))
				.thenReturn(Arrays.asList(hypertension, medication));

		when(ontologyService.fileterOntologyTerms(allOntologyIds, sourceAttributeTerms, 2,
				Arrays.asList(hypertension, medication))).thenReturn(Arrays.asList(hypertension, medication));

		when(tagGroupGenerator.applyTagMatchingCriteria(Arrays.asList(hypertension, medication), sourceAttributeTerms))
				.thenReturn(Arrays.asList(hypertensionMedicationHit));

		when(tagGroupGenerator.findTagGroups(targetAttribute, targetEntityMetaData, Collections.emptySet(),
				allOntologyIds)).thenReturn(Arrays.asList(hypertensionMedicationHit));

		when(tagGroupGenerator.generateTagGroups(sourceAttributeTerms, asList(hypertensionMedicationHit)))
				.thenReturn(Arrays.asList(hypertensionMedicationHit));

		when(tagGroupGenerator.findTagGroups(targetAttribute, targetEntityMetaData, emptySet(), allOntologyIds))
				.thenReturn(Arrays.asList(hypertensionMedicationHit));
	}

	@Test
	public void testExplainByAttribute()
	{
		SemanticSearchParameter semanticSearchParameter = SemanticSearchParameter.create(targetAttribute,
				Collections.emptySet(), targetEntityMetaData, sourceEntityMetaData, false, false, false);

		ExplainedAttributeMetaData actual = attributeMappingExplainServiceImpl
				.explainAttributeMapping(semanticSearchParameter, matchedSourceAttribute);

		ExplainedAttributeMetaData expected = create(matchedSourceAttribute,
				ExplainedQueryString.create("medication", "hypertension medication", null, 0.34146f), false);

		assertEquals(actual, expected);
	}

	@Test
	public void testExplainBySynonyms()
	{
		SemanticSearchParameter semanticSearchParameter = SemanticSearchParameter.create(targetAttribute,
				Collections.emptySet(), targetEntityMetaData, sourceEntityMetaData, false, true, false);

		ExplainedAttributeMetaData actual = attributeMappingExplainServiceImpl
				.explainAttributeMapping(semanticSearchParameter, matchedSourceAttribute);

		ExplainedAttributeMetaData expected = create(matchedSourceAttribute,
				ExplainedQueryString.create("high blood pressure medication", "high blood pressure medication",
						hypertensionMedication, 1.0f),
				true);

		assertEquals(actual, expected);
	}

	@Test
	public void testExplainByAll()
	{
		SemanticSearchParameter semanticSearchParameter = SemanticSearchParameter.create(targetAttribute,
				Collections.emptySet(), targetEntityMetaData, sourceEntityMetaData, false, true, true);

		ExplainedAttributeMetaData actual = attributeMappingExplainServiceImpl
				.explainAttributeMapping(semanticSearchParameter, matchedSourceAttribute);

		ExplainedAttributeMetaData expected = create(matchedSourceAttribute,
				ExplainedQueryString.create("high blood pressure medication", "high blood pressure medication",
						hypertensionMedication, 1.0f),
				true);

		assertEquals(actual, expected);
	}

	@Test
	public void testExplainAttributeMappingInternal()
	{
		List<QueryExpansion> collect = Arrays.asList(new QueryExpansion(hypertensionMedication, ontologyService,
				QueryExpansionParameter.create(true, true)));

		ExplainedAttributeMetaData actual = attributeMappingExplainServiceImpl
				.explainExactMapping(Sets.newHashSet(targetAttribute.getName()), collect, matchedSourceAttribute);

		ExplainedAttributeMetaData expected = create(matchedSourceAttribute,
				ExplainedQueryString.create("high blood pressure medication", "high blood pressure medication",
						hypertensionMedication, 1.0f),
				true);
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
	public void testComputeAbsoluteScoreForSourceAttributeCaseOne()
	{
		String sourceAttributeDescription = "Incident chronic obstructive pulmonary disease";
		String targetQueryTerm = "cold cut meat ham";

		OntologyTerm coldDiseaseOntologyTerm = OntologyTerm.create("iri0", "Chronic obstructive pulmonary disease",
				asList("cold"));
		OntologyTerm incidentOntologyTerm = OntologyTerm.create("iri1", "Incident", asList("incident"));
		OntologyTerm coldTempOntologyTerm = OntologyTerm.create("iri2", "Cold");
		OntologyTerm meatOntologyTerm = OntologyTerm.create("iri3", "meat");
		OntologyTerm cutOntologyTerm = OntologyTerm.create("iri4", "cut");

		OntologyTerm compositeOntologyTerm1 = OntologyTerm.and(incidentOntologyTerm, coldDiseaseOntologyTerm);
		OntologyTerm compositeOntologyTerm2 = OntologyTerm.and(coldTempOntologyTerm, cutOntologyTerm, meatOntologyTerm);
		OntologyTerm compositeOntologyTerm3 = OntologyTerm.and(coldDiseaseOntologyTerm, cutOntologyTerm,
				meatOntologyTerm);

		OntologyTermHit hit = OntologyTermHit.create(coldDiseaseOntologyTerm, "chronic obstructive pulmonary disease",
				"chronic obstructive pulmonary disease", 0.8f);

		when(ontologyService.getAtomicOntologyTerms(coldDiseaseOntologyTerm))
				.thenReturn(Arrays.asList(coldDiseaseOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm1))
				.thenReturn(Arrays.asList(incidentOntologyTerm, coldDiseaseOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm2))
				.thenReturn(Arrays.asList(coldTempOntologyTerm, meatOntologyTerm, cutOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm3))
				.thenReturn(Arrays.asList(coldDiseaseOntologyTerm, meatOntologyTerm, cutOntologyTerm));

		List<QueryExpansion> ontologyTermQueryExpansions = Arrays.asList(
				new QueryExpansion(compositeOntologyTerm2, ontologyService, QueryExpansionParameter.create(true, true)),
				new QueryExpansion(compositeOntologyTerm3, ontologyService,
						QueryExpansionParameter.create(true, true)));

		Hit<String> computeAbsoluteScoreForSourceAttribute = attributeMappingExplainServiceImpl
				.computeAbsoluteScoreForSourceAttribute(hit, ontologyTermQueryExpansions, targetQueryTerm,
						sourceAttributeDescription);

		assertEquals(computeAbsoluteScoreForSourceAttribute,
				Hit.create("chronic obstructive pulmonary disease", 0.34483f));
	}

	@Test
	public void testComputeAbsoluteScoreForSourceAttributeCaseTwo()
	{
		String sourceAttributeDescription1 = "Sister diagnosed with infarction under the age of 60";
		String sourceAttributeDescription2 = "Father diagnosed with infarction under the age of 60";
		String targetQueryTerm = "Sibling diagnosed with infarction under the age of 60";

		OntologyTerm sibilingOntologyTerm = OntologyTerm.create("iri1", "sibling");
		OntologyTerm sisterOntologyTerm = OntologyTerm.create("iri2", "sister");
		OntologyTerm brotherOntologyTerm = OntologyTerm.create("iri3", "brother");
		OntologyTerm infarctionOntologyTerm = OntologyTerm.create("iri4", "infarction");
		OntologyTerm ageOntologyTerm = OntologyTerm.create("iri5", "age");

		OntologyTerm compositeOntologyTerm1 = OntologyTerm.and(sibilingOntologyTerm, infarctionOntologyTerm,
				ageOntologyTerm);

		OntologyTerm compositeOntologyTerm2 = OntologyTerm.and(infarctionOntologyTerm, ageOntologyTerm);

		OntologyTermHit hit1 = OntologyTermHit.create(compositeOntologyTerm1, "sister infarction age",
				"sister infarction age", 0.6f);

		OntologyTermHit hit2 = OntologyTermHit.create(compositeOntologyTerm2, "infarction age", "infarction age", 0.4f);

		when(ontologyService.getAtomicOntologyTerms(sibilingOntologyTerm))
				.thenReturn(Arrays.asList(sibilingOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm1))
				.thenReturn(Arrays.asList(sibilingOntologyTerm, infarctionOntologyTerm, ageOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm2))
				.thenReturn(Arrays.asList(infarctionOntologyTerm, ageOntologyTerm));

		when(ontologyService.getLevelThreeChildren(sibilingOntologyTerm))
				.thenReturn(Arrays.asList(brotherOntologyTerm, sisterOntologyTerm));

		List<QueryExpansion> ontologyTermQueryExpansions1 = Arrays.asList(new QueryExpansion(compositeOntologyTerm1,
				ontologyService, QueryExpansionParameter.create(true, true)));

		List<QueryExpansion> ontologyTermQueryExpansions2 = Arrays.asList(new QueryExpansion(compositeOntologyTerm2,
				ontologyService, QueryExpansionParameter.create(true, true)));

		Hit<String> computeAbsoluteScoreForSourceAttribute1 = attributeMappingExplainServiceImpl
				.computeAbsoluteScoreForSourceAttribute(hit1, ontologyTermQueryExpansions1, targetQueryTerm,
						sourceAttributeDescription1);
		assertEquals(computeAbsoluteScoreForSourceAttribute1,
				Hit.create("sister infarction age diagnosed with under the of 60", 1.0f));

		Hit<String> computeAbsoluteScoreForSourceAttribute2 = attributeMappingExplainServiceImpl
				.computeAbsoluteScoreForSourceAttribute(hit2, ontologyTermQueryExpansions2, targetQueryTerm,
						sourceAttributeDescription2);
		assertEquals(computeAbsoluteScoreForSourceAttribute2,
				Hit.create("infarction age diagnosed with under the of 60", 0.86957f));
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
		public TagGroupGenerator tagGroupGenerator()
		{
			return mock(TagGroupGenerator.class);
		}

		@Bean
		public AttributeMappingExplainServiceImpl attributeMappingExplainService()
		{
			return new AttributeMappingExplainServiceImpl(ontologyService(), tagGroupGenerator());
		}
	}
}