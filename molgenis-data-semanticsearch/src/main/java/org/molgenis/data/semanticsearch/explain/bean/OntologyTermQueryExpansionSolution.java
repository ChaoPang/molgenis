package org.molgenis.data.semanticsearch.explain.bean;

import com.google.auto.value.AutoValue;
import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTermImpl;

import java.util.Map;

@AutoValue
@AutoGson(autoValueClass = AutoValue_OntologyTermQueryExpansionSolution.class)
public abstract class OntologyTermQueryExpansionSolution implements Comparable<OntologyTermQueryExpansionSolution>
{
	public abstract Map<OntologyTermImpl, OntologyTermImpl> getMatchOntologyTerms();

	public abstract boolean isHighQuality();

	public static OntologyTermQueryExpansionSolution create(Map<OntologyTermImpl, OntologyTermImpl> matchOntologyTerms,
			boolean highQuality)
	{
		return new AutoValue_OntologyTermQueryExpansionSolution(matchOntologyTerms, highQuality);
	}

	public int compareTo(OntologyTermQueryExpansionSolution other)
	{
		return Integer.compare(other.getMatchOntologyTerms().size(), getMatchOntologyTerms().size());
	}
}