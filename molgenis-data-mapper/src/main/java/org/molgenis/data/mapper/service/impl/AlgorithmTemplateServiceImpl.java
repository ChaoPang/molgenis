package org.molgenis.data.mapper.service.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.js.magma.JsMagmaScriptRegistrator.SCRIPT_TYPE_JAVASCRIPT_MAGMA;
import static org.molgenis.script.Script.ENTITY_NAME;
import static org.molgenis.script.Script.TYPE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.utils.Stemmer;
import org.molgenis.script.Script;
import org.molgenis.script.ScriptParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import static java.util.Objects.requireNonNull;

@Service
public class AlgorithmTemplateServiceImpl implements AlgorithmTemplateService
{
	private final DataService dataService;
	private final OntologyService ontologySerivce;
	private final SemanticSearchService semanticSearchService;

	@Autowired
	public AlgorithmTemplateServiceImpl(DataService dataService, OntologyService ontologySerivce,
			SemanticSearchService semanticSearchService)
	{
		this.dataService = requireNonNull(dataService);
		this.ontologySerivce = requireNonNull(ontologySerivce);
		this.semanticSearchService = requireNonNull(semanticSearchService);
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
		if (satisfyScriptTemplate(targetAttribute, script.getName()))
		{
			// find attribute for each parameter
			boolean paramMatch = true;
			Map<String, String> model = new HashMap<>();
			for (ScriptParameter param : script.getParameters())
			{
				AttributeMetaData attr = attrMatches.stream()
						.filter(sourceAttribute -> satisfyScriptTemplate(sourceAttribute, param.getName())).findFirst()
						.orElse(null);

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

	boolean satisfyScriptTemplate(AttributeMetaData attribute, String scriptParameterName)
	{
		// First of all we should check if the name of the sourceAttribute matches with the script parameter
		if (containsAllTokens(getAttributeTerms(attribute), Sets.newHashSet(scriptParameterName)))
		{
			return true;
		}
		else
		{
			// If the attribute terms cannot be matched with the script parameter, then we use the ontology term based
			// approach to try to find the sourceAttribute that satisfies the condition
			Hit<OntologyTerm> attributeOntologyTermHit = semanticSearchService.findTagsForAttribute(attribute,
					ontologySerivce.getAllOntologiesIds());

			Hit<OntologyTerm> scriptOntologyTermHit = semanticSearchService.findTags(scriptParameterName,
					ontologySerivce.getAllOntologiesIds());

			if (attributeOntologyTermHit == null || scriptOntologyTermHit == null) return false;

			// We want to check if the ontology terms associated with the target attribut contain all the ontology terms
			// associated with the script
			List<OntologyTerm> attributeAssociatedOts = ontologySerivce
					.getAtomicOntologyTerms(attributeOntologyTermHit.getResult());

			List<OntologyTerm> scriptAssociatedOts = ontologySerivce
					.getAtomicOntologyTerms(scriptOntologyTermHit.getResult());

			// if the target associated ontology terms are similar to the script associated ontology terms, the
			// corresponding template can also be used.
			boolean satisfied = true;
			for (OntologyTerm scriptOt : scriptAssociatedOts)
			{
				satisfied = satisfied && isOntologyTermMatch(attributeAssociatedOts, scriptOt);
			}
			return satisfied;
		}
	}

	private boolean isOntologyTermMatch(List<OntologyTerm> attributeAssociatedOts, OntologyTerm scriptOt)
	{
		if (attributeAssociatedOts.contains(scriptOt)) return true;
		for (OntologyTerm childOt : ontologySerivce.getChildren(scriptOt))
		{
			if (attributeAssociatedOts.contains(childOt)) return true;
		}
		return false;
	}

	boolean containsAllTokens(Set<String> listOfSynonyms1, Set<String> listOfSynonyms2)
	{
		for (String synonym1 : listOfSynonyms1)
		{
			for (String synonym2 : listOfSynonyms2)
			{
				Set<String> tokens1 = Stemmer.splitAndStem(synonym1);
				Set<String> tokens2 = Stemmer.splitAndStem(synonym2);
				if (tokens1.containsAll(tokens2)) return true;
			}
		}
		return false;
	}

	private Set<String> getAttributeTerms(AttributeMetaData attribute)
	{
		Set<String> attributeTerms = new HashSet<>();
		if (isNotBlank(attribute.getLabel()))
		{
			attributeTerms.add(attribute.getLabel());
		}
		if (isNotBlank(attribute.getDescription()))
		{
			attributeTerms.add(attribute.getDescription());
		}
		return attributeTerms;
	}
}