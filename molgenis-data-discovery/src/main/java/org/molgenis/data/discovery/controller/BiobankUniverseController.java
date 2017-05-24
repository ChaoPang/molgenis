package org.molgenis.data.discovery.controller;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.auth.User;
import org.molgenis.auth.UserMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityManager;
import org.molgenis.data.csv.CsvWriter;
import org.molgenis.data.discovery.job.BiobankUniverseJobExecution;
import org.molgenis.data.discovery.job.BiobankUniverseJobExecutionMetaData;
import org.molgenis.data.discovery.job.BiobankUniverseJobFactory;
import org.molgenis.data.discovery.job.BiobankUniverseJobImpl;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleCollectionMetaData;
import org.molgenis.data.discovery.meta.biobank.BiobankUniverseMetaData;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.AttributeMappingTablePager;
import org.molgenis.data.discovery.model.matching.BiobankSampleCollectionSimilarity;
import org.molgenis.data.discovery.model.network.VisEdge;
import org.molgenis.data.discovery.model.network.VisNetworkRequest;
import org.molgenis.data.discovery.model.network.VisNetworkResponse;
import org.molgenis.data.discovery.model.network.VisNode;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.discovery.service.BiobankUniverseService.AttributeMatchStatus;
import org.molgenis.data.i18n.LanguageService;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.meta.model.AttributeFactory;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.meta.model.EntityTypeFactory;
import org.molgenis.data.populate.IdGenerator;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.support.DynamicEntity;
import org.molgenis.file.FileStore;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.core.runas.RunAsSystemProxy;
import org.molgenis.security.core.utils.SecurityUtils;
import org.molgenis.security.user.UserAccountService;
import org.molgenis.ui.MolgenisPluginController;
import org.molgenis.ui.menu.MenuReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Collections.emptyList;
import static java.util.Collections.reverse;
import static java.util.Objects.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.data.discovery.controller.BiobankUniverseController.URI;
import static org.molgenis.data.discovery.job.BiobankUniverseJobExecutionMetaData.BIOBANK_UNIVERSE_JOB_EXECUTION;
import static org.molgenis.data.discovery.meta.matching.AttributeMappingDecisionMetaData.DecisionOptions.YES;
import static org.molgenis.data.discovery.model.network.NetworkConfiguration.NODE_SHAPE;
import static org.molgenis.data.discovery.model.network.VisNetworkRequest.NetworkType.getValueStrings;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.splitIntoUniqueTerms;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@RequestMapping(URI)
public class BiobankUniverseController extends MolgenisPluginController
{
	private final TagGroupGenerator tagGroupGenerator;
	private final DataService dataService;
	private final BiobankUniverseJobFactory biobankUniverseJobFactory;
	private final ExecutorService taskExecutor;
	private final BiobankUniverseService biobankUniverseService;
	private final OntologyService ontologyService;
	private final UserAccountService userAccountService;
	private final FileStore fileStore;
	private final EntityManager entityManager;
	private final EntityTypeFactory entityTypeFactory;
	private final AttributeFactory attributeFactory;
	private final IdGenerator idGenerator;
	private final BiobankUniverseJobExecutionMetaData biobankUniverseJobExecutionMetaData;
	private final BiobankUniverseMetaData biobankUniverseMetaData;
	private final BiobankSampleCollectionMetaData biobankSampleCollectionMetaData;
	private final MenuReaderService menuReaderService;

	public static final String DEFAULT_ONTOLOGY_URI = "UMLS";
	public static final String VIEW_BIOBANK_UNIVERSES = "view-biobank-universes";
	public static final String VIEW_SINGLE_BIOBANK_UNIVERSE = "view-single-biobank-universe";
	public static final String VIEW_BIOBANK_UNIVERSE_NETWORK = "view-biobank-universe-network";
	public static final String VIEW_BIOBANK_UNIVERSE_CURATE = "view-biobank-universe-curate";
	public static final String ID = "biobankuniverse";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;

