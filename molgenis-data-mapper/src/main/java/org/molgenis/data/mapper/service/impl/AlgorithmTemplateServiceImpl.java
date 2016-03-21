package org.molgenis.data.mapper.service.impl;

import static org.molgenis.js.magma.JsMagmaScriptRegistrator.SCRIPT_TYPE_JAVASCRIPT_MAGMA;
import static org.molgenis.script.Script.ENTITY_NAME;
import static org.molgenis.script.Script.TYPE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.OntologyTermSemanticSearch;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.script.Script;
import org.molgenis.script.ScriptParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Objects.requireNonNull;

@Service
public class AlgorithmTemplateServiceImpl implements AlgorithmTemplateService
{
	private final DataService dataService;
	private final OntologyService ontologySerivce;
	private final SemanticSearchService semanticSearchService;
	private final OntologyTermSemanticSearch ontologyTermSemanticSearch;

	private final static double DEFAULT_THRESHOLD = 0.8;

	@Autowired
	public AlgorithmTemplateServiceImpl(DataService dataService, OntologyService ontologySerivce,
			SemanticSearchService semanticSearchService, OntologyTermSemanticSearch ontologyTermSemanticSearch)
	{
		this.dataService = requireNonNull(dataService);
		this.ontologySerivce = requireNonNull(ontologySerivce);
		this.semanticSearchService = requireNonNull(semanticSearchService);
		this.ontologyTermSemanticSearch = requireNonNull(ontologyTermSemanticSearch);
	}

	@Override
	public Stream<AlgorithmTemplate> find(AttributeMetaData targetAttribute, List<AttributeMetaData> attrMatches)
	{
		// get all algorithm templates
		Stream<Script> jsScripts = dataService.findAll(ENTITY_NAME,
				new QueryImpl().eq(TYPE, SCRIPT_TYPE_JAVASCRIPT_MAGMA), Script.class);

		// select all algorithm templates that can be used with target and sources
		return jsScripts.flatMap(script -> toAlgorithmTemplate(script, targetAttribute, attrMatches));
	}

	private Stream<AlgorithmTemplate> toAlgorithmTemplate(Script script, AttributeMetaData targetAttribute,
			List<AttributeMetaData> attrMatches)
	{
		// Check if the name of the target attribute matches with the the name of the current algorithm template
		double calculateAverageDistance = calculateDistance(targetAttribute,
				createIntermediateAttribute(script.getName()));

		if (calculateAverageDistance >= DEFAULT_THRESHOLD)
		{
			// find attribute for each parameter
			boolean paramMatch = true;
			Map<String, String> model = new HashMap<>();
			for (ScriptParameter param : script.getParameters())
			{
				AttributeMetaData attr = mapParamToAttribute(param, attrMatches);
				if (attr != null)
				{
					model.put(param.getName(), attr.getName());
				}
				else
				{
					paramMatch = false;
					break;
				}
			}
			// create algorithm template if an attribute was found for all parameters
			AlgorithmTemplate algorithmTemplate = new AlgorithmTemplate(script, model);
			return paramMatch ? Stream.of(algorithmTemplate) : Stream.empty();
		}
		return Stream.empty();
	}

	double calculateDistance(AttributeMetaData attribute1, AttributeMetaData attribute2)
	{
		Hit<OntologyTerm> targetAttributeOts = semanticSearchService.findTags(attribute1,
				ontologySerivce.getAllOntologiesIds());

		Hit<OntologyTerm> scriptNameOts = semanticSearchService.findTags(attribute2,
				ontologySerivce.getAllOntologiesIds());

		try
		{
			return ontologyTermSemanticSearch.calculateAverageDistance(Arrays.asList(targetAttributeOts),
					Arrays.asList(scriptNameOts));
		}
		catch (ExecutionException e)
		{
			return -1;
		}
	}

	private AttributeMetaData mapParamToAttribute(ScriptParameter param, List<AttributeMetaData> attrMatches)
	{
		for (AttributeMetaData sourceAttribute : attrMatches)
		{
			double calculateDistance = calculateDistance(createIntermediateAttribute(param.getName()), sourceAttribute);
			if (calculateDistance >= DEFAULT_THRESHOLD)
			{
				return sourceAttribute;
			}
		}
		return null;
	}

	private AttributeMetaData createIntermediateAttribute(String name)
	{
		return new DefaultAttributeMetaData(name).setLabel(name);
	}
}