package org.molgenis.data.spss;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.MolgenisFieldTypes;
import org.molgenis.MolgenisFieldTypes.FieldTypeEnum;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisDataAccessException;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.mem.InMemoryRepository;
import org.molgenis.data.spss.bean.SpssCategoryEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.fieldtypes.FieldType;
import org.opendatafoundation.data.FileFormatInfo;
import org.opendatafoundation.data.FileFormatInfo.Format;
import org.opendatafoundation.data.spss.SPSSFile;
import org.opendatafoundation.data.spss.SPSSFileException;
import org.opendatafoundation.data.spss.SPSSVariable;

public class SpssIterator implements Iterator<Entity>
{
	private final SPSSFile spssFile;
	private final AtomicInteger descrmentCounter;
	private final AtomicInteger recordIndex;
	private final EntityMetaData entityMetaData;
	private final Map<String, InMemoryRepository> referenceRepositories;

	public SpssIterator(SPSSFile spssFile, EntityMetaData entityMetaData,
			Map<String, InMemoryRepository> referenceRepositories)
	{
		this.spssFile = Objects.requireNonNull(spssFile);
		this.entityMetaData = Objects.requireNonNull(entityMetaData);
		this.referenceRepositories = Objects.requireNonNull(referenceRepositories);
		this.descrmentCounter = new AtomicInteger(spssFile.getRecordCount());
		this.recordIndex = new AtomicInteger(1);
	}

	@Override
	public boolean hasNext()
	{
		return descrmentCounter.get() > 0;
	}

	@Override
	public Entity next()
	{
		MapEntity entity = new MapEntity();
		for (int i = 0; i < spssFile.getVariableCount(); i++)
		{
			SPSSVariable spssVariable = spssFile.getVariable(i);
			try
			{
				AttributeMetaData attributeMetaData = entityMetaData.getAttribute(spssVariable.getName());

				if (attributeMetaData == null) throw new MolgenisDataException("Could not find the attribute ("
						+ spssVariable.getName() + ") in the EntityMetaData (" + entityMetaData.getName() + ")");

				String value = spssVariable.getValueAsString(recordIndex.get(), new FileFormatInfo(Format.ASCII));
				if (StringUtils.isNotBlank(value))
				{
					value = value.trim();

					FieldType dataType = attributeMetaData.getDataType();

					if (isFieldTypeMref(dataType))
					{
						throw new UnsupportedOperationException("Do not support mref values in SPSS files!");
					}
					else if (isFieldTypeXref(dataType))
					{
						String refEntityName = attributeMetaData.getRefEntity().getName();
						if (referenceRepositories.containsKey(refEntityName))
						{
							InMemoryRepository inMemoryRepository = referenceRepositories.get(refEntityName);
							Entity refEntity = findRefEntityFromInMemoryRepository(SpssCategoryEntityMetaData.CODE,
									value, inMemoryRepository);
							entity.set(attributeMetaData.getName(), refEntity);
						}
						else
						{
							throw new MolgenisDataAccessException("Could not find the reference entity("
									+ refEntityName + ")");
						}
					}
					else
					{
						entity.set(attributeMetaData.getName(), dataType.convert(value));
					}
				}
			}
			catch (SPSSFileException e)
			{
				throw new MolgenisDataAccessException(e.getMessage());
			}
		}
		recordIndex.incrementAndGet();
		descrmentCounter.decrementAndGet();
		return entity;
	}

	private Entity findRefEntityFromInMemoryRepository(String attributeName, String value,
			InMemoryRepository inMemoryRepository)
	{
		Iterator<Entity> iterator = inMemoryRepository.iterator();
		AttributeMetaData attribute = inMemoryRepository.getEntityMetaData().getAttribute(attributeName);

		if (attribute == null) throw new MolgenisDataAccessException("Could not find the attribute(" + attributeName
				+ ") in the entity(" + inMemoryRepository.getEntityMetaData().getName() + ")");

		while (iterator.hasNext())
		{
			Entity entity = iterator.next();

			String valueForAttribute = entity.getString(attributeName);

			if (StringUtils.isNotBlank(valueForAttribute) && StringUtils.isNotBlank(value))
			{
				if (StringUtils.equalsIgnoreCase(valueForAttribute.trim(), value.trim()))
				{
					return entity;
				}
			}

		}

		return null;
	}

	private boolean isFieldTypeMref(FieldType dataType)
	{
		FieldTypeEnum enumType = dataType.getEnumType();
		return enumType.equals(MolgenisFieldTypes.CATEGORICAL_MREF.getEnumType())
				|| enumType.equals(MolgenisFieldTypes.MREF.getEnumType());
	}

	private boolean isFieldTypeXref(FieldType dataType)
	{
		FieldTypeEnum enumType = dataType.getEnumType();
		return enumType.equals(MolgenisFieldTypes.CATEGORICAL.getEnumType())
				|| enumType.equals(MolgenisFieldTypes.XREF.getEnumType());
	}
}
