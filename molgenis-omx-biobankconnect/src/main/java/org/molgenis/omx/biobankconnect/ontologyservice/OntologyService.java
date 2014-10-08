package org.molgenis.omx.biobankconnect.ontologyservice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.Entity;
import org.molgenis.data.Query;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.omx.biobankconnect.ontology.repository.OntologyIndexRepository;
import org.molgenis.omx.biobankconnect.ontology.repository.OntologyTermIndexRepository;
import org.molgenis.omx.biobankconnect.ontology.repository.OntologyTermQueryRepository;
import org.molgenis.omx.biobankconnect.ontologyindexer.AsyncOntologyIndexer;
import org.molgenis.omx.biobankconnect.utils.NGramMatchingModel;
import org.molgenis.omx.observ.target.Ontology;
import org.molgenis.search.Hit;
import org.molgenis.search.SearchRequest;
import org.molgenis.search.SearchResult;
import org.molgenis.search.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.tartarus.snowball.ext.PorterStemmer;

public class OntologyService
{
	private final SearchService searchService;
	private static final List<String> ELASTICSEARCH_RESERVED_WORDS = Arrays.asList("or", "and", "if");
	private static final String COMBINED_SCORE = "combinedScore";
	private static final String FUZZY_MATCH_SIMILARITY = "~0.8";
	private static final String SCORE = "score";
	private static final String NON_WORD_SEPARATOR = "[^a-zA-Z0-9]";
	private static final int MAX_NUMBER_MATCHES = 100;
	private static final PorterStemmer stemmer = new PorterStemmer();
	public static final Character DEFAULT_SEPARATOR = ';';
	public static final String DEFAULT_MATCHING_NAME_FIELD = "name";
	public static final String DEFAULT_MATCHING_SYNONYM_FIELD = "synonym";
	private static final String MAX_SCORE_FIELD = "maxScoreField";

	@Autowired
	public OntologyService(SearchService searchService)
	{
		if (searchService == null) throw new IllegalArgumentException("SearchService is null");
		this.searchService = searchService;
	}

	public Hit getOntologyByIri(String ontologyIri)
	{
		QueryImpl q = new QueryImpl();
		q.pageSize(Integer.MAX_VALUE);
		q.addRule(new QueryRule(OntologyIndexRepository.ENTITY_TYPE, Operator.EQUALS,
				OntologyIndexRepository.TYPE_ONTOLOGY));
		q.addRule(new QueryRule(Operator.AND));
		q.addRule(new QueryRule(OntologyIndexRepository.ONTOLOGY_IRI, Operator.EQUALS, ontologyIri));
		SearchRequest searchRequest = new SearchRequest(AsyncOntologyIndexer.createOntologyDocumentType(ontologyIri),
				q, null);
		List<Hit> searchHits = searchService.search(searchRequest).getSearchHits();
		if (searchHits.size() > 0) return searchHits.get(0);
		return new Hit(null, AsyncOntologyIndexer.createOntologyDocumentType(ontologyIri),
				Collections.<String, Object> emptyMap());
	}

	public Hit findOntologyTerm(String ontologyIri, String ontologyTermIri, String nodePath)
	{
		Query q = new QueryImpl().eq(OntologyTermIndexRepository.NODE_PATH, nodePath).and()
				.eq(OntologyTermIndexRepository.ONTOLOGY_TERM_IRI, ontologyTermIri).pageSize(5000);
		String documentType = AsyncOntologyIndexer.createOntologyTermDocumentType(ontologyIri);
		SearchResult result = searchService.search(new SearchRequest(documentType, q, null));
		for (Hit hit : result.getSearchHits())
		{
			if (hit.getColumnValueMap().get(OntologyTermIndexRepository.NODE_PATH).toString().equals(nodePath)) return hit;
		}
		return new Hit(null, documentType, Collections.<String, Object> emptyMap());
	}

	public List<Hit> getChildren(String ontologyIri, String parentOntologyTermIri, String parentNodePath)
	{
		Query q = new QueryImpl().eq(OntologyTermIndexRepository.PARENT_NODE_PATH, parentNodePath).and()
				.eq(OntologyTermIndexRepository.PARENT_ONTOLOGY_TERM_IRI, parentOntologyTermIri).pageSize(5000);
		String documentType = AsyncOntologyIndexer.createOntologyTermDocumentType(ontologyIri);
		List<Hit> listOfHits = new ArrayList<Hit>();
		Set<String> processedOntologyTerms = new HashSet<String>();
		for (Hit hit : searchService.search(new SearchRequest(documentType, q, null)).getSearchHits())
		{
			String ontologyTermIri = hit.getColumnValueMap().get(OntologyTermIndexRepository.ONTOLOGY_TERM_IRI)
					.toString();
			if (!processedOntologyTerms.contains(ontologyTermIri))
			{
				listOfHits.add(hit);
				processedOntologyTerms.add(ontologyTermIri);
			}
		}
		return listOfHits;
	}

