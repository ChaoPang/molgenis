package org.molgenis.data.spss;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisDataAccessException;
import org.molgenis.data.RepositoryCapability;
import org.molgenis.data.mem.InMemoryRepository;
import org.molgenis.data.spss.bean.SpssCategoryEntityMetaData;
import org.molgenis.data.support.AbstractRepository;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.fieldtypes.FieldType;
import org.opendatafoundation.data.spss.SPSSFile;
import org.opendatafoundation.data.spss.SPSSVariable;
import org.opendatafoundation.data.spss.SPSSVariableCategory;

import com.google.common.collect.ImmutableMap;

public class SpssRepository extends AbstractRepository
{
	private final String entityName;
	private final SPSSFile spssFile;
	private DefaultEntityMetaData entityMetaData = null;
	private final Map<String, InMemoryRepository> referenceRepositories;
	private final String DEFAULT_IDENTIFIER = "id";

	public SpssRepository(File file, String entityName)
	{
		try
		{
			this.entityName = Objects.requireNonNull(entityName);
			this.spssFile = new SPSSFile(Objects.requireNonNull(file));
			this.referenceRepositories = new LinkedHashMap<>();
			initialize();
		}
		catch (FileNotFoundException e)
		{
			throw new MolgenisDataAccessException(e.getMessage());
		}
	}

	@Override
	public EntityMetaData getEntityMetaData()
	{
		return entityMetaData;
	}

	private void initialize()
	{
		try
		{
			spssFile.loadMetadata();

			entityMetaData = new DefaultEntityMetaData(entityName);

			for (int i = 0; i < spssFile.getVariableCount(); i++)
			{
				entityMetaData.addAttributeMetaData(createAttributeMetaData(spssFile.getVariable(i)));
			}

			if (!entityMetaData.getAttributes().stream().anyMatch(this::isAttributeIdAttribute))
			{
				entityMetaData.addAttributeMetaData(new DefaultAttributeMetaData(DEFAULT_IDENTIFIER)
						.setIdAttribute(true).setAuto(true).setNillable(false));
			}
		}
		catch (Exception e)
		{
			throw new MolgenisDataAccessException("Could not load the metadata for spss file : " + spssFile.toString());
		}
	}

	private DefaultAttributeMetaData createAttributeMetaData(SPSSVariable spssVariable)
	{
		DefaultAttributeMetaData attributeMetaData = new DefaultAttributeMetaData(spssVariable.getName());
		attributeMetaData.setLabel(spssVariable.getLabel());

		FieldType fileType;
		switch (spssVariable.getDDI3DataType())
		{
			case "Decimal":
			case "Double":
				fileType = MolgenisFieldTypes.DECIMAL;
				break;

			case "BigInteger":
				fileType = spssVariable.categoryMap.size() > 0 ? MolgenisFieldTypes.CATEGORICAL : MolgenisFieldTypes.INT;
				break;

			case "Time":
			case "DateTime":
				fileType = MolgenisFieldTypes.DATETIME;
				break;

			case "Date":
				fileType = MolgenisFieldTypes.DATE;
				break;

			default:
				fileType = MolgenisFieldTypes.STRING;
				break;
		}
		attributeMetaData.setDataType(fileType);

		if (fileType.getEnumType().equals(MolgenisFieldTypes.CATEGORICAL.getEnumType()))
		{
			InMemoryRepository referenceRepo = createReferenceRepository(spssVariable);
			referenceRepositories.put(referenceRepo.getName(), referenceRepo);
			attributeMetaData.setRefEntity(referenceRepo.getEntityMetaData());
		}

		if (attributeMetaData.getName().equalsIgnoreCase(DEFAULT_IDENTIFIER))
		{
			attributeMetaData.setIdAttribute(true).setAuto(true).setNillable(false);
		}

		return attributeMetaData;
	}

	@Override
	public Iterator<Entity> iterator()
	{
		try
		{
			spssFile.loadData();

			if (spssFile.isMetadataLoaded && spssFile.isDataLoaded)
			{
				return new SpssIterator(spssFile, entityMetaData);
			}
		}
		catch (Exception e)
		{
			throw new MolgenisDataAccessException("Could not load the data for spss file : " + spssFile.toString());
		}

		return Collections.emptyIterator();
	}

	@Override
	public long count()
	{
		return (long) spssFile.getRecordCount();
	}

	@Override
	public void close() throws IOException
	{
		spssFile.close();
	}

	@Override
	public Set<RepositoryCapability> getCapabilities()
	{
		return Collections.emptySet();
	}

	public Map<String, InMemoryRepository> getReferenceRepositories()
	{
		return referenceRepositories;
	}

	private InMemoryRepository createReferenceRepository(SPSSVariable spssVariable)
	{
		InMemoryRepository inMemoryRepository = new InMemoryRepository(new SpssCategoryEntityMetaData(
				createReferenceEntityName(spssVariable)));

		for (Entry<String, SPSSVariableCategory> entrySet : spssVariable.categoryMap.entrySet())
		{
			SPSSVariableCategory spssVariableCategory = entrySet.getValue();
			inMemoryRepository.add(new MapEntity(ImmutableMap.of(SpssCategoryEntityMetaData.CODE,
					(int) spssVariableCategory.value, SpssCategoryEntityMetaData.Label, spssVariableCategory.label)));
		}

		return inMemoryRepository;
	}

	private boolean isAttributeIdAttribute(AttributeMetaData attributeMetaData)
	{
		return attributeMetaData.getName().equalsIgnoreCase(DEFAULT_IDENTIFIER);
	}

	private String createReferenceEntityName(SPSSVariable spssVariable)
	{
		return spssVariable.getName() + "_ref";
	}
}