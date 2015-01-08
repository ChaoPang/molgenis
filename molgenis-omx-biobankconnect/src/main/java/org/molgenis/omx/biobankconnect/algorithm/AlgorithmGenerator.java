package org.molgenis.omx.biobankconnect.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.DataService;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.omx.biobankconnect.ontologymatcher.AsyncOntologyMatcher;
import org.molgenis.omx.biobankconnect.ontologymatcher.OntologyMatcher;
import org.molgenis.omx.biobankconnect.ontologymatcher.OntologyMatcherRequest;
import org.molgenis.omx.biobankconnect.utils.NGramMatchingModel;
import org.molgenis.omx.observ.Category;
import org.molgenis.omx.observ.DataSet;
import org.molgenis.omx.observ.ObservableFeature;
import org.molgenis.omx.observ.target.OntologyTerm;
import org.molgenis.search.Hit;
import org.molgenis.search.SearchRequest;
import org.molgenis.search.SearchResult;
import org.molgenis.search.SearchService;
import org.molgenis.security.runas.RunAsSystem;
import org.springframework.beans.factory.annotation.Autowired;

public class AlgorithmGenerator
{
	@Autowired
	private OntologyMatcher ontologyMatcher;

	@Autowired
	private AlgorithmScriptLibrary algorithmScriptLibrary;

	@Autowired
	private AlgorithmUnitConverter algorithmUnitConverter;

	@Autowired
	private DataService dataService;

	@Autowired
	private SearchService searchService;

	private final static String NODE_PATH = "nodePath";
	private static final String ONTOLOGY_TERM_IRI = "ontologyTermIRI";
	private static final String DOT_SEPARATOR = "\\.";
	private static final Map<String, String> RESERVED_CATEGORY_MAPPINGS = new HashMap<String, String>();
	{
		RESERVED_CATEGORY_MAPPINGS.put("never", "no");
		RESERVED_CATEGORY_MAPPINGS.put("ever", "yes");
		RESERVED_CATEGORY_MAPPINGS.put("missing", "unknown");
	}

	@RunAsSystem
	public String generateAlgorithm(String userName, OntologyMatcherRequest request)
	{
		StringBuilder suggestedScript = new StringBuilder();
		List<Integer> selectedDataSetIds = request.getSelectedDataSetIds();
		if (selectedDataSetIds.size() > 0)
		{
			// Retrieved the mapped features based on the user selection, if no mapped features are selected system
			// re-generates all the potential mappings
			SearchResult searchResult;
			if (request.getMappedFeatureIds() != null && request.getMappedFeatureIds().size() != 0)
			{
				DataSet dataSet = dataService.findOne(DataSet.ENTITY_NAME, request.getSelectedDataSetIds().get(0),
						DataSet.class);
				QueryImpl query = new QueryImpl();
				query.pageSize(Integer.MAX_VALUE);
				for (Integer featureId : request.getMappedFeatureIds())
				{
					if (query.getRules().size() > 0) query.addRule(new QueryRule(Operator.OR));
					query.addRule(new QueryRule(AsyncOntologyMatcher.ENTITY_ID, Operator.EQUALS, featureId));
				}
				searchResult = searchService.search(new SearchRequest(AsyncOntologyMatcher.CATALOGUE_PREFIX
						+ dataSet.getProtocolUsed().getId(), query, null));
			}
			else
			{
				searchResult = ontologyMatcher.generateMapping(userName, request.getFeatureId(),
						request.getTargetDataSetId(), selectedDataSetIds.get(0));
			}

			ObservableFeature standardFeature = dataService.findOne(ObservableFeature.ENTITY_NAME,
					request.getFeatureId(), ObservableFeature.class);

			String scriptTemplate = algorithmScriptLibrary.findScriptTemplate(standardFeature);
			if (searchResult.getTotalHitCount() > 0)
			{
				List<Hit> standardFeatureOTs = searchOTsByFeature(standardFeature);
				for (Hit candidateFeature : searchResult.getSearchHits())
				{
					if (calculateOTsDistance(standardFeatureOTs, searchOTsByFeature(candidateFeature)) == 0)
					{
						return convertToJavascript(standardFeature, candidateFeature);
					}
				}

				if (StringUtils.isEmpty(scriptTemplate) || searchResult.getTotalHitCount() == 1)
				{
					suggestedScript.append(convertToJavascript(standardFeature, searchResult.getSearchHits().get(0)));
				}
				else
				{
					suggestedScript.append(convertToJavascriptByFormula(scriptTemplate, standardFeature, searchResult));
				}
			}
		}
		return suggestedScript.toString();
	}

