package org.molgenis.ontology.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.search.Searcher;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.google.common.collect.Sets;

public class OntologyLoader
{
	private static final String DATABASE_ANNOTATION_PATTERN = "(\\w*):(\\d*)";

	private final String ontologyIRI;
	private final String ontologyName;
	private final File ontologyFile;
	private final OWLDataFactory factory;
	private final OWLOntology ontology;
	private final OWLOntologyManager manager;

	private final Set<String> synonymProperties = Sets
			.newHashSet("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#FULL_SYN",
					"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90",
					"http://www.geneontology.org/formats/oboInOwl#hasExactSynonym",
					"http://www.ebi.ac.uk/efo/alternative_term");

	private final Set<String> associativeRelationProperties = Sets
			.newHashSet("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#is_associated_with");

	private final Set<String> ontologyTermDefinitionProperties = Sets.newHashSet("http://purl.obolibrary.org/obo/",
			"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#DEFINITION");

	public OntologyLoader(String ontologyName, File ontologyFile) throws OWLOntologyCreationException
	{
		this.ontologyFile = ontologyFile;
		this.manager = OWLManager.createOWLOntologyManager();
		this.factory = manager.getOWLDataFactory();
		this.ontologyName = ontologyName;
		this.ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
		this.ontologyIRI = ontology.getOntologyID().getOntologyIRI().toString();
	}

	public Set<OWLClass> getRootClasses()
	{
		Set<OWLClass> listOfTopClasses = new HashSet<OWLClass>();
		for (OWLClass cls : ontology.getClassesInSignature())
		{
			if (ontology.getSubClassAxiomsForSubClass(cls).size() == 0
					&& ontology.getEquivalentClassesAxioms(cls).size() == 0) listOfTopClasses.add(cls);
		}
		return listOfTopClasses;
	}

	public Set<OWLAnnotationAssertionAxiom> getAllAnnotationAxiom(OWLClass cls)
	{
		Set<OWLAnnotationAssertionAxiom> annotationAxioms = Searcher
				.annotations(ontology.getAnnotationAssertionAxioms(cls.getIRI())).stream()
				.map(annotation -> factory.getOWLAnnotationAssertionAxiom(cls.getIRI(), annotation))
				.collect(Collectors.toSet());

		return annotationAxioms;
	}

	public List<Set<OWLClass>> getAssociativeClasses(OWLClass cls)
	{
		List<Set<OWLClass>> alternativeDefinitions = new ArrayList<Set<OWLClass>>();
		for (OWLSubClassOfAxiom axiom : ontology.getSubClassAxiomsForSubClass(cls))
		{
			Set<OWLClass> associatedTerms = new HashSet<OWLClass>();
			OWLClassExpression expression = axiom.getSuperClass();
			if (expression.isAnonymous())
			{
				for (OWLObjectProperty property : expression.getObjectPropertiesInSignature())
				{
					if (associativeRelationProperties.contains(property.getIRI().toString()))
					{
						for (OWLClass associatedClass : expression.getClassesInSignature())
						{
							associatedTerms.add(associatedClass);
						}
					}
				}
			}
			alternativeDefinitions.add(associatedTerms);
		}
		return alternativeDefinitions;
	}

	public Set<OWLClass> getChildClass(OWLClass cls)
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

	// TODO: what if the ontology terms have multiple IDs?
	public String getId(OWLClass entity)
	{
		for (OWLAnnotationProperty owlObjectProperty : ontology.getAnnotationPropertiesInSignature())
		{
			if (ifExistsAnnotation(owlObjectProperty.toString(), "id"))
			{
				for (String annotation : getAnnotation(entity, owlObjectProperty.getIRI().toString()))
				{
					return annotation;
				}
			}
		}
		return StringUtils.EMPTY;
	}

	private boolean ifExistsAnnotation(String propertyUrl, String keyword)
	{
		String pattern = "[\\W_]*" + keyword + "[\\W_]*";
		// Use # as the separator
		String[] urlFragments = propertyUrl.split("[#/]");
		if (urlFragments.length > 1)
		{
			String label = urlFragments[urlFragments.length - 1].replaceAll("[\\W]", "_");
			for (String token : label.split("_"))
			{
				if (token.matches(pattern)) return true;
			}
		}
		return false;
	}

	public Set<String> getSynonyms(OWLClass cls)
	{
		Set<String> listOfSynonyms = new HashSet<String>();
		for (String eachSynonymProperty : synonymProperties)
		{
			listOfSynonyms.addAll(getAnnotation(cls, eachSynonymProperty));
		}
		listOfSynonyms.add(getLabel(cls));
		return listOfSynonyms;
	}

