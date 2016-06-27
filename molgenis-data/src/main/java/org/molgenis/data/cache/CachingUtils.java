package org.molgenis.data.cache;

import com.google.common.cache.Cache;
import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityManager;
import org.molgenis.data.meta.model.EntityMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.molgenis.MolgenisFieldTypes.FieldTypeEnum.*;

public class CachingUtils
{
	private static final Logger LOG = LoggerFactory.getLogger(CachingUtils.class);

	/**
	 * Rebuild entity from the values representing this entity.
	 *
	 * @param dehydratedEntity
	 * @return hydrated entity
	 */
	public static Entity hydrate(Map<String, Object> dehydratedEntity, EntityMetaData entityMetaData,
			EntityManager entityManager)
	{
		Entity hydratedEntity = entityManager.create(entityMetaData);
		entityMetaData.getAtomicAttributes().forEach(attribute -> {

			String name = attribute.getName();
			Object value = dehydratedEntity.get(name);
			if (value != null)
			{
				MolgenisFieldTypes.FieldTypeEnum type = attribute.getDataType().getEnumType();

				if (type.equals(MREF) || type.equals(CATEGORICAL_MREF))
				{
					// We can do this cast because during dehydration, mrefs and categorical mrefs are stored as a List of Object
					Iterable<Entity> referenceEntities = entityManager
							.getReferences(entityMetaData, (List<Object>) value);
					hydratedEntity.set(name, referenceEntities);
				}
				else if (type.equals(XREF) || type.equals(CATEGORICAL) || type.equals(FILE))
				{
					Entity referenceEntity = entityManager.getReference(entityMetaData, value);
					hydratedEntity.set(name, referenceEntity);
				}
				else
				{
					hydratedEntity.set(name, value);
				}
			}

		});

		return hydratedEntity;
	}

	/**
	 * Do not store entity in the cache since it might be updated by client code, instead store the values requires to
	 * rebuild this entity. For references to other entities only store the ids.
	 *
	 * @param entity
	 * @return
	 */
	public static Map<String, Object> dehydrate(Entity entity)
	{
		LOG.debug("Dehydrating entity {}", entity);
		Map<String, Object> dehydratedEntity = newHashMap();
		EntityMetaData entityMetaData = entity.getEntityMetaData();

		entityMetaData.getAtomicAttributes().forEach(attribute -> {
			String name = attribute.getName();
			MolgenisFieldTypes.FieldTypeEnum type = attribute.getDataType().getEnumType();

			dehydratedEntity.put(name, getValueBasedonType(entity, name, type));
		});

		return dehydratedEntity;
	}

	public static Cache<String, Map<String, Object>> createCache(int maximumSize)
	{
		return newBuilder().maximumSize(maximumSize).build();
	}

	public static String generateCacheKey(String entityName, Object id)
	{
		return entityName + "__" + id.toString();
	}

	private static Object getValueBasedonType(Entity entity, String name, MolgenisFieldTypes.FieldTypeEnum type)
	{
		LOG.debug("Dehydrating attribute '{}' of type [{}]", name, type.toString());

		Object value;
		switch (type)
		{
			case CATEGORICAL:
			case XREF:
			case FILE:
				Entity refEntity = entity.getEntity(name);
				value = refEntity != null ? refEntity.getIdValue() : null;
				break;
			case CATEGORICAL_MREF:
			case MREF:
				Iterator<Entity> refEntitiesIt = entity.getEntities(name).iterator();
				if (refEntitiesIt.hasNext())
				{
					List<Object> mrefValues = new ArrayList<>();
					do
					{
						Entity mrefEntity = refEntitiesIt.next();
						mrefValues.add(mrefEntity != null ? mrefEntity.getIdValue() : null);
					}
					while (refEntitiesIt.hasNext());
					value = mrefValues;
				}
				else
				{
					value = emptyList();
				}
				break;
			case DATE:
				// Store timestamp since data is mutable
				Date dateValue = entity.getDate(name);
				value = dateValue != null ? dateValue.getTime() : null;
				break;
			case DATE_TIME:
				// Store timestamp since data is mutable
				Timestamp dateTimeValue = entity.getTimestamp(name);
				value = dateTimeValue != null ? dateTimeValue.getTime() : null;
				break;
			case BOOL:
			case COMPOUND:
			case DECIMAL:
			case EMAIL:
			case ENUM:
			case HTML:
			case HYPERLINK:
			case INT:
			case LONG:
			case SCRIPT:
			case STRING:
			case TEXT:
				value = entity.get(name);
				break;
			default:
				throw new RuntimeException(format("Unknown attribute type [%s]", type));
		}

		LOG.debug("Dehydration resulted in value [{}]", value);
		return value;
	}
}