	/**
	 * Convert to Javascript based on candidate mapped features
	 * 
	 * @param standardFeature
	 * @param mappingCandidateSearchResult
	 * @return
	 */
	private String convertToJavascript(ObservableFeature standardFeature, Hit mappedFeatureId)
	{
		ObservableFeature customFeature = dataService.findOne(ObservableFeature.ENTITY_NAME, Integer
				.parseInt(mappedFeatureId.getColumnValueMap().get(ObservableFeature.ID.toLowerCase()).toString()),
				ObservableFeature.class);
		String conversionScript = algorithmUnitConverter.convert(standardFeature.getUnit(), customFeature.getUnit());
		StringBuilder javaScript = new StringBuilder();
		javaScript.append(createMagamaVarName(customFeature.getName(), conversionScript, false));

		// If two variables are categorical, map the value codes onto each other
		if (standardFeature.getDataType().equalsIgnoreCase(MolgenisFieldTypes.FieldTypeEnum.CATEGORICAL.toString())
				&& customFeature.getDataType()
						.equalsIgnoreCase(MolgenisFieldTypes.FieldTypeEnum.CATEGORICAL.toString()))
		{
			Iterable<Category> categoriesForStandardFeature = dataService.findAll(Category.ENTITY_NAME,
					new QueryImpl().eq(Category.OBSERVABLEFEATURE, standardFeature), Category.class);

			Iterable<Category> categoriesForCustomFeature = dataService.findAll(Category.ENTITY_NAME,
					new QueryImpl().eq(Category.OBSERVABLEFEATURE, customFeature), Category.class);

			Map<String, String> valueCodeMapping = new HashMap<String, String>();
			for (Category customCategory : categoriesForCustomFeature)
			{
				double similarityScore = 0;
				String mappedValueCode = null;
				for (Category standardCategory : categoriesForStandardFeature)
				{
					double score = NGramMatchingModel.stringMatching(customCategory.getName(),
							replaceCategoryWithReservedMapping(standardCategory), false);
					if (score > similarityScore)
					{
						similarityScore = score;
						mappedValueCode = standardCategory.getValueCode();
					}
				}
				if (mappedValueCode != null) valueCodeMapping.put(customCategory.getValueCode(), mappedValueCode);
			}
			if (valueCodeMapping.size() > 0)
			{
				javaScript.append(".map({");
				for (Entry<String, String> entry : valueCodeMapping.entrySet())
				{
					javaScript.append("'").append(entry.getKey()).append("'").append(" : ").append("'")
							.append(entry.getValue()).append("',");
				}
				javaScript.delete(javaScript.length() - 1, javaScript.length());
				javaScript.append("})");
			}
		}
		return javaScript.toString();
	}

	/**
	 * Convert to Javascript based on the formula pre-defined. E.g. BMI, Hypertension
	 * 
	 * @param scriptTemplate
	 * @param standardFeature
	 * @param searchResult
	 * @return
	 */
	private String convertToJavascriptByFormula(String scriptTemplate, ObservableFeature standardFeature,
			SearchResult searchResult)
	{
		StringBuilder backupScriptTemplate = new StringBuilder(scriptTemplate);
		for (String buildingBlock : ApplyAlgorithms.extractFeatureName(scriptTemplate))
		{
			SearchResult otsResultBuildingBlock = algorithmScriptLibrary.searchOTsByNames(Arrays.asList(buildingBlock));
			if (otsResultBuildingBlock.getTotalHitCount() > 0)
			{
				Hit bestMatchedFeature = null;
				int miniDistance = 1000000;
				for (Hit candidateFeature : searchResult.getSearchHits())
				{
					int distance = calculateOTsDistance(otsResultBuildingBlock.getSearchHits(),
							searchOTsByFeature(candidateFeature));
					if (distance >= 0 && distance < miniDistance)
					{
						miniDistance = distance;
						bestMatchedFeature = candidateFeature;
					}

					if (distance == 0) break;
				}
				if (bestMatchedFeature != null)
				{
					ObservableFeature mappedFeature = dataService.findOne(
							ObservableFeature.ENTITY_NAME,
							Integer.parseInt(bestMatchedFeature.getColumnValueMap()
									.get(ObservableFeature.ID.toLowerCase()).toString()), ObservableFeature.class);
					String conversionScript = algorithmUnitConverter.convert(standardFeature.getUnit(),
							mappedFeature.getUnit());
					String mappedFeatureJavaScriptName = createMagamaVarName(bestMatchedFeature.getColumnValueMap()
							.get(ObservableFeature.NAME.toLowerCase()).toString(), conversionScript, true);
					String standardJavaScriptName = createMagamaVarName(buildingBlock, null, true);
					scriptTemplate = scriptTemplate.replaceAll(standardJavaScriptName, mappedFeatureJavaScriptName);
				}
			}
		}
		return backupScriptTemplate.toString().equals(scriptTemplate) ? StringUtils.EMPTY : scriptTemplate;
	}

