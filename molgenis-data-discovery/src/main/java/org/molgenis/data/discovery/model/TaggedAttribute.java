package org.molgenis.data.discovery.model;

import java.util.List;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_TaggedAttribute.class)
public abstract class TaggedAttribute
{
	public abstract String getIdentifier();

	public abstract AttributeMetaData getAttribute();

	public abstract List<MappingExplanation> getMappedOntologyTerms();

	public static TaggedAttribute create(String identifier, AttributeMetaData attribute,
			List<MappingExplanation> mappedOntologyTerms)
	{
		return new AutoValue_TaggedAttribute(identifier, attribute, mappedOntologyTerms);
	}
}
