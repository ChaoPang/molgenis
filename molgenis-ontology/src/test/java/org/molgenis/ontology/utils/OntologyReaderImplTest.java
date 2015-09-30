package org.molgenis.ontology.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.springframework.util.ResourceUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.io.Resources;

public class OntologyReaderImplTest
{

	OntologyReaderImpl ontologyReaderImpl;

	@BeforeMethod
	public void setup() throws OWLOntologyCreationException, IOException
	{
		URL url = Resources.getResource("small_test_data.owl.zip");
		File ontologyTestFile = ResourceUtils.getFile(url.getFile());
		List<File> uploadedFiles = ZipFileUtil.unzip(ontologyTestFile);
		ontologyReaderImpl = new OntologyReaderImpl("test", uploadedFiles.get(0));
	}

	@Test
	public void getChildOntologyTerms()
	{
		OntologyTerm measurementOntologyTerm = OntologyTerm
				.create("http://www.molgenis.org#measurement", "measurement");
		Set<OntologyTerm> measurementChildOntologyTerms = ontologyReaderImpl
				.getChildOntologyTerms(measurementOntologyTerm);
		Assert.assertEquals(measurementChildOntologyTerms.size(), 2);
		Assert.assertTrue(measurementChildOntologyTerms.contains(OntologyTerm.create("http://www.molgenis.org#height",
				"height")));
		Assert.assertTrue(measurementChildOntologyTerms.contains(OntologyTerm.create("http://www.molgenis.org#weight",
				"weight")));
		Assert.assertFalse(measurementChildOntologyTerms.contains(OntologyTerm.create(
				"http://www.molgenis.org#body_length", "body length")));

		OntologyTerm organizationOntologyTerm = OntologyTerm.create("http://www.molgenis.org#Organization",
				"organization");
		Set<OntologyTerm> organizationChildOntologyTerms = ontologyReaderImpl
				.getChildOntologyTerms(organizationOntologyTerm);
		Assert.assertEquals(organizationChildOntologyTerms.size(), 1);
		Assert.assertTrue(organizationChildOntologyTerms.contains(OntologyTerm.create(
				"http://www.molgenis.org#hospital", "hospital")));

		OntologyTerm hospitialOntologyTerm = OntologyTerm.create("http://www.molgenis.org#hospital", "hospital");
		Set<OntologyTerm> hospitialChildOntologyTerms = ontologyReaderImpl.getChildOntologyTerms(hospitialOntologyTerm);
		Assert.assertEquals(hospitialChildOntologyTerms.size(), 1);
		Assert.assertTrue(hospitialChildOntologyTerms.contains(OntologyTerm.create("http://www.molgenis.org#GCC",
				"Genomics coordination center")));

	}

	@Test
	public void getClassAnnotations()
	{
		OntologyTerm gccOntologyTerm = OntologyTerm.create("http://www.molgenis.org#GCC",
				"Genomics coordination center");

		OWLDataFactory owlDataFactory = OWLManager.createOWLOntologyManager().getOWLDataFactory();

		Set<OntologyTermAnnotation> ontologyTermAnnotations = ontologyReaderImpl.getOntologyTermAnnotations(
				gccOntologyTerm, Optional.absent());
		OntologyTermAnnotation comment1 = OntologyTermAnnotation.create(
				owlDataFactory.getOWLClass(IRI.create("http://www.molgenis.org#GCC")),
				owlDataFactory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI()), "GCC:987654");
		OntologyTermAnnotation comment2 = OntologyTermAnnotation.create(
				owlDataFactory.getOWLClass(IRI.create("http://www.molgenis.org#GCC")),
				owlDataFactory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI()), "GCC:123456");
		OntologyTermAnnotation label = OntologyTermAnnotation.create(
				owlDataFactory.getOWLClass(IRI.create("http://www.molgenis.org#GCC")),
				owlDataFactory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
				"Genomics coordination center");

		Assert.assertEquals(ontologyTermAnnotations.size(), 3);
		Assert.assertTrue(ontologyTermAnnotations.contains(comment1));
		Assert.assertTrue(ontologyTermAnnotations.contains(comment2));
		Assert.assertTrue(ontologyTermAnnotations.contains(label));

		// Test 2
		Set<OntologyTermAnnotation> ontologyTermAnnotations2 = ontologyReaderImpl.getOntologyTermAnnotations(
				gccOntologyTerm, Optional.fromNullable("(\\w*):(\\d*)"));
		Assert.assertEquals(ontologyTermAnnotations2.size(), 2);
		Assert.assertTrue(ontologyTermAnnotations2.contains(comment1));
		Assert.assertTrue(ontologyTermAnnotations2.contains(comment2));
	}

	@Test
	public void getOntology()
	{
		Assert.assertEquals(ontologyReaderImpl.getOntology(),
				Ontology.create("http://www.molgenis.org", "http://www.molgenis.org", "test"));
	}

	@Test
	public void getRootOntologyTerms()
	{
		Set<OntologyTerm> rootOntologyTerms = ontologyReaderImpl.getRootOntologyTerms();

		Assert.assertEquals(rootOntologyTerms.size(), 3);

		Assert.assertTrue(rootOntologyTerms.contains(OntologyTerm.create("http://www.molgenis.org#measurement",
				"measurement")));
		Assert.assertTrue(rootOntologyTerms.contains(OntologyTerm.create("http://www.molgenis.org#Organization",
				"organization")));
		Assert.assertTrue(rootOntologyTerms.contains(OntologyTerm.create("http://www.molgenis.org#measurement",
				"measurement")));
		Assert.assertTrue(rootOntologyTerms.contains(OntologyTerm.create("http://www.molgenis.org#Team", "team")));

		Assert.assertFalse(rootOntologyTerms.contains(OntologyTerm.create("http://www.molgenis.org#negative_test",
				"negative_test")));
	}
}
