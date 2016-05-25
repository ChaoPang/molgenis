package org.molgenis.data.semanticsearch.explain.bean;

import java.util.List;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_QueryExpansionSolution.class)
public abstract class OntologyTermQueryExpansionSolution implements Comparable<OntologyTermQueryExpansionSolution>
{
	public abstract List<OntologyTerm> getMatchOntologyTerms();

	public abstract List<OntologyTerm> getUnmatchOntologyTerms();

	public static OntologyTermQueryExpansionSolution create(List<OntologyTerm> matchOntologyTerms,
			List<OntologyTerm> unmatchOntologyTerms)
	{
		return new AutoValue_QueryExpansionSolution(matchOntologyTerms, unmatchOntologyTerms);
	}

	public int compareTo(OntologyTermQueryExpansionSolution other)
	{
		return Integer.compare(other.getMatchOntologyTerms().size(), getMatchOntologyTerms().size());
	}
}
