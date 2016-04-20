package org.molgenis.data.semanticsearch.explain.bean;

import java.util.List;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_QueryExpansionSolution.class)
public abstract class QueryExpansionSolution implements Comparable<QueryExpansionSolution>
{
	public abstract List<OntologyTerm> getMatchOntologyTerms();

	public abstract List<OntologyTerm> getUnmatchOntologyTerms();

	public static QueryExpansionSolution create(List<OntologyTerm> matchOntologyTerms,
			List<OntologyTerm> unmatchOntologyTerms)
	{
		return new AutoValue_QueryExpansionSolution(matchOntologyTerms, unmatchOntologyTerms);
	}

	public int compareTo(QueryExpansionSolution other)
	{
		return Integer.compare(other.getMatchOntologyTerms().size(), getMatchOntologyTerms().size());
	}
}
