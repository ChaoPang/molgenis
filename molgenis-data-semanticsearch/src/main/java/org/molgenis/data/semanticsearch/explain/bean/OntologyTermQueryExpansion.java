package org.molgenis.data.semanticsearch.explain.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import static java.util.Objects.requireNonNull;

public class OntologyTermQueryExpansion
{
	private final OntologyTerm ontologyTerm;
	private final OntologyService ontologyService;
	private final boolean expanded;
	private Multimap<OntologyTerm, OntologyTerm> queryExpansionRelation;

	public OntologyTermQueryExpansion(OntologyTerm ontologyTerm, OntologyService ontologyService, boolean expanded)
	{
		this.ontologyTerm = requireNonNull(ontologyTerm);
		this.ontologyService = requireNonNull(ontologyService);
		this.expanded = requireNonNull(expanded);
		this.queryExpansionRelation = LinkedHashMultimap.create();
		populate();
	}

	public List<OntologyTerm> getOntologyTerms()
	{
		return Lists.newArrayList(queryExpansionRelation.values());
	}

	public QueryExpansionSolution getQueryExpansionSolution(Hit<OntologyTermHit> sourceOntologyTermHit)
	{
		List<OntologyTerm> sourceMatchedOntologyTerms = ontologyService
				.getAtomicOntologyTerms(sourceOntologyTermHit.getResult().getOntologyTerm());

		List<OntologyTerm> matchedOntologyTerms = new ArrayList<>();

		List<OntologyTerm> unMatchedOntologyTerms = new ArrayList<>();

		for (OntologyTerm targetMatchedOntologyTerm : queryExpansionRelation.keySet())
		{
			Collection<OntologyTerm> associcatedTargetMatchedOntologyTerms = queryExpansionRelation
					.get(targetMatchedOntologyTerm);

			if (sourceMatchedOntologyTerms.stream().anyMatch(ot -> associcatedTargetMatchedOntologyTerms.contains(ot)))
			{
				matchedOntologyTerms.add(targetMatchedOntologyTerm);
			}
			else
			{
				unMatchedOntologyTerms.add(targetMatchedOntologyTerm);
			}
		}

		return QueryExpansionSolution.create(matchedOntologyTerms, unMatchedOntologyTerms);
	}

	private void populate()
	{
		for (OntologyTerm atomicOntologyTerm : ontologyService.getAtomicOntologyTerms(ontologyTerm))
		{
			queryExpansionRelation.put(atomicOntologyTerm, atomicOntologyTerm);
			if (expanded)
			{
				queryExpansionRelation.putAll(atomicOntologyTerm,
						ontologyService.getLevelThreeChildren(atomicOntologyTerm));
			}
		}
	}
}
