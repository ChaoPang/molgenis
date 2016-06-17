package org.molgenis.data.discovery.controller;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.data.discovery.controller.BiobankUniverseController.URI;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.DataService;
import org.molgenis.data.discovery.job.BiobankUniverseJobExecution;
import org.molgenis.data.discovery.job.BiobankUniverseJobExecutionMetaData;
import org.molgenis.data.discovery.job.BiobankUniverseJobFactory;
import org.molgenis.data.discovery.job.BiobankUniverseJobImpl;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.i18n.LanguageService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParam;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
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

import com.google.common.collect.Sets;

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

	public static final String VIEW_BIOBANK_UNIVERSES = "view-biobank-universes";
	public static final String VIEW_SINGLE_BIOBANK_UNIVERSE = "view-single-biobank-universe";
	public static final String ID = "biobankuniverse";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;

	@Autowired
	public BiobankUniverseController(TagGroupGenerator tagGroupGenerator,
			BiobankUniverseJobFactory biobankUniverseJobFactory, BiobankUniverseService biobankUniverseService,
			OntologyService ontologyService, ExecutorService taskExecutor, UserAccountService userAccountService,
			DataService dataService, MenuReaderService menuReaderService, LanguageService languageService)
	{
		super(URI);
		this.tagGroupGenerator = requireNonNull(tagGroupGenerator);
		this.biobankUniverseService = requireNonNull(biobankUniverseService);
		this.ontologyService = requireNonNull(ontologyService);
		this.biobankUniverseJobFactory = requireNonNull(biobankUniverseJobFactory);
		this.taskExecutor = requireNonNull(taskExecutor);
		this.dataService = requireNonNull(dataService);
		this.userAccountService = requireNonNull(userAccountService);
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

	@RequestMapping("/universe/match")
	@ResponseBody
	public List<String> match(@RequestBody Map<String, String> request)
	{
		String collection = request.get("collection");
		String queryString = request.get("queryString");

		if (isNotBlank(collection) && isNotBlank(queryString))
		{
			List<TagGroup> generateTagGroups = tagGroupGenerator.generateTagGroups(queryString,
					ontologyService.getAllOntologiesIds());

			BiobankSampleCollection biobankSampleCollection = biobankUniverseService
					.getBiobankSampleCollection(collection);

			List<SemanticType> keyConcepts = Arrays.asList(
					SemanticType.create("1", "Gene or Genome", "test group", true),
					SemanticType.create("2", "Genetic Function", "test group", true));

			BiobankUniverse biobankUniverse = BiobankUniverse.create("1", "tes", Arrays.asList(biobankSampleCollection),
					userAccountService.getCurrentUser(), keyConcepts);

			List<IdentifiableTagGroup> collect = generateTagGroups.stream()
					.map(tagGroup -> IdentifiableTagGroup.create("1",
							ontologyService.getAtomicOntologyTerms(tagGroup.getOntologyTerm()), emptyList(),
							tagGroup.getMatchedWords(), tagGroup.getScore()))
					.collect(Collectors.toList());

			BiobankSampleAttribute target = BiobankSampleAttribute.create("1", "name", "queryString", StringUtils.EMPTY,
					biobankSampleCollection, collect);

			SemanticSearchParam semanticSearchParam = SemanticSearchParam.create(Sets.newHashSet(queryString),
					generateTagGroups, QueryExpansionParam.create(true, true));

			List<AttributeMappingCandidate> findCandidateMappings = biobankUniverseService.findCandidateMappings(
					biobankUniverse, target, semanticSearchParam, Arrays.asList(biobankSampleCollection));

			return findCandidateMappings.stream().map(AttributeMappingCandidate::getSource)
					.map(BiobankSampleAttribute::getLabel).collect(Collectors.toList());
		}
		return Collections.emptyList();
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
