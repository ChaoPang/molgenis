package org.molgenis.data.mapper.controller;

import static com.google.common.collect.Iterables.size;
import static java.lang.Integer.parseInt;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.data.mapper.controller.DataDiscoveryController.URI;
import static org.molgenis.security.core.Permission.READ;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParameter;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParameter;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.security.core.MolgenisPermissionService;
import org.molgenis.ui.MolgenisPluginController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import static java.util.Objects.requireNonNull;

@Controller
@RequestMapping(URI)
public class DataDiscoveryController extends MolgenisPluginController
{
	private final DataService dataService;
	private final SemanticSearchService semanticSearchService;
	private final AttributeMappingExplainService attributeMappingExplainService;
	private final MolgenisPermissionService molgenisPermissionService;

	public static final String ID = "datadiscovery";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;
	private static final String VIEW_DATA_DISCOVERY = "view-data-discovery";
	private static final int DEFAULT_NUMBER_OF_ATTRIBUTES = 50;

	@Autowired
	public DataDiscoveryController(DataService dataService, SemanticSearchService semanticSearchService,
			AttributeMappingExplainService attributeMappingExplainService,
			MolgenisPermissionService molgenisPermissionService)
	{
		super(URI);
		this.dataService = requireNonNull(dataService);
		this.semanticSearchService = requireNonNull(semanticSearchService);
		this.attributeMappingExplainService = requireNonNull(attributeMappingExplainService);
		this.molgenisPermissionService = requireNonNull(molgenisPermissionService);
	}

	@RequestMapping(method = RequestMethod.GET)
	public String view(Model model)
	{
		model.addAttribute("entityMetaDatas", getReadableEntityMetaDatas());
		return VIEW_DATA_DISCOVERY;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/searchTerm")
	public String searchItems(@RequestParam(required = false) boolean exactMatch,
			@RequestParam(required = true) String searchTerm, @RequestParam(required = false) String[] entityNames,
			@RequestParam(required = true) String ontologyLevel, Model model)
	{
		Map<String, List<ExplainedAttributeMetaData>> searchResult = new LinkedHashMap<>();
		List<EntityMetaData> readableEntityMetaDatas = getReadableEntityMetaDatas();

		if (isNotBlank(searchTerm))
		{
			DefaultAttributeMetaData attributeMetaData = new DefaultAttributeMetaData(searchTerm);
			QueryExpansionParameter create = QueryExpansionParameter.create(true, true, parseInt(ontologyLevel));

			SemanticSearchParameter semanticSearchParameters = SemanticSearchParameter.create(attributeMetaData,
					emptySet(), null, readableEntityMetaDatas, exactMatch, create);

			searchResult = semanticSearchService.findMultiEntityAttributes(semanticSearchParameters).entrySet().stream()
					.collect(Collectors.toMap(entry -> entry.getKey().getName(),
							entry -> explainAttributes(semanticSearchParameters, entry.getValue())));
		}

		model.addAttribute("entityMetaDatas", readableEntityMetaDatas);
		model.addAttribute("searchResult", searchResult);
		return VIEW_DATA_DISCOVERY;
	}

	private List<ExplainedAttributeMetaData> explainAttributes(SemanticSearchParameter semanticSearchParameter,
			List<AttributeMetaData> matchedAttributes)
	{
		List<ExplainedAttributeMetaData> explainedAttributeMetadatas = new ArrayList<>();

		for (AttributeMetaData attr : matchedAttributes)
		{
			ExplainedAttributeMetaData explainAttributeMapping = attributeMappingExplainService
					.explainAttributeMapping(semanticSearchParameter, attr);
			if (explainAttributeMapping.isHighQuality())
			{
				explainedAttributeMetadatas.add(explainAttributeMapping);
			}
			else break;
		}

		return explainedAttributeMetadatas;
	}

	private List<EntityMetaData> getReadableEntityMetaDatas()
	{
		List<EntityMetaData> collect = dataService.getEntityNames().map(dataService::getEntityMetaData)
				.filter(entityMetaData -> molgenisPermissionService.hasPermissionOnEntity(entityMetaData.getName(),
						READ) && size(entityMetaData.getAtomicAttributes()) > DEFAULT_NUMBER_OF_ATTRIBUTES)
				.collect(toList());
		return collect;
	}
}
