package org.molgenis.data.discovery.model.biobank;

import com.google.auto.value.AutoValue;
import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData.BiobankAttributeDataType;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.gson.AutoGson;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
@AutoGson(autoValueClass = AutoValue_BiobankSampleAttribute.class)
public abstract class BiobankSampleAttribute
{
	public abstract String getIdentifier();

	public abstract String getName();

	@Nullable
	public abstract String getLabel();

	@Nullable
	public abstract String getDescription();

	public abstract BiobankAttributeDataType getBiobankAttributeDataType();

	public abstract BiobankSampleCollection getCollection();

	public abstract List<IdentifiableTagGroup> getTagGroups();

	public static BiobankSampleAttribute create(String identifier, String name, String label, String description,
			BiobankAttributeDataType biobankAttributeDataType, BiobankSampleCollection collection,
			List<IdentifiableTagGroup> tagGroups)
	{
		return new AutoValue_BiobankSampleAttribute(identifier, name, label, description, biobankAttributeDataType,
				collection, tagGroups);
	}

	public static BiobankSampleAttribute create(BiobankSampleAttribute biobankSampleAttribute,
			List<IdentifiableTagGroup> tagGroups)
	{
		return new AutoValue_BiobankSampleAttribute(biobankSampleAttribute.getIdentifier(),
				biobankSampleAttribute.getName(), biobankSampleAttribute.getLabel(),
				biobankSampleAttribute.getDescription(), biobankSampleAttribute.getBiobankAttributeDataType(),
				biobankSampleAttribute.getCollection(), tagGroups);
	}
}
