package org.molgenis.data.mapper.algorithmgenerator.service.impl;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.mapper.algorithmgenerator.bean.GeneratedAlgorithm;
import org.molgenis.data.mapper.algorithmgenerator.service.AlgorithmGeneratorService;
import org.molgenis.data.mapper.mapping.model.AttributeMapping.AlgorithmState;
import org.molgenis.data.mapper.service.UnitResolver;
import org.molgenis.data.mapper.service.impl.AlgorithmTemplate;
import org.molgenis.data.mapper.service.impl.AlgorithmTemplateService;
import org.molgenis.data.mapper.service.impl.UnitResolverImpl;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.bean.AttributeMatchExplanation;
import org.molgenis.data.semanticsearch.explain.service.ExplainMappingService;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.script.Script;
import org.molgenis.script.ScriptParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ContextConfiguration(classes = AlgorithmGeneratorServiceImplTest.Config.class)
public class AlgorithmGeneratorServiceImplTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	OntologyService ontologyService;

	@Autowired
	DataService dataService;

	@Autowired
	AlgorithmTemplateService algorithmTemplateService;

	@Autowired
	AlgorithmGeneratorService algorithmGeneratorService;

	@Autowired
	ExplainMappingService attributeMappingExplainService;

	@BeforeMethod
	public void setUpBeforeMethod()
	{
		when(ontologyService.getOntology("http://purl.obolibrary.org/obo/uo.owl"))
				.thenReturn(Ontology.create("1", "http://purl.obolibrary.org/obo/uo.owl", "unit ontology"));
	}

	@Test
	public void testGenerateTemplateBasedAlgorithm()
	{
		DefaultEntityMetaData targetEntityMetaData = new DefaultEntityMetaData("target");
		DefaultAttributeMetaData targetBMIAttribute = new DefaultAttributeMetaData("targetHeight");
		targetBMIAttribute.setLabel("BMI kg/m²");
		targetBMIAttribute.setDataType(MolgenisFieldTypes.DECIMAL);
		targetEntityMetaData.addAttributeMetaData(targetBMIAttribute);

		DefaultEntityMetaData sourceEntityMetaData = new DefaultEntityMetaData("source");
		DefaultAttributeMetaData heightSourceAttribute = new DefaultAttributeMetaData("sourceHeight");
		heightSourceAttribute.setDataType(MolgenisFieldTypes.DECIMAL);
		heightSourceAttribute.setLabel("body length in cm");

		DefaultAttributeMetaData weightSourceAttribute = new DefaultAttributeMetaData("sourceWeight");
		weightSourceAttribute.setDataType(MolgenisFieldTypes.DECIMAL);
		weightSourceAttribute.setLabel("weight in kg");

		sourceEntityMetaData.addAttributeMetaData(heightSourceAttribute);
		sourceEntityMetaData.addAttributeMetaData(weightSourceAttribute);
		List<AttributeMetaData> sourceAttributes = Arrays.asList(heightSourceAttribute, weightSourceAttribute);

		Script script = mock(Script.class);
		ScriptParameter heightParameter = mock(ScriptParameter.class);
		when(heightParameter.getName()).thenReturn("height");
		ScriptParameter weightParameter = mock(ScriptParameter.class);
		when(weightParameter.getName()).thenReturn("weight");
		when(script.getParameters()).thenReturn(Arrays.asList(heightParameter, weightParameter));
		when(script.getContent()).thenReturn("$('weight').div($('height').pow(2)).value()");

		AlgorithmTemplate template = new AlgorithmTemplate(script,
				ImmutableMap.of("height", "sourceHeight", "weight", "sourceWeight"));

		when(algorithmTemplateService.find(targetBMIAttribute, sourceAttributes)).thenReturn(Stream.of(template));

		ExplainedAttributeMetaData explainedHeightAttribute = ExplainedAttributeMetaData.create(heightSourceAttribute,
				AttributeMatchExplanation.create("height", "height", "height", 1.0f), true);

		ExplainedAttributeMetaData explainedWeightAttribute = ExplainedAttributeMetaData.create(weightSourceAttribute,
				AttributeMatchExplanation.create("weight", "weight", "weight", 1.0f), true);

		when(attributeMappingExplainService.explainAttributeMapping(targetBMIAttribute,
				Sets.newLinkedHashSet(asList("height", "weight")), heightSourceAttribute, targetEntityMetaData))
						.thenReturn(explainedHeightAttribute);

		when(attributeMappingExplainService.explainAttributeMapping(targetBMIAttribute,
				Sets.newLinkedHashSet(asList("height", "weight")), weightSourceAttribute, targetEntityMetaData))
						.thenReturn(explainedWeightAttribute);

		GeneratedAlgorithm generate = algorithmGeneratorService.autoGenerate(targetBMIAttribute, sourceAttributes,
				targetEntityMetaData, sourceEntityMetaData);

		assertEquals(generate.getAlgorithm(), "$('sourceWeight').div($('sourceHeight').div(100.0).pow(2)).value()");
		assertEquals(generate.getAlgorithmState(), AlgorithmState.GENERATED_HIGH);
	}

	@Test
	public void testConvertUnitsAlgorithm()
	{
		DefaultEntityMetaData targetEntityMetaData = new DefaultEntityMetaData("target");
		DefaultAttributeMetaData targetAttribute = new DefaultAttributeMetaData("targetHeight");
		targetAttribute.setLabel("height in m");
		targetAttribute.setDataType(MolgenisFieldTypes.DECIMAL);
		targetEntityMetaData.addAttributeMetaData(targetAttribute);

		DefaultEntityMetaData sourceEntityMetaData = new DefaultEntityMetaData("source");
		DefaultAttributeMetaData sourceAttribute = new DefaultAttributeMetaData("sourceHeight");
		sourceAttribute.setDataType(MolgenisFieldTypes.DECIMAL);
		sourceAttribute.setLabel("body length in cm");
		sourceEntityMetaData.addAttributeMetaData(sourceAttribute);

		when(algorithmTemplateService.find(targetAttribute, Arrays.asList(sourceAttribute))).thenReturn(Stream.empty());

		String actualAlgorithm = algorithmGeneratorService.generate(targetAttribute,
				Lists.newArrayList(sourceAttribute), targetEntityMetaData, sourceEntityMetaData);

		String expectedAlgorithm = "$('sourceHeight').unit('cm').toUnit('m').value();";

		Assert.assertEquals(actualAlgorithm, expectedAlgorithm);
	}

	@Configuration
	public static class Config
	{
		@Bean
		public DataService dataService()
		{
			return mock(DataService.class);
		}

		@Bean
		public UnitResolver unitResolver()
		{
			return new UnitResolverImpl(ontologyService());
		}

		@Bean
		public OntologyService ontologyService()
		{
			return mock(OntologyService.class);
		}

		@Bean
		public AlgorithmTemplateService algorithmTemplateService()
		{
			return mock(AlgorithmTemplateService.class);
		}

		@Bean
		public ExplainMappingService attributeMappingExplainService()
		{
			return mock(ExplainMappingService.class);
		}

		@Bean
		public AlgorithmGeneratorService algorithmGeneratorService()
		{
			return new AlgorithmGeneratorServiceImpl(dataService(), attributeMappingExplainService(), unitResolver(),
					algorithmTemplateService());
		}
	}
}
