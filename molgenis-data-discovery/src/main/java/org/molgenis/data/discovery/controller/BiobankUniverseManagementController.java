package org.molgenis.data.discovery.controller;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.Entity;
import org.molgenis.data.csv.CsvRepository;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.i18n.LanguageService;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.meta.model.AttributeFactory;
import org.molgenis.data.meta.model.EntityTypeFactory;
import org.molgenis.data.populate.IdGenerator;
import org.molgenis.data.support.DynamicEntity;
import org.molgenis.file.FileStore;
import org.molgenis.ui.MolgenisPluginController;
import org.molgenis.ui.menu.MenuReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.molgenis.data.discovery.controller.BiobankUniverseManagementController.URI;
import static org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
@RequestMapping(URI)
public class BiobankUniverseManagementController extends MolgenisPluginController
{
	private final IdGenerator idGenerator;
	private final FileStore fileStore;
	private final BiobankUniverseService biobankUniverseService;
	private final MenuReaderService menuReaderService;
	private final BiobankSampleAttributeMetaData biobankSampleAttributeMetaData;
	private final EntityTypeFactory entityTypeFactory;
	private final AttributeFactory attributeFactory;

	public static final String VIEW_NAME = "view-biobank-universe-management";
	public static final String ID = "biobankuniversemanagement";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;

	private static final List<String> SAMPLE_ATTRIBUTE_HEADERS = Arrays.asList(NAME, LABEL, DESCRIPTION, DATA_TYPE);

	@Autowired
	public BiobankUniverseManagementController(BiobankUniverseService biobankUniverseService, FileStore fileStore,
			IdGenerator idGenerator, MenuReaderService menuReaderService, LanguageService languageService,
			BiobankSampleAttributeMetaData biobankSampleAttributeMetaData, EntityTypeFactory entityTypeFactory,
			AttributeFactory attributeFactory)
	{
		super(URI);
		this.biobankUniverseService = requireNonNull(biobankUniverseService);
		this.fileStore = requireNonNull(fileStore);
		this.idGenerator = requireNonNull(idGenerator);
		this.menuReaderService = requireNonNull(menuReaderService);
		this.biobankSampleAttributeMetaData = requireNonNull(biobankSampleAttributeMetaData);
		this.entityTypeFactory = requireNonNull(entityTypeFactory);
		this.attributeFactory = requireNonNull(attributeFactory);
	}

	@RequestMapping(method = GET)
	public String init(Model model)
	{
		model.addAttribute("biobankSampleCollections", biobankUniverseService.getAllBiobankSampleCollections());
		return VIEW_NAME;
	}

	@RequestMapping(value = "/importSample", method = RequestMethod.POST, headers = "Content-Type=multipart/form-data")
	public String importSample(@RequestParam("sampleName") String sampleName,
			@RequestParam(value = "file", required = true) MultipartFile file,
			@RequestParam("separator") Character separator, Model model) throws IOException
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

	@RequestMapping(value = "/removeTagGroups", method = RequestMethod.POST)
	public String tagBiobankSampleAttributes(@RequestParam("sampleName") String sampleName, Model model)
	{
		BiobankSampleCollection biobankSampleCollection = biobankUniverseService.getBiobankSampleCollection(sampleName);

		if (biobankSampleCollection != null)
		{
			biobankUniverseService.removeAllTagGroups(biobankSampleCollection);
		}

		model.addAttribute("biobankSampleCollections", biobankUniverseService.getAllBiobankSampleCollections());

		return "redirect:" + getBiobankUniverseMenuUrl();
	}

	@RequestMapping(value = "/removeBiobankSampleCollection", method = RequestMethod.POST)
	public String removeBiobankSampleCollection(@RequestParam("sampleName") String sampleName, Model model)
	{
		BiobankSampleCollection biobankSampleCollection = biobankUniverseService.getBiobankSampleCollection(sampleName);

		if (biobankSampleCollection != null)
		{
			biobankUniverseService.removeBiobankSampleCollection(biobankSampleCollection);
		}

		model.addAttribute("biobankSampleCollections", biobankUniverseService.getAllBiobankSampleCollections());

		return "redirect:" + getBiobankUniverseMenuUrl();
	}

	private void importSample(String sampleName, InputStream inputStream, Character separator) throws IOException
	{
		File uploadFile = fileStore.store(inputStream, idGenerator.generateId() + ".csv");
		CsvRepository csvRepository = new CsvRepository(uploadFile, entityTypeFactory, attributeFactory, emptyList(),
				separator);

		List<String> attributeNames = stream(csvRepository.getEntityType().getAtomicAttributes().spliterator(), false)
				.map(Attribute::getName).collect(toList());

		if (attributeNames.containsAll(SAMPLE_ATTRIBUTE_HEADERS))
		{
			Stream<Entity> biobankSampleAttributeEntityStream = stream(csvRepository.spliterator(), false)
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
		String biobankAttributeDataType = entity.getString(BiobankSampleAttributeMetaData.DATA_TYPE);

		Entity mapEntity = new DynamicEntity(biobankSampleAttributeMetaData);
		mapEntity.set(BiobankSampleAttributeMetaData.IDENTIFIER, identifier);
		mapEntity.set(BiobankSampleAttributeMetaData.NAME, name);
		mapEntity.set(BiobankSampleAttributeMetaData.LABEL, label);
		mapEntity.set(BiobankSampleAttributeMetaData.DESCRIPTION, description);
		mapEntity.set(BiobankSampleAttributeMetaData.DATA_TYPE, biobankAttributeDataType);

		return mapEntity;
	}

	private String getBiobankUniverseMenuUrl()
	{
		return menuReaderService.getMenu().findMenuItemPath(ID);
	}
}