	@Autowired
	public BiobankUniverseController(TagGroupGenerator tagGroupGenerator,
			BiobankUniverseJobFactory biobankUniverseJobFactory, BiobankUniverseService biobankUniverseService,
			OntologyService ontologyService, ExecutorService taskExecutor, UserAccountService userAccountService,
			DataService dataService, FileStore fileStore, QueryExpansionService queryExpansionService,
			BiobankUniverseRepository biobankUniverseRepository, LanguageService languageService,
			EntityManager entityManager, EntityTypeFactory entityTypeFactory, AttributeFactory attributeFactory,
			IdGenerator idGenerator, BiobankUniverseJobExecutionMetaData biobankUniverseJobExecutionMetaData,
			BiobankUniverseMetaData biobankUniverseMetaData,
			BiobankSampleCollectionMetaData biobankSampleCollectionMetaData, UserMetaData userMetaData,
			MenuReaderService menuReaderService)
	{
		super(URI);
		this.tagGroupGenerator = requireNonNull(tagGroupGenerator);
		this.biobankUniverseService = requireNonNull(biobankUniverseService);
		this.ontologyService = requireNonNull(ontologyService);
		this.biobankUniverseJobFactory = requireNonNull(biobankUniverseJobFactory);
		this.taskExecutor = requireNonNull(taskExecutor);
		this.dataService = requireNonNull(dataService);
		this.fileStore = requireNonNull(fileStore);
		this.userAccountService = requireNonNull(userAccountService);
		this.entityManager = requireNonNull(entityManager);
		this.entityTypeFactory = requireNonNull(entityTypeFactory);
		this.attributeFactory = requireNonNull(attributeFactory);
		this.idGenerator = requireNonNull(idGenerator);
		this.biobankUniverseJobExecutionMetaData = requireNonNull(biobankUniverseJobExecutionMetaData);
		this.biobankUniverseMetaData = requireNonNull(biobankUniverseMetaData);
		this.biobankSampleCollectionMetaData = requireNonNull(biobankSampleCollectionMetaData);
		this.menuReaderService = requireNonNull(menuReaderService);
	}

	@RequestMapping(method = GET)
	public String init(Model model)
	{
		model.addAttribute("biobankUniverses", getUserBiobankUniverses());
		model.addAttribute("biobankSampleCollections", getBiobankSampleCollecitons());
		model.addAttribute("semanticTypeGroups", getSemanticTypes());
		return VIEW_BIOBANK_UNIVERSES;
	}

	@RequestMapping(method = POST, value = "/universe/{id}/curate")
	public String curateMatches(@PathVariable("id") String identifier,
			@RequestParam(value = "targetAttribute", required = true) String targetAttributeIdentifier,
			@RequestParam(value = "sourceAttributes", required = true) String sourceAttributeIdentifiers,
			@RequestParam(value = "targetSampleCollection", required = true) String targetSampleCollectionName,
			@RequestParam(value = "sourceSampleCollection", required = true) String sourceSampleCollectionName,
			@RequestParam(value = "page", required = true) String page)
	{
		if (isNotBlank(identifier) && isNotBlank(targetAttributeIdentifier) && isNotBlank(targetSampleCollectionName)
				&& isNotBlank(sourceSampleCollectionName))
		{
			BiobankUniverse biobankUniverse = biobankUniverseService.getBiobankUniverse(identifier);
			if (nonNull(biobankUniverse))
			{
				BiobankSampleAttribute targetAttrinute = biobankUniverseService
						.getBiobankSampleAttribute(targetAttributeIdentifier);

				List<BiobankSampleAttribute> sourceSampleAttributes = isBlank(
						sourceAttributeIdentifiers) ? emptyList() : of(sourceAttributeIdentifiers.split(","))
						.map(StringUtils::trim).map(biobankUniverseService::getBiobankSampleAttribute)
						.collect(toList());

				BiobankSampleCollection sourceSampleCollection = biobankUniverseService
						.getBiobankSampleCollection(sourceSampleCollectionName);

				User currentUser = userAccountService.getCurrentUser();

				biobankUniverseService
						.curateAttributeMappingCandidates(biobankUniverse, targetAttrinute, sourceSampleAttributes,
								sourceSampleCollection, currentUser);
			}
		}

		//FIXME: For some reason, the method getBiobankUniverseMenuUrl() doesn't work on the server. We use hardcoded baseUrl for now
		String baseUrl = "redirect:/menu/main/biobankuniverse";
		baseUrl += "/universe/" + identifier + "?targetSampleCollectionName=" + targetSampleCollectionName + "&page="
				+ page;
		return baseUrl;
	}

