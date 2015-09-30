package org.molgenis.ontology.utils;

import java.util.Iterator;
import java.util.List;

import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.OrientedOntologyTerm;

import com.google.common.base.Optional;

public interface OntologyReader
{
	public abstract List<OntologyTerm> getRootOntologyTerms();

	public abstract List<OntologyTerm> getChildOntologyTerms(OntologyTerm ontologyTerm);

	public abstract List<OntologyTermAnnotation> getOntologyTermAnnotations(OntologyTerm ontologyTerm,
			Optional<String> optionalRegex);

	public abstract Ontology getOntology();

	public abstract Iterator<OrientedOntologyTerm> preOrderIterator();
}
