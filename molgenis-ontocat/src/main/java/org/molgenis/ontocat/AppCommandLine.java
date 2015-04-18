package org.molgenis.ontocat;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.ontocat.bioportal.BioPortalOntologyService;
import org.molgenis.ontocat.bioportal.Ontology;
import org.molgenis.ontocat.ontologyservice.OntologyService;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class AppCommandLine
{
	private final OntologyService os = new BioPortalOntologyService();
	private final CommandLine cmd;
	private final Options options;
	private final OntologyDownloader downloader = new OntologyDownloader(os);

	public AppCommandLine(String[] args) throws ParseException
	{
		options = createOptions();
		CommandLineParser parser = new BasicParser();
		cmd = parser.parse(options, args);
	}

	private static Options createOptions()
	{
		Options options = new Options();
		options.addOption(new Option("list", "List all the ontologies available at BioPortal"));
		options.addOption(new Option("acronym", true, "provide the acronym of ontology for which you want to download"));
		options.addOption(new Option("filePath", true, "provide the filePath for downloaded ontology"));
		return options;
	}

	private void run() throws ParseException, OWLOntologyCreationException, InterruptedException
	{
		if (cmd.hasOption("list"))
		{
			listOntologies();
		}
		else if (cmd.hasOption("acronym") && cmd.hasOption("filePath"))
		{
			downloadOntology();
		}
		else
		{
			showHelpMessage();
		}
	}

	private void showHelpMessage()
	{
		HelpFormatter formatter = new HelpFormatter();
		formatter
				.printHelp(
						"java -jar ontologyDownloader.jar",
						"where options include:",
						options,
						"\nTo download big ontologies such as SNOMEDCT, it is suggested to increase the maximum amount of memory allocated to java e.g. -Xmx2G");
	}

	private void downloadOntology() throws OWLOntologyCreationException, InterruptedException
	{
		String ontologyAcronym = cmd.getOptionValue("acronym");
		File output = new File(cmd.getOptionValue("filePath"));
		if (StringUtils.isNotEmpty(ontologyAcronym) && output.getParentFile().exists())
		{
			downloader.download(os, ontologyAcronym, output);
		}
	}

	private void listOntologies()
	{
		List<Ontology> ontologies = os.getOntologies();
		System.out.println("Acronym\t\tOntology Name\t\tOntology IRI");
		ontologies.forEach(ontology -> System.out.println(ontology.getId() + "\t\t" + ontology.getName() + "\t\t"
				+ ontology.getIRI()));
	}

	public static void main(String[] args) throws FileNotFoundException, OWLOntologyCreationException, ParseException,
			InterruptedException
	{
		AppCommandLine acl = new AppCommandLine(args);
		acl.run();
	}
}
