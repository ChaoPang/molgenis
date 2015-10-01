package org.molgenis.data.spss;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisDataAccessException;
import org.molgenis.data.MolgenisDataException;
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

	public SpssIterator(SPSSFile spssFile, EntityMetaData entityMetaData)
	{
		this.spssFile = Objects.requireNonNull(spssFile);
		this.entityMetaData = Objects.requireNonNull(entityMetaData);
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
					FieldType dataType = attributeMetaData.getDataType();
					entity.set(attributeMetaData.getName(), dataType.convert(value.trim()));
				}
			}
			catch (SPSSFileException e)
			{
				throw new MolgenisDataAccessException(e.getMessage());
			}
		}
		descrmentCounter.decrementAndGet();
		return entity;
	}
}
