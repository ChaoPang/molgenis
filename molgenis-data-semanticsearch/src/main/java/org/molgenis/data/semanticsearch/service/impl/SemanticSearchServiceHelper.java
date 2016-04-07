package org.molgenis.data.semanticsearch.service.impl;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisDataAccessException;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.ic.TermFrequencyService;
import org.molgenis.ontology.utils.NGramDistanceAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import static java.util.Objects.requireNonNull;

public class SemanticSearchServiceHelper
{
	private final TermFrequencyService termFrequencyService;

	private final DataService dataService;

	private final OntologyService ontologyService;

	public final static int MAX_NUM_TAGS = 3;

	private final static char SPACE_CHAR = ' ';
	private final static String COMMA_CHAR = ",";
	private final static String CARET_CHARACTER = "^";
	private final static String ESCAPED_CARET_CHARACTER = "\\^";
	private final static String ILLEGAL_CHARS_REGEX = "[^\\p{L}'a-zA-Z0-9\\.~]+";

	@Autowired
	public SemanticSearchServiceHelper(DataService dataService, OntologyService ontologyService,
			TermFrequencyService termFrequencyService)
	{
		this.dataService = requireNonNull(dataService);
		this.ontologyService = requireNonNull(ontologyService);
		this.termFrequencyService = requireNonNull(termFrequencyService);
	}

	/**
	 * A helper function to create a list of queryTerms based on the information from the targetAttribute as well as
	 * user defined searchTerms. If the user defined searchTerms exist, the targetAttribute information will not be
	 * used.
	 * 
	 * @param targetAttribute
	 * @param userQueries
	 * @return list of queryTerms
	 */
	public Set<String> createLexicalSearchQueryTerms(AttributeMetaData targetAttribute, Set<String> userQueries)
	{
		Set<String> queryTerms = new HashSet<>();
		if (userQueries != null && !userQueries.isEmpty())
		{
			queryTerms.addAll(userQueries);
		}
		if (queryTerms.size() == 0)
		{
			if (isNotBlank(targetAttribute.getLabel()))
			{
				queryTerms.add(targetAttribute.getLabel());
			}
			if (isNotBlank(targetAttribute.getDescription()))
			{
				queryTerms.add(targetAttribute.getDescription());
			}
		}
		return queryTerms;
	}

	/**
	 * Create a disMaxJunc query rule based on the given search terms as well as the information from given ontology
	 * terms
	 * 
	 * @param ontologyTerms
	 * @param searchTerms
	 * 
	 * @return disMaxJunc queryRule
	 */
	public QueryRule createDisMaxQueryRuleForAttribute(Set<String> searchTerms, List<OntologyTerm> ontologyTerms)
	{
		List<String> queryTerms = new ArrayList<String>();

		if (searchTerms != null)
		{
			queryTerms.addAll(searchTerms.stream().filter(StringUtils::isNotBlank).map(this::processQueryString)
					.collect(Collectors.toList()));
		}

		// Handle tags with only one ontologyterm
		ontologyTerms.stream().filter(ontologyTerm -> !ontologyTerm.getIRI().contains(COMMA_CHAR)).forEach(ot -> {
			queryTerms.addAll(parseOntologyTermQueries(ot));
		});

		QueryRule disMaxQueryRule = createDisMaxQueryRuleForTerms(queryTerms);

		// Handle tags with multiple ontologyterms
		ontologyTerms.stream().filter(ontologyTerm -> ontologyTerm.getIRI().contains(COMMA_CHAR)).forEach(ot -> {
			disMaxQueryRule.getNestedRules().add(createShouldQueryRule(ot.getIRI()));
		});

		return disMaxQueryRule;
	}

	/**
	 * Create disMaxJunc query rule based a list of queryTerm. All queryTerms are lower cased and stop words are removed
	 * 
	 * @param queryTerms
	 * @return disMaxJunc queryRule
	 */
	public QueryRule createDisMaxQueryRuleForTerms(List<String> queryTerms)
	{
		List<QueryRule> rules = new ArrayList<QueryRule>();
		queryTerms.stream().filter(StringUtils::isNotEmpty).map(this::escapeCharsExcludingCaretChar).forEach(query -> {
			rules.add(new QueryRule(AttributeMetaDataMetaData.LABEL, Operator.FUZZY_MATCH, query));
			rules.add(new QueryRule(AttributeMetaDataMetaData.DESCRIPTION, Operator.FUZZY_MATCH, query));
		});
		QueryRule finalDisMaxQuery = new QueryRule(rules);
		finalDisMaxQuery.setOperator(Operator.DIS_MAX);
		return finalDisMaxQuery;
	}

	/**
	 * Create a disMaxQueryRule with corresponding boosted value
	 * 
	 * @param queryTerms
	 * @param boostValue
	 * @return a disMaxQueryRule with boosted value
	 */
	public QueryRule createBoostedDisMaxQueryRuleForTerms(List<String> queryTerms, Double boostValue)
	{
		QueryRule finalDisMaxQuery = createDisMaxQueryRuleForTerms(queryTerms);
		if (boostValue != null && boostValue.intValue() != 0)
		{
			finalDisMaxQuery.setValue(boostValue);
		}
		return finalDisMaxQuery;
	}

	/**
	 * Create a boolean should query for composite tags containing multiple ontology terms
	 * 
	 * @param multiOntologyTermIri
	 * @return return a boolean should queryRule
	 */
	public QueryRule createShouldQueryRule(String multiOntologyTermIri)
	{
		QueryRule shouldQueryRule = new QueryRule(new ArrayList<QueryRule>());
		shouldQueryRule.setOperator(Operator.SHOULD);
		for (String ontologyTermIri : multiOntologyTermIri.split(COMMA_CHAR))
		{
			OntologyTerm ontologyTerm = ontologyService.getOntologyTerm(ontologyTermIri);
			List<String> queryTerms = parseOntologyTermQueries(ontologyTerm);
			Double termFrequency = getBestInverseDocumentFrequency(queryTerms);
			shouldQueryRule.getNestedRules().add(createBoostedDisMaxQueryRuleForTerms(queryTerms, termFrequency));
		}
		return shouldQueryRule;
	}

