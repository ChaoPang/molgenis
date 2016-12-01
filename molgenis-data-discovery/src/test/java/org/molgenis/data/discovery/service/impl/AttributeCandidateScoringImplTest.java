package org.molgenis.data.discovery.service.impl;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.model.matching.MatchedOntologyTermHit;
import org.molgenis.data.discovery.utils.MatchingExplanationHit;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermHit;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.ic.TermFrequencyService;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData.BiobankAttributeDataType.INT;
import static org.molgenis.data.semanticsearch.string.Stemmer.stem;
import static org.testng.Assert.assertEquals;

public class AttributeCandidateScoringImplTest
{
	OntologyService ontologyService;

	AttributeCandidateScoringImpl attributeCandidateScoringImpl;

	TermFrequencyService termFrequencyService;

	@BeforeTest
	public void beforeTest()
	{
		ontologyService = mock(OntologyService.class);
		termFrequencyService = mock(TermFrequencyService.class);
		attributeCandidateScoringImpl = new AttributeCandidateScoringImpl(ontologyService, termFrequencyService);
	}

	@Test
	public void testScore()
	{
		BiobankSampleCollection biobankSampleCollection = BiobankSampleCollection.create("test-collection");

		OntologyTerm vegetables = OntologyTerm.create("1", "iri1", "Vegetables", asList("Vegetables"));

		OntologyTerm beans = OntologyTerm.create("2", "iri2", "Beans", asList("Beans"));

		OntologyTerm consumption = OntologyTerm.create("3", "iri3", "Consumption", asList("Consumption"));

		IdentifiableTagGroup tagGroup1 = IdentifiableTagGroup
				.create("1", asList(consumption, vegetables), emptyList(), "consumption vegetables", 1.0f);

		IdentifiableTagGroup tagGroup2 = IdentifiableTagGroup
				.create("2", asList(consumption, beans), emptyList(), "consumption beans", 1.0f);

		OntologyTermHit targetOntologyTermHit1 = OntologyTermHit.create(consumption, "consumption", 0.75f);

		OntologyTermHit sourceOntologyTermHit1 = OntologyTermHit.create(consumption, "consumption", 0.78260875f);

		OntologyTermHit targetOntologyTermHit2 = OntologyTermHit.create(vegetables, "vegetables", 0.57142854f);

		OntologyTermHit sourceOntologyTermHit2 = OntologyTermHit.create(beans, "beans", 0.5263158f);

		List<MatchedOntologyTermHit> matchedOntologyTermHits = Arrays
				.asList(MatchedOntologyTermHit.create(targetOntologyTermHit1, sourceOntologyTermHit1, 1.0d),
						MatchedOntologyTermHit.create(targetOntologyTermHit2, sourceOntologyTermHit2, 0.8d));

		BiobankSampleAttribute targetAttribute = BiobankSampleAttribute
				.create("1", "targetAttribute", "Consumption of vegetables", EMPTY, INT, biobankSampleCollection,
						asList(tagGroup1));

		BiobankSampleAttribute sourceAttribute1 = BiobankSampleAttribute
				.create("2", "sourceAttribute1", "Consumption of beans", EMPTY, INT, biobankSampleCollection,
						asList(tagGroup2));

		BiobankSampleAttribute sourceAttribute2 = BiobankSampleAttribute
				.create("2", "sourceAttribute2", "CONSUMPTION BEANS", EMPTY, INT, biobankSampleCollection, emptyList());

		Multimap<OntologyTerm, OntologyTerm> relatedOntologyTerms = LinkedHashMultimap.create();
		relatedOntologyTerms.put(consumption, consumption);
		relatedOntologyTerms.put(vegetables, beans);

		when(ontologyService.getOntologyTermSemanticRelatedness(consumption, consumption)).thenReturn(1.0d);

		when(ontologyService.getOntologyTermSemanticRelatedness(vegetables, beans)).thenReturn(0.8d);

		when(ontologyService.isDescendant(consumption, consumption)).thenReturn(true);

		when(ontologyService.isDescendant(beans, vegetables)).thenReturn(true);

		when(termFrequencyService.getTermFrequency(stem("consumption"))).thenReturn(1.0f);

		when(termFrequencyService.getTermFrequency(stem("vegetable"))).thenReturn(4.0f);

		MatchingExplanationHit matchingExplanationHit1 = attributeCandidateScoringImpl
				.score(targetAttribute, sourceAttribute1, relatedOntologyTerms, false);

		assertEquals(matchingExplanationHit1, MatchingExplanationHit
				.create("consumption vegetables beans", matchedOntologyTermHits, 0.875072f, 0.875072f));

		MatchingExplanationHit matchingExplanationHit2 = attributeCandidateScoringImpl
				.score(targetAttribute, sourceAttribute2, LinkedHashMultimap.create(), false);

		assertEquals(matchingExplanationHit2,
				MatchingExplanationHit.create("consumption", emptyList(), 0.243f, 0.617f));
	}
}
