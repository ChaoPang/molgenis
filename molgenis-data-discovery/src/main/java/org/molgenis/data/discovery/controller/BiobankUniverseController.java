package org.molgenis.data.discovery.controller;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.molgenis.data.discovery.controller.BiobankUniverseController.URI;
import static org.molgenis.data.discovery.meta.BiobankSampleAttributeMetaData.DESCRIPTION;
import static org.molgenis.data.discovery.meta.BiobankSampleAttributeMetaData.LABEL;
import static org.molgenis.data.discovery.meta.BiobankSampleAttributeMetaData.NAME;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.servlet.http.Part;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.csv.CsvRepository;
import org.molgenis.data.discovery.job.BiobankUniverseJobExecution;
import org.molgenis.data.discovery.job.BiobankUniverseJobFactory;
import org.molgenis.data.discovery.job.BiobankUniverseJobImpl;
import org.molgenis.data.discovery.meta.BiobankSampleAttributeMetaData;
import org.molgenis.data.discovery.model.BiobankSampleCollection;
import org.molgenis.data.discovery.model.BiobankUniverse;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.i18n.LanguageService;
import org.molgenis.data.support.MapEntity;
import org.molgenis.file.FileStore;
import org.molgenis.security.core.runas.RunAsSystemProxy;
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
	private final IdGenerator idGenerator;
	private final FileStore fileStore;
	private final DataService dataService;
	private final BiobankUniverseJobFactory biobankUniverseJobFactory;
	private final ExecutorService taskExecutor;
	private final BiobankUniverseService biobankUniverseService;
	private final UserAccountService userAccountService;
	private final MenuReaderService menuReaderService;

	public static final String VIEW_NAME = "biobank-universe-view";
	public static final String ID = "biobankuniverse";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;

	private static final List<String> SAMPLE_ATTRIBUTE_HEADERS = Arrays.asList(NAME, LABEL, DESCRIPTION);

	@Autowired
	public BiobankUniverseController(BiobankUniverseJobFactory biobankUniverseJobFactory,
			BiobankUniverseService biobankUniverseService, ExecutorService taskExecutor,
			UserAccountService userAccountService, DataService dataService, FileStore fileStore,
			IdGenerator idGenerator, MenuReaderService menuReaderService, LanguageService languageService)
	{
		super(URI);
		this.biobankUniverseService = requireNonNull(biobankUniverseService);
		this.biobankUniverseJobFactory = requireNonNull(biobankUniverseJobFactory);
		this.taskExecutor = requireNonNull(taskExecutor);
		this.dataService = requireNonNull(dataService);
		this.fileStore = requireNonNull(fileStore);
		this.idGenerator = requireNonNull(idGenerator);
		this.userAccountService = requireNonNull(userAccountService);
		this.menuReaderService = requireNonNull(menuReaderService);
	}

	@RequestMapping(method = GET)
	public String init(Model model)
	{
		model.addAttribute("biobankSampleCollections", biobankUniverseService.getAllBiobankSampleCollections());
		return VIEW_NAME;
	}

	@RequestMapping(value = "/importSample", method = RequestMethod.POST, headers = "Content-Type=multipart/form-data")
	public String importSample(@RequestParam("sampleName") String sampleName,
			@RequestParam(value = "file", required = true) Part file, @RequestParam("separator") Character separator,
			Model model) throws IOException
	{
		if (StringUtils.isNotEmpty(sampleName) && file != null)
		{
			if (biobankUniverseService.getBiobankSampleCollection(sampleName) == null)
			{
				importSample(sampleName, file.getInputStream(), separator);
				model.addAttribute("message", "success!");
			}
			else
			{
				model.addAttribute("message", "The sample name already exists!");
			}
		}

		model.addAttribute("biobankSampleCollections", biobankUniverseService.getAllBiobankSampleCollections());

		return "redirect:" + getBiobankUniverseMenuUrl();
	}

	private void importSample(String sampleName, InputStream inputStream, Character separator) throws IOException
	{
		File uploadFile = fileStore.store(inputStream, idGenerator.generateId() + ".csv");
		CsvRepository csvRepository = new CsvRepository(uploadFile, emptyList(), separator);

		List<String> attributeNames = StreamSupport
				.stream(csvRepository.getEntityMetaData().getAtomicAttributes().spliterator(), false)
				.map(AttributeMetaData::getName).map(StringUtils::lowerCase).collect(toList());

		if (attributeNames.containsAll(SAMPLE_ATTRIBUTE_HEADERS))
		{
			Stream<Entity> biobankSampleAttributeEntityStream = csvRepository.stream()
					.map(entity -> uploadEntityToBiobankSampleAttributeEntity(sampleName, entity));

			biobankUniverseService.importSampleCollections(sampleName, biobankSampleAttributeEntityStream);
		}

		csvRepository.close();
	}

	private Entity uploadEntityToBiobankSampleAttributeEntity(String sampleName, Entity entity)
	{
		String identifier = idGenerator.generateId();
		String name = entity.getString(BiobankSampleAttributeMetaData.NAME);
		String label = entity.getString(BiobankSampleAttributeMetaData.LABEL);
		String description = entity.getString(BiobankSampleAttributeMetaData.DESCRIPTION);

		MapEntity mapEntity = new MapEntity(BiobankSampleAttributeMetaData.INSTANCE);
		mapEntity.set(BiobankSampleAttributeMetaData.IDENTIFIER, identifier);
		mapEntity.set(BiobankSampleAttributeMetaData.NAME, name);
		mapEntity.set(BiobankSampleAttributeMetaData.LABEL, label);
		mapEntity.set(BiobankSampleAttributeMetaData.DESCRIPTION, description);
		mapEntity.set(BiobankSampleAttributeMetaData.COLLECTION, sampleName);
		mapEntity.set(BiobankSampleAttributeMetaData.TAG_GROUPS, Collections.emptyList());

		return mapEntity;
	}

	@RequestMapping(value = "/addUniverse", method = RequestMethod.POST)
	public String addUniverse(@RequestParam("universe-name") String universeName,
			@RequestParam(required = false) String[] entityNames, Model model)
	{
		if (StringUtils.isNotBlank(universeName) && entityNames != null)
		{
			BiobankUniverse biobankUniverse = biobankUniverseService.addBiobankUniverse(universeName,
					userAccountService.getCurrentUser());

			submit(biobankUniverse, biobankUniverse.getMembers());
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
			List<BiobankSampleCollection> biobankSampleCollections = biobankUniverseService
					.getBiobankSampleCollections(Stream.of(entityNames).collect(Collectors.toList()));

			submit(universe, biobankSampleCollections);
		}

		return VIEW_NAME;
	}

	private void submit(BiobankUniverse biobankUniverse, List<BiobankSampleCollection> biobankSampleCollections)
	{
		BiobankUniverseJobExecution jobExecution = new BiobankUniverseJobExecution(dataService, biobankUniverseService);
		jobExecution.setUniverse(biobankUniverse);
		jobExecution.setMembers(biobankSampleCollections);

		RunAsSystemProxy.runAsSystem(() -> {
			dataService.add(BiobankUniverseJobExecution.ENTITY_NAME, jobExecution);
		});

		BiobankUniverseJobImpl biobankUniverseJobImpl = biobankUniverseJobFactory.create(jobExecution);
		taskExecutor.submit(biobankUniverseJobImpl);
	}

	private String getBiobankUniverseMenuUrl()
	{
		return menuReaderService.getMenu().findMenuItemPath(ID);
	}
}