	@RequestMapping("/universe/download/{id}")
	public void download(@PathVariable("id") String identifier,
			@RequestParam(value = "targetSampleCollectionName") String targetSampleCollectionName,
			HttpServletResponse response) throws IOException
	{
		if (isNotBlank(identifier))
		{
			BiobankUniverse biobankUniverse = biobankUniverseService.getBiobankUniverse(identifier);

			if (nonNull(biobankUniverse) && biobankUniverse.getMembers().size() > 1)
			{
				BiobankSampleCollection biobankSampleCollection = biobankUniverseService
						.getBiobankSampleCollection(targetSampleCollectionName);

				int total = biobankUniverseService.countBiobankSampleAttributes(biobankSampleCollection);
				AttributeMappingTablePager attributeMappingTablePager = AttributeMappingTablePager
						.create(total, total, 1);

				Table<BiobankSampleAttribute, BiobankSampleCollection, List<AttributeMappingCandidate>> candidateMappingCandidates = biobankUniverseService
						.getCuratedAttributeMatchCandidates(biobankUniverse, biobankSampleCollection,
								attributeMappingTablePager, userAccountService.getCurrentUser());

				CsvWriter csvWriter = new CsvWriter(response.getOutputStream(), ',');

				try
				{
					response.setContentType("text/csv");
					response.addHeader("Content-Disposition",
							"attachment; filename=" + generateCsvFileName(biobankUniverse.getName()));
					List<String> columnHeaders = Stream.concat(Stream.of("targetAttribute"),
							candidateMappingCandidates.columnKeySet().stream().map(BiobankSampleCollection::getName))
							.collect(toList());
					csvWriter.writeAttributeNames(columnHeaders);

					EntityType entityMetaData = createDynamicEntityMetaData(columnHeaders);

					for (Entry<BiobankSampleAttribute, Map<BiobankSampleCollection, List<AttributeMappingCandidate>>> rowMapEntry : candidateMappingCandidates
							.rowMap().entrySet())
					{
						BiobankSampleAttribute targetAttribute = rowMapEntry.getKey();

						Entity row = new DynamicEntity(entityMetaData);
						row.set("targetAttribute", targetAttribute.getName());
						for (Entry<BiobankSampleCollection, List<AttributeMappingCandidate>> columnMapEntry : rowMapEntry
								.getValue().entrySet())
						{
							BiobankSampleCollection sourceBiobankSampleCollection = columnMapEntry.getKey();
							String matchedSourceAttributeNames = columnMapEntry.getValue().stream()
									.filter(candidate -> candidate.getDecisions().stream()
											.anyMatch(decision -> decision.getDecision().equals(YES)))
									.map(AttributeMappingCandidate::getSource).map(BiobankSampleAttribute::getName)
									.collect(Collectors.joining(","));
							row.set(sourceBiobankSampleCollection.getName(), matchedSourceAttributeNames);
						}

						csvWriter.add(row);
					}
				}
				finally
				{
					if (csvWriter != null) IOUtils.closeQuietly(csvWriter);
				}
			}
		}
	}