	public List<Hit> getRootOntologyTerms(String ontologyIri)
	{
		SearchRequest searchRequest = new SearchRequest(
				AsyncOntologyIndexer.createOntologyTermDocumentType(ontologyIri), new QueryImpl().pageSize(
						Integer.MAX_VALUE).eq(OntologyTermIndexRepository.ROOT, true), null);
		List<Hit> listOfHits = new ArrayList<Hit>();
		Set<String> processedOntologyTerms = new HashSet<String>();
		for (Hit hit : searchService.search(searchRequest))
		{
			String ontologyTermIri = hit.getColumnValueMap().get(OntologyTermIndexRepository.ONTOLOGY_TERM_IRI)
					.toString();
			if (!processedOntologyTerms.contains(ontologyTermIri))
			{
				listOfHits.add(hit);
				processedOntologyTerms.add(ontologyTermIri);
			}
		}
		return listOfHits;
	}

	public List<Ontology> getAllOntologies()
	{
		List<Ontology> ontologies = new ArrayList<Ontology>();
		QueryImpl q = new QueryImpl();
		q.pageSize(Integer.MAX_VALUE);
		q.addRule(new QueryRule(OntologyIndexRepository.ENTITY_TYPE, Operator.EQUALS,
				OntologyIndexRepository.TYPE_ONTOLOGY));
		SearchRequest searchRequest = new SearchRequest(null, q, null);
		for (Hit hit : searchService.search(searchRequest).getSearchHits())
		{
			Ontology ontology = new Ontology();
			ontology.setIdentifier(hit.getColumnValueMap().get(OntologyIndexRepository.ONTOLOGY_IRI).toString());
			ontology.setOntologyURI(hit.getColumnValueMap().get(OntologyIndexRepository.ONTOLOGY_IRI).toString());
			ontology.setName(hit.getColumnValueMap().get(OntologyIndexRepository.ONTOLOGY_NAME).toString());
			ontologies.add(ontology);
		}
		return ontologies;
	}

