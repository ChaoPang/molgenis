package org.molgenis.ontology.core.repository;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.elasticsearch.common.collect.ImmutableSet.of;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.mockito.ArgumentCaptor;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.Query;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.meta.OntologyMetaData;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.molgenis.ontology.core.meta.OntologyTermNodePathMetaData;
import org.molgenis.ontology.core.meta.OntologyTermSynonymMetaData;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

@ContextConfiguration(classes = OntologyTermRepositoryTest.Config.class)
public class OntologyTermRepositoryTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	DataService dataService;

	@Autowired
	OntologyTermRepository ontologyTermRepository;

	private Entity ontologyTermEntity;

	@BeforeTest
	public void beforeTest()
	{
		ontologyTermEntity = new MapEntity(OntologyTermMetaData.INSTANCE);
		ontologyTermEntity.set(OntologyTermMetaData.ID, "12");
		ontologyTermEntity.set(OntologyTermMetaData.ONTOLOGY, "34");
		ontologyTermEntity.set(OntologyTermMetaData.ONTOLOGY_TERM_IRI, "http://www.test.nl/iri");
		ontologyTermEntity.set(OntologyTermMetaData.ONTOLOGY_TERM_NAME, "Ontology term");
	}

	@Test
	public void testFindExcatOntologyTerms()
	{
		MapEntity synonymEntity1 = new MapEntity(OntologyTermSynonymMetaData.INSTANCE);
		synonymEntity1.set(OntologyTermSynonymMetaData.ID, "synonym-1");
		synonymEntity1.set(OntologyTermSynonymMetaData.ONTOLOGY_TERM_SYNONYM, "Weight Reduction Diet");

		MapEntity synonymEntity2 = new MapEntity(OntologyTermSynonymMetaData.INSTANCE);
		synonymEntity2.set(OntologyTermSynonymMetaData.ID, "synonym-2");
		synonymEntity2.set(OntologyTermSynonymMetaData.ONTOLOGY_TERM_SYNONYM, "Weight loss Diet");

		MapEntity synonymEntity3 = new MapEntity(OntologyTermSynonymMetaData.INSTANCE);
		synonymEntity3.set(OntologyTermSynonymMetaData.ID, "synonym-3");
		synonymEntity3.set(OntologyTermSynonymMetaData.ONTOLOGY_TERM_SYNONYM, "Diet, Reducing");

		MapEntity ontologyTermEntity1 = new MapEntity(OntologyTermMetaData.INSTANCE);
		ontologyTermEntity1.set(OntologyTermMetaData.ID, "1");
		ontologyTermEntity1.set(OntologyTermMetaData.ONTOLOGY, "34");
		ontologyTermEntity1.set(OntologyTermMetaData.ONTOLOGY_TERM_IRI, "http://www.test.nl/iri/1");
		ontologyTermEntity1.set(OntologyTermMetaData.ONTOLOGY_TERM_NAME, "Diet, Reducing");
		ontologyTermEntity1.set(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM,
				Arrays.asList(synonymEntity1, synonymEntity2, synonymEntity3));

		MapEntity synonymEntity4 = new MapEntity(OntologyTermSynonymMetaData.INSTANCE);
		synonymEntity4.set(OntologyTermSynonymMetaData.ID, "synonym-4");
		synonymEntity4.set(OntologyTermSynonymMetaData.ONTOLOGY_TERM_SYNONYM, "Weight");

		MapEntity ontologyTermEntity2 = new MapEntity(OntologyTermMetaData.INSTANCE);
		ontologyTermEntity2.set(OntologyTermMetaData.ID, "12");
		ontologyTermEntity2.set(OntologyTermMetaData.ONTOLOGY, "34");
		ontologyTermEntity2.set(OntologyTermMetaData.ONTOLOGY_TERM_IRI, "http://www.test.nl/iri/2");
		ontologyTermEntity2.set(OntologyTermMetaData.ONTOLOGY_TERM_NAME, "Weight");
		ontologyTermEntity2.set(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, Arrays.asList(synonymEntity4));

		ArgumentCaptor<Query> queryCaptor = forClass(Query.class);
		when(dataService.findAll(eq(OntologyTermMetaData.ENTITY_NAME), queryCaptor.capture()))
				.thenReturn(Stream.of(ontologyTermEntity1, ontologyTermEntity2));

		List<OntologyTerm> exactOntologyTerms = ontologyTermRepository.findExcatOntologyTerms(asList("1", "2"),
				of("weight"), 100);

		Assert.assertEquals(exactOntologyTerms, Arrays.asList(
				OntologyTerm.create("12", "http://www.test.nl/iri/2", "Weight", null, Arrays.asList("Weight"))));
	}

	@Test
	public void testFindOntologyTerms()
	{
		ArgumentCaptor<Query> queryCaptor = forClass(Query.class);
		when(dataService.findAll(eq(OntologyTermMetaData.ENTITY_NAME), queryCaptor.capture()))
				.thenReturn(Stream.of(ontologyTermEntity));

		List<OntologyTerm> terms = ontologyTermRepository.findOntologyTerms(asList("1", "2"),
				of("term1", "term2", "term3"), 100);

		assertEquals(terms,
				asList(OntologyTerm.create("12", "http://www.test.nl/iri", "Ontology term", null, emptyList())));
		assertEquals(queryCaptor.getValue().toString(),
				"rules=['ontology' IN [1, 2], AND, ('ontologyTermSynonym' FUZZY_MATCH 'term1', OR, 'ontologyTermSynonym' FUZZY_MATCH 'term2', OR, 'ontologyTermSynonym' FUZZY_MATCH 'term3')], pageSize=100");
	}

	@Test
	public void testGetChildOntologyTermsByNodePath()
	{
		Entity ontologyEntity = new MapEntity(ImmutableMap.of(OntologyMetaData.ONTOLOGY_IRI, "http://www.molgenis.org",
				OntologyMetaData.ONTOLOGY_NAME, "molgenis"));

		Entity nodePathEntity_1 = new MapEntity(
				ImmutableMap.of(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH, "0[0].1[1]"));
		Entity nodePathEntity_2 = new MapEntity(
				ImmutableMap.of(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH, "0[0].1[1].0[2]"));
		Entity nodePathEntity_3 = new MapEntity(
				ImmutableMap.of(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH, "0[0].1[1].1[2]"));

		MapEntity ontologyTermEntity_1 = new MapEntity();
		ontologyTermEntity_1.set(OntologyTermMetaData.ONTOLOGY, ontologyEntity);
		ontologyTermEntity_1.set(OntologyTermMetaData.ID, "1");
		ontologyTermEntity_1.set(OntologyTermMetaData.ONTOLOGY_TERM_IRI, "iri 1");
		ontologyTermEntity_1.set(OntologyTermMetaData.ONTOLOGY_TERM_NAME, "name 1");
		ontologyTermEntity_1.set(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH, Arrays.asList(nodePathEntity_1));
		ontologyTermEntity_1.set(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, Collections.emptyList());

		MapEntity ontologyTermEntity_2 = new MapEntity();
		ontologyTermEntity_2.set(OntologyTermMetaData.ONTOLOGY, ontologyEntity);
		ontologyTermEntity_2.set(OntologyTermMetaData.ID, "2");
		ontologyTermEntity_2.set(OntologyTermMetaData.ONTOLOGY_TERM_IRI, "iri 2");
		ontologyTermEntity_2.set(OntologyTermMetaData.ONTOLOGY_TERM_NAME, "name 2");
		ontologyTermEntity_2.set(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH, Arrays.asList(nodePathEntity_2));
		ontologyTermEntity_2.set(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, Collections.emptyList());

		MapEntity ontologyTermEntity_3 = new MapEntity();
		ontologyTermEntity_3.set(OntologyTermMetaData.ONTOLOGY, ontologyEntity);
		ontologyTermEntity_3.set(OntologyTermMetaData.ID, "3");
		ontologyTermEntity_3.set(OntologyTermMetaData.ONTOLOGY_TERM_IRI, "iri 3");
		ontologyTermEntity_3.set(OntologyTermMetaData.ONTOLOGY_TERM_NAME, "name 3");
		ontologyTermEntity_3.set(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH, Arrays.asList(nodePathEntity_3));
		ontologyTermEntity_3.set(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, Collections.emptyList());

		OntologyTerm ontologyTerm_1 = OntologyTerm.create("1", "iri 1", "name 1", null, emptyList(),
				Arrays.asList("0[0].1[1]"));
		OntologyTerm ontologyTerm_2 = OntologyTerm.create("2", "iri 2", "name 2", null, emptyList(),
				Arrays.asList("0[0].1[1].0[2]"));
		OntologyTerm ontologyTerm_3 = OntologyTerm.create("3", "iri 3", "name 3", null, emptyList(),
				asList("0[0].1[1].1[2]"));

		when(dataService.findAll(OntologyTermMetaData.ENTITY_NAME,
				new QueryImpl(new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH, Operator.FUZZY_MATCH,
						"\"0[0].1[1]\"")).and().eq(OntologyTermMetaData.ONTOLOGY, ontologyEntity))).thenReturn(
								Stream.of(ontologyTermEntity_1, ontologyTermEntity_2, ontologyTermEntity_3));

		List<OntologyTerm> childOntologyTermsByNodePath = newArrayList(ontologyTermRepository.childOntologyTermStream(
				ontologyTerm_1, ontologyEntity,
				nodePathEntity_1.getString(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH), (x, y) -> true));

		assertEquals(childOntologyTermsByNodePath.size(), 2);
		assertEquals(childOntologyTermsByNodePath.get(0), ontologyTerm_2);
		assertEquals(childOntologyTermsByNodePath.get(1), ontologyTerm_3);
	}

	@Test
	public void testCalculateNodePathDistance()
	{
		// Case 1
		assertEquals(ontologyTermRepository.calculateNodePathDistance("0[0].0[1]", "0[0].0[1].1[2]"), 1);

		// Case 2
		assertEquals(ontologyTermRepository.calculateNodePathDistance("0[0].0[1].1[2]", "0[0].0[1]"), 1);

		// Case 3
		assertEquals(ontologyTermRepository.calculateNodePathDistance("0[0].0[1].1[2].2[3]", "0[0].0[1].0[2].2[3]"), 4);

		// Case 4
		assertEquals(ontologyTermRepository.calculateNodePathDistance("0[0].0[1]", "0[0].0[1].0[2].1[3].2[4]"), 3);

		// Case 5
		assertEquals(ontologyTermRepository.calculateNodePathDistance("0[0].0[1]", "0[0].0[1]"), 0);

		// Case 6
		assertEquals(ontologyTermRepository.calculateNodePathDistance(StringUtils.EMPTY, StringUtils.EMPTY), 20);

		// Case 7
		assertEquals(ontologyTermRepository.calculateNodePathDistance("0[0].0[1]", StringUtils.EMPTY), 12);
	}

	@Test
	public void testCalculateRelatedness()
	{
		DecimalFormat decimalFormat = new DecimalFormat("##.##", DecimalFormatSymbols.getInstance(Locale.US));

		String case1 = decimalFormat.format(ontologyTermRepository.calculateRelatedness("0[0].0[1]", "0[0].0[1].1[2]"));
		assertEquals(case1, "0.8");
		String case2 = decimalFormat.format(ontologyTermRepository.calculateRelatedness("0[0].0[1].1[2]", "0[0].0[1]"));
		assertEquals(case2, "0.8");
		String case3 = decimalFormat
				.format(ontologyTermRepository.calculateRelatedness("0[0].0[1].1[2].2[3]", "0[0].0[1].0[2].2[3]"));
		assertEquals(case3, "0.5");
		String case4 = decimalFormat
				.format(ontologyTermRepository.calculateRelatedness("0[0].0[1]", "0[0].0[1].0[2].1[3].2[4]"));
		assertEquals(case4, "0.57");
		String case5 = decimalFormat.format(ontologyTermRepository.calculateRelatedness("0[0].0[1]", "0[0].0[1]"));
		assertEquals(case5, "1");
		String case6 = decimalFormat
				.format(ontologyTermRepository.calculateRelatedness(StringUtils.EMPTY, StringUtils.EMPTY));
		assertEquals(case6, "0.09");
		String case7 = decimalFormat
				.format(ontologyTermRepository.calculateRelatedness("0[0].0[1]", StringUtils.EMPTY));
		assertEquals(case7, "0.15");
	}

	@Test
	public void testGetOntologyTerm()
	{
		when(dataService.findOne(OntologyTermMetaData.ENTITY_NAME,
				QueryImpl.EQ(OntologyTermMetaData.ONTOLOGY_TERM_IRI, "http://www.test.nl/iri")))
						.thenReturn(ontologyTermEntity);

		String[] iris =
		{ "http://www.test.nl/iri" };

		OntologyTerm ontologyTerm = ontologyTermRepository.getOntologyTerm(iris);
		assertEquals(ontologyTerm, OntologyTerm.create("12", "http://www.test.nl/iri", "Ontology term", emptyList()));
	}

	@Configuration
	public static class Config
	{
		@Bean
		public DataService dataService()
		{
			return mock(DataService.class);
		}

		@Bean
		public OntologyTermRepository ontologyTermRepository()
		{
			return new OntologyTermRepository(dataService());
		}
	}
}