	@RequestMapping(value = "/universe/{id}/attributematch", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public List<AttributeMappingCandidate> getAttributeMatches(@PathVariable("id") String identifier,
			@RequestParam(value = "targetAttribute", required = true) String targetAttributeIdentifier,
			@RequestParam(value = "sourceBiobankSampleCollection", required = true) String sourceBiobankSampleCollectionName)
	{
		BiobankUniverse biobankUniverse = biobankUniverseService.getBiobankUniverse(identifier);

		BiobankSampleAttribute targetAttribute = biobankUniverseService
				.getBiobankSampleAttribute(targetAttributeIdentifier);

		BiobankSampleCollection sourceBiobankSampleCollection = biobankUniverseService
				.getBiobankSampleCollection(sourceBiobankSampleCollectionName);

		if (nonNull(biobankUniverse) && nonNull(targetAttribute) && nonNull(sourceBiobankSampleCollection))
		{
			return biobankUniverseService.getCuratedAttributeMatchCandidates(biobankUniverse, targetAttribute,
					sourceBiobankSampleCollection);
		}

		return emptyList();
	}

	@RequestMapping("/universe/{id}")
	public String getUniverse(@PathVariable("id") String identifier,
			@RequestParam(value = "targetSampleCollectionName", required = false) String targetSampleCollectionName,
			@RequestParam(value = "page", required = false) Integer page, Model model)
	{
		if (isNotBlank(identifier))
		{
			BiobankUniverse biobankUniverse = biobankUniverseService.getBiobankUniverse(identifier);

			if (nonNull(biobankUniverse) && biobankUniverse.getMembers().size() > 1)
			{
				List<BiobankSampleCollection> members = biobankUniverse.getMembers().stream().skip(0).collect(toList());
				reverse(members);

				BiobankSampleCollection biobankSampleCollection = isNotBlank(
						targetSampleCollectionName) ? biobankUniverseService
						.getBiobankSampleCollection(targetSampleCollectionName) : members.get(0);

				AttributeMappingTablePager attributeMappingTablePager = AttributeMappingTablePager
						.create(biobankUniverseService.countBiobankSampleAttributes(biobankSampleCollection),
								isNull(page) ? 1 : page);

				Table<BiobankSampleAttribute, BiobankSampleCollection, AttributeMatchStatus> attributeMatchStatusTable = biobankUniverseService
						.getAttributeMatchStatus(biobankUniverse, biobankSampleCollection, attributeMappingTablePager,
								userAccountService.getCurrentUser());

				Map<String, Map<String, AttributeMatchStatus>> candidateMappingCandidatesFreemarker = new HashMap<>();
				Map<String, BiobankSampleAttribute> biobankSampleAttributeMap = new HashMap<>();

				for (Cell<BiobankSampleAttribute, BiobankSampleCollection, AttributeMatchStatus> cell : attributeMatchStatusTable
						.cellSet())
				{
					BiobankSampleAttribute targetAttribute = cell.getRowKey();
					BiobankSampleCollection sourceBiobankSampleCollection = cell.getColumnKey();
					AttributeMatchStatus attributeMatchStatus = cell.getValue();

					String targetAttributeName = targetAttribute.getName();
					String sourceBiobankSampleCollectionName = sourceBiobankSampleCollection.getName();

					if (!candidateMappingCandidatesFreemarker.containsKey(targetAttributeName))
					{
						candidateMappingCandidatesFreemarker.put(targetAttributeName, new HashMap<>());
					}

					candidateMappingCandidatesFreemarker.get(targetAttributeName)
							.put(sourceBiobankSampleCollectionName, attributeMatchStatus);

					if (!biobankSampleAttributeMap.containsKey(targetAttributeName))
					{
						biobankSampleAttributeMap.put(targetAttributeName, targetAttribute);
					}
				}

				model.addAttribute("biobankUniverse", biobankUniverse);
				model.addAttribute("sampleCollections", members);
				model.addAttribute("targetSampleCollection", biobankSampleCollection);
				model.addAttribute("candidateMappingCandidates", candidateMappingCandidatesFreemarker);
				model.addAttribute("biobankSampleAttributeMap", biobankSampleAttributeMap);
				model.addAttribute("attributeMappingTablePager", attributeMappingTablePager);
			}
		}

		return VIEW_BIOBANK_UNIVERSE_CURATE;
	}

	@RequestMapping(method = GET, value = "/network")
	public String network(Model model)
	{
		model.addAttribute("biobankSampleCollections", biobankUniverseService.getAllBiobankSampleCollections());
		model.addAttribute("biobankUniverses", biobankUniverseService.getBiobankUniverses());
		model.addAttribute("networkTypes", getValueStrings());

		return VIEW_BIOBANK_UNIVERSE_NETWORK;
	}

	@RequestMapping(method = POST, value = "/network/topic", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public List<OntologyTerm> createNetwork(@RequestBody String queryString)
	{
		if (isNotBlank(queryString))
		{
			Ontology defaultOntology = ontologyService.getOntology(DEFAULT_ONTOLOGY_URI);

			List<String> ontologyIds = nonNull(defaultOntology) ? Collections
					.singletonList(defaultOntology.getId()) : ontologyService.getAllOntologyIds();

			List<OntologyTerm> ontologyTerms = ontologyService
					.findOntologyTerms(ontologyIds, splitIntoUniqueTerms(queryString), 20);

			return ontologyTerms;
		}

		return emptyList();
	}

	@RequestMapping(method = POST, value = "/network/create", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public VisNetworkResponse createNetwork(@RequestBody VisNetworkRequest visNetworkRequest)
	{
		List<VisNode> nodes = new ArrayList<>();

		List<VisEdge> edges = new ArrayList<>();

		BiobankUniverse biobankUniverse = biobankUniverseService
				.getBiobankUniverse(visNetworkRequest.getBiobankUniverseIdentifier());

		List<OntologyTerm> ontologyTermTopics = ontologyService
				.getOntologyTerms(visNetworkRequest.getOntologyTermIris());

		if (Objects.nonNull(biobankUniverse))
		{
			List<BiobankSampleCollectionSimilarity> collectionSimilarities = biobankUniverseService
					.getCollectionSimilarities(biobankUniverse, visNetworkRequest.getNetworkTypeEnum(),
							ontologyTermTopics);

			List<BiobankSampleCollection> uniqueBiobankCollections = collectionSimilarities.stream().flatMap(
					collectionSimilarity -> of(collectionSimilarity.getTarget(), collectionSimilarity.getSource()))
					.distinct().collect(toList());

			for (BiobankSampleCollection biobankSampleCollection : uniqueBiobankCollections)
			{
				int size = biobankUniverseService.countBiobankSampleAttributes(biobankSampleCollection);
				nodes.add(VisNode.create(biobankSampleCollection.getName(), biobankSampleCollection.getName(), size,
						NODE_SHAPE));
			}

			Multimap<String, String> existingPairs = LinkedHashMultimap.create();
			for (BiobankSampleCollectionSimilarity biobankCollectionSimilarity : collectionSimilarities)
			{
				String targetName = biobankCollectionSimilarity.getTarget().getName();
				String sourceName = biobankCollectionSimilarity.getSource().getName();
				String label = biobankCollectionSimilarity.getLabel();

				if (!existingPairs.containsEntry(targetName, sourceName) && !existingPairs
						.containsEntry(sourceName, targetName))
				{
					String identifier = targetName + sourceName;
					double distance = Math.round(biobankCollectionSimilarity.getSimilarity() * 100.0) / 100.0;
					edges.add(VisEdge.create(identifier, label, distance, targetName, sourceName));
					existingPairs.put(targetName, sourceName);
				}
			}
		}

		return VisNetworkResponse.create(nodes, edges);
	}

	@RequestMapping(value = "/addUniverse", method = RequestMethod.POST)
	public String addUniverse(@RequestParam("universeName") String universeName,
			@RequestParam(required = false) String[] biobankSampleCollectionNames,
			@RequestParam(required = false) String[] semanticTypes, Model model)
	{
		if (isNotBlank(universeName))
		{
			BiobankUniverse biobankUniverse = biobankUniverseService.addBiobankUniverse(universeName,
					semanticTypes != null ? of(semanticTypes).collect(toList()) : emptyList(),
					userAccountService.getCurrentUser());

			if (biobankSampleCollectionNames != null)
			{
				List<BiobankSampleCollection> biobankSampleCollections = biobankUniverseService
						.getBiobankSampleCollections(of(biobankSampleCollectionNames).collect(toList()));

				submit(biobankUniverse, biobankSampleCollections);
			}
		}

		model.addAttribute("biobankUniverses", getUserBiobankUniverses());
		model.addAttribute("biobankSampleCollections", getBiobankSampleCollecitons());
		model.addAttribute("semanticTypeGroups", getSemanticTypes());

		return init(model);
	}

	@RequestMapping(value = "/removeBiobankuniverse", method = RequestMethod.POST)
	public String deleteBiobankUniverse(@RequestParam(required = true) String biobankUniverseId, Model model)
	{
		biobankUniverseService.deleteBiobankUniverse(biobankUniverseId);
		model.addAttribute("biobankUniverses", getUserBiobankUniverses());
		model.addAttribute("biobankSampleCollections", getBiobankSampleCollecitons());
		model.addAttribute("semanticTypeGroups", getSemanticTypes());
		return init(model);
	}

	@RequestMapping(value = "/addUniverseMembers", method = POST)
	public String addMembers(@RequestParam(required = true) String biobankUniverseId,
			@RequestParam(required = false) String[] biobankSampleCollectionNames, Model model)
	{
		BiobankUniverse biobankUniverse = biobankUniverseService.getBiobankUniverse(biobankUniverseId);

		if (biobankUniverse != null && biobankSampleCollectionNames != null)
		{
			List<BiobankSampleCollection> biobankSampleCollections = biobankUniverseService
					.getBiobankSampleCollections(of(biobankSampleCollectionNames).collect(toList()));

			submit(biobankUniverse, biobankSampleCollections);
		}

		return init(model);
	}

	@RequestMapping(value = "/addKeyConcepts", method = POST)
	public String addKeyConcepts(@RequestParam(required = true) String biobankUniverseId,
			@RequestParam(required = false) String[] semanticTypes, Model model)
	{
		BiobankUniverse universe = biobankUniverseService.getBiobankUniverse(biobankUniverseId);

		biobankUniverseService
				.addKeyConcepts(universe, semanticTypes != null ? of(semanticTypes).collect(toList()) : emptyList());

		model.addAttribute("biobankUniverses", getUserBiobankUniverses());
		model.addAttribute("biobankSampleCollections", getBiobankSampleCollecitons());
		model.addAttribute("semanticTypeGroups", getSemanticTypes());

		return init(model);
	}

	private void submit(BiobankUniverse biobankUniverse, List<BiobankSampleCollection> biobankSampleCollections)
	{
		List<BiobankSampleCollection> newMembers = biobankSampleCollections.stream()
				.filter(member -> !biobankUniverse.getMembers().contains(member)).collect(toList());

		// Add new members to the universe
		biobankUniverseService.addBiobankUniverseMember(biobankUniverse, newMembers);

		BiobankUniverseJobExecution jobExecution = new BiobankUniverseJobExecution(biobankUniverseJobExecutionMetaData,
				biobankUniverseMetaData, biobankSampleCollectionMetaData, biobankUniverseService, entityManager);
		jobExecution.setIdentifier(idGenerator.generateId());
		jobExecution.setUniverse(biobankUniverse);
		jobExecution.setMembers(newMembers);
		jobExecution.setUser(userAccountService.getCurrentUser());

		RunAsSystemProxy.runAsSystem(() ->
		{
			dataService.add(BIOBANK_UNIVERSE_JOB_EXECUTION, jobExecution);
		});

		BiobankUniverseJobImpl biobankUniverseJobImpl = biobankUniverseJobFactory.create(jobExecution);
		taskExecutor.submit(biobankUniverseJobImpl);
	}

	private EntityType createDynamicEntityMetaData(List<String> columnHeaders)
	{
		EntityType entityMetaData = entityTypeFactory.create();
		entityMetaData.setId("DownloadAttributeMapping");
		for (String columnHeader : columnHeaders)
		{
			Attribute attributeMetaData = attributeFactory.create();
			attributeMetaData.setIdentifier(columnHeader);
			entityMetaData.addAttribute(attributeMetaData);
		}
		return entityMetaData;
	}

	private Set<String> getSemanticTypes()
	{
		return newLinkedHashSet(
				ontologyService.getAllSemanticTypes().stream().map(SemanticType::getName).collect(toList()));
	}

	private List<BiobankUniverse> getUserBiobankUniverses()
	{
		return biobankUniverseService.getBiobankUniverses().stream()
				.filter(universe -> SecurityUtils.currentUserIsSu() || universe.getOwner().getUsername()
						.equals(SecurityUtils.getCurrentUsername())).collect(toList());
	}

	private List<BiobankSampleCollection> getBiobankSampleCollecitons()
	{
		return biobankUniverseService.getAllBiobankSampleCollections();
	}

	private String getBiobankUniverseMenuUrl()
	{
		return menuReaderService.getMenu().findMenuItemPath(ID);
	}

	private String generateCsvFileName(String dataSetName)
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dataSetName + "_" + dateFormat.format(new Date()) + ".csv";
	}
}
