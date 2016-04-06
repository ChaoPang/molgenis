package org.molgenis.data.semanticsearch.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.OntologyTermSemanticSearch;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.Distance;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import static java.util.Objects.requireNonNull;

public class OntologyTermSemanticSearchImpl implements OntologyTermSemanticSearch
{
	// A pseudo ontologyterm used to achieve the symmetry of the distance matrix
	public final static OntologyTerm PSEUDO_ONTOLOGY_TERM = OntologyTerm.create("PSEUDO", StringUtils.EMPTY);

	// A cached variable to store the distances between ontology terms for quick retrieval
	private LoadingCache<OntologyTerm, Double> cachedOntologyTermRelatedness = CacheBuilder.newBuilder()
			.maximumSize(1000).expireAfterWrite(1, TimeUnit.HOURS).build(new CacheLoader<OntologyTerm, Double>()
			{
				public Double load(OntologyTerm ontologyTerm)
				{
					List<OntologyTerm> resolveOntologyTerms = resolveOntologyTerms(ontologyTerm);
					if (resolveOntologyTerms.size() > 1)
					{
						return ontologyService.getOntologyTermSemanticRelatedness(resolveOntologyTerms.get(0),
								resolveOntologyTerms.get(1));
					}
					return INVALID_DISTANCE;
				}
			});

	private final static String ONTOLOGY_TERM_IRI_SEPARATOR_CHAR = ",";
	private final static double INVALID_DISTANCE = -1;

	private final SemanticSearchService semanticSearchService;
	private final OntologyService ontologyService;

	@Autowired
	public OntologyTermSemanticSearchImpl(SemanticSearchService semanticSearchService, OntologyService ontologyService)
	{
		this.semanticSearchService = requireNonNull(semanticSearchService);
		this.ontologyService = requireNonNull(ontologyService);
	}

	@Override
	public Distance<String> getDistance(String queryOne, String queryTwo) throws ExecutionException
	{
		List<Hit<OntologyTerm>> ontologyTermsForAttr1 = findOntologyTerms(queryOne);
		List<Hit<OntologyTerm>> ontologyTermsForAttr2 = findOntologyTerms(queryTwo);
		double distance = calculateAverageDistance(ontologyTermsForAttr1, ontologyTermsForAttr2);
		boolean isValid = distance == INVALID_DISTANCE;

		return Distance.create(queryOne, queryTwo, isValid, distance);
	}

	@Override
	public Distance<AttributeMetaData> getDistance(AttributeMetaData attr1, AttributeMetaData attr2,
			EntityMetaData entityMetaData1, EntityMetaData entityMetaData2) throws ExecutionException
	{
		List<Hit<OntologyTerm>> ontologyTermsForAttr1 = findOntologyTerms(attr1, entityMetaData1);
		List<Hit<OntologyTerm>> ontologyTermsForAttr2 = findOntologyTerms(attr2, entityMetaData2);
		double distance = calculateAverageDistance(ontologyTermsForAttr1, ontologyTermsForAttr2);
		boolean isValid = distance == INVALID_DISTANCE;

		return Distance.create(attr1, attr2, isValid, distance);
	}

	@Override
	public double calculateAverageDistance(List<Hit<OntologyTerm>> ontologyTermsForAttr1,
			List<Hit<OntologyTerm>> ontologyTermsForAttr2) throws ExecutionException
	{
		int listOneSize = ontologyTermsForAttr1.size();
		int listTwoSize = ontologyTermsForAttr2.size();

		if (listOneSize == 0 || listTwoSize == 0)
		{
			return INVALID_DISTANCE;
		}

		// Create an artificial symmetric 2-d array
		int maxSize = listOneSize > listTwoSize ? listOneSize : listTwoSize;
		double[] distances = new double[maxSize];
		double[][] distanceMatrix = new double[maxSize][maxSize];
		for (int i = 0; i < maxSize; i++)
		{
			OntologyTerm ot1 = i < listOneSize ? ontologyTermsForAttr1.get(i).getResult() : PSEUDO_ONTOLOGY_TERM;
			float annotationQuality1 = ontologyTermsForAttr1.get(i).getScore();
			for (int j = 0; j < maxSize; j++)
			{
				OntologyTerm ot2 = j < listTwoSize ? ontologyTermsForAttr2.get(j).getResult() : PSEUDO_ONTOLOGY_TERM;
				Double ontologyTermDistance = cachedOntologyTermRelatedness.get(OntologyTerm.and(ot1, ot2));
				float annotationQuality2 = ontologyTermsForAttr2.get(j).getScore();
				distanceMatrix[i][j] = ontologyTermDistance * annotationQuality1 * annotationQuality2;
			}
			distances[i] = 1;
		}

		// get the best pairwise match of the artificial symmetric matrix
		int counter = 0;
		Set<Integer> matchedIndices = new HashSet<>();
		while (counter < maxSize)
		{
			double maxRelatedness = 0;
			int maxRelatednessColumnIndex = 0;
			for (int i = 0; i < distanceMatrix.length; i++)
			{
				for (int j = 0; j < distanceMatrix[i].length; j++)
				{
					if (matchedIndices.contains(j)) continue;
					if (maxRelatedness == 0 || maxRelatedness < distanceMatrix[i][j])
					{
						maxRelatedness = distanceMatrix[i][j];
						maxRelatednessColumnIndex = j;
					}
				}
			}
			matchedIndices.add(maxRelatednessColumnIndex);
			distances[counter] = maxRelatedness;
			counter++;
		}
		return StatUtils.sum(distances) / maxSize;
	}

	List<Hit<OntologyTerm>> findOntologyTerms(String queryTerm)
	{
		List<Hit<OntologyTermHit>> findAllTagsForAttr = semanticSearchService.findAllTags(queryTerm,
				ontologyService.getAllOntologiesIds());

		List<Hit<OntologyTerm>> ontologyTerms = findAllTagsForAttr.stream()
				.map(hit -> Hit.create(hit.getResult().getOntologyTerm(), hit.getScore())).collect(Collectors.toList());

		return ontologyTerms;
	}

	List<Hit<OntologyTerm>> findOntologyTerms(AttributeMetaData attr, EntityMetaData entityMetaData)
	{
		List<Hit<OntologyTermHit>> findAllTagsForAttr = semanticSearchService.findAllTagsForAttr(attr,
				ontologyService.getAllOntologiesIds());

		List<Hit<OntologyTerm>> ontologyTerms = findAllTagsForAttr.stream()
				.map(hit -> Hit.create(hit.getResult().getOntologyTerm(), hit.getScore())).collect(Collectors.toList());

		return ontologyTerms;
	}

	List<OntologyTerm> resolveOntologyTerms(OntologyTerm ontologyTerm)
	{
		List<OntologyTerm> resolvedOntologyTerms = new ArrayList<>();

		if (ontologyTerm.getIRI().contains(ONTOLOGY_TERM_IRI_SEPARATOR_CHAR))
		{
			for (String atomicIri : ontologyTerm.getIRI().split(ONTOLOGY_TERM_IRI_SEPARATOR_CHAR))
			{
				OntologyTerm partialOntologyTerm = atomicIri.equals(PSEUDO_ONTOLOGY_TERM.getIRI())
						? PSEUDO_ONTOLOGY_TERM : ontologyService.getOntologyTerm(atomicIri);
				resolvedOntologyTerms.add(partialOntologyTerm);
			}
		}
		else
		{
			resolvedOntologyTerms.add(ontologyTerm);
		}
		return resolvedOntologyTerms;
	}
}