	public String getDefinition(OWLClass cls)
	{
		for (String definitionProperty : ontologyTermDefinitionProperties)
		{
			for (String definition : getAnnotation(cls, definitionProperty))
			{
				return definition;
			}
		}
		return StringUtils.EMPTY;
	}

	public String getLabel(OWLEntity entity)
	{
		for (String annotation : getAnnotation(entity, OWLRDFVocabulary.RDFS_LABEL.toString()))
		{
			return annotation;
		}
		return extractOWLClassId(entity);
	}

	private Set<String> getAnnotation(OWLEntity entity, String property)
	{
		OWLAnnotationProperty owlAnnotationProperty = factory.getOWLAnnotationProperty(IRI.create(property));
		Set<String> literalAnnotations = Searcher
				.annotations(ontology.getAnnotationAssertionAxioms(entity.getIRI()), owlAnnotationProperty).stream()
				.filter(this::isAnnotationOwlLiteral)
				.map(annotation -> ((OWLLiteral) annotation.getValue()).getLiteral()).collect(Collectors.toSet());

		return Sets.newHashSet(literalAnnotations);
	}

	private boolean isAnnotationOwlLiteral(OWLAnnotation annotation)
	{
		return annotation != null && annotation.getValue() instanceof OWLLiteral;
	}

	// TODO : FIXME replace the getAllDatabaseIds later on
	public Set<String> getDatabaseIds(OWLClass entity)
	{
		Set<String> dbAnnotations = new HashSet<String>();
		for (OWLAnnotation annotation : Searcher.annotations(ontology.getAnnotationAssertionAxioms(entity.getIRI())))
		{
			if (annotation.getValue() instanceof OWLLiteral)
			{
				OWLLiteral val = (OWLLiteral) annotation.getValue();
				String value = val.getLiteral().toString();
				if (value.matches(DATABASE_ANNOTATION_PATTERN))
				{
					dbAnnotations.add(value);
				}
			}
		}
		return dbAnnotations;
	}

	public String getOntologyLabel()
	{
		OWLAnnotationProperty labelProperty = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
		String ontologyLabel = StringUtils.EMPTY;
		for (OWLAnnotation annotation : ontology.getAnnotations())
		{
			if (annotation.getProperty().equals(labelProperty) && annotation.getValue() instanceof OWLLiteral)
			{
				OWLLiteral val = (OWLLiteral) annotation.getValue();
				ontologyLabel = val.getLiteral();
			}
		}
		return ontologyLabel;
	}

	public String extractOWLClassId(OWLEntity cls)
	{
		StringBuilder stringBuilder = new StringBuilder();
		String clsIri = cls.getIRI().toString();
		// Case where id is separated by #
		String[] split = null;
		if (clsIri.contains("#"))
		{
			split = clsIri.split("#");
		}
		else
		{
			split = clsIri.split("/");
		}
		stringBuilder.append(split[split.length - 1]);
		return stringBuilder.toString();
	}

	public String getOntologyIRI()
	{
		return ontologyIRI;
	}

	public String getOntologyName()
	{
		return ontologyName;
	}

	public String getOntologyFilePath()
	{
		return ontologyFile.getAbsolutePath();
	}

	public Set<OWLSubClassOfAxiom> getSubClassAxiomsForSuperClass(OWLClass cls)
	{
		return ontology.getSubClassAxiomsForSuperClass(cls);
	}

	public Set<OWLSubClassOfAxiom> getSubClassAxiomsForSubClass(OWLClass cls)
	{
		return ontology.getSubClassAxiomsForSubClass(cls);
	}

	public void addSynonymsProperties(Set<String> synonymsProperties)
	{
		this.synonymProperties.addAll(synonymsProperties);
	}

	public OWLClass createClass(String iri, Set<OWLClass> rootClasses)
	{
		OWLClass owlClass = factory.getOWLClass(IRI.create(iri));
		for (OWLClass rootClass : rootClasses)
		{
			if (rootClass != owlClass) addClass(rootClass, owlClass);
		}
		return owlClass;
	}

	public void addClass(OWLClass cls, OWLClass parentClass)
	{
		if (parentClass == null) parentClass = factory.getOWLThing();
		manager.applyChange(new AddAxiom(ontology, factory.getOWLSubClassOfAxiom(cls, parentClass)));
	}

	public long count()
	{
		return ontology.getClassesInSignature().size();
	}

	public Set<OWLClass> getAllclasses()
	{
		return ontology.getClassesInSignature();
	}
}