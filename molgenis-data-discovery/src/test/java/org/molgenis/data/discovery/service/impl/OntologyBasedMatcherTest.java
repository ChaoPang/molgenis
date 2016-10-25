package org.molgenis.data.discovery.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.bean.SearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.utils.NGramDistanceAlgorithm;
import org.molgenis.ontology.utils.Stemmer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.data.QueryRule.Operator.*;
import static org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData.BiobankAttributeDataType.CATEGORICAL;
import static org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData.IDENTIFIER;

@ContextConfiguration(classes = OntologyBasedMatcherTest.Config.class)
public class OntologyBasedMatcherTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	BiobankUniverseRepository biobankUniverseRepository;

	@Autowired
	QueryExpansionService queryExpansionService;

	BiobankSampleCollection biobankSampleCollection;

	OntologyBasedMatcher ontologyBasedMatcher;

	BiobankSampleAttribute vegAttribute;

	BiobankSampleAttribute tomatoAttribute;

	BiobankSampleAttribute beanAttribute;

	BiobankSampleAttribute diseaseAttribute;

	OntologyTerm beanOntologyTerm;

	OntologyTerm vegOntologyTerm;

	OntologyTerm tomatoOntologyTerm;

	OntologyTerm diseaseOntologyTerm;

	@BeforeMethod
	public void init()
	{
		biobankSampleCollection = BiobankSampleCollection.create("test");

		beanOntologyTerm = OntologyTerm
				.create("ot1", "iri1", "Bean", StringUtils.EMPTY, Collections.emptyList(), Arrays.asList("1.2.3.4.5.6"),
						Collections.emptyList(), Collections.emptyList());

		vegOntologyTerm = OntologyTerm.create("ot2", "iri2", "Vegetables", StringUtils.EMPTY, Collections.emptyList(),
				Arrays.asList("1.2.3.4.5"), Collections.emptyList(), Collections.emptyList());

		tomatoOntologyTerm = OntologyTerm.create("ot3", "iri3", "Tomatoes", StringUtils.EMPTY, Collections.emptyList(),
				Arrays.asList("1.2.3.4.5.7"), Collections.emptyList(), Collections.emptyList());

		diseaseOntologyTerm = OntologyTerm.create("ot4", "iri4", "Disease", StringUtils.EMPTY, Collections.emptyList(),
				Arrays.asList("1.4.5.6.7"), Collections.emptyList(), Collections.emptyList());

		IdentifiableTagGroup beanTag = IdentifiableTagGroup
				.create("tag1", Arrays.asList(beanOntologyTerm), Collections.emptyList(), "bean", 0.5f);

		IdentifiableTagGroup tomatoTag = IdentifiableTagGroup
				.create("tag2", Arrays.asList(tomatoOntologyTerm), Collections.emptyList(), "tomato", 0.5f);

		IdentifiableTagGroup vegTag = IdentifiableTagGroup
				.create("tag3", Arrays.asList(vegOntologyTerm), Collections.emptyList(), "vegetables", 0.5f);

		IdentifiableTagGroup diseaseTag = IdentifiableTagGroup
				.create("tag4", Arrays.asList(diseaseOntologyTerm), Collections.emptyList(), "disease", 1.0f);

		tomatoAttribute = BiobankSampleAttribute
				.create("1", "tomato", "tomatoes", StringUtils.EMPTY, CATEGORICAL, biobankSampleCollection,
						Arrays.asList(tomatoTag));

		beanAttribute = BiobankSampleAttribute
				.create("2", "bean", "consumption of beans", StringUtils.EMPTY, CATEGORICAL, biobankSampleCollection,
						Arrays.asList(beanTag));

		vegAttribute = BiobankSampleAttribute
				.create("3", "vegetables", "consumption of vegetables", StringUtils.EMPTY, CATEGORICAL,
						biobankSampleCollection, Arrays.asList(vegTag));

		diseaseAttribute = BiobankSampleAttribute
				.create("4", "diseases", "History of Disease", StringUtils.EMPTY, CATEGORICAL, biobankSampleCollection,
						Arrays.asList(diseaseTag));

		ontologyBasedMatcher = new OntologyBasedMatcher(
				Arrays.asList(tomatoAttribute, beanAttribute, vegAttribute, diseaseAttribute),
				biobankUniverseRepository, queryExpansionService);
	}

	@Test
	public void testLexicalSearchBiobankSampleAttributes()
	{
		SearchParam searchParam = SearchParam.create(Sets.newHashSet(vegAttribute.getLabel()),
				Arrays.asList(TagGroup.create(vegOntologyTerm, "vegetables", 0.5f)));

		String queryString = SemanticSearchServiceUtils.splitIntoTerms(vegAttribute.getLabel()).stream()
				.filter(w -> !NGramDistanceAlgorithm.STOPWORDSLIST.contains(w)).map(Stemmer::stem)
				.collect(Collectors.joining(" "));

		QueryRule finalDisMaxQuery = new QueryRule(
				Arrays.asList(new QueryRule(BiobankSampleAttributeMetaData.LABEL, FUZZY_MATCH, queryString),
						new QueryRule(BiobankSampleAttributeMetaData.DESCRIPTION, FUZZY_MATCH, queryString)));

		finalDisMaxQuery.setOperator(Operator.DIS_MAX);

		List<QueryRule> finalQueryRules = Lists
				.newArrayList(new QueryRule(IDENTIFIER, IN, Arrays.asList("1", "2", "3", "4")), new QueryRule(AND),
						finalDisMaxQuery);

		when(queryExpansionService.expand(searchParam)).thenReturn(finalDisMaxQuery);

		when(biobankUniverseRepository.getBiobankSampleAttributes(
				new QueryImpl(finalQueryRules).pageSize(OntologyBasedMatcher.MAX_NUMBER_LEXICAL_MATCHES)))
				.thenReturn(Arrays.asList(vegAttribute, beanAttribute));

		List<BiobankSampleAttribute> lexicalSearchBiobankSampleAttributes = ontologyBasedMatcher
				.lexicalSearchBiobankSampleAttributes(searchParam);

		Assert.assertEquals(lexicalSearchBiobankSampleAttributes, Arrays.asList(vegAttribute, beanAttribute));
	}

	@Test
	public void testSemanticSearchBiobankSampleAttributes()
	{
		List<BiobankSampleAttribute> semanticSearchBiobankSampleAttributes1 = ontologyBasedMatcher
				.semanticSearchBiobankSampleAttributes(vegOntologyTerm);

		Assert.assertEquals(semanticSearchBiobankSampleAttributes1,
				Arrays.asList(tomatoAttribute, beanAttribute, vegAttribute));

		List<BiobankSampleAttribute> semanticSearchBiobankSampleAttributes2 = ontologyBasedMatcher
				.semanticSearchBiobankSampleAttributes(beanOntologyTerm);

		Assert.assertEquals(semanticSearchBiobankSampleAttributes2, Arrays.asList(beanAttribute, vegAttribute));
	}

	@Test
	public void getAllParents()
	{
		List<String> actual = stream(
				ontologyBasedMatcher.getAllParents("A1836683.A2656910.A2655472.A2656419.A2655207").spliterator(), false)
				.collect(toList());

		List<String> expected = Lists
				.newArrayList("A1836683.A2656910.A2655472.A2656419", "A1836683.A2656910.A2655472", "A1836683.A2656910",
						"A1836683");

		Assert.assertEquals(actual, expected);
	}

	@Configuration
	public static class Config
	{
		@Bean
		public BiobankUniverseRepository biobankUniverseRepository()
		{
			return mock(BiobankUniverseRepository.class);
		}

		@Bean
		public QueryExpansionService queryExpansionService()
		{
			return mock(QueryExpansionService.class);
		}
	}
}
