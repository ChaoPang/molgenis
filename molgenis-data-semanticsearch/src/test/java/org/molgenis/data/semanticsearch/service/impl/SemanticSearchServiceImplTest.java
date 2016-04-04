package org.molgenis.data.semanticsearch.service.impl;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.stream.Stream.of;
import static org.elasticsearch.common.collect.Sets.newHashSet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.data.QueryRule.Operator.AND;
import static org.molgenis.data.QueryRule.Operator.DIS_MAX;
import static org.molgenis.data.QueryRule.Operator.FUZZY_MATCH;
import static org.molgenis.data.QueryRule.Operator.IN;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.DESCRIPTION;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.ENTITY_NAME;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.IDENTIFIER;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.LABEL;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.NAME;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.common.collect.Sets;
import org.mockito.Mockito;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.QueryRule;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.OntologyTagService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;

@ContextConfiguration(classes = SemanticSearchServiceImplTest.Config.class)
public class SemanticSearchServiceImplTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private SemanticSearchServiceHelper semanticSearchServiceHelper;

	@Autowired
	private DataService dataService;

	@Autowired
	private OntologyTagService ontologyTagService;

	@Autowired
	private SemanticSearchServiceImpl semanticSearchService;

	private List<String> ontologyIds;

	private OntologyTerm standingHeight;

	private OntologyTerm bodyWeight;

	private OntologyTerm hypertension;

	private OntologyTerm maternalHypertension;

	private List<OntologyTerm> ontologyTerms;

	private DefaultAttributeMetaData attribute;

	private static final String UNIT_ONTOLOGY_IRI = "http://purl.obolibrary.org/obo/uo.owl";

	@BeforeTest
	public void beforeTest()
	{
		ontologyIds = asList("1", "2");
		standingHeight = OntologyTerm.create("http://onto/height", "Standing height",
				Arrays.asList("Standing height", "length"));
		bodyWeight = OntologyTerm.create("http://onto/bmi", "Body weight",
				Arrays.asList("Body weight", "Mass in kilograms"));

		hypertension = OntologyTerm.create("http://onto/hyp", "Hypertension");
		maternalHypertension = OntologyTerm.create("http://onto/mhyp", "Maternal hypertension");
		ontologyTerms = asList(standingHeight, bodyWeight, hypertension, maternalHypertension);
		attribute = new DefaultAttributeMetaData("attr1");
	}

	@BeforeMethod
	public void init()
	{
		when(semanticSearchServiceHelper.getOtLabelAndSynonyms(standingHeight))
				.thenReturn(Sets.newHashSet("Standing height", "Standing height", "length"));

		when(semanticSearchServiceHelper.getOtLabelAndSynonyms(bodyWeight))
				.thenReturn(Sets.newHashSet("Body weight", "Body weight", "Mass in kilograms"));

		when(semanticSearchServiceHelper.getOtLabelAndSynonyms(hypertension))
				.thenReturn(Sets.newHashSet("Hypertension"));

		when(semanticSearchServiceHelper.getOtLabelAndSynonyms(maternalHypertension))
				.thenReturn(Sets.newHashSet("Maternal hypertension"));

		when(ontologyService.getAllOntologiesIds()).thenReturn(ontologyIds);

		when(ontologyService.getOntology(UNIT_ONTOLOGY_IRI)).thenReturn(null);
	}

	@Test
	public void testSearchHypertension() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("History of Hypertension");
		when(ontologyService.findOntologyTerms(ontologyIds, ImmutableSet.<String> of("history", "hypertens"), 100))
				.thenReturn(ontologyTerms);
		Hit<OntologyTerm> result = semanticSearchService.findTagsForAttribute(attribute, ontologyIds);
		assertEquals(result, null);
	}

	@Test
	public void testDistanceFrom()
	{
		Assert.assertEquals(
				semanticSearchService.distanceFrom("Hypertension", ImmutableSet.<String> of("history", "hypertens")),
				.6923, 0.0001, "String distance should be equal");
		Assert.assertEquals(
				semanticSearchService.distanceFrom("Maternal Hypertension",
						ImmutableSet.<String> of("history", "hypertens")),
				.5454, 0.0001, "String distance should be equal");
		;
	}

	@Test
	public void testSearchDescription() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("Standing height in meters.");
		when(ontologyService.findOntologyTerms(ontologyIds, ImmutableSet.<String> of("standing", "height", "meters"),
				20)).thenReturn(ontologyTerms);
		Hit<OntologyTerm> result = semanticSearchService.findTagsForAttribute(attribute, ontologyIds);
		assertEquals(result, Hit.<OntologyTerm> create(standingHeight, 0.81250f));
	}

	@Test
	public void testSearchLabel() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("Standing height (m.)");

		when(ontologyService.findOntologyTerms(ontologyIds, ImmutableSet.<String> of("standing", "height", "m"), 20))
				.thenReturn(ontologyTerms);
		Hit<OntologyTerm> result = semanticSearchService.findTagsForAttribute(attribute, ontologyIds);
		assertEquals(result, Hit.<OntologyTerm> create(standingHeight, 0.92857f));
	}

	@Test
	public void testFindAttributes()
	{
		DefaultAttributeMetaData targetHeight = new DefaultAttributeMetaData("targetHeight");
		targetHeight.setLabel("standing height");
		DefaultAttributeMetaData targetWeight = new DefaultAttributeMetaData("targetWeight");
		targetWeight.setLabel("body weight");
		DefaultEntityMetaData targetEntityMetaData = new DefaultEntityMetaData("targetEntityMetaData");

		DefaultEntityMetaData sourceEntityMetaData = new DefaultEntityMetaData("sourceEntityMetaData");
		AttributeMetaData sourceAttributeHeight = new DefaultAttributeMetaData("height_0");
		AttributeMetaData sourceAttributeWeight = new DefaultAttributeMetaData("weight_0");
		sourceEntityMetaData.addAttributeMetaData(sourceAttributeHeight);
		sourceEntityMetaData.addAttributeMetaData(sourceAttributeWeight);

		// Mock the id's of the attribute entities that should be searched
		List<String> attributeIdentifiers = Arrays.asList("1", "2");

		// Mock the result of searching for the attribute height
		QueryRule disMaxQueryRuleForHeight = new QueryRule(Arrays.asList(
				new QueryRule(LABEL, FUZZY_MATCH, "targetHeight"), new QueryRule(LABEL, FUZZY_MATCH, "height"),
				new QueryRule(LABEL, FUZZY_MATCH, "standing height"), new QueryRule(LABEL, FUZZY_MATCH, "length")));
		disMaxQueryRuleForHeight.setOperator(DIS_MAX);

		MapEntity entityHeight = new MapEntity(
				of(NAME, "height_0", LABEL, "height", DESCRIPTION, "this is a height measurement in m!"));

		// Mock the result of searching for the attribute weight
		// Mock the result of searching for the attribute height
		QueryRule disMaxQueryRuleForWeight = new QueryRule(
				Arrays.asList(new QueryRule(LABEL, FUZZY_MATCH, "targetWeight"),
						new QueryRule(LABEL, FUZZY_MATCH, "weight"), new QueryRule(LABEL, FUZZY_MATCH, "body weight")));
		disMaxQueryRuleForWeight.setOperator(DIS_MAX);

		MapEntity entityWeight = new MapEntity(
				of(NAME, "weight_0", LABEL, "weight", DESCRIPTION, "this is a weight measurement in kg!"));

		// Case 1: mock the createDisMaxQueryRule method for the attribute Height
		when(semanticSearchServiceHelper.getAttributeIdentifiers(sourceEntityMetaData))
				.thenReturn(attributeIdentifiers);
		when(semanticSearchServiceHelper.createLexicalSearchQueryTerms(targetHeight, emptySet()))
				.thenReturn(newHashSet("targetHeight", "standing height"));
		when(ontologyTagService.getTagsForAttribute(targetEntityMetaData, targetHeight))
				.thenReturn(LinkedListMultimap.create());
		when(ontologyService.findOntologyTerms(ontologyIds, newHashSet("standing", "height"), 20))
				.thenReturn(asList(standingHeight));
		when(semanticSearchServiceHelper.createDisMaxQueryRuleForAttribute(
				newHashSet("targetHeight", "standing height"), asList(standingHeight)))
						.thenReturn(disMaxQueryRuleForHeight);
		when(dataService.findAll(ENTITY_NAME, new QueryImpl(asList(new QueryRule(IDENTIFIER, IN, attributeIdentifiers),
				new QueryRule(AND), disMaxQueryRuleForHeight)).pageSize(100))).thenReturn(of(entityHeight));

		when(semanticSearchServiceHelper.entityToAttributeMetaData(entityHeight, sourceEntityMetaData))
				.thenReturn(sourceAttributeHeight);

		assertEquals(semanticSearchService.findAttributes(targetHeight, targetEntityMetaData, sourceEntityMetaData,
				emptySet()), asList(sourceAttributeHeight));

		// Case 2: mock the createDisMaxQueryRule method for the attribute Weight
		when(semanticSearchServiceHelper.getAttributeIdentifiers(sourceEntityMetaData))
				.thenReturn(attributeIdentifiers);
		when(semanticSearchServiceHelper.createLexicalSearchQueryTerms(targetWeight, emptySet()))
				.thenReturn(newHashSet("targetWeight", "body weight"));
		when(ontologyTagService.getTagsForAttribute(targetEntityMetaData, targetWeight))
				.thenReturn(LinkedListMultimap.create());
		when(ontologyService.findOntologyTerms(ontologyIds, newHashSet("body", "weight"), 20))
				.thenReturn(asList(bodyWeight));
		when(semanticSearchServiceHelper.createDisMaxQueryRuleForAttribute(newHashSet("targetWeight", "body weight"),
				Arrays.asList(bodyWeight))).thenReturn(disMaxQueryRuleForWeight);
		when(dataService.findAll(ENTITY_NAME, new QueryImpl(asList(new QueryRule(IDENTIFIER, IN, attributeIdentifiers),
				new QueryRule(AND), disMaxQueryRuleForWeight)).pageSize(100))).thenReturn(of(entityWeight));
		when(semanticSearchServiceHelper.entityToAttributeMetaData(entityWeight, sourceEntityMetaData))
				.thenReturn(sourceAttributeWeight);

		assertEquals(semanticSearchService.findAttributes(targetWeight, targetEntityMetaData, sourceEntityMetaData,
				emptySet()), asList(sourceAttributeWeight));
	}

	@Test
	public void testSearchUnicode2()
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("Standing height (Ångstrøm)");

		when(ontologyService.findOntologyTerms(ontologyIds, ImmutableSet.of("standing", "height", "ångstrøm"), 20))
				.thenReturn(ontologyTerms);
		Hit<OntologyTerm> result = semanticSearchService.findTagsForAttribute(attribute, ontologyIds);
		assertEquals(result, Hit.<OntologyTerm> create(standingHeight, 0.76471f));
	}

	@Test
	public void testSearchUnicode() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("/əˈnædrəməs/");

		when(ontologyService.findOntologyTerms(ontologyIds, ImmutableSet.of("əˈnædrəməs"), 20))
				.thenReturn(ontologyTerms);
		Hit<OntologyTerm> result = semanticSearchService.findTagsForAttribute(attribute, ontologyIds);
		assertEquals(result, null);
	}

	@Test
	public void testSearchMultipleTags() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("Body mass index");

		when(ontologyService.findOntologyTerms(ontologyIds, ImmutableSet.of("body", "mass", "index"), 20))
				.thenReturn(ontologyTerms);
		Hit<OntologyTerm> result = semanticSearchService.findTagsForAttribute(attribute, ontologyIds);
		assertEquals(result, null);
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
			return new SemanticSearchServiceImpl(dataService(), ontologyService(), ontologyTagService(),
					semanticSearchServiceHelper());
		}

		@Bean
		OntologyTagService ontologyTagService()
		{
			return mock(OntologyTagService.class);
		}

		@Bean
		DataService dataService()
		{
			return mock(DataService.class);
		}

		@Bean
		SemanticSearchServiceHelper semanticSearchServiceHelper()
		{
			return mock(SemanticSearchServiceHelper.class);
		}
	}
}
