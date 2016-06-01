package org.molgenis.data.semanticsearch.explain.service.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData.create;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.molgenis.data.semanticsearch.explain.bean.AttributeMatchExplanation;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermQueryExpansion;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParam;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.data.semanticsearch.service.impl.SemanticSearchParamFactory;
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
	ExplainMappingServiceImpl attributeMappingExplainServiceImpl;

	@Autowired
	TagGroupGenerator tagGroupGenerator;

	private Ontology ontology;
	private OntologyTerm hypertension;
	private OntologyTerm medication;
	private OntologyTerm systolicHypertension;
	private OntologyTerm distolicHypertension;
	private OntologyTerm hypertensionMedication;

	private TagGroup hypertensionMedicationHit;

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

		hypertensionMedicationHit = TagGroup.create(hypertensionMedication, "high blood pressure medication", 1.0f);

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

		when(tagGroupGenerator.applyTagMatchingCriterion(Arrays.asList(hypertension, medication), sourceAttributeTerms))
				.thenReturn(Arrays.asList(hypertensionMedicationHit));

		when(tagGroupGenerator.combineTagGroups(sourceAttributeTerms, asList(hypertensionMedicationHit)))
				.thenReturn(Arrays.asList(hypertensionMedicationHit));
	}

	@Test
	public void testExplainByAttribute()
	{
		SemanticSearchParam semanticSearchParameter = SemanticSearchParamFactory.create(targetAttribute,
				Collections.emptySet(), targetEntityMetaData, false, false);

		ExplainedAttributeMetaData actual = attributeMappingExplainServiceImpl.explainMapping(semanticSearchParameter,
				matchedSourceAttribute.getLabel());

		ExplainedAttributeMetaData expected = create(matchedSourceAttribute,
				AttributeMatchExplanation.create("medication", "hypertension medication", null, 0.34146f), false);

		assertEquals(actual, expected);
	}

	@Test
	public void testExplainBySynonyms()
	{
		SemanticSearchParam semanticSearchParameter = SemanticSearchParamFactory.create(targetAttribute,
				Collections.emptySet(), targetEntityMetaData, true, false);

		ExplainedAttributeMetaData actual = attributeMappingExplainServiceImpl.explainMapping(semanticSearchParameter,
				matchedSourceAttribute.getLabel());

		ExplainedAttributeMetaData expected = create(matchedSourceAttribute,
				AttributeMatchExplanation.create("high blood pressure medication", "high blood pressure medication",
						hypertensionMedication, 1.0f),
				true);

		assertEquals(actual, expected);
	}

	@Test
	public void testExplainByAll()
	{
		SemanticSearchParamFactory semanticSearchParameter = SemanticSearchParamFactory.create(targetAttribute,
				Collections.emptySet(), targetEntityMetaData, sourceEntityMetaData, true, true);

		ExplainedAttributeMetaData actual = attributeMappingExplainServiceImpl.explainMapping(semanticSearchParameter,
				matchedSourceAttribute);

		ExplainedAttributeMetaData expected = create(matchedSourceAttribute,
				AttributeMatchExplanation.create("high blood pressure medication", "high blood pressure medication",
						hypertensionMedication, 1.0f),
				true);

		assertEquals(actual, expected);
	}

	@Test
	public void testExplainAttributeMappingInternal()
	{
		List<OntologyTermQueryExpansion> collect = Arrays.asList(new OntologyTermQueryExpansion(hypertensionMedication,
				ontologyService, QueryExpansionParam.create(true, true)));

		ExplainedAttributeMetaData actual = attributeMappingExplainServiceImpl
				.explainMapping(Sets.newHashSet(targetAttribute.getName()), collect, matchedSourceAttribute);

		ExplainedAttributeMetaData expected = create(matchedSourceAttribute,
				AttributeMatchExplanation.create("high blood pressure medication", "high blood pressure medication",
						hypertensionMedication, 1.0f),
				true);
		assertEquals(actual, expected);
	}

	@Test
	public void findBestQueryTerm()
	{
		AttributeMatchExplanation lexicalBasedExplanation = attributeMappingExplainServiceImpl
				.createLexicalBasedExplanation(Sets.newHashSet("Hypertensive", "HBP"), "hypertension");
		assertEquals(lexicalBasedExplanation,
				AttributeMatchExplanation.create("hypertension", "hypertension", null, 1.0f));
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

		TagGroup hit = TagGroup.create(coldDiseaseOntologyTerm, "chronic obstructive pulmonary disease", 0.8f);

		when(ontologyService.getAtomicOntologyTerms(coldDiseaseOntologyTerm))
				.thenReturn(Arrays.asList(coldDiseaseOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm1))
				.thenReturn(Arrays.asList(incidentOntologyTerm, coldDiseaseOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm2))
				.thenReturn(Arrays.asList(coldTempOntologyTerm, meatOntologyTerm, cutOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm3))
				.thenReturn(Arrays.asList(coldDiseaseOntologyTerm, meatOntologyTerm, cutOntologyTerm));

		List<OntologyTermQueryExpansion> ontologyTermQueryExpansions = Arrays.asList(
				new OntologyTermQueryExpansion(compositeOntologyTerm2, ontologyService,
						QueryExpansionParam.create(true, true)),
				new OntologyTermQueryExpansion(compositeOntologyTerm3, ontologyService,
						QueryExpansionParam.create(true, true)));

		Hit<String> computeAbsoluteScoreForSourceAttribute = attributeMappingExplainServiceImpl
				.computeScoreForMatchedSource(hit, ontologyTermQueryExpansions, targetQueryTerm,
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

		TagGroup hit1 = TagGroup.create(compositeOntologyTerm1, "sister infarction age", 0.6f);

		TagGroup hit2 = TagGroup.create(compositeOntologyTerm2, "infarction age", 0.4f);

		when(ontologyService.getAtomicOntologyTerms(sibilingOntologyTerm))
				.thenReturn(Arrays.asList(sibilingOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm1))
				.thenReturn(Arrays.asList(sibilingOntologyTerm, infarctionOntologyTerm, ageOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm2))
				.thenReturn(Arrays.asList(infarctionOntologyTerm, ageOntologyTerm));

		when(ontologyService.getLevelThreeChildren(sibilingOntologyTerm))
				.thenReturn(Arrays.asList(brotherOntologyTerm, sisterOntologyTerm));

		List<OntologyTermQueryExpansion> ontologyTermQueryExpansions1 = Arrays.asList(new OntologyTermQueryExpansion(
				compositeOntologyTerm1, ontologyService, QueryExpansionParam.create(true, true)));

		List<OntologyTermQueryExpansion> ontologyTermQueryExpansions2 = Arrays.asList(new OntologyTermQueryExpansion(
				compositeOntologyTerm2, ontologyService, QueryExpansionParam.create(true, true)));

		Hit<String> computeAbsoluteScoreForSourceAttribute1 = attributeMappingExplainServiceImpl
				.computeScoreForMatchedSource(hit1, ontologyTermQueryExpansions1, targetQueryTerm,
						sourceAttributeDescription1);
		assertEquals(computeAbsoluteScoreForSourceAttribute1,
				Hit.create("sister infarction age diagnosed with under the of 60", 1.0f));

		Hit<String> computeAbsoluteScoreForSourceAttribute2 = attributeMappingExplainServiceImpl
				.computeScoreForMatchedSource(hit2, ontologyTermQueryExpansions2, targetQueryTerm,
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
		public ExplainMappingServiceImpl attributeMappingExplainService()
		{
			return new ExplainMappingServiceImpl(ontologyService(), tagGroupGenerator());
		}
	}
}