package org.molgenis.data.semanticsearch.explain.bean;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;

import com.google.common.collect.Lists;

import static java.util.Objects.requireNonNull;

public class OntologyTermQueryExpansion
{
	private final OntologyTerm ontologyTerm;
	private final OntologyService ontologyService;
	private final boolean expanded;
	private Map<OntologyTerm, List<OntologyTerm>> queryExpansionRelation = null;

	public OntologyTermQueryExpansion(OntologyTerm ontologyTerm, OntologyService ontologyService, boolean expanded)
	{
		this.ontologyTerm = requireNonNull(ontologyTerm);
		this.ontologyService = requireNonNull(ontologyService);
		this.expanded = requireNonNull(expanded);
	}

	public List<OntologyTerm> getOntologyTerms()
	{
		if (queryExpansionRelation == null)
		{
			queryExpansionRelation = new LinkedHashMap<>();
			for (OntologyTerm atomicOntologyTerm : ontologyService.getAtomicOntologyTerms(ontologyTerm))
			{
				if (!queryExpansionRelation.containsKey(atomicOntologyTerm))
				{
					queryExpansionRelation.put(atomicOntologyTerm, Lists.newArrayList(atomicOntologyTerm));
				}
				if (expanded)
				{
					queryExpansionRelation.get(atomicOntologyTerm)
							.addAll(ontologyService.getLevelThreeChildren(atomicOntologyTerm));
				}
			}
		}
		List<OntologyTerm> ontologyTerms = new ArrayList<>();
		queryExpansionRelation.values().forEach(ots -> ontologyTerms.addAll(ots));
		return ontologyTerms;
	}

	public Set<String> getUnusedOntologyTermQueries(Hit<OntologyTermHit> hit)
	{
		List<OntologyTerm> matcheOntologyTermsToSource = ontologyService
				.getAtomicOntologyTerms(hit.getResult().getOntologyTerm());

		Function<List<String>, String> getShortestSynonymMapper = synonyms -> {
			synonyms.sort(new Comparator<String>()
			{
				public int compare(String o1, String o2)
				{
					return Integer.compare(o1.length(), o2.length());
				}
			});
			return synonyms.size() > 0 ? synonyms.get(0) : StringUtils.EMPTY;
		};

		Predicate<Entry<OntologyTerm, List<OntologyTerm>>> getUnusedOntologyTermFilter = entrySet -> matcheOntologyTermsToSource
				.stream().allMatch(ot -> !entrySet.getValue().contains(ot));

		Set<String> collect = queryExpansionRelation.entrySet().stream().filter(getUnusedOntologyTermFilter)
				.map(entry -> newArrayList(entry.getKey().getSynonyms())).map(getShortestSynonymMapper)
				.collect(toSet());

		return collect;
	}
}
