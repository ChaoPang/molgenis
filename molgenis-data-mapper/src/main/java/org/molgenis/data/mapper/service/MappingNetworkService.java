package org.molgenis.data.mapper.service;

import java.util.List;

import org.molgenis.data.mapper.data.request.AttributeConnectionResponse;
import org.molgenis.data.mapper.mapping.model.MappingProject;

public interface MappingNetworkService
{
	AttributeConnectionResponse createConnections(List<MappingProject> mappingProjects);
}
