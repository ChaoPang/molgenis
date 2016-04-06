package org.molgenis.data.mapper.controller;

import static org.molgenis.data.mapper.controller.BiobankAttributeMatrixController.URI;

import java.util.concurrent.ExecutionException;

import org.molgenis.data.DataService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ui.MolgenisPluginController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(URI)
public class BiobankAttributeMatrixController extends MolgenisPluginController
{
	@Autowired
	private DataService dataService;

	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private SemanticSearchService semanticSearchService;

	public static final String ID = "attributematrix";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;
	private static final String VIEW_ATTRIBUTE_MATRIX = "view-attribute-matrix";

	public BiobankAttributeMatrixController()
	{
		super(URI);
	}

	@RequestMapping
	public String viewMappingProjects(Model model) throws ExecutionException
	{
		return null;
	}
}
