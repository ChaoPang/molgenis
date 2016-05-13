package org.molgenis.ontology.core.service;

import java.util.List;
import java.util.Set;

import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.OntologyTermChildrenPredicate;

public interface OntologyService
{
	/**
	 * Retrieves all ontologies.
	 */
	List<Ontology> getOntologies();

	/**
	 * Retrieves a specific ontology
	 * 
	 * @param iri
	 *            IRI of the ontology to retrieve.
	 * @return the Ontology
	 */
	Ontology getOntology(String iri);

	/**
	 * Finds ontology terms that are exact matches to a certain search string.
	 * 
	 * @param ontologies
	 *            {@link Ontology}s to search in
	 * @param search
	 *            search term
	 * @param pageSize
	 *            number of results to return.
	 * @return List of {@link OntologyTerm}s that match the search term.
	 */
	List<OntologyTerm> findExcatOntologyTerms(List<String> ontologyIds, Set<String> terms, int pageSize);

	/**
	 * Finds ontology terms that match a certain search string.
	 * 
	 * @param ontologies
	 *            {@link Ontology}s to search in
	 * @param search
	 *            search term
	 * @param pageSize
	 *            number of results to return.
	 * @return List of {@link OntologyTerm}s that match the search term.
	 */
	List<OntologyTerm> findOntologyTerms(List<String> ontologyIds, Set<String> terms, int pageSize);

	/**
	 * Finds all {@link OntologyTerm}s in the give ontologyTerm scope
	 * 
	 * @param ontologyIds
	 * @param terms
	 * @param pageSize
	 * @param scope
	 * @return
	 */
	List<OntologyTerm> fileterOntologyTerms(List<String> ontologyIds, Set<String> terms, int pageSize,
			List<OntologyTerm> scope);

	/**
	 * Retrieve all ontology terms from the specified ontology
	 * 
	 * @param ontologyIri
	 * 
	 * @return all the {@link OntologyTerm}
	 */
	List<OntologyTerm> getAllOntologyTerms(String ontologyIri);

	// voor de tag service
	/**
	 * Retrieves a specific OntologyTerm
	 * 
	 * @param ontology
	 *            the IRI of the {@link Ontology} to search in
	 * @param IRI
	 *            comma separated list of IRIs to look for
	 * 
	 * @return Combined {@link OntologyTerm} for all IRI's listed
	 */
	OntologyTerm getOntologyTerm(String iri);

	/**
	 * Retrieves a list of atomic ontology terms if the provided ontology term has a composite iri such as "iri1,iri2"
	 * 
	 * @param iri
	 * @return a list of {@link OntologyTerm} as atomic ontology terms
	 */
	List<OntologyTerm> getAtomicOntologyTerms(OntologyTerm ontologyTerm);

	/**
	 * Retrieves children with a predicate indicating at which level in the hierarchy the children should be retrieved.
	 * 
	 * @param ontologyTerm
	 * @param continuePredicate
	 * @return
	 */
	List<OntologyTerm> getChildren(OntologyTerm ontologyTerm, OntologyTermChildrenPredicate continuePredicate);

	/**
	 * Retrieves children within the 3 levels down the hierarchy from the current {@link OntologyTerm}.
	 * 
	 * @param ontologyTerm
	 * @return
	 */
	List<OntologyTerm> getLevelThreeChildren(OntologyTerm ontologyTerm);

	/**
	 * Calculate distance between two ontology terms
	 * 
	 * @param ontologyTerm1
	 * @param ontologyTerm2
	 * @return
	 */
	Integer getOntologyTermDistance(OntologyTerm ontologyTerm1, OntologyTerm ontologyTerm2);

	/**
	 * Calculate relatedness between two ontology terms by the 2 * overlap / (depth1 + depth2)
	 * 
	 * @param ontologyTerm1
	 * @param ontologyTerm2
	 * @return
	 */
	Double getOntologyTermSemanticRelatedness(OntologyTerm ontologyTerm1, OntologyTerm ontologyTerm2);

	/**
	 * Retrieves all ontologies ids.
	 * 
	 * @return String Ontology Id
	 */
	List<String> getAllOntologiesIds();

	/**
	 * Compute best lexical similarity score between two ontology terms
	 * 
	 * @param ontologyTerm1
	 * @param ontologyTerm2
	 * @return
	 */
	Double getOntologyTermLexicalSimilarity(OntologyTerm ontologyTerm1, OntologyTerm ontologyTerm2);

	List<OntologyTerm> getParents(OntologyTerm ontologyTerm, OntologyTermChildrenPredicate continuePredicate);
}
