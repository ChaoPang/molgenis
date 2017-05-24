package org.molgenis.ontology.sorta;

import com.google.common.collect.ImmutableMap;
import org.molgenis.data.AbstractMolgenisSpringTest;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.QueryRule;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.support.DynamicEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.config.OntologyTestConfig;
import org.molgenis.ontology.core.meta.*;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.roc.InformationContentService;
import org.molgenis.ontology.sorta.bean.SortaHit;
import org.molgenis.ontology.sorta.meta.OntologyTermHitMetaData;
import org.molgenis.ontology.sorta.service.SortaService;
import org.molgenis.ontology.sorta.service.impl.SortaServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.data.QueryRule.Operator.*;
import static org.molgenis.data.meta.AttributeType.STRING;
import static org.molgenis.ontology.core.meta.OntologyMetaData.ONTOLOGY;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM;
import static org.testng.Assert.assertEquals;

@ContextConfiguration(classes = { SortaServiceImplTest.Config.class })
public class SortaServiceImplTest extends AbstractMolgenisSpringTest
{
	private static final String ONTOLOGY_IRI = "http://www.molgenis.org/";

	@Autowired
	private SortaService sortaService;

	@Autowired
	private DataService dataService;

	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private OntologyFactory ontologyFactory;

	@Autowired
	private OntologyTermFactory ontologyTermFactory;

	@Autowired
	private OntologyTermSynonymFactory ontologyTermSynonymFactory;

	@Autowired
	private OntologyTermDynamicAnnotationFactory ontologyTermDynamicAnnotationFactory;

	@BeforeMethod
	public void beforeMethod()
	{
		// Mock ontology entity
		OntologyEntity ontology = ontologyFactory.create();
		ontology.setId("1");
		ontology.setOntologyIri(ONTOLOGY_IRI);
		ontology.setOntologyName("name");

		when(ontologyService.getOntology(ONTOLOGY_IRI)).thenReturn(Ontology.create("1", ONTOLOGY_IRI, "name"));

		QueryRule queryRule = new QueryRule(
				singletonList(new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH, "hear")));
		queryRule.setOperator(DIS_MAX);

