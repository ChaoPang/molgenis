package org.molgenis.ontology.importer.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.Repository;
import org.molgenis.data.mem.InMemoryRepository;
import org.molgenis.data.support.FileRepositoryCollection;
import org.molgenis.data.support.GenericImporterExtensions;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.UuidGenerator;
import org.molgenis.ontology.core.meta.OntologyMetaData;
import org.molgenis.ontology.core.meta.OntologyTermDynamicAnnotationMetaData;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.molgenis.ontology.core.meta.OntologyTermNodePathMetaData;
import org.molgenis.ontology.core.meta.OntologyTermSynonymMetaData;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.OrientedOntologyTerm;
import org.molgenis.ontology.utils.OWLClassContainer;
import org.molgenis.ontology.utils.OntologyLoader;
import org.molgenis.ontology.utils.OntologyReader;
import org.molgenis.ontology.utils.OntologyReaderImpl;
import org.molgenis.ontology.utils.OntologyTermAnnotation;
import org.molgenis.ontology.utils.ZipFileUtil;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * RepositoryCollection for the import of an owl file.
 * 
 * Reads the owl file's contents using an {@link OntologyLoader}. Fills {@link InMemoryRepository}s with their contents.
 */
public class OntologyRepositoryCollection extends FileRepositoryCollection
{
	private final IdGenerator idGenerator = new UuidGenerator();

	// repositories
	private final Repository ontologyRepository = new InMemoryRepository(OntologyMetaData.INSTANCE);
	private final Repository nodePathRepository = new InMemoryRepository(OntologyTermNodePathMetaData.INSTANCE);
	private final Repository ontologyTermRepository = new InMemoryRepository(OntologyTermMetaData.INSTANCE);
	private final Repository annotationRepository = new InMemoryRepository(
			OntologyTermDynamicAnnotationMetaData.INSTANCE);
	private final Repository synonymRepository = new InMemoryRepository(OntologyTermSynonymMetaData.INSTANCE);
	private Map<String, Repository> repositories = ImmutableMap.of(OntologyTermDynamicAnnotationMetaData.ENTITY_NAME,
			annotationRepository, OntologyTermSynonymMetaData.ENTITY_NAME, synonymRepository,
			OntologyTermNodePathMetaData.ENTITY_NAME, nodePathRepository, OntologyMetaData.ENTITY_NAME,
			ontologyRepository, OntologyTermMetaData.ENTITY_NAME, ontologyTermRepository);

	private final OntologyReader ontologyReader;
	private final Multimap<String, Entity> nodePathsPerOntologyTerm = ArrayListMultimap.create();
	private Entity ontologyEntity;

	private static final String DATABASE_ANNOTATION_PATTERN = "(\\w*):(\\d*)";

	/**
	 * Creates a new {@link OntologyRepositoryCollection} for an ontology file
	 * 
	 * @param file
	 *            the ontology file
	 */
	public OntologyRepositoryCollection(File file) throws OWLOntologyCreationException, FileNotFoundException,
			IOException
	{
		super(GenericImporterExtensions.getOntology());
		if (file == null) throw new IllegalArgumentException("file is null");

		String name = file.getName();
		if (name.endsWith(GenericImporterExtensions.OBO_ZIP.toString()))
		{
			name = name.substring(0, name.lastIndexOf('.' + GenericImporterExtensions.OBO_ZIP.toString())).replace('.',
					'_');
		}
		else if (name.endsWith(GenericImporterExtensions.OWL_ZIP.toString()))
		{
			name = name.substring(0, name.lastIndexOf('.' + GenericImporterExtensions.OWL_ZIP.toString())).replace('.',
					'_');
		}
		else
		{
			throw new IllegalArgumentException("Not a obo.zip or owl.zip file [" + file.getName() + "]");
		}

		List<File> uploadedFiles = ZipFileUtil.unzip(file);
		ontologyReader = new OntologyReaderImpl(name, uploadedFiles.get(0));
		createOntology();
		createNodePaths();
		createOntologyTerms();
	}

