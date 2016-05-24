package org.molgenis.data.semanticsearch.service.impl;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceImpl.MAX_NUMBER_ATTRIBTUES;
import static org.molgenis.ontology.core.model.OntologyTerm.and;
import static org.molgenis.ontology.utils.Stemmer.splitAndStem;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.mockito.Mockito;
import org.molgenis.data.semanticsearch.service.OntologyTagService;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

@ContextConfiguration(classes = TagGroupGeneratorImplTest.Config.class)
public class TagGroupGeneratorImplTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	OntologyService ontologyService;

	@Autowired
	OntologyTagService ontologyTagService;

	@Autowired
	TagGroupGeneratorImpl tagGroupGenerator;

	private DefaultAttributeMetaData attribute;
	private OntologyTerm standingHeight;
	private OntologyTerm bodyWeight;
	private List<OntologyTerm> ontologyTerms;
	private List<String> ontologyIds;

	@BeforeMethod
	public void init()
	{
		attribute = new DefaultAttributeMetaData("attr1").setLabel("attribute 1");
		standingHeight = OntologyTerm.create("http://onto/height", "Standing height",
				Arrays.asList("Standing height", "length"));
		bodyWeight = OntologyTerm.create("http://onto/bmi", "Body weight",
				Arrays.asList("Body weight", "Mass in kilograms"));
		ontologyTerms = Arrays.asList(standingHeight, bodyWeight);
		ontologyIds = Arrays.asList("1");
	}

	@Test
	public void testRemoveStopWords()
	{
		String description = "falling in the ocean!";
		Set<String> actual = SemanticSearchServiceUtils.splitRemoveStopWords(description);
		Set<String> expected = Sets.newHashSet("falling", "ocean");
		assertEquals(actual, expected);
	}

	@Test
	public void testSearchCircumflex() throws InterruptedException, ExecutionException
	{
		String description = "body^0.5 length^0.5";
		Set<String> expected = Sets.newHashSet("length", "body", "0.5");
		Set<String> actual = SemanticSearchServiceUtils.splitRemoveStopWords(description);
		assertEquals(actual.size(), 3);
		assertTrue(actual.containsAll(expected));
	}

	@Test
	public void testSearchTilde() throws InterruptedException, ExecutionException
	{
		String description = "body~0.5 length~0.5";
		Set<String> expected = Sets.newLinkedHashSet(Sets.newHashSet("length~0.5", "body~0.5"));
		Set<String> actual = SemanticSearchServiceUtils.splitRemoveStopWords(description);
		assertEquals(actual, expected);
	}

	@Test
	public void testSearchUnderScore() throws InterruptedException, ExecutionException
	{
		String description = "body_length";
		Set<String> expected = Sets.newHashSet("body", "length");
		Set<String> actual = SemanticSearchServiceUtils.splitRemoveStopWords(description);
		assertEquals(actual, expected);
	}

	@Test
	public void testSearchHypertension() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);

		List<String> ontologyIds = Arrays.asList("1");
		OntologyTerm standingHeight = OntologyTerm.create("http://onto/height", "Standing height",
				Arrays.asList("Standing height", "length"));
		OntologyTerm bodyWeight = OntologyTerm.create("http://onto/bmi", "Body weight",
				Arrays.asList("Body weight", "Mass in kilograms"));
		OntologyTerm hypertension = OntologyTerm.create("http://onto/hyp", "Hypertension");
		OntologyTerm maternalHypertension = OntologyTerm.create("http://onto/mhyp", "Maternal hypertension");

		List<OntologyTerm> ontologyTerms = asList(standingHeight, bodyWeight, hypertension, maternalHypertension);
		Set<String> searchTerms = Sets.newLinkedHashSet(asList("history", "hypertension"));

		when(ontologyService.findOntologyTerms(ontologyIds, searchTerms, MAX_NUMBER_ATTRIBTUES))
				.thenReturn(ontologyTerms);

		List<TagGroup> result = tagGroupGenerator.findTagGroups("history hypertension", ontologyIds);

		assertEquals(result, asList(TagGroup.create(hypertension, "hypertension", 0.69231f)));
	}

	@Test
	public void testSearchDescription() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);

		List<String> ontologyIds = Arrays.asList("1");
		OntologyTerm standingHeight = OntologyTerm.create("http://onto/height", "Standing height",
				Arrays.asList("Standing height", "length"));
		OntologyTerm bodyWeight = OntologyTerm.create("http://onto/bmi", "Body weight",
				Arrays.asList("Body weight", "Mass in kilograms"));
		OntologyTerm hypertension = OntologyTerm.create("http://onto/hyp", "Hypertension");
		OntologyTerm maternalHypertension = OntologyTerm.create("http://onto/mhyp", "Maternal hypertension");

		List<OntologyTerm> ontologyTerms = asList(standingHeight, bodyWeight, hypertension, maternalHypertension);
		Set<String> searchTerms = Sets.newLinkedHashSet(asList("standing", "height", "meters"));

		when(ontologyService.findOntologyTerms(ontologyIds, searchTerms, MAX_NUMBER_ATTRIBTUES))
				.thenReturn(ontologyTerms);

		List<TagGroup> result = tagGroupGenerator.findTagGroups("standing height meters", ontologyIds);

		assertEquals(result, asList(TagGroup.create(standingHeight, "standing height", 0.81250f)));
	}

	@Test
	public void testCreateOntologyTermPairwiseCombination()
	{
		OntologyTerm ot1 = OntologyTerm.create("iri1", "septin 4", Arrays.asList("SEPT4"));
		OntologyTerm ot2 = OntologyTerm.create("iri2", "4th of September", Arrays.asList("SEPT4"));
		OntologyTerm ot3 = OntologyTerm.create("iri3", "National Security Agency", Arrays.asList("NSA"));
		OntologyTerm ot4 = OntologyTerm.create("iri4", "National Security Advisor", Arrays.asList("NSA"));
		OntologyTerm ot5 = OntologyTerm.create("iri5", "National Security Area", Arrays.asList("NSA"));
		OntologyTerm ot6 = OntologyTerm.create("iri6", "	Activity", Arrays.asList("ACT"));

		Multimap<String, OntologyTerm> multiMap = LinkedListMultimap.create();
		multiMap.putAll("SEPT4", Arrays.asList(ot1, ot2));
		multiMap.putAll("NSA", Arrays.asList(ot3, ot4, ot5));
		multiMap.putAll("ACT", Arrays.asList(ot6));

		List<OntologyTerm> actual = tagGroupGenerator.createTagGroups(multiMap);
		List<OntologyTerm> expected = Lists.newArrayList(and(and(ot1, ot3), ot6), and(and(ot1, ot4), ot6),
				and(and(ot1, ot5), ot6), and(and(ot2, ot3), ot6), and(and(ot2, ot4), ot6), and(and(ot2, ot5), ot6));

		assertEquals(actual, expected);
	}

	@Test
	public void testCombineOntologyTerms()
	{
		OntologyTerm ot = OntologyTerm.create("iri02", "weight", Arrays.asList("measured weight"));
		OntologyTerm ot0 = OntologyTerm.create("iri01", "height", Arrays.asList("standing height", "body length"));
		OntologyTerm ot1 = OntologyTerm.create("iri1", "septin 4", Arrays.asList("SEPT4"));
		OntologyTerm ot2 = OntologyTerm.create("iri2", "4th of September", Arrays.asList("SEPT4"));
		OntologyTerm ot3 = OntologyTerm.create("iri3", "National Security Agency", Arrays.asList("NSA"));
		OntologyTerm ot4 = OntologyTerm.create("iri4", "National Security Advisor", Arrays.asList("NSA"));
		OntologyTerm ot5 = OntologyTerm.create("iri5", "National Security Area", Arrays.asList("NSA"));
		OntologyTerm ot6 = OntologyTerm.create("iri6", "Movement", Arrays.asList("Moved"));
		OntologyTerm ot7 = OntologyTerm.create("iri7", "NSA movement SEPT4");

		Set<String> searchTerms = splitAndStem("NSA has a movement on SEPT4");

		List<OntologyTerm> relevantOntologyTerms = Lists.newArrayList(ot, ot0, ot1, ot2, ot3, ot4, ot5, ot6, ot7);

		// Randomize the order of the ontology terms
		Collections.shuffle(relevantOntologyTerms);

		List<TagGroup> ontologyTermHits = tagGroupGenerator.applyTagMatchingCriteria(relevantOntologyTerms,
				searchTerms);

		List<TagGroup> combineOntologyTerms = tagGroupGenerator.generateTagGroups(searchTerms, ontologyTermHits);

		List<OntologyTerm> actualOntologyTerms = combineOntologyTerms.stream().map(hit -> hit.getOntologyTerm())
				.collect(toList());
		List<OntologyTerm> expected = Lists.newArrayList(ot7, and(and(ot6, ot2), ot4), and(and(ot6, ot2), ot3),
				and(and(ot6, ot2), ot5), and(and(ot6, ot1), ot3), and(and(ot6, ot1), ot4), and(and(ot6, ot1), ot5));

		assertTrue(combineOntologyTerms.stream().allMatch(hit -> hit.getScore() == 0.92683f));
		assertTrue(actualOntologyTerms.containsAll(expected));
		assertTrue(expected.containsAll(actualOntologyTerms));
	}

	@Test
	public void testDistanceFrom()
	{
		assertEquals(tagGroupGenerator.distanceFrom("Hypertension", of("history", "hypertens")), .6923, 0.0001,
				"String distance should be equal");
		assertEquals(tagGroupGenerator.distanceFrom("Maternal Hypertension", of("history", "hypertens")), .5454, 0.0001,
				"String distance should be equal");
	}

	@Test
	public void testSearchUnicode2()
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("Standing height (Ångstrøm)");

		when(ontologyService.findOntologyTerms(ontologyIds, newLinkedHashSet(asList("standing", "height", "ångstrøm")),
				MAX_NUMBER_ATTRIBTUES)).thenReturn(ontologyTerms);

		List<TagGroup> result = tagGroupGenerator.findTagGroups(attribute.getDescription(), ontologyIds);

		assertEquals(result.size(), 1);
		assertEquals(result.get(0), TagGroup.create(standingHeight, "standing height", 0.76471f));
	}

	@Test
	public void testSearchUnicode() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("/əˈnædrəməs/");

		when(ontologyService.findOntologyTerms(ontologyIds, ImmutableSet.of("əˈnædrəməs"), MAX_NUMBER_ATTRIBTUES))
				.thenReturn(ontologyTerms);

		List<TagGroup> result = tagGroupGenerator.findTagGroups(attribute.getDescription(), ontologyIds);

		assertEquals(result, Collections.emptyList());
	}

	@Test
	public void testSearchMultipleTags() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("Body mass index");

		when(ontologyService.findOntologyTerms(ontologyIds, newLinkedHashSet(asList("body", "mass", "index")),
				MAX_NUMBER_ATTRIBTUES)).thenReturn(ontologyTerms);

		List<TagGroup> result = tagGroupGenerator.findTagGroups(attribute.getDescription(), ontologyIds);

		assertEquals(result, Collections.emptyList());
	}

	@Test
	public void testSearchLabel() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("Standing height (m.)");

		when(ontologyService.findOntologyTerms(ontologyIds, newLinkedHashSet(asList("standing", "height", "m.")),
				MAX_NUMBER_ATTRIBTUES)).thenReturn(ontologyTerms);

		List<TagGroup> result = tagGroupGenerator.findTagGroups(attribute.getDescription(), ontologyIds);

		TagGroup ontologyTermHit = TagGroup.create(standingHeight, "standing height", 0.92857f);

		assertEquals(result, Arrays.asList(ontologyTermHit));
	}

	@Configuration
	public static class Config
	{
		@Bean
		OntologyService ontologyService()
		{
			return mock(OntologyService.class);
		}

		@Bean
		OntologyTagService ontologyTagService()
		{
			return mock(OntologyTagService.class);
		}

		@Bean
		TagGroupGeneratorImpl tagGroupGenerator()
		{
			return new TagGroupGeneratorImpl(ontologyService(), ontologyTagService());
		}
	}
}