		QueryRule queryRule2 = new QueryRule(
				singletonList(new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH, "impair")));
		queryRule2.setOperator(DIS_MAX);

		when(dataService.findAll(ONTOLOGY)).thenReturn(Collections.<Entity>singletonList(ontology).stream());

		// ########################### TEST ONE ###########################
		// Mock the first ontology term entity only with name
		OntologyTermSynonym ontologyTermSynonym0 = ontologyTermSynonymFactory.create();
		ontologyTermSynonym0.setOntologyTermSynonym("hearing impairment");

		OntologyTermEntity ontologyTerm0 = ontologyTermFactory.create();
		ontologyTerm0.setId("1");
		ontologyTerm0.setOntology(ontology);
		ontologyTerm0.setOntologyTermName("hearing impairment");
		ontologyTerm0.setOntologyTermIri(ONTOLOGY_IRI + '1');
		ontologyTerm0.setOntologyTermSynonyms(singletonList(ontologyTermSynonym0));
		ontologyTerm0.setOntologyTermDynamicAnnotations(emptyList());

		// Mock the second ontology term entity only with name
		OntologyTermSynonym ontologyTermSynonym1 = ontologyTermSynonymFactory.create();
		ontologyTermSynonym1.setOntologyTermSynonym("mixed hearing impairment");

		OntologyTermEntity ontologyTerm1 = ontologyTermFactory.create();
		ontologyTerm1.setId("2");
		ontologyTerm1.setOntology(ontology);
		ontologyTerm1.setOntologyTermName("mixed hearing impairment");
		ontologyTerm1.setOntologyTermIri(ONTOLOGY_IRI + '2');
		ontologyTerm1.setOntologyTermSynonyms(singletonList(ontologyTermSynonym1));
		ontologyTerm1.setOntologyTermDynamicAnnotations(emptyList());

		// DataService action for regular matching ontology term synonyms
		QueryRule disMaxRegularQueryRule = new QueryRule(singletonList(
				new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH, "hear~0.8 impair~0.8")));
		disMaxRegularQueryRule.setOperator(DIS_MAX);

		List<QueryRule> finalQueryRules = asList(new QueryRule(OntologyTermMetaData.ONTOLOGY, EQUALS, ontology.getId()),
				new QueryRule(AND), disMaxRegularQueryRule);

		when(dataService.findAll(ONTOLOGY_TERM, new QueryImpl<OntologyTermEntity>(finalQueryRules).pageSize(50),
				OntologyTermEntity.class))
				.thenReturn(Arrays.<OntologyTermEntity>asList(ontologyTerm0, ontologyTerm1).stream());

		// DataService action for n-gram matching ontology term synonyms
		QueryRule disMaxNGramQueryRule = new QueryRule(singletonList(
				new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH_NGRAM, "hear impair")));
		disMaxNGramQueryRule.setOperator(DIS_MAX);

		when(dataService.findAll(ONTOLOGY_TERM, new QueryImpl<OntologyTermEntity>(
				asList(new QueryRule(OntologyTermMetaData.ONTOLOGY, EQUALS, ontology.getId()), new QueryRule(AND),
						disMaxNGramQueryRule)).pageSize(10), OntologyTermEntity.class))
				.thenReturn(Arrays.<OntologyTermEntity>asList(ontologyTerm0, ontologyTerm1).stream());

		// ########################### TEST TWO ###########################
		OntologyTermSynonym ontologyTermSynonym2 = ontologyTermSynonymFactory.create();
		ontologyTermSynonym2.setOntologyTermSynonym("ot_3");

		// Mock ontologyTermDynamicAnnotation entities
		OntologyTermDynamicAnnotation ontologyTermDynamicAnnotation_3_1 = ontologyTermDynamicAnnotationFactory.create();
		ontologyTermDynamicAnnotation_3_1.setName("OMIM");
		ontologyTermDynamicAnnotation_3_1.setValue("123456");
		ontologyTermDynamicAnnotation_3_1.setLabel("OMIM:123456");

		// Mock ontologyTerm entity based on the previous entities defined
		OntologyTermEntity ontologyTermEntity_3 = ontologyTermFactory.create();
		ontologyTermEntity_3.setId("3");
		ontologyTermEntity_3.setOntology(ontology);
		ontologyTermEntity_3.setOntologyTermName("ot_3");
		ontologyTermEntity_3.setOntologyTermIri(ONTOLOGY_IRI + '3');
		ontologyTermEntity_3.setOntologyTermSynonyms(singletonList(ontologyTermSynonym2));
		ontologyTermEntity_3.
				set(OntologyTermMetaData.ONTOLOGY_TERM_DYNAMIC_ANNOTATION,
						singletonList(ontologyTermDynamicAnnotation_3_1));

		// DataService action for matching ontology term annotation
		QueryRule annotationQueryRule = new QueryRule(
				asList(new QueryRule(OntologyTermDynamicAnnotationMetaData.NAME, EQUALS, "OMIM"), new QueryRule(AND),
						new QueryRule(OntologyTermDynamicAnnotationMetaData.VALUE, EQUALS, "123456")));

		when(dataService.findAll(OntologyTermDynamicAnnotationMetaData.ONTOLOGY_TERM_DYNAMIC_ANNOTATION,
				new QueryImpl<Entity>(singletonList(annotationQueryRule)).pageSize(Integer.MAX_VALUE)))
				.thenReturn(Collections.<Entity>singletonList(ontologyTermDynamicAnnotation_3_1).stream());

		when(dataService.findAll(ONTOLOGY_TERM, new QueryImpl<OntologyTermEntity>(
						asList(new QueryRule(OntologyTermMetaData.ONTOLOGY, EQUALS, ontology.getId()), new QueryRule(AND),
								new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_DYNAMIC_ANNOTATION, IN,
										singletonList(ontologyTermDynamicAnnotation_3_1)))).pageSize(Integer.MAX_VALUE),
				OntologyTermEntity.class))
				.thenReturn(Collections.<OntologyTermEntity>singletonList(ontologyTermEntity_3).stream());

		// DataService action for elasticsearch regular matching ontology term synonyms
		QueryRule disMaxRegularQueryRule_2 = new QueryRule(
				singletonList(new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH, "input~0.8")));
		disMaxRegularQueryRule_2.setOperator(DIS_MAX);
		when(dataService.findAll(ONTOLOGY_TERM, new QueryImpl<OntologyTermEntity>(
				asList(new QueryRule(OntologyTermMetaData.ONTOLOGY, EQUALS, ontology.getId()), new QueryRule(AND),
						disMaxRegularQueryRule_2)).pageSize(49), OntologyTermEntity.class)).thenReturn(Stream.empty());

		// DataService action for n-gram matching ontology term synonyms
		QueryRule disMaxNGramQueryRule_2 = new QueryRule(
				singletonList(new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH_NGRAM, "input")));
		disMaxNGramQueryRule_2.setOperator(DIS_MAX);
		when(dataService.findAll(ONTOLOGY_TERM, new QueryImpl<OntologyTermEntity>(
				asList(new QueryRule(OntologyTermMetaData.ONTOLOGY, EQUALS, ontology.getId()), new QueryRule(AND),
						disMaxNGramQueryRule_2)).pageSize(10), OntologyTermEntity.class)).thenReturn(Stream.empty());

		// ########################### TEST THREE ###########################
		// Define the input for test three

		OntologyTermSynonym ontologyTermSynonym_4_1 = ontologyTermSynonymFactory.create();
		ontologyTermSynonym_4_1.setOntologyTermSynonym("protruding eye");

		OntologyTermSynonym ontologyTermSynonym_4_2 = ontologyTermSynonymFactory.create();
		ontologyTermSynonym_4_2.setOntologyTermSynonym("proptosis");

		OntologyTermSynonym ontologyTermSynonym_4_3 = ontologyTermSynonymFactory.create();
		ontologyTermSynonym_4_3.setOntologyTermSynonym("Exophthalmos");

		// Mock ontologyTerm entity based on the previous entities defined
		OntologyTermEntity ontologyTermEntity_4 = ontologyTermFactory.create();
		ontologyTermEntity_4.setId("4");
		ontologyTermEntity_4.setOntology(ontology);
		ontologyTermEntity_4.setOntologyTermName("protruding eye");
		ontologyTermEntity_4.setOntologyTermIri(ONTOLOGY_IRI + '4');
		ontologyTermEntity_4.setOntologyTermSynonyms(
				asList(ontologyTermSynonym_4_1, ontologyTermSynonym_4_2, ontologyTermSynonym_4_3));
		ontologyTermEntity_4.setOntologyTermDynamicAnnotations(emptyList());

		// DataService action for elasticsearch regular matching ontology term synonyms
		QueryRule disMaxRegularQueryRule_3 = new QueryRule(singletonList(
				new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH,
						"proptosi~0.8 protrud~0.8 ey~0.8 exophthalmo~0.8")));
		disMaxRegularQueryRule_3.setOperator(DIS_MAX);

		when(dataService.findAll(ONTOLOGY_TERM, new QueryImpl<OntologyTermEntity>(
				asList(new QueryRule(OntologyTermMetaData.ONTOLOGY, EQUALS, ontology.getId()), new QueryRule(AND),
						disMaxRegularQueryRule_3)).pageSize(50), OntologyTermEntity.class))
				.thenReturn(Collections.<OntologyTermEntity>singletonList(ontologyTermEntity_4).stream());

		// DataService action for elasticsearch ngram matching ontology term synonyms
		QueryRule disMaxNGramQueryRule_3 = new QueryRule(singletonList(
				new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH_NGRAM,
						"proptosi protrud ey exophthalmo")));
		disMaxNGramQueryRule_3.setOperator(QueryRule.Operator.DIS_MAX);

		when(dataService.findAll(ONTOLOGY_TERM, new QueryImpl<OntologyTermEntity>(
				asList(new QueryRule(OntologyTermMetaData.ONTOLOGY, EQUALS, ontology.getId()), new QueryRule(AND),
						disMaxNGramQueryRule_3)).pageSize(10), OntologyTermEntity.class))
				.thenReturn(Collections.<OntologyTermEntity>singletonList(ontologyTermEntity_4).stream());

	}

	@Test
	public void findOntologyTermEntities()
	{
		Attribute nameAttr = when(mock(Attribute.class).getName()).thenReturn("Name").getMock();
		when(nameAttr.getDataType()).thenReturn(STRING);
		Attribute omimAttr = when(mock(Attribute.class).getName()).thenReturn("OMIM").getMock();
		when(omimAttr.getDataType()).thenReturn(STRING);

		EntityType entityType = mock(EntityType.class);
		when(entityType.getAtomicAttributes()).thenReturn(asList(nameAttr, omimAttr));
		when(entityType.getAttribute("Name")).thenReturn(nameAttr);
		when(entityType.getAttribute("OMIM")).thenReturn(omimAttr);

		// Test one: match only the name of input with ontologyterms
		Entity firstInput = new DynamicEntity(ontologyTermFactory.getEntityType(),
				ImmutableMap.<String, Object>of("Name", "hearing impairment"));

		List<SortaHit> ontologyTerms_test1 = sortaService.findOntologyTermEntities(ONTOLOGY_IRI, firstInput);
		Iterator<SortaHit> iterator_test1 = ontologyTerms_test1.iterator();

		assertEquals(iterator_test1.hasNext(), true);
		SortaHit firstMatch_test1 = iterator_test1.next();
		assertEquals((int) firstMatch_test1.getWeightedScore(), 100);

		assertEquals(iterator_test1.hasNext(), true);
		SortaHit secondMatch_test1 = iterator_test1.next();
		assertEquals((int) secondMatch_test1.getWeightedScore(), new Double(85).intValue());

		assertEquals(iterator_test1.hasNext(), false);

		// Test two: match the database annotation of input with ontologyterms
		Entity secondInput = new DynamicEntity(ontologyTermFactory.getEntityType(),
				ImmutableMap.of("Name", "input", "OMIM", "123456"));

		Iterable<SortaHit> ontologyTerms_test2 = sortaService.findOntologyTermEntities(ONTOLOGY_IRI, secondInput);
		Iterator<SortaHit> iterator_test2 = ontologyTerms_test2.iterator();

		assertEquals(iterator_test2.hasNext(), true);
		SortaHit firstMatch_test2 = iterator_test2.next();
		assertEquals((int) firstMatch_test2.getWeightedScore(), 100);

		assertEquals(iterator_test2.hasNext(), false);

		// Test three: match only the name of input with ontologyterms, since the name contains multiple synonyms
		// therefore add up all the scores from synonyms
		Entity thirdInput = new DynamicEntity(ontologyTermFactory.getEntityType(),
				ImmutableMap.of("Name", "proptosis, protruding eye, Exophthalmos "));
		Iterable<SortaHit> ontologyTerms_test3 = sortaService.findOntologyTermEntities(ONTOLOGY_IRI, thirdInput);
		Iterator<SortaHit> iterator_test3 = ontologyTerms_test3.iterator();

		assertEquals(iterator_test3.hasNext(), true);
		SortaHit firstMatch_test3 = iterator_test3.next();
		assertEquals((int) firstMatch_test3.getWeightedScore(), 100);

		assertEquals(iterator_test3.hasNext(), false);
	}

	@Configuration
	@Import({ OntologyTestConfig.class, OntologyTermHitMetaData.class })
	public static class Config
	{
		@Bean
		public DataService dataService()
		{
			return mock(DataService.class);
		}

		@Bean
		public OntologyService ontologyService()
		{
			return mock(OntologyService.class);
		}

		@Bean
		public InformationContentService informationContentService()
		{
			return mock(InformationContentService.class);
		}

		@Bean
		public SortaServiceImpl sortaServiceImpl()
		{
			return new SortaServiceImpl(dataService(), ontologyService(), informationContentService());
		}
	}
}
