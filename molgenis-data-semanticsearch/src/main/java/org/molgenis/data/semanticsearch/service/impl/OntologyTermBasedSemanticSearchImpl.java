package org.molgenis.data.semanticsearch.service.impl;

import static com.google.common.collect.Iterables.size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semantic.Relation;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.OntologyTagService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.DistanceMatrixReport;
import org.molgenis.data.semanticsearch.service.bean.DistanceMetric;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.core.runas.RunAsSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Multimap;

import static java.util.Objects.requireNonNull;

public class OntologyTermBasedSemanticSearchImpl
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

	private final static String UMLS_ONTOLOGY_IRI = "UMLS";
	private final static String ONTOLOGY_TERM_IRI_SEPARATOR_CHAR = ",";
	private final static double INVALID_DISTANCE = -1;

	private final SemanticSearchService semanticSearchService;
	private final OntologyService ontologyService;
	private final OntologyTagService ontologyTagService;

	@Autowired
	public OntologyTermBasedSemanticSearchImpl(SemanticSearchService semanticSearchService,
			OntologyService ontologyService, OntologyTagService tagService)
	{
		this.semanticSearchService = requireNonNull(semanticSearchService);
		this.ontologyService = requireNonNull(ontologyService);
		this.ontologyTagService = requireNonNull(tagService);
	}

	public List<DistanceMetric> getAttrDistance(String attrName1, EntityMetaData entityMetaData1,
			EntityMetaData entityMetaData2) throws ExecutionException
	{
		List<DistanceMetric> distanceMetrics = new ArrayList<>();
		AttributeMetaData attr1 = entityMetaData1.getAttribute(attrName1);
		if (attr1 == null) return distanceMetrics;

		for (AttributeMetaData attr2 : entityMetaData2.getAtomicAttributes())
		{
			distanceMetrics.add(getAttrDistance(attr1, attr2, entityMetaData1, entityMetaData2));
		}

		return distanceMetrics;
	}

	public DistanceMetric getAttrDistance(AttributeMetaData attr1, AttributeMetaData attr2,
			EntityMetaData entityMetaData1, EntityMetaData entityMetaData2) throws ExecutionException
	{
		List<OntologyTerm> ontologyTermsForAttr1 = findOntologyTerms(attr1, entityMetaData1);
		List<OntologyTerm> ontologyTermsForAttr2 = findOntologyTerms(attr2, entityMetaData2);
		double distance = calculateAverageDistance(ontologyTermsForAttr1, ontologyTermsForAttr2);
		boolean isValid = distance == INVALID_DISTANCE;

		return DistanceMetric.create(attr1, attr2, isValid, distance);
	}

	@Async
	@RunAsSystem
	public void getAsyncEntitiesDistance(DistanceMatrixReport distanceMatrixReport, EntityMetaData entityMetaData1,
			EntityMetaData entityMetaData2) throws ExecutionException
	{
		int finishedNumber = 0;
		double totalNumber = size(entityMetaData1.getAtomicAttributes()) * size(entityMetaData2.getAtomicAttributes());

		for (AttributeMetaData attr1 : entityMetaData1.getAtomicAttributes())
		{
			List<OntologyTerm> ontologyTermsForAttr1 = findOntologyTerms(attr1, entityMetaData1);
			for (AttributeMetaData attr2 : entityMetaData2.getAtomicAttributes())
			{
				List<OntologyTerm> ontologyTermsForAttr2 = findOntologyTerms(attr2, entityMetaData2);
				double distance = calculateAverageDistance(ontologyTermsForAttr1, ontologyTermsForAttr2);
				boolean isValid = distance == INVALID_DISTANCE;

				distanceMatrixReport.setProgress(++finishedNumber / totalNumber);
				distanceMatrixReport.setDistanceMetrics(attr1.getName() + ":" + attr1.getLabel(),
						DistanceMetric.create(attr1, attr2, isValid, distance));
			}
		}

		distanceMatrixReport.setFinished(true);
		distanceMatrixReport.setProgress(100.0);
	}

	double calculateAverageDistance(List<OntologyTerm> ontologyTermsForAttr1, List<OntologyTerm> ontologyTermsForAttr2)
			throws ExecutionException
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
			OntologyTerm ot1 = i < listOneSize ? ontologyTermsForAttr1.get(i) : PSEUDO_ONTOLOGY_TERM;
			for (int j = 0; j < maxSize; j++)
			{
				OntologyTerm ot2 = j < listTwoSize ? ontologyTermsForAttr2.get(j) : PSEUDO_ONTOLOGY_TERM;
				Double ontologyTermDistance = cachedOntologyTermRelatedness.get(OntologyTerm.and(ot1, ot2));
				distanceMatrix[i][j] = ontologyTermDistance;
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

	List<OntologyTerm> findOntologyTerms(AttributeMetaData attr, EntityMetaData entityMetaData)
	{
		Multimap<Relation, OntologyTerm> tagsForAttribute = ontologyTagService.getTagsForAttribute(entityMetaData,
				attr);
		List<OntologyTerm> ontologyTerms = new ArrayList<>();
		if (tagsForAttribute.size() > 0)
		{
			for (OntologyTerm ot : tagsForAttribute.values())
			{
				ontologyTerms.addAll(resolveOntologyTerms(ot));
			}
		}
		else
		{
			Ontology ontology = ontologyService.getOntology(UMLS_ONTOLOGY_IRI);
			if (ontology != null)
			{
				Hit<OntologyTerm> findTags = semanticSearchService.findTags(attr, Arrays.asList(ontology.getId()));
				if (findTags != null)
				{
					ontologyTerms.addAll(resolveOntologyTerms(findTags.getResult()));
				}
			}
		}
		return ontologyTerms;
	}

	List<OntologyTerm> resolveOntologyTerms(OntologyTerm ontologyTerm)
	{
		List<OntologyTerm> resolvedOntologyTerms = new ArrayList<>();
		if (ontologyTerm.getIRI().contains(ONTOLOGY_TERM_IRI_SEPARATOR_CHAR))
		{
			for (String partialTag : ontologyTerm.getIRI().split(ONTOLOGY_TERM_IRI_SEPARATOR_CHAR))
			{
				OntologyTerm partialOntologyTerm = partialTag.equals(PSEUDO_ONTOLOGY_TERM.getIRI())
						? PSEUDO_ONTOLOGY_TERM : ontologyService.getOntologyTerm(partialTag);
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
