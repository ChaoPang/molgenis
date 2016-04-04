package org.molgenis.data.mapper.service.impl;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.js.magma.JsMagmaScriptRegistrator.SCRIPT_TYPE_JAVASCRIPT_MAGMA;
import static org.molgenis.script.Script.TYPE;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.molgenis.data.DataService;
import org.molgenis.data.Query;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.script.Script;
import org.molgenis.script.ScriptParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@ContextConfiguration(classes = AlgorithmTemplateServiceImplTest.Config.class)
public class AlgorithmTemplateServiceImplTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	private AlgorithmTemplateServiceImpl algorithmTemplateServiceImpl;

	@Autowired
	private DataService dataService;

	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private SemanticSearchService semanticSearchService;

	private Script script0;
	private String param0Name = "length", param1Name = "weight";
	private List<String> ontologyIds = Arrays.asList("1");
	private DefaultAttributeMetaData targetAttribute = new DefaultAttributeMetaData("Body Mass Index in kg/m2");
	private DefaultAttributeMetaData sourceAttr0 = new DefaultAttributeMetaData("height_0");
	private DefaultAttributeMetaData sourceAttr1 = new DefaultAttributeMetaData("weight_0");

	@BeforeMethod
	public void setUpBeforeMethod()
	{
		ScriptParameter param0 = new ScriptParameter(dataService);
		param0.setName(param0Name);

		ScriptParameter param1 = new ScriptParameter(dataService);
		param1.setName(param1Name);

		script0 = new Script(dataService);
		script0.setName("body mass index");
		script0.setContent(String.format("$('%s').div($('%s').pow(2));", param1, param0));
		script0.set(Script.PARAMETERS, Arrays.asList(param0, param1));

		Query q = new QueryImpl().eq(TYPE, SCRIPT_TYPE_JAVASCRIPT_MAGMA);
		when(dataService.findAll(Script.ENTITY_NAME, q, Script.class)).thenReturn(Stream.of(script0));
		when(dataService.findOne(ScriptParameter.ENTITY_NAME, param0Name)).thenReturn(param0);
		when(dataService.findOne(ScriptParameter.ENTITY_NAME, param1Name)).thenReturn(param1);
		when(ontologyService.getAllOntologiesIds()).thenReturn(ontologyIds);

		OntologyTerm heightOntologyTerm = OntologyTerm.create("iri1", "height");

		OntologyTerm weightOntologyTerm = OntologyTerm.create("iri2", "weight");

		Hit<OntologyTerm> heightOntologyTermHit = Hit.<OntologyTerm> create(heightOntologyTerm, (float) 1);
		Hit<OntologyTerm> weightOntologyTermHit = Hit.<OntologyTerm> create(weightOntologyTerm, (float) 1);
		Hit<OntologyTerm> bmiOntologyTermHit = Hit
				.<OntologyTerm> create(OntologyTerm.and(heightOntologyTerm, weightOntologyTerm), (float) 1);

		when(semanticSearchService.findTagsForAttribute(targetAttribute, ontologyIds)).thenReturn(bmiOntologyTermHit);
		when(semanticSearchService.findTagsForAttribute(sourceAttr0, ontologyIds)).thenReturn(heightOntologyTermHit);
		when(semanticSearchService.findTagsForAttribute(sourceAttr1, ontologyIds)).thenReturn(weightOntologyTermHit);
		when(semanticSearchService.findTags(param0Name, ontologyIds)).thenReturn(heightOntologyTermHit);
		when(semanticSearchService.findTags(param1Name, ontologyIds)).thenReturn(weightOntologyTermHit);
		when(ontologyService.getAtomicOntologyTerms(bmiOntologyTermHit.getResult()))
				.thenReturn(asList(heightOntologyTerm, weightOntologyTerm));
		when(ontologyService.getAtomicOntologyTerms(heightOntologyTermHit.getResult()))
				.thenReturn(asList(heightOntologyTerm));
		when(ontologyService.getAtomicOntologyTerms(weightOntologyTermHit.getResult()))
				.thenReturn(asList(weightOntologyTerm));
	}

	@Test
	public void find()
	{
		Stream<AlgorithmTemplate> templateStream = algorithmTemplateServiceImpl.find(targetAttribute,
				Arrays.asList(sourceAttr0, sourceAttr1));

		Map<String, String> model = new HashMap<>();
		model.put(param0Name, sourceAttr0.getName());
		model.put(param1Name, sourceAttr1.getName());
		AlgorithmTemplate expectedAlgorithmTemplate = new AlgorithmTemplate(script0, model);

		assertEquals(templateStream.collect(Collectors.toList()),
				Stream.of(expectedAlgorithmTemplate).collect(Collectors.toList()));
	}

	@Configuration
	public static class Config
	{
		@Bean
		public AlgorithmTemplateServiceImpl algorithmTemplateServiceImpl()
		{
			return new AlgorithmTemplateServiceImpl(dataService(), ontologyService(), semanticSearchService());
		}

		@Bean
		public DataService dataService()
		{
			return mock(DataService.class);
		}

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
	}
}
