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
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.OntologyTagService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
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
	private SemanticSearchServiceUtils semanticSearchServiceUtils;

	@Autowired
	private DataService dataService;

	@Autowired
	private OntologyTagService ontologyTagService;

	@Autowired
	private SemanticSearchServiceImpl semanticSearchService;

	private List<String> ontologyIds;

	private OntologyTerm standingHeight;

	private OntologyTerm bodyWeight;

	private List<OntologyTerm> ontologyTerms;

	private DefaultAttributeMetaData attribute;

	@BeforeTest
	public void beforeTest()
	{
		ontologyIds = asList("1", "2");

		attribute = new DefaultAttributeMetaData("attr1").setLabel("attribute 1");
	}

	@BeforeMethod
	public void init()
	{
		when(semanticSearchServiceUtils.splitIntoTerms(attribute.getLabel()))
				.thenReturn(Sets.newHashSet("attribute", "1"));

		when(ontologyService.getOntologies()).thenReturn(asList(Ontology.create("1", "ontology iri 1", "ontoloyg 1"),
				Ontology.create("2", "ontology iri 2", "ontoloyg 2")));

		when(semanticSearchServiceUtils.findOntologyTermCombination(Sets.newHashSet("attribute", "1"),
				Arrays.asList("1", "2")));
	}

	@Test
	public void testSearchLabel() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("Standing height (m.)");

		when(ontologyService.findOntologyTerms(ontologyIds, ImmutableSet.<String> of("standing", "height", "m"), 20))
				.thenReturn(ontologyTerms);
		Hit<OntologyTerm> result = semanticSearchService.findTagForAttr(attribute, ontologyIds);
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
		when(semanticSearchServiceUtils.getAttributeIdentifiers(sourceEntityMetaData)).thenReturn(attributeIdentifiers);
		when(semanticSearchServiceUtils.getQueryTermsFromAttribute(targetHeight, emptySet()))
				.thenReturn(newHashSet("targetHeight", "standing height"));
		when(ontologyTagService.getTagsForAttribute(targetEntityMetaData, targetHeight))
				.thenReturn(LinkedListMultimap.create());
		when(ontologyService.findOntologyTerms(ontologyIds, newHashSet("standing", "height"), 20))
				.thenReturn(asList(standingHeight));
		when(semanticSearchServiceUtils.createDisMaxQueryRule(newHashSet("targetHeight", "standing height"),
				asList(standingHeight), true)).thenReturn(disMaxQueryRuleForHeight);
		when(dataService.findAll(ENTITY_NAME, new QueryImpl(asList(new QueryRule(IDENTIFIER, IN, attributeIdentifiers),
				new QueryRule(AND), disMaxQueryRuleForHeight)).pageSize(100))).thenReturn(of(entityHeight));

		when(semanticSearchServiceUtils.entityToAttributeMetaData(entityHeight, sourceEntityMetaData))
				.thenReturn(sourceAttributeHeight);

		assertEquals(semanticSearchService.findAttributes(targetHeight, targetEntityMetaData, sourceEntityMetaData,
				emptySet()), asList(sourceAttributeHeight));

		// Case 2: mock the createDisMaxQueryRule method for the attribute Weight
		when(semanticSearchServiceUtils.getAttributeIdentifiers(sourceEntityMetaData)).thenReturn(attributeIdentifiers);
		when(semanticSearchServiceUtils.getQueryTermsFromAttribute(targetWeight, emptySet()))
				.thenReturn(newHashSet("targetWeight", "body weight"));
		when(ontologyTagService.getTagsForAttribute(targetEntityMetaData, targetWeight))
				.thenReturn(LinkedListMultimap.create());
		when(ontologyService.findOntologyTerms(ontologyIds, newHashSet("body", "weight"), 20))
				.thenReturn(asList(bodyWeight));
		when(semanticSearchServiceUtils.createDisMaxQueryRule(newHashSet("targetWeight", "body weight"),
				Arrays.asList(bodyWeight), true)).thenReturn(disMaxQueryRuleForWeight);
		when(dataService.findAll(ENTITY_NAME, new QueryImpl(asList(new QueryRule(IDENTIFIER, IN, attributeIdentifiers),
				new QueryRule(AND), disMaxQueryRuleForWeight)).pageSize(100))).thenReturn(of(entityWeight));
		when(semanticSearchServiceUtils.entityToAttributeMetaData(entityWeight, sourceEntityMetaData))
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
		Hit<OntologyTerm> result = semanticSearchService.findTagForAttr(attribute, ontologyIds);
		assertEquals(result, Hit.<OntologyTerm> create(standingHeight, 0.76471f));
	}

	@Test
	public void testSearchUnicode() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("/əˈnædrəməs/");

		when(ontologyService.findOntologyTerms(ontologyIds, ImmutableSet.of("əˈnædrəməs"), 20))
				.thenReturn(ontologyTerms);
		Hit<OntologyTerm> result = semanticSearchService.findTagForAttr(attribute, ontologyIds);
		assertEquals(result, null);
	}

	@Test
	public void testSearchMultipleTags() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("Body mass index");

		when(ontologyService.findOntologyTerms(ontologyIds, ImmutableSet.of("body", "mass", "index"), 20))
				.thenReturn(ontologyTerms);
		Hit<OntologyTerm> result = semanticSearchService.findTagForAttr(attribute, ontologyIds);
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
			return new SemanticSearchServiceImpl(dataService(), ontologyService(), semanticSearchServiceUtils(),
					attributeMappingExplainService());
		}

		@Bean
		AttributeMappingExplainService attributeMappingExplainService()
		{
			return mock(AttributeMappingExplainService.class);
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
		SemanticSearchServiceUtils semanticSearchServiceUtils()
		{
			return mock(SemanticSearchServiceUtils.class);
		}
	}
}
