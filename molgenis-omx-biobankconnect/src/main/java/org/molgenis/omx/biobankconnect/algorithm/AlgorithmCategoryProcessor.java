package org.molgenis.omx.biobankconnect.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.molgenis.omx.biobankconnect.utils.NGramMatchingModel;
import org.molgenis.omx.observ.Category;

public class AlgorithmCategoryProcessor
{
	public static final List<Set<String>> RESERVED_CATEGORY_MAPPINGS = new ArrayList<Set<String>>();
	static
	{
		RESERVED_CATEGORY_MAPPINGS.add(new HashSet<String>(Arrays.asList("no", "never")));
		RESERVED_CATEGORY_MAPPINGS.add(new HashSet<String>(Arrays.asList("yes", "ever")));
		RESERVED_CATEGORY_MAPPINGS.add(new HashSet<String>(Arrays.asList("missing", "do not known", "not known",
				"unknown")));
	}

	public static final Map<String, List<Category>> RESERVED_MAPPERS = new HashMap<String, List<Category>>();
	static
	{
		List<Category> fastingCategories = new ArrayList<Category>();

		Category category_no = new Category();
		category_no.set(Category.VALUECODE, "0");
		category_no.set(Category.NAME, "no");
		fastingCategories.add(category_no);

		Category category_yes = new Category();
		category_yes.set(Category.VALUECODE, "1");
		category_yes.set(Category.NAME, "yes");
		fastingCategories.add(category_yes);

		Category category_missing = new Category();
		category_missing.set(Category.VALUECODE, "9");
		category_missing.set(Category.NAME, "missing");
		fastingCategories.add(category_missing);

		RESERVED_MAPPERS.put("fasting", fastingCategories);
	}

	/**
	 * An internal function to replace the category codes if standard category codes contain reserved words
	 * 
	 * @param standardCategory
	 * @return
	 */
	public static Set<String> retrieveRelevantCategoryGroup(Category standardCategory)
	{
		String standardCategoryName = standardCategory.getName().toLowerCase();
		for (Set<String> categoryGroup : RESERVED_CATEGORY_MAPPINGS)
		{
			for (String reservedCategoryName : categoryGroup)
			{
				if (standardCategoryName.contains(reservedCategoryName)) return categoryGroup;
			}
		}
		return Collections.emptySet();
	}

	public static String matchCategory(Iterable<Category> categoriesForStandardFeature,
			Iterable<Category> categoriesForCustomFeature)
	{
		Map<String, String> valueCodeMapping = new HashMap<String, String>();
		for (Category customCategory : categoriesForCustomFeature)
		{
			double similarityScore = 0;
			String mappedValueCode = null;
			for (Category standardCategory : categoriesForStandardFeature)
			{
				Set<String> retrieveRelevantCategoryGroup = new HashSet<String>(
						retrieveRelevantCategoryGroup(standardCategory));
				retrieveRelevantCategoryGroup.add(standardCategory.getName());
				for (String relevantCategoryName : retrieveRelevantCategoryGroup)
				{
					double score = NGramMatchingModel.stringMatching(customCategory.getName(), relevantCategoryName,
							false);
					if (score > similarityScore)
					{
						similarityScore = score;
						mappedValueCode = standardCategory.getValueCode();
					}
				}
			}
			if (mappedValueCode != null) valueCodeMapping.put(customCategory.getValueCode(), mappedValueCode);
		}
		StringBuilder javaScript = new StringBuilder();
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
		return javaScript.toString();
	}
}