	/**
	 * A helper function to create feature representation in Magama syntax
	 * 
	 * @param mappedFeatureName
	 * @param suffix
	 * @param escaped
	 * @return
	 */
	private String createMagamaVarName(String mappedFeatureName, String suffix, boolean escaped)
	{
		StringBuilder javaScriptName = new StringBuilder();
		javaScriptName.append(escaped ? "\\$\\('" : "$('").append(mappedFeatureName).append(escaped ? "'\\)" : "')");
		if (!StringUtils.isEmpty(suffix)) javaScriptName.append(suffix);
		return javaScriptName.toString();
	}

	/**
	 * A helper function to search for suitable ontology terms for given feature based on annotations or lexical
	 * similarity
	 * 
	 * @param candidateFeature
	 * @return
	 */
	private List<Hit> searchOTsByFeature(Hit candidateFeature)
	{
		ObservableFeature feature = dataService.findOne(
				ObservableFeature.ENTITY_NAME,
				Integer.parseInt(candidateFeature.getColumnValueMap().get(ObservableFeature.ID.toLowerCase())
						.toString()), ObservableFeature.class);
		return searchOTsByFeature(feature);
	}

	/**
	 * A helper function to search for suitable ontology terms for given feature based on annotations or lexical
	 * similarity
	 * 
	 * @param feature
	 * @return
	 */
	private List<Hit> searchOTsByFeature(ObservableFeature feature)
	{
		if (feature.getDefinitions().size() > 0)
		{
			QueryImpl query = new QueryImpl();
			for (OntologyTerm ot : feature.getDefinitions())
			{
				if (query.getRules().size() > 0) query.addRule(new QueryRule(Operator.OR));
				query.addRule(new QueryRule(ONTOLOGY_TERM_IRI, Operator.EQUALS, ot.getTermAccession()));
			}
			return searchService.search(new SearchRequest(null, query, null)).getSearchHits();
		}
		return algorithmScriptLibrary
				.searchOTsByNames(
						Arrays.asList(
								feature.getName(),
								(StringUtils.isEmpty(feature.getDescription()) ? StringUtils.EMPTY : feature
										.getDescription()))).getSearchHits();
	}

	/**
	 * An internal function to replace the category codes if standard category codes contain reserved words
	 * 
	 * @param standardCategory
	 * @return
	 */
	private String replaceCategoryWithReservedMapping(Category standardCategory)
	{
		String name = standardCategory.getName().toLowerCase();
		for (String reservedCategoryName : RESERVED_CATEGORY_MAPPINGS.keySet())
		{
			if (name.contains(reservedCategoryName)) return RESERVED_CATEGORY_MAPPINGS.get(reservedCategoryName);
		}
		return standardCategory.getName();
	}

	/**
	 * To calculate the minimal distance between target ontology term and a set of candidate ontology terms
	 * 
	 * @param targetOntologyTerm
	 * @param sourceOntologyTerms
	 * @return
	 */
	private int calculateOTsDistance(List<Hit> targetOntologyTerms, List<Hit> sourceOntologyTerms)
	{
		int aggregatedDistance = -1;
		for (Hit targetOntologyTerm : targetOntologyTerms)
		{
			int miniDistance = 1000000;
			List<String> totTermPathParts = Arrays.asList(targetOntologyTerm.getColumnValueMap().get(NODE_PATH)
					.toString().split(DOT_SEPARATOR));
			Set<String> uniquePaths = new HashSet<String>();
			for (Hit sourceOntologyTerm : sourceOntologyTerms)
			{
				if (sourceOntologyTerm.getColumnValueMap().containsKey(NODE_PATH))
				{
					uniquePaths.add(sourceOntologyTerm.getColumnValueMap().get(NODE_PATH).toString());
				}
			}
			for (String uniquePath : uniquePaths)
			{
				List<String> sosPathParts = new ArrayList<String>(Arrays.asList(uniquePath.split(DOT_SEPARATOR)));
				int beforeRemove = sosPathParts.size();
				sosPathParts.removeAll(totTermPathParts);
				int afterRemove = sosPathParts.size();
				int distance = (beforeRemove + totTermPathParts.size()) - 2 * (beforeRemove - afterRemove);
				if (distance == 0)
				{
					miniDistance = distance;
					break;
				}
				else if (distance > 0 && distance < miniDistance) miniDistance = distance;
			}
			aggregatedDistance = aggregatedDistance == -1 ? miniDistance : aggregatedDistance + miniDistance;
		}
		return aggregatedDistance;
	}
}
