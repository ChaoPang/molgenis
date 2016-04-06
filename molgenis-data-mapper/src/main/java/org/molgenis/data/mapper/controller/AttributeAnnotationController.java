package org.molgenis.data.mapper.controller;

import static org.molgenis.data.mapper.controller.AttributeAnnotationController.URI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ui.MolgenisPluginController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(URI)
public class AttributeAnnotationController extends MolgenisPluginController
{
	@Autowired
	private DataService dataService;

	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private SemanticSearchService semanticSearchService;

	public static final String ID = "attributeannotation";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;
	private static final String VIEW_ATTRIBUTE_MATRIX = "view-attribute-annotation";

	public AttributeAnnotationController()
	{
		super(URI);
	}

	@RequestMapping
	public String viewMappingProjects(Model model) throws ExecutionException
	{
		EntityMetaData entityMetaData1 = dataService.getEntityMetaData("finrisk_1_finrisk_2000");
		EntityMetaData entityMetaData2 = dataService.getEntityMetaData("finrisk_finrisk_2007");

		model.addAttribute("entity_one", entityMetaData1.getName());
		model.addAttribute("entity_two", entityMetaData2.getName());

		return VIEW_ATTRIBUTE_MATRIX;
	}

	@RequestMapping(value = "/annotate", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, List<Hit<OntologyTermHit>>> findOntologyTerms(@RequestBody Map<String, String> request)
			throws ExecutionException
	{
		String entityName = request.get("entityName");
		EntityMetaData entityMetaData = dataService.getEntityMetaData(entityName);
		Map<String, List<Hit<OntologyTermHit>>> annotationMap = new HashMap<>();
		for (AttributeMetaData attribute : entityMetaData.getAtomicAttributes())
		{
			List<Hit<OntologyTermHit>> findTags = semanticSearchService.findAllTagsForAttr(attribute,
					ontologyService.getAllOntologiesIds());
			System.out.format("the attribute is %s; the tagged is %s%n",
					attribute.getName() + ':' + attribute.getLabel(),
					findTags == null ? StringUtils.EMPTY : findTags.toString());
			annotationMap.put(attribute.getName() + ':' + attribute.getLabel(), findTags);
		}
		return annotationMap;
	}
}
