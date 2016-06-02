package org.molgenis.data.discovery.controller;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.molgenis.data.discovery.controller.BiobankUniverseController.URI;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.List;
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
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.i18n.LanguageService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import static java.util.Objects.requireNonNull;

@Controller
@RequestMapping(URI)
public class BiobankUniverseController extends MolgenisPluginController
{
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
	public BiobankUniverseController(BiobankUniverseJobFactory biobankUniverseJobFactory,
			BiobankUniverseService biobankUniverseService, OntologyService ontologyService,
			ExecutorService taskExecutor, UserAccountService userAccountService, DataService dataService,
			MenuReaderService menuReaderService, LanguageService languageService)
	{
		super(URI);
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
		model.addAttribute("semanticTypeGroups", getSemanticTypeGroups());
		return VIEW_BIOBANK_UNIVERSES;
	}

	@RequestMapping("/universe/{id}")
	public String getUniverse(@PathVariable("id") String identifier, Model model)
	{
		// TODO: decide what to show in a universe
		return VIEW_SINGLE_BIOBANK_UNIVERSE;
	}

	@RequestMapping(value = "/addUniverse", method = RequestMethod.POST)
	public String addUniverse(@RequestParam("universeName") String universeName,
			@RequestParam(required = false) String[] biobankSampleCollectionNames,
			@RequestParam(required = false) String[] semanticTypeGroups, Model model)
	{
		if (StringUtils.isNotBlank(universeName))
		{
			BiobankUniverse biobankUniverse = biobankUniverseService.addBiobankUniverse(universeName,
					semanticTypeGroups != null ? of(semanticTypeGroups).collect(toList()) : emptyList(),
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
		model.addAttribute("semanticTypeGroups", getSemanticTypeGroups());

		return "redirect:" + getBiobankUniverseMenuUrl();
	}

	@RequestMapping(value = "/removeBiobankuniverse", method = RequestMethod.POST)
	public String deleteBiobankUniverse(@RequestParam(required = true) String biobankUniverseId, Model model)
	{
		biobankUniverseService.deleteBiobankUniverse(biobankUniverseId);
		model.addAttribute("biobankUniverses", getUserBiobankUniverses());
		model.addAttribute("biobankSampleCollections", getBiobankSampleCollecitons());
		model.addAttribute("semanticTypeGroups", getSemanticTypeGroups());
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
			@RequestParam(required = false) String[] semanticTypeGroups, Model model)
	{
		BiobankUniverse universe = biobankUniverseService.getBiobankUniverse(biobankUniverseId);

		biobankUniverseService.addKeyConcepts(universe,
				semanticTypeGroups != null ? of(semanticTypeGroups).collect(toList()) : emptyList());

		model.addAttribute("biobankUniverses", getUserBiobankUniverses());
		model.addAttribute("biobankSampleCollections", getBiobankSampleCollecitons());
		model.addAttribute("semanticTypeGroups", getSemanticTypeGroups());

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

	private Set<String> getSemanticTypeGroups()
	{
		return newLinkedHashSet(
				ontologyService.getAllSemanticTypes().stream().map(SemanticType::getGroup).collect(toList()));
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