	/**
	 * This method is to search multiple fields information collected from the
	 * given entity. The fields 'name' and 'synonym' in the entity will be
	 * translated into ontologyTermSynonym in ElasticSearch. However there might
	 * be other dynamic fields involved such as OMIM
	 * 
	 * @param ontologyIriBySession
	 * @param entity
	 * @return
	 */
	public OntologyServiceResult searchEntity(String ontologyIri, Entity entity)
	{
		List<QueryRule> allQueryRules = new ArrayList<QueryRule>();
		List<QueryRule> rulesForOntologyTermFields = new ArrayList<QueryRule>();
		for (String attributeName : entity.getAttributeNames())
		{
			if (!StringUtils.isEmpty(entity.getString(attributeName)))
			{
				// The attribute name is either equal to 'Name' or starts with
				// string 'Synonym'
				if (DEFAULT_MATCHING_NAME_FIELD.equals(attributeName.toLowerCase())
						|| attributeName.toLowerCase().startsWith(DEFAULT_MATCHING_SYNONYM_FIELD))
				{
					String medicalStemProxy = medicalStemProxy(entity.getString(attributeName));
					if (!StringUtils.isEmpty(medicalStemProxy))
					{
						rulesForOntologyTermFields.add(new QueryRule(OntologyTermIndexRepository.SYNONYMS,
								Operator.EQUALS, medicalStemProxy));
					}
				}
				else if (entity.get(attributeName) != null
						&& !StringUtils.isEmpty(entity.get(attributeName).toString()))
				{
					allQueryRules.add(new QueryRule(attributeName, Operator.EQUALS, entity.get(attributeName)));
				}
			}
		}

		if (rulesForOntologyTermFields.size() == 0) return new OntologyServiceResult(
				"Please specify the headers of the input data!");

		QueryRule nestedQueryRule = new QueryRule(rulesForOntologyTermFields);
		nestedQueryRule.setOperator(Operator.DIS_MAX);
		allQueryRules.add(nestedQueryRule);

		QueryRule finalQueryRule = new QueryRule(allQueryRules);
		finalQueryRule.setOperator(Operator.DIS_MAX);

		SearchRequest request = new SearchRequest(AsyncOntologyIndexer.createOntologyTermDocumentType(ontologyIri),
				new QueryImpl(finalQueryRule).pageSize(MAX_NUMBER_MATCHES), null);

		Iterator<Hit> iterator = searchService.search(request).getSearchHits().iterator();
		Map<String, Object> inputData = new HashMap<String, Object>();
		String maxScoreField = null;
		List<ComparableHit> comparableHits = new ArrayList<ComparableHit>();
		int count = 0;
		while (iterator.hasNext())
		{
			Hit hit = iterator.next();
			Map<String, Object> columnValueMap = hit.getColumnValueMap();
			BigDecimal maxNgramScore = new BigDecimal(0);
			for (String attributeName : entity.getAttributeNames())
			{
				if (!StringUtils.isEmpty(entity.getString(attributeName)))
				{
					if (DEFAULT_MATCHING_NAME_FIELD.equals(attributeName.toLowerCase())
							|| attributeName.toLowerCase().startsWith(DEFAULT_MATCHING_SYNONYM_FIELD))
					{
						BigDecimal ngramScore = matchOntologyTerm(entity.getString(attributeName), hit);
						if (maxNgramScore.doubleValue() < ngramScore.doubleValue())
						{
							maxNgramScore = ngramScore;
							maxScoreField = attributeName;
						}
						if (count == 0) inputData.put(attributeName, entity.getString(attributeName));
					}
					else
					{
						for (String key : columnValueMap.keySet())
						{
							// Check if indexed ontology term contains such
							// external database reference
							if (attributeName.equalsIgnoreCase(key))
							{
								if (columnValueMap.containsKey(key)
										&& !StringUtils.isEmpty(columnValueMap.get(key).toString()))
								{
									for (Object databaseId : (List<?>) columnValueMap.get(key))
									{
										BigDecimal ngramScore = new BigDecimal(NGramMatchingModel.stringMatching(
												entity.getString(attributeName), databaseId.toString()));
										if (maxNgramScore.doubleValue() < ngramScore.doubleValue())
										{
											maxNgramScore = ngramScore;
											maxScoreField = attributeName;
										}
									}
									if (count == 0) inputData.put(attributeName, entity.getString(attributeName));
								}
							}
						}
					}
				}
			}
			comparableHits.add(new ComparableHit(hit, maxNgramScore, maxScoreField));
			count++;
		}
		Collections.sort(comparableHits);
		return convertResults(inputData, comparableHits);
	}

	/**
	 * This method is to search simple query in a specified ontology. By
	 * default, ontology ontologyTermSynonym field is involved in the
	 * ElasticSearch
	 * 
	 * @param ontologyIri
	 * @param queryString
	 * @return
	 */
	public OntologyServiceResult search(String ontologyIri, String queryString)
	{
		Set<String> uniqueTerms = new HashSet<String>(Arrays.asList(queryString.toLowerCase().trim()
				.split(NON_WORD_SEPARATOR)));
		uniqueTerms.removeAll(NGramMatchingModel.STOPWORDSLIST);
		List<QueryRule> rules = new ArrayList<QueryRule>();
		for (String term : uniqueTerms)
		{
			if (!StringUtils.isEmpty(term) && !term.matches(OntologyTermQueryRepository.MULTI_WHITESPACES))
			{
				stemmer.setCurrent(term.replaceAll(OntologyTermQueryRepository.ILLEGAL_CHARACTERS_PATTERN,
						StringUtils.EMPTY));
				stemmer.stem();
				rules.add(new QueryRule(OntologyTermIndexRepository.SYNONYMS, Operator.EQUALS, stemmer.getCurrent()
						+ FUZZY_MATCH_SIMILARITY));
			}
		}
		QueryRule finalQuery = new QueryRule(rules);
		finalQuery.setOperator(Operator.SHOULD);
		SearchRequest request = new SearchRequest(AsyncOntologyIndexer.createOntologyTermDocumentType(ontologyIri),
				new QueryImpl(finalQuery).pageSize(MAX_NUMBER_MATCHES), null);
		Iterator<Hit> iterator = searchService.search(request).getSearchHits().iterator();

		Map<String, Object> inputData = new HashMap<String, Object>();
		inputData.put(DEFAULT_MATCHING_NAME_FIELD, queryString);
		List<ComparableHit> comparableHits = new ArrayList<ComparableHit>();
		String cleanedQueryString = StringUtils.join(uniqueTerms,
				OntologyTermQueryRepository.ILLEGAL_CHARACTERS_REPLACEMENT);
		while (iterator.hasNext())
		{
			Hit hit = iterator.next();
			BigDecimal ngramScore = matchOntologyTerm(cleanedQueryString, hit);
			BigDecimal luceneScore = new BigDecimal(hit.getColumnValueMap().get(SCORE).toString());
			comparableHits.add(new ComparableHit(hit, luceneScore.multiply(ngramScore), null));
		}
		Collections.sort(comparableHits);
		return convertResults(inputData, comparableHits);
	}

