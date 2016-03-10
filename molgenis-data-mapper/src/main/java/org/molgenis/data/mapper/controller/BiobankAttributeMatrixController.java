package org.molgenis.data.mapper.controller;

import static org.molgenis.data.mapper.controller.BiobankAttributeMatrixController.URI;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.common.lang3.StringUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.DistanceMatrixReport;
import org.molgenis.data.semanticsearch.service.bean.DistanceMatrixRequest;
import org.molgenis.data.semanticsearch.service.bean.DistanceMetric;
import org.molgenis.data.semanticsearch.service.impl.OntologyTermBasedSemanticSearchImpl;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ui.MolgenisPluginController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;

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

	@Autowired
	private OntologyTermBasedSemanticSearchImpl ontologyTermBasedSemanticSearch;

	public static final String ID = "attributematrix";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;
	private static final String VIEW_ATTRIBUTE_MATRIX = "view-attribute-matrix";

	private LoadingCache<DistanceMatrixRequest, DistanceMatrixReport> dbInMemory = CacheBuilder.newBuilder()
			.maximumSize(1000).expireAfterWrite(5, TimeUnit.HOURS)
			.build(new CacheLoader<DistanceMatrixRequest, DistanceMatrixReport>()
			{
				public DistanceMatrixReport load(DistanceMatrixRequest distanceMatrixRequest) throws ExecutionException
				{
					return startJob(distanceMatrixRequest);
				}

				private DistanceMatrixReport startJob(DistanceMatrixRequest distanceMatrixRequest)
						throws ExecutionException
				{
					String entityName1 = distanceMatrixRequest.getEntityName1();
					String entityName2 = distanceMatrixRequest.getEntityName2();
					EntityMetaData entityMetaData1 = dataService.getEntityMetaData(entityName1);
					EntityMetaData entityMetaData2 = dataService.getEntityMetaData(entityName2);
					DistanceMatrixReport distanceMatrixReport = new DistanceMatrixReport();
					ontologyTermBasedSemanticSearch.getAsyncEntitiesDistance(distanceMatrixReport, entityMetaData1,
							entityMetaData2);
					return distanceMatrixReport;
				}
			});

	public BiobankAttributeMatrixController()
	{
		super(URI);
	}

	@RequestMapping
	public String viewMappingProjects(Model model) throws ExecutionException
	{
		EntityMetaData entityMetaData1 = dataService.getEntityMetaData("finrisk_1_finrisk_2000");
		EntityMetaData entityMetaData2 = dataService.getEntityMetaData("finrisk_finrisk_2007");

		dbInMemory.get(DistanceMatrixRequest.create(entityMetaData1.getName(), entityMetaData2.getName()));

		model.addAttribute("entity_one", entityMetaData1.getName());
		model.addAttribute("entity_two", entityMetaData2.getName());

		return VIEW_ATTRIBUTE_MATRIX;
	}

	// @RequestMapping
	// public String viewMappingProjects(Model model) throws ExecutionException
	// {
	// EntityMetaData entityMetaData1 = dataService.getEntityMetaData("lifelines");
	// EntityMetaData entityMetaData2 = dataService.getEntityMetaData("prevend");
	//
	// dbInMemory.get(DistanceMatrixRequest.create(entityMetaData1.getName(), entityMetaData2.getName()));
	//
	// model.addAttribute("entity_one", entityMetaData1.getName());
	// model.addAttribute("entity_two", entityMetaData2.getName());
	//
	// return VIEW_ATTRIBUTE_MATRIX;
	// }

	@RequestMapping(value = "/report", method = RequestMethod.POST)
	@ResponseBody
	public DistanceMatrixReport getDistanceMatrix(@RequestBody Map<String, String> request) throws ExecutionException
	{
		if (!request.containsKey("entity_one") || !request.containsKey("entity_two")) return null;

		String entity_one = request.get("entity_one");
		String entity_two = request.get("entity_two");

		if (StringUtils.isBlank(entity_one) || StringUtils.isBlank(entity_two)) return null;

		EntityMetaData entityMetaData1 = dataService.getEntityMetaData(entity_one);
		EntityMetaData entityMetaData2 = dataService.getEntityMetaData(entity_two);

		DistanceMatrixReport distanceMatrixReport = dbInMemory
				.get(DistanceMatrixRequest.create(entityMetaData1.getName(), entityMetaData2.getName()));

		return distanceMatrixReport;
	}

	@RequestMapping(value = "/attribute", method = RequestMethod.POST)
	@ResponseBody
	public List<DistanceMetric> getAttributeDistance(@RequestBody Map<String, String> request) throws ExecutionException
	{
		if (!request.containsKey("entity_one") || !request.containsKey("entity_two")
				|| !request.containsKey("attr_one"))
			return null;

		String entity_one = request.get("entity_one");
		String entity_two = request.get("entity_two");
		String attr_one = request.get("attr_one");

		if (StringUtils.isBlank(entity_one) || StringUtils.isBlank(entity_two) || StringUtils.isBlank(attr_one))
			return null;

		EntityMetaData entityMetaData1 = dataService.getEntityMetaData(entity_one);
		EntityMetaData entityMetaData2 = dataService.getEntityMetaData(entity_two);

		return ontologyTermBasedSemanticSearch.getAttrDistance(attr_one, entityMetaData1, entityMetaData2);
	}

	@RequestMapping(value = "/annotate", method = RequestMethod.POST)
	@ResponseBody
	public List<OntologyTerm> findOntologyTerms(@RequestBody Map<String, String> request) throws ExecutionException
	{
		if (!request.containsKey("term")) return null;

		String term = request.get("term");

		if (StringUtils.isBlank(term)) Collections.emptyList();

		Ontology ontology = ontologyService.getOntology("UMLS");

		if (ontology == null) Collections.emptyList();

		return ontologyService.findOntologyTerms(Arrays.asList(ontology.getId()), Sets.newHashSet(term.split(" ")),
				100);
	}

	@RequestMapping(value = "/annotateattr", method = RequestMethod.POST)
	@ResponseBody
	public OntologyTerm annotateAttribute(@RequestBody Map<String, String> request) throws ExecutionException
	{
		if (!request.containsKey("entityName") || !request.containsKey("attributeName")) return null;

		String entityName = request.get("entityName");
		String attributeName = request.get("attributeName");

		if (StringUtils.isBlank(entityName) || StringUtils.isBlank(attributeName)) Collections.emptyList();

		EntityMetaData entityMetaData = dataService.getEntityMetaData(entityName);

		if (entityMetaData == null) return null;

		AttributeMetaData attribute = entityMetaData.getAttribute(attributeName);

		if (attribute == null) return null;

		Ontology ontology = ontologyService.getOntology("UMLS");

		if (ontology == null) return null;

		Hit<OntologyTerm> findTags = semanticSearchService.findTags(attribute, Arrays.asList(ontology.getId()));

		return findTags.getResult();
	}
}
