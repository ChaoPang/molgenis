package org.molgenis.ontology.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.OrientedOntologyTerm;
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
import com.google.common.collect.TreeTraverser;

public class OntologyReaderImpl implements OntologyReader
{
	private final OWLDataFactory factory;
	private final OWLOntology ontology;
	private final OWLOntologyManager manager;
	private final String ontologyName;

	private static final String PSEUDO_ROOT_CLASS_NODEPATH = "0[0]";

	private static final Set<String> synonymProperties = Sets
			.newHashSet("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#FULL_SYN",
					"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90",
					"http://www.geneontology.org/formats/oboInOwl#hasExactSynonym",
					"http://www.ebi.ac.uk/efo/alternative_term");

	private static final Set<String> ontologyTermDefinitionProperties = Sets.newHashSet(
			"http://purl.obolibrary.org/obo/", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#DEFINITION");

	public OntologyReaderImpl(String ontologyName, File ontologyFile) throws OWLOntologyCreationException
	{
		this.manager = OWLManager.createOWLOntologyManager();
		this.factory = manager.getOWLDataFactory();
		this.ontologyName = Objects.requireNonNull(ontologyName);
		this.ontology = manager.loadOntologyFromOntologyDocument(Objects.requireNonNull(ontologyFile));
	}

	@Override
	public Iterator<OrientedOntologyTerm> preOrderIterator()
	{
		final OntologyTerm artificialTopOntologyTerm = OntologyTerm.create(ontologyName, ontologyName);
		TreeTraverser<OrientedOntologyTerm> traverser = new TreeTraverser<OrientedOntologyTerm>()
		{
			@Override
			public Iterable<OrientedOntologyTerm> children(OrientedOntologyTerm orientedOntologyTerm)
			{
				int count = 0;
				List<OrientedOntologyTerm> childOrientedOntologyTerms = new ArrayList<>();
				String parentNodePath = orientedOntologyTerm.getNodePath();
				OntologyTerm currentOntologyTerm = orientedOntologyTerm.getOntologyTerm();

				for (OntologyTerm childOntologyTerm : (artificialTopOntologyTerm == currentOntologyTerm ? getRootOntologyTerms() : getChildOntologyTerms(currentOntologyTerm)))
				{
					childOrientedOntologyTerms.add(OrientedOntologyTerm.create(childOntologyTerm,
							constructNodePath(parentNodePath, count), false));
					count++;
				}
				return childOrientedOntologyTerms;
			}
		};
		return traverser.preOrderTraversal(
				OrientedOntologyTerm.create(artificialTopOntologyTerm, PSEUDO_ROOT_CLASS_NODEPATH, true)).iterator();
	}

	@Override
	public List<OntologyTerm> getRootOntologyTerms()
	{
		return ontology.getClassesInSignature().stream().filter(this::isOwlClassTop)
				.map(this::transformOWLClassToOntologyTerm).collect(Collectors.toList());
	}

	@Override
	public List<OntologyTerm> getChildOntologyTerms(OntologyTerm ontologyTerm)
	{
		OWLClass owlClass = factory.getOWLClass(IRI.create(ontologyTerm.getIRI()));
		return owlClass != null ? getSubClasses(owlClass).stream().map(this::transformOWLClassToOntologyTerm)
				.collect(Collectors.toList()) : Collections.emptyList();
	}

	@Override
	public List<OntologyTermAnnotation> getOntologyTermAnnotations(OntologyTerm ontologyTerm,
			Optional<String> optionalRegex)
	{
		OWLClass owlClass = factory.getOWLClass(IRI.create(ontologyTerm.getIRI()));
		return owlClass != null ? getOntologyTermAnnotations(owlClass, Optional.fromNullable(null), optionalRegex) : Collections
				.emptyList();
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

	/**
	 * Get all the annotations of the current {@link OWLClass}.
	 * 
	 * @param cls
	 * @param property
	 * @param optionalRegexFilters
	 * @return the {@link OntologyTermAnnotation}s of the current {@link OWLClass}
	 */
	List<OntologyTermAnnotation> getOntologyTermAnnotations(OWLClass cls, Optional<String> property,
			Optional<String> optionalRegexFilters)
	{
		List<OntologyTermAnnotation> ontologyTermAnnotations = new ArrayList<>();
		if (property.isPresent())
		{
			ontologyTermAnnotations.addAll(getClassAnnotations(cls, property.get(), optionalRegexFilters));
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

	/**
	 * Get a list of synonyms including the label of the current {@link OWLClass}.
	 * 
	 * @param cls
	 * @return the synonyms of the current {@link OWLClass}
	 */
	Set<String> getClassSynonyms(OWLClass cls)
	{
		Set<String> listOfSynonyms = Sets.newHashSet(getClassLabel(cls));
		for (String eachSynonymProperty : synonymProperties)
		{
			listOfSynonyms.addAll(getClassAnnotations(cls, eachSynonymProperty, Optional.absent()).stream()
					.map(OntologyTermAnnotation::getValue).collect(Collectors.toSet()));
		}
		return listOfSynonyms;
	}

	/**
	 * Get the first encountered definition of the current {@link OWLClass}. If the definition does not exist, the null
	 * value will be returned.
	 * 
	 * @param cls
	 * @return The definition of the current {@link OWLClass}
	 */
	String getClassDefinition(OWLClass cls)
	{
		for (String definitionProperty : ontologyTermDefinitionProperties)
		{
			for (OntologyTermAnnotation ontologyTermDefinitionAnnotation : getClassAnnotations(cls, definitionProperty,
					Optional.absent()))
			{
				return ontologyTermDefinitionAnnotation.getValue();
			}
		}
		return null;
	}

	/**
	 * Get a label for the current {@link OWLClass}. If the label exists, it will be retrieved. Otherwise the last part
	 * of the URL behind the forward slash or hash symbol
	 * 
	 * @param cls
	 * @return The display label of the current {@link OWLClass}
	 */
	String getClassLabel(OWLClass cls)
	{
		for (OntologyTermAnnotation ontologyTermLabelAnnotation : getClassAnnotations(cls,
				OWLRDFVocabulary.RDFS_LABEL.toString(), Optional.absent()))
		{
			return ontologyTermLabelAnnotation.getValue();
		}
		String clsIri = cls.getIRI().toString();
		String[] split = clsIri.contains("#") ? clsIri.split("#") : clsIri.split("/");
		return split[split.length - 1];
	}

	/**
	 * Get all the {@link OntologyTermAnnotation}s based on the specified annotationProperty for the current
	 * {@link OWLClass}
	 * 
	 * @param cls
	 * @param annotationProperty
	 * @param optionalRegexFilters
	 * @return a list of {@link OntologyTermAnnotation}s that match the specified regular expression filter
	 */
	private Set<OntologyTermAnnotation> getClassAnnotations(OWLClass cls, String annotationProperty,
			Optional<String> optionalRegexFilters)
	{
		OWLAnnotationProperty owlAnnotationProperty = factory.getOWLAnnotationProperty(IRI.create(annotationProperty));
		Collection<OWLAnnotation> literalAnnotations = Searcher.annotations(
				ontology.getAnnotationAssertionAxioms(cls.getIRI()), owlAnnotationProperty);

		return filterAnnotations(literalAnnotations, optionalRegexFilters).stream()
				.map(annotation -> OntologyTermAnnotation.create(cls, owlAnnotationProperty, annotation))
				.collect(Collectors.toSet());
	}

	/**
	 * Filter the {@link OWLAnnotation}s that matches the regular expression
	 * 
	 * @param annotations
	 * @param optionalRegexFilters
	 * @return a list of filtered of {@link OWLAnnotation}s
	 */
	private Collection<String> filterAnnotations(Collection<OWLAnnotation> annotations,
			Optional<String> optionalRegexFilters)
	{
		Set<String> literalAnnotations = annotations.stream().filter(this::isAnnotationOwlLiteral)
				.map(this::owlLiteralToString).collect(Collectors.toSet());

		return optionalRegexFilters.isPresent() ? literalAnnotations.stream()
				.filter(annotation -> annotation.matches(optionalRegexFilters.get())).collect(Collectors.toSet()) : literalAnnotations;
	}

	/**
	 * A helper function to convert {@link OWLAnnotation} to a {@link String} value representation
	 * 
	 * @param annotation
	 * @return the string value of the {@link OWLAnnotation}
	 */
	private String owlLiteralToString(OWLAnnotation annotation)
	{
		return ((OWLLiteral) annotation.getValue()).getLiteral();
	}

	/**
	 * A helper function to check if the annotation is of type {@link OWLLiteral}
	 * 
	 * @param annotation
	 * @return
	 */
	private boolean isAnnotationOwlLiteral(OWLAnnotation annotation)
	{
		return annotation != null && annotation.getValue() instanceof OWLLiteral;
	}

	/**
	 * Constructs the node path string for a child node
	 * 
	 * @param parentNodePath
	 *            node path string of the node's parent
	 * @param currentPosition
	 *            position of the node in the parent's child list
	 * @return node path string
	 */
	private String constructNodePath(String parentNodePath, int currentPosition)
	{
		StringBuilder nodePathStringBuilder = new StringBuilder();
		if (!StringUtils.isEmpty(parentNodePath)) nodePathStringBuilder.append(parentNodePath).append('.');
		nodePathStringBuilder.append(currentPosition).append('[')
				.append(nodePathStringBuilder.toString().split("\\.").length - 1).append(']');
		return nodePathStringBuilder.toString();
	}
}