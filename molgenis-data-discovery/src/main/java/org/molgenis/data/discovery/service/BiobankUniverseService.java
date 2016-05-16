package org.molgenis.data.discovery.service;

import java.util.List;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.discovery.model.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.BiobankUniverse;

public interface BiobankUniverseService
{
	public abstract List<AttributeMappingCandidate> findAttributeMappingCandidates(String queryTerm);

	public abstract BiobankUniverse createBiobankUniverse(String universeName, MolgenisUser owner);

	public abstract List<BiobankUniverse> getAllUniverses();

	public abstract BiobankUniverse getUniverse(String identifier);
}
