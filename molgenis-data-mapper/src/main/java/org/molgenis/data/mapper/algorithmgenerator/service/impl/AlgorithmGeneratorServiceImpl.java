package org.molgenis.data.mapper.algorithmgenerator.service.impl;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.molgenis.data.mapper.mapping.model.AttributeMapping.AlgorithmState.GENERATED_HIGH;
import static org.molgenis.data.mapper.mapping.model.AttributeMapping.AlgorithmState.GENERATED_LOW;
import static utils.AlgorithmGeneratorHelper.extractSourceAttributesFromAlgorithm;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.mapper.algorithmgenerator.bean.GeneratedAlgorithm;
import org.molgenis.data.mapper.algorithmgenerator.generator.AlgorithmGenerator;
import org.molgenis.data.mapper.algorithmgenerator.generator.NumericAlgorithmGenerator;
import org.molgenis.data.mapper.algorithmgenerator.generator.OneToManyCategoryAlgorithmGenerator;
import org.molgenis.data.mapper.algorithmgenerator.generator.OneToOneCategoryAlgorithmGenerator;
import org.molgenis.data.mapper.algorithmgenerator.service.AlgorithmGeneratorService;
import org.molgenis.data.mapper.mapping.model.AttributeMapping.AlgorithmState;
import org.molgenis.data.mapper.service.UnitResolver;
import org.molgenis.data.mapper.service.impl.AlgorithmTemplate;
import org.molgenis.data.mapper.service.impl.AlgorithmTemplateService;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParameter;
import org.molgenis.script.ScriptParameter;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.Objects.requireNonNull;

import utils.MagmaUnitConverter;

public class AlgorithmGeneratorServiceImpl implements AlgorithmGeneratorService
{
	private final List<AlgorithmGenerator> generators;
	private final AlgorithmTemplateService algorithmTemplateService;
	private final AttributeMappingExplainService attributeMappingExplainService;
	private final UnitResolver unitResolver;
	private final MagmaUnitConverter magmaUnitConverter = new MagmaUnitConverter();

	@Autowired
	public AlgorithmGeneratorServiceImpl(DataService dataService,
			AttributeMappingExplainService attributeMappingExplainService, UnitResolver unitResolver,
			AlgorithmTemplateService algorithmTemplateService)
	{
		this.algorithmTemplateService = requireNonNull(algorithmTemplateService);
		this.attributeMappingExplainService = requireNonNull(attributeMappingExplainService);
		this.unitResolver = requireNonNull(unitResolver);
		this.generators = Arrays.asList(new OneToOneCategoryAlgorithmGenerator(dataService),
				new OneToManyCategoryAlgorithmGenerator(dataService), new NumericAlgorithmGenerator(unitResolver));
	}

	@Override
	public String generate(AttributeMetaData targetAttribute, List<AttributeMetaData> sourceAttributes,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData)
	{
		String algorithm = runAlgorithmTemplate(targetAttribute, sourceAttributes, targetEntityMetaData,
				sourceEntityMetaData, null);
		if (StringUtils.isBlank(algorithm))
		{
			algorithm = runAlgorithmGenerator(targetAttribute, sourceAttributes, targetEntityMetaData,
					sourceEntityMetaData);
		}
		return algorithm;
	}

