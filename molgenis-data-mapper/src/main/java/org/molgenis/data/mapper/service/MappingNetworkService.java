package org.molgenis.data.mapper.service;

import org.molgenis.data.EntityMetaData;

public interface MappingNetworkService
{
	public void computeDistance(EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData);
}
