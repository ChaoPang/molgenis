package org.molgenis.ontology.utils;

import javax.annotation.Nullable;

import org.molgenis.gson.AutoGson;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_OntologyTermAnnotation.class)
public abstract class OntologyTermAnnotation
{
	@Nullable
	public abstract OWLClass getSubject();

	@Nullable
	public abstract OWLAnnotationProperty getProperty();

	@Nullable
	public abstract String getValue();

	public static OntologyTermAnnotation create(OWLClass subject, OWLAnnotationProperty property, String value)
	{
		return new AutoValue_OntologyTermAnnotation(subject, property, value);
	}
}