	/**
	 * Create a list of string queries based on the information collected from current ontologyterm including label,
	 * synonyms and child ontologyterms
	 * 
	 * @param ontologyTerm
	 * @return
	 */
	public List<String> parseOntologyTermQueries(OntologyTerm ontologyTerm)
	{
		List<String> queryTerms = getOtLabelAndSynonyms(ontologyTerm).stream().map(this::processQueryString)
				.collect(toList());
		ontologyService.getLevelThreeChildren(ontologyTerm).forEach(childOt -> {
			double boostedNumber = ontologyService.getOntologyTermSemanticRelatedness(ontologyTerm, childOt);
			List<String> collect = getOtLabelAndSynonyms(childOt).stream()
					.map(query -> parseBoostQueryString(query, boostedNumber)).collect(Collectors.toList());
			queryTerms.addAll(collect);
		});
		return queryTerms;
	}

	/**
	 * A helper function to collect synonyms as well as label of ontologyterm
	 * 
	 * @param ontologyTerm
	 * @return a list of synonyms plus label
	 */
	public Set<String> getOtLabelAndSynonyms(OntologyTerm ontologyTerm)
	{
		Set<String> allTerms = Sets.newLinkedHashSet(ontologyTerm.getSynonyms());
		allTerms.add(ontologyTerm.getLabel());
		return allTerms;
	}

	/**
	 * A helper function that gets identifiers of all the attributes from one entityMetaData
	 * 
	 * @param sourceEntityMetaData
	 * @return
	 */
	public List<String> getAttributeIdentifiers(EntityMetaData sourceEntityMetaData)
	{
		Entity entityMetaDataEntity = dataService.findOne(EntityMetaDataMetaData.ENTITY_NAME,
				new QueryImpl().eq(EntityMetaDataMetaData.FULL_NAME, sourceEntityMetaData.getName()));

		if (entityMetaDataEntity == null) throw new MolgenisDataAccessException(
				"Could not find EntityMetaDataEntity by the name of " + sourceEntityMetaData.getName());

		List<String> attributeIdentifiers = new ArrayList<String>();

		recursivelyCollectAttributeIdentifiers(entityMetaDataEntity.getEntities(EntityMetaDataMetaData.ATTRIBUTES),
				attributeIdentifiers);

		return attributeIdentifiers;
	}

	private void recursivelyCollectAttributeIdentifiers(Iterable<Entity> attributeEntities,
			List<String> attributeIdentifiers)
	{
		for (Entity attributeEntity : attributeEntities)
		{
			if (!attributeEntity.getString(AttributeMetaDataMetaData.DATA_TYPE)
					.equals(MolgenisFieldTypes.COMPOUND.toString()))
			{
				attributeIdentifiers.add(attributeEntity.getString(AttributeMetaDataMetaData.IDENTIFIER));
			}
			Iterable<Entity> entities = attributeEntity.getEntities(AttributeMetaDataMetaData.PARTS);

			if (entities != null)
			{
				recursivelyCollectAttributeIdentifiers(entities, attributeIdentifiers);
			}
		}
	}

	public AttributeMetaData entityToAttributeMetaData(Entity attributeEntity, EntityMetaData entityMetaData)
	{
		String attributeName = attributeEntity.getString(AttributeMetaDataMetaData.NAME);
		AttributeMetaData attribute = entityMetaData.getAttribute(attributeName);
		if (attribute == null)
		{
			throw new MolgenisDataAccessException("The attributeMetaData : " + attributeName
					+ " does not exsit in EntityMetaData : " + entityMetaData.getName());
		}
		return attribute;
	}

	public List<OntologyTerm> findTags(String description, List<String> ontologyIds)
	{
		Set<String> searchTerms = removeStopWords(description);

		List<OntologyTerm> matchingOntologyTerms = ontologyService.findOntologyTerms(ontologyIds, searchTerms,
				MAX_NUM_TAGS);

		return matchingOntologyTerms;
	}

	public String processQueryString(String queryString)
	{
		return StringUtils.join(removeStopWords(queryString), SPACE_CHAR);
	}

	public String parseBoostQueryString(String queryString, double boost)
	{
		return join(removeStopWords(queryString).stream().map(word -> word + CARET_CHARACTER + boost).collect(toSet()),
				SPACE_CHAR);
	}

	public String escapeCharsExcludingCaretChar(String string)
	{
		return QueryParser.escape(string).replace(ESCAPED_CARET_CHARACTER, CARET_CHARACTER);
	}

	public Set<String> removeStopWords(String description)
	{
		Set<String> searchTerms = stream(description.split(ILLEGAL_CHARS_REGEX)).map(String::toLowerCase)
				.filter(w -> !NGramDistanceAlgorithm.STOPWORDSLIST.contains(w) && StringUtils.isNotEmpty(w))
				.collect(Collectors.toSet());
		return searchTerms;
	}

	private Double getBestInverseDocumentFrequency(List<String> terms)
	{
		Optional<String> findFirst = terms.stream().sorted(new Comparator<String>()
		{
			public int compare(String o1, String o2)
			{
				return Integer.compare(o1.length(), o2.length());
			}
		}).findFirst();

		return findFirst.isPresent() ? termFrequencyService.getTermFrequency(findFirst.get()) : null;
	}
}
