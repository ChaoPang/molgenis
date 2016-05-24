package org.molgenis.data.discovery.repo;

import java.util.List;

import org.molgenis.data.discovery.model.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.BiobankSampleCollection;
import org.molgenis.data.discovery.model.BiobankUniverse;

public interface BiobankUniverseRepository
{
	public abstract List<BiobankUniverse> getAllUniverses();

	public abstract BiobankUniverse getUniverse(String identifier);

	public abstract void addUniverse(BiobankUniverse biobankUniverse);

	public abstract void addUniverseMembers(BiobankUniverse biobankUniverse, List<BiobankSampleCollection> members);

	public abstract List<BiobankSampleCollection> getBiobankSampleCollections(List<String> collectionIdentifiers);

	public abstract List<BiobankSampleAttribute> getAttributesFromCollection(BiobankSampleCollection collection);

	public abstract void tagAttributesInBiobankSampleCollection(BiobankSampleCollection collection);

	public abstract void addAttributeMatchCandidates(List<AttributeMappingCandidate> candidates);

	public abstract List<AttributeMappingCandidate> getAttributeMatchCandidates(
			List<BiobankSampleAttribute> biobankSampleAttributes);
}
