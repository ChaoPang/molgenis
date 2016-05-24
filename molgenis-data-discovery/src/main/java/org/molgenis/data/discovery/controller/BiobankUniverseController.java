package org.molgenis.data.discovery.controller;

import static java.util.stream.Stream.of;
import static org.molgenis.data.discovery.controller.BiobankUniverseController.URI;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.molgenis.data.DataService;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.discovery.job.BiobankUniverseJobExecution;
import org.molgenis.data.discovery.job.BiobankUniverseJobFactory;
import org.molgenis.data.discovery.job.BiobankUniverseJobImpl;
import org.molgenis.data.discovery.model.BiobankUniverse;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.i18n.LanguageService;
import org.molgenis.security.core.runas.RunAsSystemProxy;
import org.molgenis.security.user.UserAccountService;
import org.molgenis.ui.MolgenisPluginController;
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
	private final UserAccountService userAccountService;

	public static final String VIEW_NAME = "biobank-universe-view";
	public static final String ID = "biobankuniverse";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;

	@Autowired
	public BiobankUniverseController(BiobankUniverseJobFactory biobankUniverseJobFactory,
			BiobankUniverseService biobankUniverseService, ExecutorService taskExecutor,
			UserAccountService userAccountService, DataService dataService, LanguageService languageService)
	{
		super(URI);
		this.biobankUniverseService = requireNonNull(biobankUniverseService);
		this.biobankUniverseJobFactory = requireNonNull(biobankUniverseJobFactory);
		this.taskExecutor = requireNonNull(taskExecutor);
		this.dataService = requireNonNull(dataService);
		this.userAccountService = requireNonNull(userAccountService);
	}

	@RequestMapping(method = GET)
	public String init(Model model)
	{
		return VIEW_NAME;
	}

	@RequestMapping(value = "/addUniverse", method = RequestMethod.POST)
	public String addUniverse(@RequestParam("universe-name") String universeName,
			@RequestParam(required = false) String[] entityNames, Model model)
	{
		BiobankUniverse universe = biobankUniverseService.createBiobankUniverse(universeName,
				userAccountService.getCurrentUser());

		if (universe != null && entityNames != null)
		{
			List<EntityMetaData> collect = of(entityNames).map(entityName -> dataService.getEntityMetaData(entityName))
					.collect(Collectors.toList());

			submit(universe, collect);
		}

		model.addAttribute("biobankUniverses", biobankUniverseService.getBiobankUniverses());

		return VIEW_NAME;
	}

	@RequestMapping("/universe/{id}")
	public String getUniverse(@PathVariable("id") String identifier, Model model)
	{
		return VIEW_NAME;
	}

	@RequestMapping(method = POST)
	public String addMembers(@RequestParam(required = true) String biobankUniverseId,
			@RequestParam(required = false) String[] entityNames, Model model)
	{
		BiobankUniverse universe = biobankUniverseService.getBiobankUniverse(biobankUniverseId);

		if (universe != null && entityNames != null)
		{
			List<EntityMetaData> collect = of(entityNames).map(entityName -> dataService.getEntityMetaData(entityName))
					.collect(Collectors.toList());

			submit(universe, collect);
		}

		return VIEW_NAME;
	}

	private void submit(BiobankUniverse biobankUniverse, List<EntityMetaData> entityMetaDatas)
	{
		BiobankUniverseJobExecution jobExecution = new BiobankUniverseJobExecution(dataService, biobankUniverseService);
		jobExecution.setUniverse(biobankUniverse);
		jobExecution.setMembers(entityMetaDatas);

		RunAsSystemProxy.runAsSystem(() -> {
			dataService.add(BiobankUniverseJobExecution.ENTITY_NAME, jobExecution);
		});

		BiobankUniverseJobImpl biobankUniverseJobImpl = biobankUniverseJobFactory.create(jobExecution);
		taskExecutor.submit(biobankUniverseJobImpl);
	}
}
