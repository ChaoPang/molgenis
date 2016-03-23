package org.molgenis.data.mapper.repository.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.mapper.mapping.model.AttributeMapping;
import org.molgenis.data.mapper.meta.AttributeMappingMetaData;
import org.molgenis.data.mapper.meta.EntityMappingMetaData;
import org.molgenis.data.mapper.repository.AttributeMappingRepository;
import org.molgenis.data.support.MapEntity;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class AttributeMappingRepositoryImpl implements AttributeMappingRepository
{
	public static final EntityMetaData META_DATA = new AttributeMappingMetaData();

	@Autowired
	private IdGenerator idGenerator;

	private final DataService dataService;

	public AttributeMappingRepositoryImpl(DataService dataService)
	{
		this.dataService = dataService;
	}

	@Override
	public List<Entity> upsert(Collection<AttributeMapping> attributeMappings)
	{
		List<Entity> result = new ArrayList<Entity>();
		List<Entity> mappingEntitiesToAdd = new ArrayList<>();
		List<Entity> mappingEntitiesToUpdate = new ArrayList<>();

		for (AttributeMapping attributeMapping : attributeMappings)
		{
			Entity entity;
			if (StringUtils.isBlank(attributeMapping.getIdentifier()))
			{
				attributeMapping.setIdentifier(idGenerator.generateId());
				entity = toAttributeMappingEntity(attributeMapping);
				mappingEntitiesToAdd.add(entity);
			}
			else
			{
				entity = toAttributeMappingEntity(attributeMapping);
				mappingEntitiesToUpdate.add(entity);
			}
			result.add(entity);
		}

		if (mappingEntitiesToAdd.size() > 0)
		{
			dataService.add(AttributeMappingRepositoryImpl.META_DATA.getName(), mappingEntitiesToAdd.stream());
		}

		if (mappingEntitiesToUpdate.size() > 0)
		{
			dataService.update(AttributeMappingRepositoryImpl.META_DATA.getName(), mappingEntitiesToUpdate.stream());
		}

		return result;
	}

	// private Entity upsert(AttributeMapping attributeMapping)
	// {
	// Entity result;
	// if (attributeMapping.getIdentifier() == null)
	// {
	// attributeMapping.setIdentifier(idGenerator.generateId());
	// result = toAttributeMappingEntity(attributeMapping);
	// dataService.add(AttributeMappingRepositoryImpl.META_DATA.getName(), result);
	// }
	// else
	// {
	// result = toAttributeMappingEntity(attributeMapping);
	// dataService.update(AttributeMappingRepositoryImpl.META_DATA.getName(), result);
	// }
	// return result;
	// }

	@Override
	public List<AttributeMapping> getAttributeMappings(List<Entity> attributeMappingEntities,
			EntityMetaData sourceEntityMetaData, EntityMetaData targetEntityMetaData)
	{
		return Lists.transform(attributeMappingEntities, new Function<Entity, AttributeMapping>()
		{
			@Override
			public AttributeMapping apply(Entity attributeMappingEntity)
			{
				return toAttributeMapping(attributeMappingEntity, sourceEntityMetaData, targetEntityMetaData);
			}
		});

	}

	@Override
	public List<AttributeMetaData> retrieveAttributeMetaDatasFromAlgorithm(String algorithm,
			EntityMetaData sourceEntityMetaData)
	{
		List<AttributeMetaData> sourceAttributeMetaDatas = new ArrayList<AttributeMetaData>();

		Pattern pattern = Pattern.compile("\\$\\('([^']+)'\\)");
		Matcher matcher = pattern.matcher(algorithm);

		while (matcher.find())
		{
			AttributeMetaData attribute = sourceEntityMetaData.getAttribute(matcher.group(1));
			if (!sourceAttributeMetaDatas.contains(attribute))
			{
				sourceAttributeMetaDatas.add(attribute);
			}
		}

		return sourceAttributeMetaDatas;
	}

	private AttributeMapping toAttributeMapping(Entity attributeMappingEntity, EntityMetaData sourceEntityMetaData,
			EntityMetaData targetEntityMetaData)
	{
		String identifier = attributeMappingEntity.getString(EntityMappingMetaData.IDENTIFIER);
		String targetAtributeName = attributeMappingEntity.getString(AttributeMappingMetaData.TARGETATTRIBUTEMETADATA);
		AttributeMetaData targetAttributeMetaData = targetEntityMetaData.getAttribute(targetAtributeName);
		String algorithm = attributeMappingEntity.getString(AttributeMappingMetaData.ALGORITHM);
		String algorithmState = attributeMappingEntity.getString(AttributeMappingMetaData.ALGORITHMSTATE);
		Double similarity = attributeMappingEntity.getDouble(AttributeMappingMetaData.SIMILARITY_SCORE);
		List<AttributeMetaData> sourceAttributeMetaDatas = retrieveAttributeMetaDatasFromAlgorithm(algorithm,
				sourceEntityMetaData);

		return new AttributeMapping(identifier, targetAttributeMetaData, algorithm, sourceAttributeMetaDatas,
				algorithmState, similarity == null ? 0 : similarity);
	}

	private Entity toAttributeMappingEntity(AttributeMapping attributeMapping)
	{
		Entity attributeMappingEntity = new MapEntity(META_DATA);
		attributeMappingEntity.set(AttributeMappingMetaData.IDENTIFIER, attributeMapping.getIdentifier());
		attributeMappingEntity.set(AttributeMappingMetaData.TARGETATTRIBUTEMETADATA,
				attributeMapping.getTargetAttributeMetaData() != null
						? attributeMapping.getTargetAttributeMetaData().getName() : null);
		attributeMappingEntity.set(AttributeMappingMetaData.ALGORITHM, attributeMapping.getAlgorithm());
		attributeMappingEntity.set(AttributeMappingMetaData.SOURCEATTRIBUTEMETADATAS, attributeMapping
				.getSourceAttributeMetaDatas().stream().map(AttributeMetaData::getName).collect(Collectors.toList()));
		attributeMappingEntity.set(AttributeMappingMetaData.ALGORITHMSTATE, attributeMapping.getAlgorithmState());
		attributeMappingEntity.set(AttributeMappingMetaData.SIMILARITY_SCORE, attributeMapping.getSimilarity());
		return attributeMappingEntity;
	}

	@Override
	public void delete(List<AttributeMapping> attributeMappings)
	{
		Stream<Entity> stream = StreamSupport.stream(attributeMappings.spliterator(), false)
				.map(this::toAttributeMappingEntity);
		dataService.delete(META_DATA.getName(), stream);
	}
}
