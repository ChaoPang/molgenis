package org.molgenis.data.discovery.service;

import java.util.List;

import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.service.impl.BiobankUniverseScore;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;

public interface OntologyBasedExplainService
{
	public abstract List<AttributeMappingCandidate> explain(BiobankUniverse biobankUniverse,
			SemanticSearchParam semanticSearchParam, BiobankSampleAttribute targetAttribute,
			List<BiobankSampleAttribute> sourceAttributes, BiobankUniverseScore similarity);
}
