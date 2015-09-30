package org.molgenis.ontology.utils;

import java.util.Set;

import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.common.base.Optional;

public interface OntologyReader
{
	public abstract Set<OntologyTerm> getRootOntologyTerms();

	public abstract Set<OntologyTerm> getChildOntologyTerms(OntologyTerm ontologyTerm);

	public abstract Set<OntologyTermAnnotation> getOntologyTermAnnotations(OntologyTerm ontologyTerm,
			Optional<String> optionalRegex);

	public abstract Ontology getOntology();
}
