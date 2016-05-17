package org.molgenis.data.discovery.repo;

import java.util.List;

import org.molgenis.data.EntityMetaData;
import org.molgenis.data.discovery.model.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.BiobankUniverse;
import org.molgenis.data.discovery.model.TaggedAttribute;
import org.molgenis.ontology.core.model.OntologyTerm;

public interface BiobankUniverseRepository
{
	public abstract List<BiobankUniverse> getAllUniverses();

	public abstract BiobankUniverse getUniverse(String identifier);

	public abstract void addBiobankUniverse(BiobankUniverse biobankUniverse);

	public abstract void addUniverseMembers(BiobankUniverse biobankUniverse, List<EntityMetaData> entityMetaDatas);

	public abstract List<TaggedAttribute> getTaggedAttributes(EntityMetaData entityMetaData);

	public abstract void addTaggedAttributes(EntityMetaData entityMetaData, List<TaggedAttribute> taggedAttributes);

	public abstract void addAttributeMappingCandidates(List<AttributeMappingCandidate> candidates);

	public abstract List<TaggedAttribute> findTaggedAttributes(List<OntologyTerm> ontologyTerms);
}
