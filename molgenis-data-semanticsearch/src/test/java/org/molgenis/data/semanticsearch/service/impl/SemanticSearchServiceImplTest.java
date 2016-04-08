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
import static org.molgenis.ontology.core.model.Ontology.create;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.common.collect.Sets;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.QueryRule;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
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
	private SemanticSearchServiceImpl semanticSearchService;

	private List<String> ontologyIds;
	private OntologyTerm standingHeight;
	private OntologyTerm bodyWeight;
	private List<Ontology> ontologies;
	private DefaultAttributeMetaData attribute;

	@BeforeTest
	public void beforeTest()
	{
		standingHeight = OntologyTerm.create("http://onto/height", "Standing height",
				Arrays.asList("Standing height", "length"));
		bodyWeight = OntologyTerm.create("http://onto/bmi", "Body weight",
				Arrays.asList("Body weight", "Mass in kilograms"));
		ontologies = asList(create("1", "ontology iri 1", "ontoloyg 1"), create("2", "ontology iri 2", "ontoloyg 2"));
		ontologyIds = ontologies.stream().map(Ontology::getId).collect(Collectors.toList());
		attribute = new DefaultAttributeMetaData("attr1").setLabel("attribute 1");
	}

	@BeforeMethod
	public void init()
	{
		when(semanticSearchServiceUtils.splitIntoTerms(attribute.getLabel())).thenReturn(newHashSet("attribute", "1"));
		when(ontologyService.getOntologies()).thenReturn(ontologies);
		when(ontologyService.getAllOntologiesIds()).thenReturn(ontologyIds);
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

		MapEntity entityHeight = new MapEntity(
				of(NAME, "height_0", LABEL, "height", DESCRIPTION, "this is a height measurement in m!"));

		MapEntity entityWeight = new MapEntity(
				of(NAME, "weight_0", LABEL, "weight", DESCRIPTION, "this is a weight measurement in kg!"));

		// Mock the id's of the attribute entities that should be searched
		List<String> attributeIdentifiers = Arrays.asList("1", "2");

		// Mock the disMaxJunc query for the attribute height
		QueryRule queryRule1 = new QueryRule(Arrays.asList(new QueryRule(LABEL, FUZZY_MATCH, "standing height")));
		queryRule1.setOperator(DIS_MAX);
		QueryRule queryRule2 = new QueryRule(Arrays.asList(new QueryRule(LABEL, FUZZY_MATCH, "standing height"),
				new QueryRule(LABEL, FUZZY_MATCH, "length")));
		queryRule2.setOperator(DIS_MAX);
		QueryRule disMaxQueryRuleForHeight = new QueryRule(Arrays.asList(queryRule1, queryRule2));
		disMaxQueryRuleForHeight.setOperator(DIS_MAX);

		// Mock the disMaxJunc query for the attribute weight
		QueryRule queryRule3 = new QueryRule(Arrays.asList(new QueryRule(LABEL, FUZZY_MATCH, "body weight")));
		queryRule3.setOperator(DIS_MAX);
		QueryRule queryRule4 = new QueryRule(Arrays.asList(new QueryRule(LABEL, FUZZY_MATCH, "body weight"),
				new QueryRule(LABEL, FUZZY_MATCH, "mass in kilograms")));
		queryRule4.setOperator(DIS_MAX);
		QueryRule disMaxQueryRuleForWeight = new QueryRule(Arrays.asList(queryRule3, queryRule4));
		disMaxQueryRuleForWeight.setOperator(DIS_MAX);

		when(semanticSearchServiceUtils.getQueryTermsFromAttribute(targetHeight, emptySet()))
				.thenReturn(Sets.newHashSet(targetHeight.getLabel()));

		when(semanticSearchServiceUtils.findOntologyTermsForAttr(targetHeight, targetEntityMetaData, emptySet(),
				ontologyIds)).thenReturn(Arrays.asList(standingHeight));

		when(semanticSearchServiceUtils.createDisMaxQueryRule(Sets.newHashSet(targetHeight.getLabel()),
				Arrays.asList(standingHeight), true)).thenReturn(disMaxQueryRuleForHeight);

		when(semanticSearchServiceUtils.getAttributeIdentifiers(sourceEntityMetaData)).thenReturn(attributeIdentifiers);

		when(dataService.findAll(ENTITY_NAME, new QueryImpl(asList(new QueryRule(IDENTIFIER, IN, attributeIdentifiers),
				new QueryRule(AND), disMaxQueryRuleForHeight)).pageSize(100))).thenReturn(of(entityHeight));

		when(semanticSearchServiceUtils.entityToAttributeMetaData(entityHeight, sourceEntityMetaData))
				.thenReturn(sourceAttributeHeight);

		assertEquals(semanticSearchService.findAttributes(targetHeight, targetEntityMetaData, sourceEntityMetaData,
				emptySet(), true), asList(sourceAttributeHeight));

		// Case 2: mock the createDisMaxQueryRule method for the attribute Weight
		when(semanticSearchServiceUtils.getQueryTermsFromAttribute(targetWeight, emptySet()))
				.thenReturn(newHashSet(targetWeight.getLabel()));

		when(semanticSearchServiceUtils.findOntologyTermsForAttr(targetWeight, targetEntityMetaData, emptySet(),
				ontologyIds)).thenReturn(asList(bodyWeight));

		when(semanticSearchServiceUtils.createDisMaxQueryRule(newHashSet(targetWeight.getLabel()),
				Arrays.asList(bodyWeight), true)).thenReturn(disMaxQueryRuleForWeight);

		when(dataService.findAll(ENTITY_NAME, new QueryImpl(asList(new QueryRule(IDENTIFIER, IN, attributeIdentifiers),
				new QueryRule(AND), disMaxQueryRuleForWeight)).pageSize(100))).thenReturn(of(entityWeight));

		when(semanticSearchServiceUtils.entityToAttributeMetaData(entityWeight, sourceEntityMetaData))
				.thenReturn(sourceAttributeWeight);

		assertEquals(semanticSearchService.findAttributes(targetWeight, targetEntityMetaData, sourceEntityMetaData,
				emptySet(), true), asList(sourceAttributeWeight));
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
