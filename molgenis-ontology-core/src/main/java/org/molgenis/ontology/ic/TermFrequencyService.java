package org.molgenis.ontology.ic;

public interface TermFrequencyService
{
	abstract Float getTermFrequency(String term);

	abstract Integer getTermOccurrence(String term);

	abstract void updateTermFrequency();
}
