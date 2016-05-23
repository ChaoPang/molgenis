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
import static org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceImpl.MAX_NUMBER_ATTRIBTUES;
import static org.molgenis.ontology.core.model.Ontology.create;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.common.collect.Sets;
import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.QueryRule;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParameter;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParameter;
import org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntity;
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
	private DataService dataService;

	@Autowired
	private SemanticSearchServiceImpl semanticSearchService;

	@Autowired
	private TagGroupGenerator tagGroupGenerator;

	@Autowired
	private QueryExpansionService queryExpansionService;

	private List<String> ontologyIds;
	private OntologyTerm standingHeight;
	private OntologyTerm bodyWeight;
	private List<Ontology> ontologies;

	@BeforeTest
	public void beforeTest()
	{
		standingHeight = OntologyTerm.create("http://onto/height", "Standing height",
				Arrays.asList("Standing height", "length"));
		bodyWeight = OntologyTerm.create("http://onto/bmi", "Body weight",
				Arrays.asList("Body weight", "Mass in kilograms"));
		ontologies = asList(create("1", "ontology iri 1", "ontoloyg 1"), create("2", "ontology iri 2", "ontoloyg 2"));
		ontologyIds = ontologies.stream().map(Ontology::getId).collect(Collectors.toList());
	}

	@BeforeMethod
	public void init()
	{
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

		MapEntity targetHeightEntity = new MapEntity();
		targetHeightEntity.set(AttributeMetaDataMetaData.IDENTIFIER, "1");
		targetHeightEntity.set(AttributeMetaDataMetaData.NAME, "targetHeight");
		targetHeightEntity.set(AttributeMetaDataMetaData.DATA_TYPE, MolgenisFieldTypes.DECIMAL);
		targetHeightEntity.set(AttributeMetaDataMetaData.LABEL, "standing height");

		MapEntity targetWeightEntity = new MapEntity();
		targetWeightEntity.set(AttributeMetaDataMetaData.IDENTIFIER, "2");
		targetWeightEntity.set(AttributeMetaDataMetaData.NAME, "targetWeight");
		targetWeightEntity.set(AttributeMetaDataMetaData.DATA_TYPE, MolgenisFieldTypes.DECIMAL);
		targetWeightEntity.set(AttributeMetaDataMetaData.LABEL, "body weight");

		DefaultEntityMetaData sourceEntityMetaData = new DefaultEntityMetaData("sourceEntityMetaData");
		AttributeMetaData sourceAttributeHeight = new DefaultAttributeMetaData("height_0");
		AttributeMetaData sourceAttributeWeight = new DefaultAttributeMetaData("weight_0");
		sourceEntityMetaData.addAttributeMetaData(sourceAttributeHeight);
		sourceEntityMetaData.addAttributeMetaData(sourceAttributeWeight);

		MapEntity sourceEntityMetaDataEntity = new MapEntity();
		sourceEntityMetaDataEntity.set(EntityMetaDataMetaData.FULL_NAME, "sourceEntityMetaData");
		sourceEntityMetaDataEntity.set(EntityMetaDataMetaData.ATTRIBUTES,
				Arrays.asList(targetHeightEntity, targetWeightEntity));

		OntologyTermHit standingHeightHit = OntologyTermHit.create(standingHeight, "standing height", "standing height",
				1.0f);

		OntologyTermHit bodyWeightHit = OntologyTermHit.create(bodyWeight, "body weight", "body weight", 1.0f);

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

		when(dataService.findOne(EntityMetaDataMetaData.ENTITY_NAME,
				new QueryImpl().eq(EntityMetaDataMetaData.FULL_NAME, sourceEntityMetaData.getName())))
						.thenReturn(sourceEntityMetaDataEntity);

		when(tagGroupGenerator.findTagGroups(targetHeight, targetEntityMetaData, emptySet(), ontologyIds))
				.thenReturn(Arrays.asList(standingHeightHit));

		when(queryExpansionService.expand(Sets.newHashSet(targetHeight.getLabel()), Arrays.asList(standingHeightHit),
				QueryExpansionParameter.create(true, true))).thenReturn(disMaxQueryRuleForHeight);

		when(dataService
				.findAll(ENTITY_NAME,
						new QueryImpl(asList(new QueryRule(IDENTIFIER, IN, attributeIdentifiers), new QueryRule(AND),
								disMaxQueryRuleForHeight)).pageSize(MAX_NUMBER_ATTRIBTUES)))
										.thenReturn(of(entityHeight));

		assertEquals(
				semanticSearchService.findAttributes(SemanticSearchParameter.create(targetHeight,
						Collections.emptySet(), targetEntityMetaData, sourceEntityMetaData, true, true, true)),
				asList(sourceAttributeHeight));

		// Case 2: mock the createDisMaxQueryRule method for the attribute Weight
		when(tagGroupGenerator.findTagGroups(targetWeight, targetEntityMetaData, emptySet(), ontologyIds))
				.thenReturn(asList(bodyWeightHit));

		when(queryExpansionService.expand(newHashSet(targetWeight.getLabel()), asList(bodyWeightHit),
				QueryExpansionParameter.create(true, true))).thenReturn(disMaxQueryRuleForWeight);

		when(dataService
				.findAll(ENTITY_NAME,
						new QueryImpl(asList(new QueryRule(IDENTIFIER, IN, attributeIdentifiers), new QueryRule(AND),
								disMaxQueryRuleForWeight)).pageSize(MAX_NUMBER_ATTRIBTUES)))
										.thenReturn(of(entityWeight));

		assertEquals(
				semanticSearchService.findAttributes(SemanticSearchParameter.create(targetWeight,
						Collections.emptySet(), targetEntityMetaData, sourceEntityMetaData, true, true, true)),
				asList(sourceAttributeWeight));
	}

	@Test
	public void testGetAttributeIdentifiers()
	{
		EntityMetaData sourceEntityMetaData = new DefaultEntityMetaData("sourceEntityMetaData");
		Entity entityMetaDataEntity = mock(DefaultEntity.class);

		when(dataService.findOne(EntityMetaDataMetaData.ENTITY_NAME,
				new QueryImpl().eq(EntityMetaDataMetaData.FULL_NAME, sourceEntityMetaData.getName())))
						.thenReturn(entityMetaDataEntity);

		Entity attributeEntity1 = new MapEntity();
		attributeEntity1.set(AttributeMetaDataMetaData.IDENTIFIER, "1");
		attributeEntity1.set(AttributeMetaDataMetaData.DATA_TYPE, "string");
		Entity attributeEntity2 = new MapEntity();
		attributeEntity2.set(AttributeMetaDataMetaData.IDENTIFIER, "2");
		attributeEntity2.set(AttributeMetaDataMetaData.DATA_TYPE, "string");
		when(entityMetaDataEntity.getEntities(EntityMetaDataMetaData.ATTRIBUTES))
				.thenReturn(Arrays.<Entity> asList(attributeEntity1, attributeEntity2));

		List<String> expactedAttributeIdentifiers = Arrays.<String> asList("1", "2");
		assertEquals(semanticSearchService.getAttributeIdentifiers(sourceEntityMetaData), expactedAttributeIdentifiers);
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
			return new SemanticSearchServiceImpl(dataService(), ontologyService(), tagGroupGenerator(),
					queryExpansionService(), attributeMappingExplainService());
		}

		@Bean
		TagGroupGenerator tagGroupGenerator()
		{
			return mock(TagGroupGenerator.class);
		}

		@Bean
		QueryExpansionService queryExpansionService()
		{
			return mock(QueryExpansionService.class);
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
