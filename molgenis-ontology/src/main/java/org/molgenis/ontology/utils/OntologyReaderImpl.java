package org.molgenis.ontology.utils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.search.Searcher;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class OntologyReaderImpl implements OntologyReader
{
	private final OWLDataFactory factory;
	private final OWLOntology ontology;
	private final OWLOntologyManager manager;
	private final String ontologyName;

	private final Set<String> synonymProperties = Sets
			.newHashSet("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#FULL_SYN",
					"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90",
					"http://www.geneontology.org/formats/oboInOwl#hasExactSynonym",
					"http://www.ebi.ac.uk/efo/alternative_term");

	private final Set<String> ontologyTermDefinitionProperties = Sets.newHashSet("http://purl.obolibrary.org/obo/",
			"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#DEFINITION");

	public OntologyReaderImpl(String ontologyName, File ontologyFile) throws OWLOntologyCreationException
	{
		this.manager = OWLManager.createOWLOntologyManager();
		this.factory = manager.getOWLDataFactory();
		this.ontologyName = Objects.requireNonNull(ontologyName);
		this.ontology = manager.loadOntologyFromOntologyDocument(Objects.requireNonNull(ontologyFile));
	}

	@Override
	public Set<OntologyTerm> getRootOntologyTerms()
	{
		return ontology.getClassesInSignature().stream().filter(this::isOwlClassTop)
				.map(this::transformOWLClassToOntologyTerm).collect(Collectors.toSet());
	}

	@Override
	public Set<OntologyTerm> getChildOntologyTerms(OntologyTerm ontologyTerm)
	{
		OWLClass owlClass = factory.getOWLClass(IRI.create(ontologyTerm.getIRI()));
		return owlClass != null ? getSubClasses(owlClass).stream().map(this::transformOWLClassToOntologyTerm)
				.collect(Collectors.toSet()) : Collections.emptySet();
	}

	@Override
	public Set<OntologyTermAnnotation> getOntologyTermAnnotations(OntologyTerm ontologyTerm,
			Optional<String> optionalRegex)
	{
		OWLClass owlClass = factory.getOWLClass(IRI.create(ontologyTerm.getIRI()));
		return owlClass != null ? getOntologyTermAnnotations(owlClass, Optional.fromNullable(null), optionalRegex) : Collections
				.emptySet();
	}

	@Override
	public Ontology getOntology()
	{
		Optional<IRI> ontologyIRIObject = ontology.getOntologyID().getOntologyIRI();
		return Objects.requireNonNull(ontologyIRIObject.isPresent() ? Ontology.create(ontologyIRIObject.get()
				.toString(), ontologyIRIObject.get().toString(), ontologyName) : null);
	}

	private OntologyTerm transformOWLClassToOntologyTerm(OWLClass cls)
	{
		return OntologyTerm.create(cls.getIRI().toString(), getClassLabel(cls), getClassDefinition(cls),
				Lists.newArrayList(getClassSynonyms(cls)));
	}

	private boolean isOwlClassTop(OWLClass cls)
	{
		return ontology.getSubClassAxiomsForSubClass(cls).size() == 0
				&& ontology.getEquivalentClassesAxioms(cls).size() == 0;
	}

	private Set<OWLClass> getSubClasses(OWLClass cls)
	{
		Set<OWLClass> listOfClasses = new HashSet<OWLClass>();
		for (OWLSubClassOfAxiom axiom : ontology.getSubClassAxiomsForSuperClass(cls))
		{
			OWLClassExpression expression = axiom.getSubClass();
			if (!expression.isAnonymous())
			{
				OWLClass asOWLClass = expression.asOWLClass();
				listOfClasses.add(asOWLClass);
			}
		}
		return listOfClasses;
	}

	private Set<OntologyTermAnnotation> getOntologyTermAnnotations(OWLClass cls, Optional<String> property,
			Optional<String> optionalRegexFilters)
	{
		Set<OntologyTermAnnotation> ontologyTermAnnotations = new HashSet<>();
		if (property.isPresent())
		{
			OWLAnnotationProperty owlAnnotationProperty = factory.getOWLAnnotationProperty(IRI.create(property.get()));
			Set<String> annotations = getClassAnnotations(cls, property.get(), optionalRegexFilters);

			ontologyTermAnnotations.addAll(annotations.stream()
					.map(annotation -> OntologyTermAnnotation.create(cls, owlAnnotationProperty, annotation))
					.collect(Collectors.toSet()));
		}
		else
		{
			for (OWLAnnotationProperty owlAnnotationProperty : ontology.getAnnotationPropertiesInSignature())
			{
				ontologyTermAnnotations.addAll(getOntologyTermAnnotations(cls,
						Optional.fromNullable(owlAnnotationProperty.getIRI().toString()), optionalRegexFilters));
			}
		}
		return ontologyTermAnnotations;
	}

	private Set<String> getClassSynonyms(OWLClass cls)
	{
		Set<String> listOfSynonyms = Sets.newHashSet(getClassLabel(cls));
		for (String eachSynonymProperty : synonymProperties)
		{
			listOfSynonyms.addAll(getClassAnnotations(cls, eachSynonymProperty, Optional.absent()));
		}
		return listOfSynonyms;
	}

	private String getClassDefinition(OWLClass cls)
	{
		for (String definitionProperty : ontologyTermDefinitionProperties)
		{
			for (String definition : getClassAnnotations(cls, definitionProperty, Optional.absent()))
			{
				return definition;
			}
		}
		return null;
	}

	private String getClassLabel(OWLClass cls)
	{
		for (String annotation : getClassAnnotations(cls, OWLRDFVocabulary.RDFS_LABEL.toString(), Optional.absent()))
		{
			return annotation;
		}

		String[] split;
		String clsIri = cls.getIRI().toString();
		if (clsIri.contains("#"))
		{
			split = clsIri.split("#");
		}
		else
		{
			split = clsIri.split("/");
		}
		return split[split.length - 1];
	}

	private Set<String> getClassAnnotations(OWLClass cls, String property, Optional<String> optionalRegexFilters)
	{
		OWLAnnotationProperty owlAnnotationProperty = factory.getOWLAnnotationProperty(IRI.create(property));
		Collection<OWLAnnotation> literalAnnotations = Searcher.annotations(
				ontology.getAnnotationAssertionAxioms(cls.getIRI()), owlAnnotationProperty);

		return Sets.newHashSet(filterAnnotations(literalAnnotations, optionalRegexFilters));
	}

	private Collection<String> filterAnnotations(Collection<OWLAnnotation> annotations,
			Optional<String> optionalRegexFilters)
	{
		Set<String> literalAnnotations = annotations.stream().filter(this::isAnnotationOwlLiteral)
				.map(this::owlLiteralToString).collect(Collectors.toSet());

		return optionalRegexFilters.isPresent() ? literalAnnotations.stream()
				.filter(annotation -> annotation.matches(optionalRegexFilters.get())).collect(Collectors.toSet()) : literalAnnotations;
	}

	private String owlLiteralToString(OWLAnnotation annotation)
	{
		return ((OWLLiteral) annotation.getValue()).getLiteral();
	}

	private boolean isAnnotationOwlLiteral(OWLAnnotation annotation)
	{
		return annotation != null && annotation.getValue() instanceof OWLLiteral;
	}
}