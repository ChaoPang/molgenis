package org.molgenis.data.discovery.controller;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.molgenis.data.discovery.controller.BiobankUniverseController.URI;
import static org.molgenis.data.discovery.model.network.NetworkConfiguration.NODE_SHAPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.DataService;
import org.molgenis.data.Repository;
import org.molgenis.data.discovery.job.BiobankUniverseJobExecution;
import org.molgenis.data.discovery.job.BiobankUniverseJobExecutionMetaData;
import org.molgenis.data.discovery.job.BiobankUniverseJobFactory;
import org.molgenis.data.discovery.job.BiobankUniverseJobImpl;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.BiobankCollectionSimilarity;
import org.molgenis.data.discovery.model.network.VisEdge;
import org.molgenis.data.discovery.model.network.VisNetworkRequest;
import org.molgenis.data.discovery.model.network.VisNetworkResponse;
import org.molgenis.data.discovery.model.network.VisNode;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.discovery.validation.BiobankUniverseEvaluationTool;
import org.molgenis.data.discovery.validation.CalculateSimilaritySimulationImpl;
import org.molgenis.data.excel.ExcelRepositoryCollection;
import org.molgenis.data.i18n.LanguageService;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.file.FileStore;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

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
	private final MenuReaderService menuReaderService;
	private final CalculateSimilaritySimulationImpl calculateSimilaritySimulation;
	private final FileStore fileStore;

	public static final String VIEW_BIOBANK_UNIVERSES = "view-biobank-universes";
	public static final String VIEW_SINGLE_BIOBANK_UNIVERSE = "view-single-biobank-universe";
	public static final String VIEW_BIOBANK_UNIVERSE_TEST = "view-biobank-universe-test";
	public static final String ID = "biobankuniverse";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;

	@Autowired
	public BiobankUniverseController(TagGroupGenerator tagGroupGenerator,
			BiobankUniverseJobFactory biobankUniverseJobFactory, BiobankUniverseService biobankUniverseService,
			OntologyService ontologyService, ExecutorService taskExecutor, UserAccountService userAccountService,
			DataService dataService, FileStore fileStore, MenuReaderService menuReaderService,
			QueryExpansionService queryExpansionService, BiobankUniverseRepository biobankUniverseRepository,
			LanguageService languageService)
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
		this.menuReaderService = requireNonNull(menuReaderService);

		this.calculateSimilaritySimulation = new CalculateSimilaritySimulationImpl(ontologyService,
				biobankUniverseService, biobankUniverseRepository, queryExpansionService);
	}

	@RequestMapping(method = GET)
	public String init(Model model)
	{
		model.addAttribute("biobankUniverses", getUserBiobankUniverses());
		model.addAttribute("biobankSampleCollections", getBiobankSampleCollecitons());
		model.addAttribute("semanticTypeGroups", getSemanticTypes());
		return VIEW_BIOBANK_UNIVERSES;
	}

	@RequestMapping("/universe/{id}")
	public String getUniverse(@PathVariable("id") String identifier, Model model)
	{
		// TODO: decide what to show in a universe
		return VIEW_SINGLE_BIOBANK_UNIVERSE;
	}

	@RequestMapping("/universe/tag")
	@ResponseBody
	public List<TagGroup> tag(@RequestBody Map<String, String> request)
	{
		String queryString = request.get("queryString");
		if (StringUtils.isNotBlank(queryString))
		{
			return tagGroupGenerator.generateTagGroups(queryString, ontologyService.getAllOntologiesIds());
		}
		return Collections.emptyList();
	}

	@RequestMapping(method = GET, value = "/test")
	public String test(Model model) throws Exception
	{
		model.addAttribute("biobankSampleCollections", biobankUniverseService.getAllBiobankSampleCollections());
		model.addAttribute("biobankUniverses", biobankUniverseService.getBiobankUniverses());

		return VIEW_BIOBANK_UNIVERSE_TEST;
	}

	@RequestMapping(method = POST, value = "/test/upload", headers = "Content-Type=multipart/form-data")
	public String upload(@RequestParam(value = "target", required = true) String target,
			@RequestParam(value = "sources", required = true) String[] sources,
			@RequestParam(value = "file", required = true) Part file, Model model,
			HttpServletRequest httpServletRequest) throws Exception
	{
		if (Objects.nonNull(target) && Objects.nonNull(sources))
		{
			File relevantMatchFile = fileStore.store(file.getInputStream(),
					httpServletRequest.getSession().getId() + "_input.xls");

			ExcelRepositoryCollection excelRepositoryCollection = new ExcelRepositoryCollection(relevantMatchFile);
			Repository manualMatchRepository = excelRepositoryCollection.getSheet(0);

			Multimap<String, String> collectRelevantMatches = BiobankUniverseEvaluationTool
					.collectRelevantMatches(manualMatchRepository);

			BiobankSampleCollection targetBiobankSampleCollection = biobankUniverseService
					.getBiobankSampleCollection(target);

			List<BiobankSampleCollection> sourceBiobankSampleCollections = biobankUniverseService
					.getBiobankSampleCollections(of(sources).collect(toList()));

			calculateSimilaritySimulation.testCollectionSimilarity(targetBiobankSampleCollection,
					sourceBiobankSampleCollections, collectRelevantMatches);
		}

		return test(model);
	}

	@RequestMapping(method = GET, value = "/test/calculate")
	public String upload(@RequestParam(value = "biobankUniverseIdentifier", required = true) String identifier,
			Model model) throws Exception
	{
		BiobankUniverse biobankUniverse = biobankUniverseService.getBiobankUniverse(identifier);

		Map<String, List<BiobankCollectionSimilarity>> result = new LinkedHashMap<>();

		if (Objects.nonNull(biobankUniverse))
		{
			List<BiobankCollectionSimilarity> collectionSimilarities = biobankUniverseService.getBiobankUniverses()
					.stream().flatMap(universe -> biobankUniverseService.getCollectionSimilarities(universe).stream())
					.collect(Collectors.toList());

			List<BiobankSampleCollection> uniqueCollections = Stream
					.concat(collectionSimilarities.stream().map(BiobankCollectionSimilarity::getTarget),
							collectionSimilarities.stream().map(BiobankCollectionSimilarity::getSource))
					.distinct().collect(Collectors.toList());

			double[][] similarities = new double[uniqueCollections.size()][uniqueCollections.size()];
			int[][] ontologyTermGroupSizes = new int[uniqueCollections.size()][uniqueCollections.size()];

			for (BiobankCollectionSimilarity biobankCollectionSimilarity : collectionSimilarities)
			{
				BiobankSampleCollection target = biobankCollectionSimilarity.getTarget();
				BiobankSampleCollection source = biobankCollectionSimilarity.getSource();

				int rowIndex = uniqueCollections.indexOf(target);
				int colIndex = uniqueCollections.indexOf(source);

				similarities[rowIndex][colIndex] = biobankCollectionSimilarity.getSimilarity();
				ontologyTermGroupSizes[rowIndex][colIndex] = biobankCollectionSimilarity.getCoverage();
			}

			for (int rowIndex = 0; rowIndex < similarities.length; rowIndex++)
			{
				BiobankSampleCollection target = uniqueCollections.get(rowIndex);
				result.put(target.getName(), new ArrayList<>());
				for (int colIndex = 0; colIndex < similarities[rowIndex].length; colIndex++)
				{
					BiobankSampleCollection source = uniqueCollections.get(colIndex);
					String fakeIdentifier = target.getName() + source.getName();
					double similarity = similarities[rowIndex][colIndex];
					int size = ontologyTermGroupSizes[rowIndex][colIndex];
					result.get(target.getName()).add(BiobankCollectionSimilarity.create(fakeIdentifier, target, source,
							similarity, size, biobankUniverse));
				}
			}
		}

		model.addAttribute("collectionSimilarityMap", result);

		return test(model);
	}

	@RequestMapping(method = POST, value = "/test/network", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public VisNetworkResponse createNetwork(@RequestBody VisNetworkRequest visNetworkRequest)
	{
		List<VisNode> nodes = new ArrayList<>();

		List<VisEdge> edges = new ArrayList<>();

		BiobankUniverse biobankUniverse = biobankUniverseService
				.getBiobankUniverse(visNetworkRequest.getBiobankUniverseIdentifier());

		if (Objects.nonNull(biobankUniverse))
		{
			List<BiobankCollectionSimilarity> collectionSimilarities = biobankUniverseService
					.getCollectionSimilarities(biobankUniverse);

			List<BiobankSampleCollection> uniqueBiobankCollections = Stream
					.concat(collectionSimilarities.stream().map(BiobankCollectionSimilarity::getTarget),
							collectionSimilarities.stream().map(BiobankCollectionSimilarity::getSource))
					.distinct().collect(Collectors.toList());

			for (BiobankSampleCollection biobankSampleCollection : uniqueBiobankCollections)
			{
				int size = biobankUniverseService.countBiobankSampleAttributes(biobankSampleCollection);
				nodes.add(VisNode.create(biobankSampleCollection.getName(), biobankSampleCollection.getName(), size,
						NODE_SHAPE));
			}

			Multimap<String, String> existingPairs = LinkedHashMultimap.create();
			for (BiobankCollectionSimilarity biobankCollectionSimilarity : collectionSimilarities)
			{
				String targetName = biobankCollectionSimilarity.getTarget().getName();
				String sourceName = biobankCollectionSimilarity.getSource().getName();

				if (!existingPairs.containsEntry(targetName, sourceName)
						&& !existingPairs.containsEntry(sourceName, targetName))
				{
					String identifier = biobankCollectionSimilarity.getIdentifier();
					double distance = Math.round(biobankCollectionSimilarity.getSimilarity() * 100.0) / 100.0;
					String label = Double.toString(distance);
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
		if (StringUtils.isNotBlank(universeName))
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

		return "redirect:" + getBiobankUniverseMenuUrl();
	}

	@RequestMapping(value = "/removeBiobankuniverse", method = RequestMethod.POST)
	public String deleteBiobankUniverse(@RequestParam(required = true) String biobankUniverseId, Model model)
	{
		biobankUniverseService.deleteBiobankUniverse(biobankUniverseId);
		model.addAttribute("biobankUniverses", getUserBiobankUniverses());
		model.addAttribute("biobankSampleCollections", getBiobankSampleCollecitons());
		model.addAttribute("semanticTypeGroups", getSemanticTypes());
		return "redirect:" + getBiobankUniverseMenuUrl();
	}

	@RequestMapping(value = "/addUniverseMembers", method = POST)
	public String addMembers(@RequestParam(required = true) String biobankUniverseId,
			@RequestParam(required = false) String[] biobankSampleCollectionNames, Model model)
	{
		BiobankUniverse biobankUniverse = biobankUniverseService.getBiobankUniverse(biobankUniverseId);

		if (biobankUniverse != null && biobankSampleCollectionNames != null)
		{
			List<BiobankSampleCollection> biobankSampleCollections = biobankUniverseService
					.getBiobankSampleCollections(Stream.of(biobankSampleCollectionNames).collect(Collectors.toList()));

			submit(biobankUniverse, biobankSampleCollections);
		}

		return "redirect:" + getBiobankUniverseMenuUrl();
	}

	@RequestMapping(value = "/addKeyConcepts", method = POST)
	public String addKeyConcepts(@RequestParam(required = true) String biobankUniverseId,
			@RequestParam(required = false) String[] semanticTypes, Model model)
	{
		BiobankUniverse universe = biobankUniverseService.getBiobankUniverse(biobankUniverseId);

		biobankUniverseService.addKeyConcepts(universe,
				semanticTypes != null ? of(semanticTypes).collect(toList()) : emptyList());

		model.addAttribute("biobankUniverses", getUserBiobankUniverses());
		model.addAttribute("biobankSampleCollections", getBiobankSampleCollecitons());
		model.addAttribute("semanticTypeGroups", getSemanticTypes());

		return "redirect:" + getBiobankUniverseMenuUrl();
	}

	private void submit(BiobankUniverse biobankUniverse, List<BiobankSampleCollection> biobankSampleCollections)
	{
		List<BiobankSampleCollection> newMembers = biobankSampleCollections.stream()
				.filter(member -> !biobankUniverse.getMembers().contains(member)).collect(toList());

		// Add new members to the universe
		biobankUniverseService.addBiobankUniverseMember(biobankUniverse, newMembers);

		BiobankUniverseJobExecution jobExecution = new BiobankUniverseJobExecution(dataService, biobankUniverseService);
		jobExecution.setUniverse(biobankUniverse);
		jobExecution.setMembers(newMembers);
		jobExecution.setUser(userAccountService.getCurrentUser());

		RunAsSystemProxy.runAsSystem(() -> {
			dataService.add(BiobankUniverseJobExecutionMetaData.ENTITY_NAME, jobExecution);
		});

		BiobankUniverseJobImpl biobankUniverseJobImpl = biobankUniverseJobFactory.create(jobExecution);
		taskExecutor.submit(biobankUniverseJobImpl);
	}

	private Set<String> getSemanticTypes()
	{
		return newLinkedHashSet(
				ontologyService.getAllSemanticTypes().stream().map(SemanticType::getName).collect(toList()));
	}

	private List<BiobankUniverse> getUserBiobankUniverses()
	{
		return biobankUniverseService.getBiobankUniverses().stream()
				.filter(universe -> SecurityUtils.currentUserIsSu()
						|| universe.getOwner().getUsername().equals(SecurityUtils.getCurrentUsername()))
				.collect(toList());
	}

	private List<BiobankSampleCollection> getBiobankSampleCollecitons()
	{
		return biobankUniverseService.getAllBiobankSampleCollections();
	}

	private String getBiobankUniverseMenuUrl()
	{
		return menuReaderService.getMenu().findMenuItemPath(ID);
	}
}