	/**
	 * Initializes the {@link #ontologyEntity} and adds it to the {@link #ontologyRepository}.
	 */
	private void createOntology()
	{
		Ontology ontology = ontologyReader.getOntology();
		ontologyEntity = new MapEntity(OntologyMetaData.INSTANCE);
		ontologyEntity.set(OntologyMetaData.ID, idGenerator.generateId());
		ontologyEntity.set(OntologyMetaData.ONTOLOGY_IRI, ontology.getIRI());
		ontologyEntity.set(OntologyMetaData.ONTOLOGY_NAME, ontology.getName());
		ontologyRepository.add(ontologyEntity);
	}

	/**
	 * Creates {@link OntologyTermNodePathMetaData} {@link Entity}s for an entire ontology tree and writes them to the
	 * {@link #nodePathsPerOntologyTerm} {@link Multimap}.
	 */
	private void createNodePaths()
	{
		Iterator<OrientedOntologyTerm> preOrderIterator = ontologyReader.preOrderIterator();
		while (preOrderIterator.hasNext())
		{
			OrientedOntologyTerm orientedOntologyTerm = preOrderIterator.next();
			OntologyTerm ontologyTerm = orientedOntologyTerm.getOntologyTerm();
			String ontologyTermNodePath = orientedOntologyTerm.getNodePath();
			String ontologyTermIRI = ontologyTerm.getIRI().toString();
			Entity nodePathEntity = createNodePathEntity(orientedOntologyTerm, ontologyTermNodePath);
			nodePathsPerOntologyTerm.put(ontologyTermIRI, nodePathEntity);
		}
	}

	/**
	 * Creates {@link OntologyTermMetaData} {@link Entity}s for all {@link OWLClass}ses in the {@link #loader} and adds
	 * them to the {@link #ontologyTermRepository}.
	 */
	private void createOntologyTerms()
	{
		Iterator<OrientedOntologyTerm> preOrderIterator = ontologyReader.preOrderIterator();
		while (preOrderIterator.hasNext())
		{
			createOntologyTerm(preOrderIterator.next());
		}
	}

	/**
	 * Creates an {@link OntologyTermMetaData} {@link Entity} and adds it in the {@link #ontologyTermRepository}
	 * 
	 * @param ontologyTerm
	 *            the OWLClass to create an entity for
	 * @return the created ontology term {@link Entity}
	 */
	private Entity createOntologyTerm(OrientedOntologyTerm orientedOntologyTerm)
	{
		String ontologyTermIRI = orientedOntologyTerm.getOntologyTerm().getIRI().toString();
		String ontologyTermName = orientedOntologyTerm.getOntologyTerm().getLabel();
		Entity entity = new MapEntity(OntologyTermMetaData.INSTANCE);
		entity.set(OntologyTermMetaData.ID, idGenerator.generateId());
		entity.set(OntologyTermMetaData.ONTOLOGY_TERM_IRI, ontologyTermIRI);
		entity.set(OntologyTermMetaData.ONTOLOGY_TERM_NAME, ontologyTermName);
		entity.set(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, createSynonyms(orientedOntologyTerm));
		entity.set(OntologyTermMetaData.ONTOLOGY_TERM_DYNAMIC_ANNOTATION,
				createDynamicAnnotations(orientedOntologyTerm));
		entity.set(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH, nodePathsPerOntologyTerm.get(ontologyTermIRI));
		entity.set(OntologyTermMetaData.ONTOLOGY, ontologyEntity);
		ontologyTermRepository.add(entity);
		return entity;
	}

	/**
	 * Creates {@link OntologyTermSynonymMetaData} {@link Entity}s for an ontology term
	 * 
	 * @param ontologyTerm
	 *            {@link OWLClass} for the ontology term
	 * @return {@link List} of created synonym {@link Entity}s
	 */
	private List<Entity> createSynonyms(OrientedOntologyTerm orientedOntologyTerm)
	{
		return orientedOntologyTerm.getOntologyTerm().getSynonyms().stream().map(this::createSynonym)
				.collect(Collectors.toList());
	}

