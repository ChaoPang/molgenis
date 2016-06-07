package org.molgenis.validator;

import org.molgenis.data.csv.CsvRepository;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Sets.difference;
import static java.util.stream.Collectors.toCollection;

public class EmxValidator
{
	private static final String ID = "id";
	HashSet<String> ontologyTermIds;

	private void validate(String fileDirectory) throws IOException
	{
		CsvRepository ontologyTerm = new CsvRepository(fileDirectory + File.separator + "Ontology_OntologyTerm.csv");

		// ==============================================================

		System.out.println("Creating ontologyTermSynonym sets...");
		ontologyTermIds = getIdsForEntity(ontologyTerm, "ontologyTermSynonym").collect(toCollection(HashSet::new));

		HashSet<String> ontologyTermSynonym = getIdsForEntity(
				new CsvRepository(fileDirectory + File.separator + "Ontology_OntologyTermSynonym.csv"), ID)
				.collect(Collectors.toCollection(HashSet::new));

		calculateDifference(ontologyTermIds, ontologyTermSynonym, "Ontology_OntologyTerm",
				"Ontology_OntologyTermSynonym");
		System.out.println("Checked ontologyTermSynonym references, clearing...");
		ontologyTermSynonym.clear();

		// ==============================================================

		System.out.println("Creating ontologyTermNodePath set...");
		ontologyTermIds = getIdsForEntity(ontologyTerm, "nodePath").collect(toCollection(HashSet::new));

		HashSet<String> ontologyTermNodePath = getIdsForEntity(
				new CsvRepository(fileDirectory + File.separator + "Ontology_OntologyTermNodePath.csv"), ID)
				.collect(Collectors.toCollection(HashSet::new));

		calculateDifference(ontologyTermIds, ontologyTermNodePath, "Ontology_OntologyTerm",
				"Ontology_OntologyTermNodePath");
		System.out.println("Checked ontologyTermNodePath references, clearing...");
		ontologyTermNodePath.clear();

		// ==============================================================

		System.out.println("Creating SemanticType set...");
		ontologyTermIds = getIdsForEntity(ontologyTerm, "semanticType").map(id -> id.trim())
				.collect(toCollection(HashSet::new));

		HashSet<String> SemanticType = getIdsForEntity(
				new CsvRepository(fileDirectory + File.separator + "Ontology_SemanticType.csv"), ID)
				.map(id -> id.trim()).collect(Collectors.toCollection(HashSet::new));

		calculateDifference(ontologyTermIds, SemanticType, "Ontology_OntologyTerm", "Ontology_SemanticType");
		System.out.println("Checked SemanticType references, clearing...");
		SemanticType.clear();

		// ==============================================================

		System.out.println("Creating ontology set...");
		ontologyTermIds = getIdsForEntity(ontologyTerm, "ontology").map(id -> id.trim())
				.collect(toCollection(HashSet::new));

		HashSet<String> ontology = getIdsForEntity(
				new CsvRepository(fileDirectory + File.separator + "Ontology_Ontology.csv"), ID).map(id -> id.trim())
				.collect(Collectors.toCollection(HashSet::new));

		calculateDifference(ontologyTermIds, ontology, "Ontology_OntologyTerm", "Ontology_Ontology");
		System.out.println("Checked ontology references, clearing...");
		ontology.clear();
	}

	public Stream<String> getIdsForEntity(CsvRepository repository, String attributeName)
	{
		return repository.stream().map(entity -> entity.getString(attributeName));
	}

	public void calculateDifference(HashSet<String> set1, HashSet<String> set2, String entityName, String refEntityName)
	{
		System.out.println("Calculating set difference between [" + entityName + "] and [" + refEntityName + "]");
		Set<String> difference = difference(set1, set2);
		if (!difference.isEmpty())
		{
			System.out.println("difference = " + difference);
		}
		System.out.println("Moving on to next entity");
	}

	public static void main(String[] args) throws IOException
	{
		EmxValidator emxValidator = new EmxValidator();
		emxValidator.validate(args[0]);

	}
}
