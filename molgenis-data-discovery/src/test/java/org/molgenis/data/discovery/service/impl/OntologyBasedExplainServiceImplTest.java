package org.molgenis.data.discovery.service.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.model.matching.MatchingExplanation;
import org.molgenis.data.discovery.service.OntologyBasedExplainService;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParam;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@ContextConfiguration(classes = OntologyBasedExplainServiceImplTest.Config.class)
public class OntologyBasedExplainServiceImplTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	OntologyService ontologyService;

	@Autowired
	OntologyBasedExplainService ontologyBasedExplainServiceImpl;

	@Autowired
	IdGenerator idGenerator;

	@Autowired
	AttributeCandidateScoringImpl attributeCandidateScoringImpl;

	@BeforeMethod
	public void setup()
	{
		when(idGenerator.generateId()).thenReturn("identifier");
	}

	@Test
	public void computeScoreForMatchedSource()
	{
		SemanticType semanticType = SemanticType.create("1", "smoking", "smoking", true);

		OntologyTerm sourceOntologyTerm = OntologyTerm.create("C0302836", "C0302836", "Smoking tobacco",
				StringUtils.EMPTY, Lists.newArrayList("Smoking tobacco", "Smoked Tobacco"), emptyList(), emptyList(),
				asList(semanticType));

		OntologyTerm targetOntologyTerm = OntologyTerm.create("C0302836", "C0302836", "Cigar Smoker", StringUtils.EMPTY,
				Lists.newArrayList("Cigar Smoker"), emptyList(), emptyList(), asList(semanticType));

		Multimap<OntologyTerm, OntologyTerm> relatedOntologyTerms = LinkedHashMultimap.create();
		relatedOntologyTerms.put(targetOntologyTerm, sourceOntologyTerm);

		IdentifiableTagGroup targetTagGroup = IdentifiableTagGroup.create("1", Arrays.asList(targetOntologyTerm),
				Collections.emptyList(), "cigar smoker", 0.7f);

		IdentifiableTagGroup sourceTagGroup = IdentifiableTagGroup.create("2", Arrays.asList(sourceOntologyTerm),
				Collections.emptyList(), "tobacco smoke", 0.3f);

		BiobankSampleCollection collection = BiobankSampleCollection.create("test collection");

		SemanticSearchParam semanticSearchParam = SemanticSearchParam.create(Collections.emptySet(),
				Collections.emptyList(), QueryExpansionParam.create(false, false));

		BiobankUniverse biobankUniverse = BiobankUniverse.create("1", "test universe", emptyList(), new MolgenisUser(),
				emptyList(), emptyList());

		BiobankSampleAttribute target = BiobankSampleAttribute.create("1", "SMK_CIGAR_CURRENT", "Current Cigar Smoker",
				"", collection, Arrays.asList(targetTagGroup));

		BiobankSampleAttribute source = BiobankSampleAttribute.create("2", "SMK121",
				"How many hours a day you are exposed to the tobacco smoke of others? (Repeat) (1)", "", collection,
				Arrays.asList(sourceTagGroup));

		when(ontologyService.isDescendant(sourceOntologyTerm, targetOntologyTerm)).thenReturn(true);

		when(ontologyService.getOntologyTermSemanticRelatedness(sourceOntologyTerm, targetOntologyTerm))
				.thenReturn(0.5);

		when(ontologyService.related(targetOntologyTerm, sourceOntologyTerm, OntologyBasedMatcher.STOP_LEVEL))
				.thenReturn(true);

		when(ontologyService.areWithinDistance(targetOntologyTerm, sourceOntologyTerm,
				OntologyBasedMatcher.EXPANSION_LEVEL)).thenReturn(true);

		when(attributeCandidateScoringImpl.score(target, source, biobankUniverse, relatedOntologyTerms,
				semanticSearchParam.isStrictMatch())).thenReturn(Hit.create("cigar smoker tobacco smoking", 0.4f));

		List<AttributeMappingCandidate> explain = ontologyBasedExplainServiceImpl.explain(biobankUniverse,
				semanticSearchParam, target, Arrays.asList(source), attributeCandidateScoringImpl);

		AttributeMappingCandidate create = AttributeMappingCandidate.create("identifier", biobankUniverse, target,
				source, MatchingExplanation.create("identifier", Arrays.asList(sourceOntologyTerm),
						"cigar smoker tobacco smoking", "tobacco smoking cigar smoker", 0.4f));

		Assert.assertEquals(explain, Arrays.asList(create));
	}

	//
	// @Test
	// public void explain()
	// {
	//
	// }
	//
	// @Test
	// public void findAllRelatedOntologyTerms()
	// {
	//
	// }
	//
	// @Test
	// public void findLeftUnmatchedWords()
	// {
	//
	// }
	//
	// @Test
	// public void findMatchedSynonymsInAttribute()
	// {
	//
	// }
	//
	// @Test
	// public void findMatchedWords()
	// {
	//
	// }

	@Configuration
	public static class Config
	{
		@Bean
		public IdGenerator idGenerator()
		{
			return mock(IdGenerator.class);
		}

		@Bean
		public OntologyService ontologyService()
		{
			return mock(OntologyService.class);
		}

		@Bean
		public AttributeCandidateScoringImpl attributeCandidateScoringImpl()
		{
			return mock(AttributeCandidateScoringImpl.class);
		}

		@Bean
		public OntologyBasedExplainService ontologyBasedExplainService()
		{
			return new OntologyBasedExplainServiceImpl(idGenerator(), ontologyService());
		}
	}
}
