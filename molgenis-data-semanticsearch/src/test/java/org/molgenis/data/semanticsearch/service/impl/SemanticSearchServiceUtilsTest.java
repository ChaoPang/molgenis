package org.molgenis.data.semanticsearch.service.impl;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.ontology.core.model.OntologyTerm.and;
import static org.molgenis.ontology.utils.Stemmer.splitAndStem;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.mockito.Mockito;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.semantic.Relation;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.OntologyTagService;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntity;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.ic.TermFrequencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

@ContextConfiguration(classes = SemanticSearchServiceUtilsTest.Config.class)
public class SemanticSearchServiceUtilsTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private SemanticSearchServiceUtils semanticSearchServiceUtils;

	@Autowired
	private DataService dataService;

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
	public void testCreateDisMaxQueryRule()
	{
		List<String> createdTargetAttributeQueries = Arrays.asList("Height", "Standing height in cm", "body_length",
				"Sitting height", "sitting_length", "Height", "sature");
		QueryRule actualRule = semanticSearchServiceUtils.createDisMaxQueryRuleForTerms(createdTargetAttributeQueries,
				null);
		String expectedQueryRuleToString = "DIS_MAX ('label' FUZZY_MATCH 'Height', 'description' FUZZY_MATCH 'Height', 'label' FUZZY_MATCH 'Standing height in cm', 'description' FUZZY_MATCH 'Standing height in cm', 'label' FUZZY_MATCH 'body_length', 'description' FUZZY_MATCH 'body_length', 'label' FUZZY_MATCH 'Sitting height', 'description' FUZZY_MATCH 'Sitting height', 'label' FUZZY_MATCH 'sitting_length', 'description' FUZZY_MATCH 'sitting_length', 'label' FUZZY_MATCH 'Height', 'description' FUZZY_MATCH 'Height', 'label' FUZZY_MATCH 'sature', 'description' FUZZY_MATCH 'sature')";
		assertEquals(actualRule.getOperator(), Operator.DIS_MAX);
		assertEquals(actualRule.toString(), expectedQueryRuleToString);

		List<String> createdTargetAttributeQueries2 = Arrays.asList("(Height) [stand^~]");
		QueryRule actualRule2 = semanticSearchServiceUtils.createDisMaxQueryRuleForTerms(createdTargetAttributeQueries2,
				null);
		String expectedQueryRuleToString2 = "DIS_MAX ('label' FUZZY_MATCH '\\(Height\\) \\[stand^\\~\\]', 'description' FUZZY_MATCH '\\(Height\\) \\[stand^\\~\\]')";
		assertEquals(actualRule2.getOperator(), Operator.DIS_MAX);
		assertEquals(actualRule2.toString(), expectedQueryRuleToString2);
	}

	@Test
	public void testCreateQueryRulesForOntologyTerms()
	{
		OntologyTerm ontologyTerm_1 = OntologyTerm.create("http://www.molgenis.org/1", "molgenis label in the gcc");
		OntologyTerm ontologyTerm_2 = OntologyTerm.create("http://www.molgenis.org/2",
				"molgenis label 2 in the genetics", Arrays.asList("label 2"));
		OntologyTerm ontologyTerm_3 = OntologyTerm.create("http://www.molgenis.org/3", "molgenis child",
				Arrays.asList("child"));
		OntologyTerm ontologyTerm_4 = OntologyTerm.and(ontologyTerm_1, ontologyTerm_2);

		Hit<OntologyTerm> hit_1 = Hit.create(ontologyTerm_1, 0.5f);
		Hit<OntologyTerm> hit_2 = Hit.create(ontologyTerm_2, 0.5f);
		Hit<OntologyTerm> hit_4 = Hit.create(ontologyTerm_4, 1.0f);

		when(ontologyService.getAtomicOntologyTerms(ontologyTerm_1)).thenReturn(Arrays.asList(ontologyTerm_1));
		when(ontologyService.getAtomicOntologyTerms(ontologyTerm_2)).thenReturn(Arrays.asList(ontologyTerm_2));
		when(ontologyService.getAtomicOntologyTerms(ontologyTerm_4)).thenReturn(asList(ontologyTerm_1, ontologyTerm_2));
		when(ontologyService.getLevelThreeChildren(ontologyTerm_1)).thenReturn(Collections.emptyList());
		when(ontologyService.getLevelThreeChildren(ontologyTerm_2)).thenReturn(Arrays.asList(ontologyTerm_3));
		when(ontologyService.getOntologyTermSemanticRelatedness(ontologyTerm_2, ontologyTerm_3)).thenReturn(0.5d);

		// Case one
		List<QueryRule> createQueryRulesForOntologyTerms1 = semanticSearchServiceUtils
				.createQueryRulesForOntologyTerms(asList(hit_1, hit_2), false);

		String expectedShouldQueryRuleToString1 = "[DIS_MAX ('label' FUZZY_MATCH 'the^0.1 gcc molgenis label in^0.1', 'description' FUZZY_MATCH 'the^0.1 gcc molgenis label in^0.1'), DIS_MAX ('label' FUZZY_MATCH '2 label', 'description' FUZZY_MATCH '2 label', 'label' FUZZY_MATCH '2 the^0.1 genetics molgenis label in^0.1', 'description' FUZZY_MATCH '2 the^0.1 genetics molgenis label in^0.1')]";
		assertEquals(createQueryRulesForOntologyTerms1.size(), 2);
		assertTrue(createQueryRulesForOntologyTerms1.stream()
				.allMatch(rule -> rule.getOperator().equals(Operator.DIS_MAX)));
		assertEquals(createQueryRulesForOntologyTerms1.toString(), expectedShouldQueryRuleToString1);

		// Case two
		List<QueryRule> createQueryRulesForOntologyTerms2 = semanticSearchServiceUtils
				.createQueryRulesForOntologyTerms(asList(hit_4), false);

		String expectedShouldQueryRuleToString2 = "[SHOULD (DIS_MAX ('label' FUZZY_MATCH 'the^0.1 gcc molgenis label in^0.1', 'description' FUZZY_MATCH 'the^0.1 gcc molgenis label in^0.1'), DIS_MAX ('label' FUZZY_MATCH '2 label', 'description' FUZZY_MATCH '2 label', 'label' FUZZY_MATCH '2 the^0.1 genetics molgenis label in^0.1', 'description' FUZZY_MATCH '2 the^0.1 genetics molgenis label in^0.1'))]";
		assertEquals(createQueryRulesForOntologyTerms2.size(), 1);
		assertTrue(createQueryRulesForOntologyTerms2.stream()
				.allMatch(rule -> rule.getOperator().equals(Operator.SHOULD)));
		assertEquals(createQueryRulesForOntologyTerms2.toString(), expectedShouldQueryRuleToString2);

		// Case three
		List<QueryRule> createQueryRulesForOntologyTerms3 = semanticSearchServiceUtils
				.createQueryRulesForOntologyTerms(asList(hit_4), true);

		String expectedShouldQueryRuleToString3 = "[SHOULD (DIS_MAX ('label' FUZZY_MATCH 'the^0.1 gcc molgenis label in^0.1', 'description' FUZZY_MATCH 'the^0.1 gcc molgenis label in^0.1'), DIS_MAX ('label' FUZZY_MATCH '2 label', 'description' FUZZY_MATCH '2 label', 'label' FUZZY_MATCH '2 the^0.1 genetics molgenis label in^0.1', 'description' FUZZY_MATCH '2 the^0.1 genetics molgenis label in^0.1', 'label' FUZZY_MATCH 'child^0.25', 'description' FUZZY_MATCH 'child^0.25', 'label' FUZZY_MATCH 'molgenis^0.25 child^0.25', 'description' FUZZY_MATCH 'molgenis^0.25 child^0.25'))]";
		assertEquals(createQueryRulesForOntologyTerms3.size(), 1);
		assertTrue(createQueryRulesForOntologyTerms3.stream()
				.allMatch(rule -> rule.getOperator().equals(Operator.SHOULD)));
		assertEquals(createQueryRulesForOntologyTerms3.toString(), expectedShouldQueryRuleToString3);
	}

	@Test
	public void testCreateTargetAttributeQueryTerms()
	{
		DefaultAttributeMetaData targetAttribute_1 = new DefaultAttributeMetaData("targetAttribute 1");
		targetAttribute_1.setDescription("Height");

		DefaultAttributeMetaData targetAttribute_2 = new DefaultAttributeMetaData("targetAttribute 2");
		targetAttribute_2.setLabel("Height");

		Multimap<Relation, OntologyTerm> tags = LinkedHashMultimap.create();
		OntologyTerm ontologyTerm1 = OntologyTerm.create("http://onto/standingheight", "Standing height",
				"Description is not used", asList("body_length"));
		OntologyTerm ontologyTerm2 = OntologyTerm.create("http://onto/sittingheight", "Sitting height",
				"Description is not used", asList("sitting_length"));
		OntologyTerm ontologyTerm3 = OntologyTerm.create("http://onto/height", "Height", "Description is not used",
				asList("sature"));

		tags.put(Relation.isAssociatedWith, ontologyTerm1);
		tags.put(Relation.isRealizationOf, ontologyTerm2);
		tags.put(Relation.isDefinedBy, ontologyTerm3);

		when(ontologyService.getAtomicOntologyTerms(ontologyTerm1)).thenReturn(Arrays.asList(ontologyTerm1));
		when(ontologyService.getAtomicOntologyTerms(ontologyTerm2)).thenReturn(Arrays.asList(ontologyTerm2));
		when(ontologyService.getAtomicOntologyTerms(ontologyTerm3)).thenReturn(Arrays.asList(ontologyTerm3));

		List<Hit<OntologyTerm>> ontologyTermHits = tags.values().stream().map(ot -> Hit.create(ot, 1.0f))
				.collect(Collectors.toList());

		// Case 1
		QueryRule actualTargetAttributeQueryTerms_1 = semanticSearchServiceUtils
				.createDisMaxQueryRule(newLinkedHashSet(asList("targetAttribute 1", "Height")), ontologyTermHits, true);
		String expecteddisMaxQueryRuleToString_1 = "DIS_MAX (DIS_MAX ('label' FUZZY_MATCH '1 targetattribute', 'description' FUZZY_MATCH '1 targetattribute', 'label' FUZZY_MATCH 'height', 'description' FUZZY_MATCH 'height'), DIS_MAX ('label' FUZZY_MATCH 'length body', 'description' FUZZY_MATCH 'length body', 'label' FUZZY_MATCH 'standing height', 'description' FUZZY_MATCH 'standing height'), DIS_MAX ('label' FUZZY_MATCH 'length sitting', 'description' FUZZY_MATCH 'length sitting', 'label' FUZZY_MATCH 'sitting height', 'description' FUZZY_MATCH 'sitting height'), DIS_MAX ('label' FUZZY_MATCH 'sature', 'description' FUZZY_MATCH 'sature', 'label' FUZZY_MATCH 'height', 'description' FUZZY_MATCH 'height'))";
		assertEquals(actualTargetAttributeQueryTerms_1.toString(), expecteddisMaxQueryRuleToString_1);

		// Case 2
		QueryRule expecteddisMaxQueryRuleToString_2 = semanticSearchServiceUtils
				.createDisMaxQueryRule(Sets.newHashSet("Height"), ontologyTermHits, true);
		String expectedTargetAttributeQueryTermsToString_2 = "DIS_MAX (DIS_MAX ('label' FUZZY_MATCH 'height', 'description' FUZZY_MATCH 'height'), DIS_MAX ('label' FUZZY_MATCH 'length body', 'description' FUZZY_MATCH 'length body', 'label' FUZZY_MATCH 'standing height', 'description' FUZZY_MATCH 'standing height'), DIS_MAX ('label' FUZZY_MATCH 'length sitting', 'description' FUZZY_MATCH 'length sitting', 'label' FUZZY_MATCH 'sitting height', 'description' FUZZY_MATCH 'sitting height'), DIS_MAX ('label' FUZZY_MATCH 'sature', 'description' FUZZY_MATCH 'sature', 'label' FUZZY_MATCH 'height', 'description' FUZZY_MATCH 'height'))";
		assertEquals(expecteddisMaxQueryRuleToString_2.toString(), expectedTargetAttributeQueryTermsToString_2);

		// Case 3
		QueryRule expecteddisMaxQueryRuleToString_3 = semanticSearchServiceUtils
				.createDisMaxQueryRule(newHashSet("targetAttribute 3"), ontologyTermHits, true);
		String expectedTargetAttributeQueryTermsToString_3 = "DIS_MAX (DIS_MAX ('label' FUZZY_MATCH '3 targetattribute', 'description' FUZZY_MATCH '3 targetattribute'), DIS_MAX ('label' FUZZY_MATCH 'length body', 'description' FUZZY_MATCH 'length body', 'label' FUZZY_MATCH 'standing height', 'description' FUZZY_MATCH 'standing height'), DIS_MAX ('label' FUZZY_MATCH 'length sitting', 'description' FUZZY_MATCH 'length sitting', 'label' FUZZY_MATCH 'sitting height', 'description' FUZZY_MATCH 'sitting height'), DIS_MAX ('label' FUZZY_MATCH 'sature', 'description' FUZZY_MATCH 'sature', 'label' FUZZY_MATCH 'height', 'description' FUZZY_MATCH 'height'))";
		assertEquals(expecteddisMaxQueryRuleToString_3.toString(), expectedTargetAttributeQueryTermsToString_3);
	}

	@Test
	public void testCollectQueryTermsFromOntologyTerm()
	{
		// Case 1
		OntologyTerm ontologyTerm1 = OntologyTerm.create("http://onto/standingheight", "Standing height",
				"Description is not used", Arrays.<String> asList("body_length"));
		when(ontologyService.getLevelThreeChildren(ontologyTerm1)).thenReturn(Collections.emptyList());
		List<String> actual_1 = semanticSearchServiceUtils.getQueryTermsFromOntologyTerm(ontologyTerm1, true);
		assertEquals(actual_1, Arrays.asList("length body", "standing height"));

		// Case 2
		OntologyTerm ontologyTerm2 = OntologyTerm.create("http://onto/standingheight", "height",
				"Description is not used", Collections.emptyList());

		OntologyTerm ontologyTerm3 = OntologyTerm.create("http://onto/standingheight-children", "length",
				Arrays.<String> asList("body_length"));

		when(ontologyService.getLevelThreeChildren(ontologyTerm2)).thenReturn(Lists.newArrayList(ontologyTerm3));

		when(ontologyService.getOntologyTermSemanticRelatedness(ontologyTerm2, ontologyTerm3)).thenReturn(0.5);

		List<String> actual_2 = semanticSearchServiceUtils.getQueryTermsFromOntologyTerm(ontologyTerm2, true);

		assertEquals(actual_2, Arrays.asList("height", "length^0.25 body^0.25", "length^0.25"));
	}

	@Test
	public void testGetAttributeIdentifiers()
	{
		EntityMetaData sourceEntityMetaData = new DefaultEntityMetaData("sourceEntityMetaData");
		Entity entityMetaDataEntity = mock(DefaultEntity.class);

		when(dataService.findOne(EntityMetaDataMetaData.ENTITY_NAME,
				new QueryImpl().eq(EntityMetaDataMetaData.FULL_NAME, sourceEntityMetaData.getName())))
						.thenReturn(entityMetaDataEntity);

		Entity attributeEntity1 = new MapEntity();
		attributeEntity1.set(AttributeMetaDataMetaData.IDENTIFIER, "1");
		attributeEntity1.set(AttributeMetaDataMetaData.DATA_TYPE, "string");
		Entity attributeEntity2 = new MapEntity();
		attributeEntity2.set(AttributeMetaDataMetaData.IDENTIFIER, "2");
		attributeEntity2.set(AttributeMetaDataMetaData.DATA_TYPE, "string");
		when(entityMetaDataEntity.getEntities(EntityMetaDataMetaData.ATTRIBUTES))
				.thenReturn(Arrays.<Entity> asList(attributeEntity1, attributeEntity2));

		List<String> expactedAttributeIdentifiers = Arrays.<String> asList("1", "2");
		assertEquals(semanticSearchServiceUtils.getAttributeIdentifiers(sourceEntityMetaData),
				expactedAttributeIdentifiers);
	}

	@Test
	public void testParseBoostQueryString()
	{
		String description = "falling in the ocean!";
		String actual = semanticSearchServiceUtils.parseBoostQueryString(description, 0.5);
		assertEquals(actual, "ocean^0.5 the^0.1 falling^0.5 in^0.1");
	}

	@Test
	public void testRemoveStopWords()
	{
		String description = "falling in the ocean!";
		Set<String> actual = semanticSearchServiceUtils.splitRemoveStopWords(description);
		Set<String> expected = Sets.newHashSet("falling", "ocean");
		assertEquals(actual, expected);
	}

	@Test
	public void testSearchCircumflex() throws InterruptedException, ExecutionException
	{
		String description = "body^0.5 length^0.5";
		Set<String> expected = Sets.newHashSet("length", "body", "0.5");
		Set<String> actual = semanticSearchServiceUtils.splitRemoveStopWords(description);
		assertEquals(actual.size(), 3);
		assertTrue(actual.containsAll(expected));
	}

	@Test
	public void testSearchTilde() throws InterruptedException, ExecutionException
	{
		String description = "body~0.5 length~0.5";
		Set<String> expected = Sets.newLinkedHashSet(Sets.newHashSet("length~0.5", "body~0.5"));
		Set<String> actual = semanticSearchServiceUtils.splitRemoveStopWords(description);
		assertEquals(actual, expected);
	}

	@Test
	public void testSearchUnderScore() throws InterruptedException, ExecutionException
	{
		String description = "body_length";
		Set<String> expected = Sets.newHashSet("body", "length");
		Set<String> actual = semanticSearchServiceUtils.splitRemoveStopWords(description);
		assertEquals(actual, expected);
	}

	@Test
	public void testEscapeCharsExcludingCaretChar()
	{
		Assert.assertEquals(semanticSearchServiceUtils.escapeCharsExcludingCaretChar("(hypertension^4)~[]"),
				"\\(hypertension^4\\)\\~\\[\\]");

		Assert.assertEquals(semanticSearchServiceUtils.escapeCharsExcludingCaretChar("hypertension^4"),
				"hypertension^4");
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

		when(ontologyService.findOntologyTerms(ontologyIds, searchTerms, 20)).thenReturn(ontologyTerms);

		List<Hit<OntologyTermHit>> result = semanticSearchServiceUtils
				.findOntologyTermCombination("history hypertension", ontologyIds);

		assertEquals(result, asList(Hit.create(OntologyTermHit.create(hypertension, "hypertension"), 0.69231f)));
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

		when(ontologyService.findOntologyTerms(ontologyIds, searchTerms, 20)).thenReturn(ontologyTerms);

		List<Hit<OntologyTermHit>> result = semanticSearchServiceUtils
				.findOntologyTermCombination("standing height meters", ontologyIds);

		assertEquals(result, asList(Hit.create(OntologyTermHit.create(standingHeight, "standing height"), 0.81250f)));
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

		List<OntologyTerm> actual = semanticSearchServiceUtils.createOntologyTermPairwiseCombination(multiMap);
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

		Set<String> searchTerms = splitAndStem("NSA has a movement on SEPT4");

		List<OntologyTerm> relevantOntologyTerms = Lists.newArrayList(ot, ot0, ot1, ot2, ot3, ot4, ot5, ot6);
		// Randomize the order of the ontology terms
		Collections.shuffle(relevantOntologyTerms);

		List<Hit<OntologyTermHit>> combineOntologyTerms = semanticSearchServiceUtils.combineOntologyTerms(searchTerms,
				relevantOntologyTerms);

		List<OntologyTerm> actualOntologyTerms = combineOntologyTerms.stream()
				.map(hit -> hit.getResult().getOntologyTerm()).collect(toList());
		List<OntologyTerm> expected = Lists.newArrayList(and(and(ot6, ot2), ot4), and(and(ot6, ot2), ot3),
				and(and(ot6, ot2), ot5), and(and(ot6, ot1), ot3), and(and(ot6, ot1), ot4), and(and(ot6, ot1), ot5));

		assertTrue(combineOntologyTerms.stream().allMatch(hit -> hit.getScore() == 0.92683f));
		assertTrue(actualOntologyTerms.containsAll(expected));
		assertTrue(expected.containsAll(actualOntologyTerms));
	}

	@Test
	public void testDistanceFrom()
	{
		assertEquals(semanticSearchServiceUtils.distanceFrom("Hypertension", of("history", "hypertens")), .6923, 0.0001,
				"String distance should be equal");
		assertEquals(semanticSearchServiceUtils.distanceFrom("Maternal Hypertension", of("history", "hypertens")),
				.5454, 0.0001, "String distance should be equal");
	}

	@Test
	public void testSearchUnicode2()
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("Standing height (Ångstrøm)");

		when(ontologyService.findOntologyTerms(ontologyIds, newLinkedHashSet(asList("standing", "height", "ångstrøm")),
				20)).thenReturn(ontologyTerms);

		List<Hit<OntologyTermHit>> result = semanticSearchServiceUtils
				.findOntologyTermCombination(attribute.getDescription(), ontologyIds);

		assertEquals(result.size(), 1);
		assertEquals(result.get(0), Hit.create(OntologyTermHit.create(standingHeight, "standing height"), 0.76471f));
	}

	@Test
	public void testSearchUnicode() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("/əˈnædrəməs/");

		when(ontologyService.findOntologyTerms(ontologyIds, ImmutableSet.of("əˈnædrəməs"), 20))
				.thenReturn(ontologyTerms);

		List<Hit<OntologyTermHit>> result = semanticSearchServiceUtils
				.findOntologyTermCombination(attribute.getDescription(), ontologyIds);

		assertEquals(result, Collections.emptyList());
	}

	@Test
	public void testSearchMultipleTags() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("Body mass index");

		when(ontologyService.findOntologyTerms(ontologyIds, newLinkedHashSet(asList("body", "mass", "index")), 20))
				.thenReturn(ontologyTerms);

		List<Hit<OntologyTermHit>> result = semanticSearchServiceUtils
				.findOntologyTermCombination(attribute.getDescription(), ontologyIds);

		assertEquals(result, Collections.emptyList());
	}

	@Test
	public void testSearchLabel() throws InterruptedException, ExecutionException
	{
		Mockito.reset(ontologyService);
		attribute.setDescription("Standing height (m.)");

		when(ontologyService.findOntologyTerms(ontologyIds, newLinkedHashSet(asList("standing", "height", "m.")), 20))
				.thenReturn(ontologyTerms);

		List<Hit<OntologyTermHit>> result = semanticSearchServiceUtils
				.findOntologyTermCombination(attribute.getDescription(), ontologyIds);

		Hit<OntologyTermHit> ontologyTermHit = Hit
				.<OntologyTermHit> create(OntologyTermHit.create(standingHeight, "standing height"), 0.92857f);

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
		AttributeMappingExplainService attributeMappingExplainService()
		{
			return mock(AttributeMappingExplainService.class);
		}

		@Bean
		DataService dataService()
		{
			return mock(DataService.class);
		}

		@Bean
		TermFrequencyService termFrequencyService()
		{
			return mock(TermFrequencyService.class);
		}

		@Bean
		SemanticSearchServiceUtils semanticSearchServiceUtils()
		{
			return new SemanticSearchServiceUtils(dataService(), ontologyService(), ontologyTagService(),
					termFrequencyService());
		}
	}
}
