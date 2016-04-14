package org.molgenis.data.mapper.controller;

import static java.util.stream.Collectors.toList;
import static org.molgenis.data.mapper.controller.AttributeConnectionController.URI;

import java.util.List;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.mapper.data.request.AttributeConnectionResponse;
import org.molgenis.data.mapper.mapping.model.MappingProject;
import org.molgenis.data.mapper.service.MappingNetworkService;
import org.molgenis.data.mapper.service.MappingService;
import org.molgenis.security.user.UserAccountService;
import org.molgenis.ui.MolgenisPluginController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import static java.util.Objects.requireNonNull;

@Controller
@RequestMapping(URI)
public class AttributeConnectionController extends MolgenisPluginController
{
	private final MappingNetworkService mappingNetworkService;
	private final MappingService mappingService;
	private final UserAccountService userAccountService;

	public static final String ID = "attributeconnection";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;

	@Autowired
	public AttributeConnectionController(MappingNetworkService mappingNetworkService, MappingService mappingService,
			UserAccountService userAccountService)
	{
		super(URI);
		this.mappingNetworkService = requireNonNull(mappingNetworkService);
		this.mappingService = requireNonNull(mappingService);
		this.userAccountService = requireNonNull(userAccountService);
	}

	@RequestMapping(value = "/getMappings", method = RequestMethod.GET)
	@ResponseBody
	public AttributeConnectionResponse getMapping()
	{
		MolgenisUser currentUser = userAccountService.getCurrentUser();
		List<MappingProject> mappingProjects = mappingService.getAllMappingProjects().stream()
				.filter(p -> p.getOwner().equals(currentUser) || currentUser.isSuperuser()).collect(toList());
		return mappingNetworkService.createConnections(mappingProjects);
	}
}