	/**
	 * Creates an {@link OntologyTermSynonymMetaData} {@link Entity} and adds it to the {@link #synonymRepository}.
	 * 
	 * @param synonym
	 *            String of the synonym to create an {@link Entity} for
	 * @return the created {@link Entity}
	 */
	private Entity createSynonym(String synonym)
	{
		MapEntity entity = new MapEntity(OntologyTermSynonymMetaData.INSTANCE);
		entity.set(OntologyTermSynonymMetaData.ID, idGenerator.generateId());
		entity.set(OntologyTermSynonymMetaData.ONTOLOGY_TERM_SYNONYM, synonym);
		synonymRepository.add(entity);
		return entity;
	}

	/**
	 * Creates {@link OntologyTermDynamicAnnotationMetaData} {@link Entity}s for the databaseIds of an ontology term.
	 * 
	 * @param owlClass
	 *            the term to create annotation entities for
	 * @return List of created {@link Entity}s.
	 */
	private List<Entity> createDynamicAnnotations(OrientedOntologyTerm orientedOntologyTerm)
	{
		List<OntologyTermAnnotation> ontologyTermAnnotations = ontologyReader.getOntologyTermAnnotations(
				orientedOntologyTerm.getOntologyTerm(), Optional.of(DATABASE_ANNOTATION_PATTERN));
		return ontologyTermAnnotations.stream().map(this::createDynamicAnnotation).collect(Collectors.toList());
	}

	/**
	 * Creates an {@link OntologyTermDynamicAnnotationMetaData} {@link Entity} for a key:value label.
	 * 
	 * @param label
	 *            the key:value label
	 * @return the {@link Entity}
	 */
	private Entity createDynamicAnnotation(OntologyTermAnnotation ontologyTermAnnotation)
	{
		String annotationValue = ontologyTermAnnotation.getValue();
		Entity entity = new MapEntity(OntologyTermDynamicAnnotationMetaData.INSTANCE);
		entity.set(OntologyTermDynamicAnnotationMetaData.ID, idGenerator.generateId());
		String fragments[] = annotationValue.split(":");
		entity.set(OntologyTermDynamicAnnotationMetaData.NAME, fragments[0]);
		entity.set(OntologyTermDynamicAnnotationMetaData.VALUE, fragments[1]);
		entity.set(OntologyTermDynamicAnnotationMetaData.LABEL, annotationValue);
		annotationRepository.add(entity);
		return entity;
	}

	/**
	 * Creates a {@link OntologyTermNodePathMetaData} {@link Entity} and stores it in the {@link #nodePathRepository}.
	 * 
	 * @param container
	 *            {@link OWLClassContainer} for the path to the ontology term
	 * @param ontologyTermNodePath
	 *            the node path
	 * @return the created {@link Entity}
	 */
	private Entity createNodePathEntity(OrientedOntologyTerm orientedOntologyTerm, String ontologyTermNodePath)
	{
		MapEntity entity = new MapEntity(OntologyTermNodePathMetaData.INSTANCE);
		entity.set(OntologyTermNodePathMetaData.ID, idGenerator.generateId());
		entity.set(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH, ontologyTermNodePath);
		entity.set(OntologyTermNodePathMetaData.ROOT, orientedOntologyTerm.isRoot());
		nodePathRepository.add(entity);
		return entity;
	}

	@Override
	public Iterable<String> getEntityNames()
	{
		return repositories.keySet();
	}

	@Override
	public String getName()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Repository addEntityMeta(EntityMetaData entityMeta)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<Repository> iterator()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Repository getRepository(String name)
	{
		if (!repositories.containsKey(name)) throw new MolgenisDataException("Unknown entity name [" + name + "]");
		return repositories.get(name);
	}

	@Override
	public boolean hasRepository(String name)
	{
		if (null == name) return false;
		Iterator<String> entityNames = getEntityNames().iterator();
		while (entityNames.hasNext())
		{
			if (entityNames.next().equals(name)) return true;
		}
		return false;
	}
}