	@Override
	public GeneratedAlgorithm autoGenerate(SemanticSearchParameter semanticSearchParameters,
			List<AttributeMetaData> sourceAttributes)
	{
		String algorithm = StringUtils.EMPTY;
		AlgorithmState algorithmState = null;
		Set<AttributeMetaData> mappedSourceAttributes = null;
		Double score = 0.0d;
		Set<String> additionalQueryTerms = new LinkedHashSet<>();

		if (sourceAttributes.size() > 0 && semanticSearchParameters.getSourceEntityMetaDatas().size() > 0)
		{
			AttributeMetaData targetAttribute = semanticSearchParameters.getTargetAttribute();
			EntityMetaData targetEntityMetaData = semanticSearchParameters.getTargetEntityMetaData();
			EntityMetaData sourceEntityMetaData = semanticSearchParameters.getSourceEntityMetaDatas().get(0);

			algorithm = runAlgorithmTemplate(targetAttribute, sourceAttributes, targetEntityMetaData,
					sourceEntityMetaData, additionalQueryTerms);

			if (isBlank(algorithm))
			{
				AttributeMetaData sourceAttribute = sourceAttributes.stream().findFirst().get();
				algorithm = runAlgorithmGenerator(targetAttribute, asList(sourceAttribute), targetEntityMetaData,
						sourceEntityMetaData);
			}

			// Get a list of source attributes from the algorithm.
			mappedSourceAttributes = extractSourceAttributesFromAlgorithm(algorithm, sourceEntityMetaData);

			// For each source attribute, an explanation is provided by the explain service.

			List<ExplainedAttributeMetaData> explainedSourceAttributes = mappedSourceAttributes.stream()
					.map(sourceAttribute -> attributeMappingExplainService
							.explainAttributeMapping(semanticSearchParameters, sourceAttribute))
					.collect(toList());

			// if all source attributes are matched with high quality, then the algorithm is high quality.
			algorithmState = explainedSourceAttributes.stream().allMatch(ExplainedAttributeMetaData::isHighQuality)
					? GENERATED_HIGH : GENERATED_LOW;

			// FIXME: how do i deal with the attributes that are matched to the the script parameters?

			// Calculate the final similarity score by summing up all the matched scores.
			score = explainedSourceAttributes.stream().map(ExplainedAttributeMetaData::getExplainedQueryString)
					.mapToDouble(ExplainedQueryString::getScore).sum();

			if (additionalQueryTerms.size() > 0) score = score / additionalQueryTerms.size();
		}

		return GeneratedAlgorithm.create(algorithm, mappedSourceAttributes, algorithmState, score);
	}

	String runAlgorithmTemplate(AttributeMetaData targetAttribute, List<AttributeMetaData> sourceAttributes,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData, Set<String> additionalQueryTerms)
	{
		String algorithm = null;
		if (sourceAttributes.size() > 0)
		{
			AlgorithmTemplate algorithmTemplate = algorithmTemplateService.find(targetAttribute, sourceAttributes)
					.findFirst().orElse(null);
			if (algorithmTemplate != null)
			{
				algorithm = algorithmTemplate.render();
				algorithm = convertUnitForTemplateAlgorithm(algorithm, targetAttribute, targetEntityMetaData,
						sourceEntityMetaData);

				// Put the parameters in the additionalQueryTerms if this varaible exists
				if (additionalQueryTerms != null)
				{
					additionalQueryTerms.addAll(algorithmTemplate.getScript().getParameters().stream()
							.map(ScriptParameter::getName).collect(toList()));
				}
			}
		}
		return algorithm;
	}

	String runAlgorithmGenerator(AttributeMetaData targetAttribute, List<AttributeMetaData> sourceAttributes,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData)
	{
		if (sourceAttributes.size() > 0)
		{
			for (AlgorithmGenerator generator : generators)
			{
				if (generator.isSuitable(targetAttribute, sourceAttributes))
				{
					return generator.generate(targetAttribute, sourceAttributes, targetEntityMetaData,
							sourceEntityMetaData);
				}
			}

			StringBuilder stringBuilder = new StringBuilder();
			if (sourceAttributes.size() == 1)
			{
				stringBuilder.append(String.format("$('%s').value();", sourceAttributes.get(0).getName()));
			}
			else
			{
				for (AttributeMetaData sourceAttribute : sourceAttributes)
				{
					stringBuilder.append(runAlgorithmGenerator(targetAttribute, Arrays.asList(sourceAttribute),
							targetEntityMetaData, sourceEntityMetaData));
				}
			}
			return stringBuilder.toString();
		}
		return StringUtils.EMPTY;
	}

	String convertUnitForTemplateAlgorithm(String algorithm, AttributeMetaData targetAttribute,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData)
	{
		Set<AttributeMetaData> sourceAttributes = extractSourceAttributesFromAlgorithm(algorithm, sourceEntityMetaData);
		Unit<? extends Quantity> targetUnit = unitResolver.resolveUnit(targetAttribute, targetEntityMetaData);
		for (AttributeMetaData sourceAttribute : sourceAttributes)
		{
			Unit<? extends Quantity> sourceUnit = unitResolver.resolveUnit(sourceAttribute, sourceEntityMetaData);

			String convertUnit = magmaUnitConverter.convertUnit(targetUnit, sourceUnit);

			if (StringUtils.isNotBlank(convertUnit))
			{
				String attrMagamSyntax = String.format("$('%s')", sourceAttribute.getName());
				String unitConvertedMagamSyntax = convertUnit.startsWith(".") ? attrMagamSyntax + convertUnit
						: attrMagamSyntax + "." + convertUnit;
				algorithm = StringUtils.replace(algorithm, attrMagamSyntax, unitConvertedMagamSyntax);
			}
		}
		return algorithm;
	}
}