	private BigDecimal matchOntologyTerm(String queryString, Hit hit)
	{
		Map<String, Object> columnValueMap = hit.getColumnValueMap();
		String ontologyTermSynonym = columnValueMap.get(OntologyTermIndexRepository.SYNONYMS).toString();
		String ontologyTerm = columnValueMap.get(OntologyTermIndexRepository.ONTOLOGY_TERM).toString();

		BigDecimal ngramScore = null;
		if (!ontologyTerm.equalsIgnoreCase(ontologyTermSynonym))
		{
			double score_1 = NGramMatchingModel.stringMatching(queryString, ontologyTerm);
			double score_2 = NGramMatchingModel.stringMatching(queryString, ontologyTermSynonym);

			ngramScore = new BigDecimal(score_1 > score_2 ? score_1 : score_2);
		}
		else
		{
			ngramScore = new BigDecimal(NGramMatchingModel.stringMatching(queryString, ontologyTermSynonym));
		}

		return ngramScore;
	}

	private String medicalStemProxy(String queryString)
	{
		StringBuilder stringBuilder = new StringBuilder();
		Set<String> uniqueTerms = new HashSet<String>(Arrays.asList(queryString.toLowerCase().trim()
				.split(NON_WORD_SEPARATOR)));
		uniqueTerms.removeAll(NGramMatchingModel.STOPWORDSLIST);
		for (String term : uniqueTerms)
		{
			if (!StringUtils.isEmpty(term) && !term.matches(OntologyTermQueryRepository.MULTI_WHITESPACES)
					&& !(ELASTICSEARCH_RESERVED_WORDS.contains(term)))
			{
				stemmer.setCurrent(term.replaceAll(OntologyTermQueryRepository.ILLEGAL_CHARACTERS_PATTERN,
						StringUtils.EMPTY));
				stemmer.stem();
				stringBuilder.append(stemmer.getCurrent()).append(FUZZY_MATCH_SIMILARITY)
						.append(OntologyTermQueryRepository.SINGLE_WHITESPACE);
			}
		}
		return stringBuilder.toString().trim();
	}

	private OntologyServiceResult convertResults(Map<String, Object> inputData, List<ComparableHit> comparableHits)
	{
		List<Hit> hits = new ArrayList<Hit>();
		Set<String> uniqueIdentifiers = new HashSet<String>();
		for (ComparableHit comparableHit : comparableHits)
		{
			Hit hit = comparableHit.getHit();
			String identifier = hit.getColumnValueMap().get(OntologyTermIndexRepository.ONTOLOGY_TERM_IRI).toString();
			if (uniqueIdentifiers.contains(identifier)) continue;
			uniqueIdentifiers.add(identifier);
			Map<String, Object> columnValueMap = new HashMap<String, Object>();
			columnValueMap.putAll(hit.getColumnValueMap());
			columnValueMap.put(COMBINED_SCORE, comparableHit.getSimilarityScore().doubleValue());
			columnValueMap.put(MAX_SCORE_FIELD, comparableHit.getMaxScoreField());
			Hit copyHit = new Hit(hit.getId(), hit.getDocumentType(), columnValueMap);
			hits.add(copyHit);
		}
		return new OntologyServiceResult(inputData, hits, hits.size());
	}

	class ComparableHit implements Comparable<ComparableHit>
	{
		private final String maxScoreField;
		private final Hit hit;
		private final BigDecimal similarityScore;

		public ComparableHit(Hit hit, BigDecimal similarityScore, String maxScoreField)
		{
			this.hit = hit;
			this.similarityScore = similarityScore;
			this.maxScoreField = maxScoreField;
		}

		public String getMaxScoreField()
		{
			return maxScoreField;
		}

		private BigDecimal getSimilarityScore()
		{
			return similarityScore;
		}

		public Hit getHit()
		{
			return hit;
		}

		@Override
		public int compareTo(ComparableHit other)
		{
			return similarityScore.compareTo(other.getSimilarityScore()) * (-1);
		}
	}
}