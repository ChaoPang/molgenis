package org.molgenis.ontocat;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.molgenis.ontocat.bioportal.Ontology;
import org.molgenis.ontocat.bioportal.OntologyTerm;
import org.molgenis.ontocat.io.OWLOntologyWriter;
import org.molgenis.ontocat.io.OWLOntologyWriterImpl;
import org.molgenis.ontocat.ontologyservice.OntologyService;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import com.google.common.collect.Maps;

public class OntologyDownloader
{
	private OntologyService os;
	private final ConcurrentMap<String, String> ontologyTermIris = Maps.newConcurrentMap();
	private OWLOntologyWriter writer;
	private int totalNumberOfClasses;
	private AtomicInteger jobsLeft;
	private ExecutorService executors = Executors.newFixedThreadPool(15);

	public OntologyDownloader(OntologyService os)
	{
		this.os = os;
	}

	void download(OntologyService os, String ontologyAcronym, File output) throws OWLOntologyCreationException,
			InterruptedException
	{
		totalNumberOfClasses = os.getProxyCountForOntology(ontologyAcronym);
		Ontology ontology = os.getOntology(ontologyAcronym);
		writer = new OWLOntologyWriterImpl(ontology.getIRI());

		List<OntologyTerm> rootTerms = os.getRootTerms(ontologyAcronym);
		jobsLeft = new AtomicInteger(rootTerms.size());
		rootTerms.forEach(ot -> executors.submit(() -> downloadChildren(ot, null)));

		// wait for all jobs to have finished with a maximum of 1 hour
		executors.awaitTermination(1, TimeUnit.HOURS);

		writer.saveOWLOntology(output);
	}

	private void downloadChildren(OntologyTerm ontologyTerm, OWLClass parentClass)
	{
		try
		{
			OWLClass cls = addParentChildRelation(ontologyTerm, writer, parentClass);
			if (ontologyTermIris.putIfAbsent(ontologyTerm.getIRI(), ontologyTerm.getIRI()) == null)
			{
				System.out.print('.');
				if (ontologyTermIris.size() % 50 == 0)
				{
					System.out.println("INFO : " + ontologyTermIris.size() + " out of " + totalNumberOfClasses
							+ " classes have been downloaded! jobsLeft=" + jobsLeft.get());
				}
				List<OntologyTerm> children = os.getChildren(ontologyTerm);
				for (OntologyTerm child : children)
				{
					jobsLeft.incrementAndGet();
					executors.submit(() -> downloadChildren(child, cls));
				}
			}
		}
		catch (Exception ex)
		{
			System.out.println("Error adding class " + ontologyTerm.getIRI() + ": " + ex);
		}
		if (jobsLeft.decrementAndGet() == 0)
		{
			System.out.println("No more jobs left. Shutting down.");
			executors.shutdown();
		}
	}

	private synchronized OWLClass addParentChildRelation(OntologyTerm ontologyTerm, OWLOntologyWriter writer,
			OWLClass parentClass)
	{
		OWLClass cls = writer.createOWLClass(ontologyTerm.getIRI(), ontologyTerm.getLabel(),
				ontologyTerm.getSynonyms(), ontologyTerm.getDescription(), parentClass);
		return cls;
	}

}
