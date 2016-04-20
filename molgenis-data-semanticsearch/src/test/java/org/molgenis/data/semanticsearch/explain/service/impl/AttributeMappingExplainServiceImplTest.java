package org.molgenis.data.semanticsearch.explain.service.impl;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
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

import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermQueryExpansion;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
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

		when(ontologyService.getOntologies()).thenReturn(Arrays.asList(ontology));

		when(ontologyService.getAllOntologiesIds()).thenReturn(allOntologyIds);

		when(ontologyService.getAtomicOntologyTerms(hypertensionMedicationOntology))
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

		when(semanticSearchServiceUtils.combineOntologyTerms(sourceAttributeTerms, asList(hypertension, medication)))
				.thenReturn(
						asList(create(create(hypertensionMedicationOntology, "high blood pressure medication"), 1.0f)));

		when(semanticSearchServiceUtils.findOntologyTermsForAttr(targetAttribute, targetEntityMetaData, emptySet(),
				allOntologyIds)).thenReturn(Arrays.asList(Hit.create(hypertensionMedicationOntology, 1.0f)));

		when(semanticSearchServiceUtils.getQueryTermsFromAttribute(targetAttribute, emptySet()))
				.thenReturn(Sets.newHashSet(targetAttribute.getName()));

		when(semanticSearchServiceUtils.getQueryTermsFromAttribute(matchedSourceAttribute, null))
				.thenReturn(Sets.newHashSet(matchedSourceAttribute.getName()));

		when(semanticSearchServiceUtils.splitRemoveStopWords(matchedSourceAttribute.getName()))
				.thenReturn(sourceAttributeTerms);

		when(semanticSearchServiceUtils.splitIntoTerms(matchedSourceAttribute.getName()))
				.thenReturn(sourceAttributeTerms);

		when(semanticSearchServiceUtils.getLowerCaseTermsFromOntologyTerm(hypertension))
				.thenReturn(Sets.newLinkedHashSet(Arrays.asList("hypertension", "high blood pressure", "HBP")));

		when(semanticSearchServiceUtils.getLowerCaseTermsFromOntologyTerm(medication))
				.thenReturn(Sets.newLinkedHashSet(Arrays.asList("medication")));
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
		List<OntologyTermQueryExpansion> collect = Arrays
				.asList(new OntologyTermQueryExpansion(hypertensionMedicationOntology, ontologyService, true));

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

		Hit<OntologyTermHit> hit = Hit
				.create(OntologyTermHit.create(coldDiseaseOntologyTerm, "chronic obstructive pulmonary disease"), 0.8f);

		when(semanticSearchServiceUtils.splitIntoTerms(targetQueryTerm))
				.thenReturn(newLinkedHashSet(asList("cold", "cut", "meat", "ham")));

		when(ontologyService.getAtomicOntologyTerms(coldDiseaseOntologyTerm))
				.thenReturn(Arrays.asList(coldDiseaseOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm1))
				.thenReturn(Arrays.asList(incidentOntologyTerm, coldDiseaseOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm2))
				.thenReturn(Arrays.asList(coldTempOntologyTerm, meatOntologyTerm, cutOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm3))
				.thenReturn(Arrays.asList(coldDiseaseOntologyTerm, meatOntologyTerm, cutOntologyTerm));

		when(semanticSearchServiceUtils.getLowerCaseTermsFromOntologyTerm(coldDiseaseOntologyTerm))
				.thenReturn(Sets.newLinkedHashSet(Arrays.asList("chronic obstructive pulmonary disease", "cold")));

		when(semanticSearchServiceUtils.getLowerCaseTermsFromOntologyTerm(incidentOntologyTerm))
				.thenReturn(Sets.newLinkedHashSet(Arrays.asList("incident")));

		when(semanticSearchServiceUtils.getLowerCaseTermsFromOntologyTerm(coldTempOntologyTerm))
				.thenReturn(Sets.newHashSet("cold"));

		when(semanticSearchServiceUtils.getLowerCaseTermsFromOntologyTerm(meatOntologyTerm))
				.thenReturn(Sets.newHashSet("meat"));

		when(semanticSearchServiceUtils.getLowerCaseTermsFromOntologyTerm(cutOntologyTerm))
				.thenReturn(Sets.newHashSet("cut"));

		List<OntologyTermQueryExpansion> ontologyTermQueryExpansions = Arrays.asList(
				new OntologyTermQueryExpansion(compositeOntologyTerm2, ontologyService, false),
				new OntologyTermQueryExpansion(compositeOntologyTerm3, ontologyService, false));

		Hit<String> computeAbsoluteScoreForSourceAttribute = attributeMappingExplainServiceImpl
				.computeAbsoluteScoreForSourceAttribute(hit, ontologyTermQueryExpansions, targetQueryTerm,
						sourceAttributeDescription);

		assertEquals(computeAbsoluteScoreForSourceAttribute,
				Hit.create("chronic obstructive pulmonary disease", 0.43478f));
	}

	@Test
	public void testComputeAbsoluteScoreForSourceAttributeCaseTwo()
	{
		String sourceAttributeDescription = "Sister diagnosed with infarction under the age of 60";
		String targetQueryTerm = "Sibling diagnosed with infarction under the age of 60";

		OntologyTerm sibilingOntologyTerm = OntologyTerm.create("iri1", "sibling");
		OntologyTerm sisterOntologyTerm = OntologyTerm.create("iri2", "sister");
		OntologyTerm brotherOntologyTerm = OntologyTerm.create("iri3", "brother");
		OntologyTerm infarctionOntologyTerm = OntologyTerm.create("iri4", "infarction");
		OntologyTerm ageOntologyTerm = OntologyTerm.create("iri5", "age");

		OntologyTerm compositeOntologyTerm = OntologyTerm.and(sibilingOntologyTerm, infarctionOntologyTerm,
				ageOntologyTerm);

		Hit<OntologyTermHit> hit = Hit.create(OntologyTermHit.create(compositeOntologyTerm, "sister infarction age"),
				0.6f);

		when(semanticSearchServiceUtils.splitIntoTerms(targetQueryTerm)).thenReturn(newLinkedHashSet(
				asList("sibling", "diagnosed", "with", "infarction", "under", "the", "age", "of", "60")));

		when(semanticSearchServiceUtils.splitIntoTerms(sourceAttributeDescription)).thenReturn(newLinkedHashSet(
				asList("sister", "diagnosed", "with", "infarction", "under", "the", "age", "of", "60")));

		when(ontologyService.getAtomicOntologyTerms(sibilingOntologyTerm))
				.thenReturn(Arrays.asList(sibilingOntologyTerm));

		when(ontologyService.getAtomicOntologyTerms(compositeOntologyTerm))
				.thenReturn(Arrays.asList(sibilingOntologyTerm, infarctionOntologyTerm, ageOntologyTerm));

		when(ontologyService.getLevelThreeChildren(sibilingOntologyTerm))
				.thenReturn(Arrays.asList(brotherOntologyTerm, sisterOntologyTerm));

		when(semanticSearchServiceUtils.getLowerCaseTermsFromOntologyTerm(sibilingOntologyTerm))
				.thenReturn(Sets.newLinkedHashSet(Arrays.asList("sibling")));

		when(semanticSearchServiceUtils.getLowerCaseTermsFromOntologyTerm(infarctionOntologyTerm))
				.thenReturn(Sets.newLinkedHashSet(Arrays.asList("infarction")));

		when(semanticSearchServiceUtils.getLowerCaseTermsFromOntologyTerm(ageOntologyTerm))
				.thenReturn(Sets.newLinkedHashSet(Arrays.asList("age")));

		List<OntologyTermQueryExpansion> ontologyTermQueryExpansions = Arrays
				.asList(new OntologyTermQueryExpansion(compositeOntologyTerm, ontologyService, true));

		Hit<String> computeAbsoluteScoreForSourceAttribute = attributeMappingExplainServiceImpl
				.computeAbsoluteScoreForSourceAttribute(hit, ontologyTermQueryExpansions, targetQueryTerm,
						sourceAttributeDescription);

		assertEquals(computeAbsoluteScoreForSourceAttribute,
				Hit.create("sister infarction age diagnosed with under the of 60", 1.0f));
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