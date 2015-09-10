package org.molgenis.data.semanticsearch.controller;

import static org.molgenis.data.semanticsearch.controller.SemanticSearchController.URI;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.utils.EntityToMapTransformer;
import org.molgenis.ui.MolgenisPluginController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(URI)
public class SemanticSearchController extends MolgenisPluginController
{
	private final DataService dataService;
	private final SemanticSearchService semanticSearchService;
	public static final String ID = "semanticsearch";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;

	@Autowired
	public SemanticSearchController(DataService dataService, SemanticSearchService semanticSearchService)
	{
		super(URI);

		if (dataService == null) throw new IllegalArgumentException("dataService is null");
		if (semanticSearchService == null) throw new IllegalArgumentException("SemanticSearchService is null");

		this.dataService = dataService;
		this.semanticSearchService = semanticSearchService;
	}

	@RequestMapping(method = RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public List<Map<String, Object>> semanticSearchEntities(@RequestBody SemanticSearchRequest request)
	{
		String entityName = request.getEntityName();
		Set<String> attributeNames = request.getAttributeNames();
		Set<String> searchTerms = request.getSearchTerms();

		if (StringUtils.isNotBlank(entityName) && dataService.hasRepository(entityName) && searchTerms != null
				&& searchTerms.size() > 0)
		{
			EntityMetaData entityMetaData = dataService.getRepository(entityName).getEntityMetaData();

			Set<AttributeMetaData> searchAttributes = attributeNames != null ? attributeNames.stream()
					.map(attributeName -> entityMetaData.getAttribute(attributeName)).collect(Collectors.toSet()) : null;

			Iterable<Entity> entities = semanticSearchService.find(entityMetaData, searchAttributes, searchTerms);

			return EntityToMapTransformer.getEntityAsMap(entities);
		}

		return Collections.emptyList();
	}

	class SemanticSearchRequest
	{
		private final String entityName;
		private final Set<String> attributeNames;
		private final Set<String> searchTerms;

		public SemanticSearchRequest(String entityName, Set<String> attributeNames, Set<String> searchTerms)
		{
			this.entityName = entityName;
			this.attributeNames = attributeNames;
			this.searchTerms = searchTerms;
		}

		public String getEntityName()
		{
			return entityName;
		}

		public Set<String> getAttributeNames()
		{
			return attributeNames;
		}

		public Set<String> getSearchTerms()
		{
			return searchTerms;
		}
	}
}
