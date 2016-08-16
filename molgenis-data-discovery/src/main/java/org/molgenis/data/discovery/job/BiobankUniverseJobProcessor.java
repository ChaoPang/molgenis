package org.molgenis.data.discovery.job;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.molgenis.data.discovery.controller.BiobankUniverseController;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.discovery.service.impl.OntologyBasedMatcher;
import org.molgenis.data.jobs.Progress;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParam;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.core.runas.RunAsSystemProxy;
import org.molgenis.ui.menu.MenuReaderService;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class BiobankUniverseJobProcessor
{
	private static final int PROGRESS_UPDATE_BATCH_SIZE = 50;

	private final BiobankUniverse biobankUniverse;
	private final List<BiobankSampleCollection> newMembers;
	private final BiobankUniverseService biobankUniverseService;
	private final BiobankUniverseRepository biobankUniverseRepository;
	private final QueryExpansionService queryExpansionService;
	private final OntologyService ontologyService;
	private final AtomicInteger counter;

	private final Progress progress;
	private final MenuReaderService menuReaderService;

	public BiobankUniverseJobProcessor(BiobankUniverse biobankUniverse,
			List<BiobankSampleCollection> biobankSampleCollections, BiobankUniverseService biobankUniverseService,
			BiobankUniverseRepository biobankUniverseRepository, QueryExpansionService queryExpansionService,
			OntologyService ontologyService, Progress progress, MenuReaderService menuReaderService)
	{
		this.biobankUniverse = requireNonNull(biobankUniverse);
		this.newMembers = requireNonNull(biobankSampleCollections);
		this.biobankUniverseService = requireNonNull(biobankUniverseService);
		this.biobankUniverseRepository = requireNonNull(biobankUniverseRepository);
		this.queryExpansionService = requireNonNull(queryExpansionService);
		this.ontologyService = requireNonNull(ontologyService);
		this.progress = requireNonNull(progress);
		this.counter = new AtomicInteger(0);
		this.menuReaderService = requireNonNull(menuReaderService);
	}

	public void process()
	{
		RunAsSystemProxy.runAsSystem(() -> {

			List<BiobankSampleCollection> existingMembers = Lists.newArrayList(biobankUniverse.getMembers());
			existingMembers.removeAll(newMembers);

			int totalNumberOfAttributes = newMembers.stream()
					.map(member -> biobankUniverseRepository.getBiobankSampleAttributeIdentifiers(member).size())
					.mapToInt(Integer::intValue).sum();

			// The process includes tagging and matching, therefore the total number is multiplied by 2
			progress.setProgressMax(totalNumberOfAttributes * 2);

			// Tag all the biobankSampleAttributes in the new members
			for (BiobankSampleCollection biobankSampleCollection : newMembers)
			{
				if (!biobankUniverseService.isBiobankSampleCollectionTagged(biobankSampleCollection))
				{
					List<BiobankSampleAttribute> biobankSampleAttributesToUpdate = new ArrayList<>();

					biobankUniverseRepository.getBiobankSampleAttributes(biobankSampleCollection)
							.forEach(biobankSampleAttribute -> {

						List<IdentifiableTagGroup> identifiableTagGroups = biobankUniverseService
								.findTagGroupsForAttributes(biobankSampleAttribute);

						biobankSampleAttributesToUpdate
								.add(BiobankSampleAttribute.create(biobankSampleAttribute, identifiableTagGroups));

						// Update the progress only when the progress proceeds the threshold
						if (counter.incrementAndGet() % PROGRESS_UPDATE_BATCH_SIZE == 0)
						{
							progress.progress(counter.get(), "Processed " + counter);
						}
					});

					biobankUniverseRepository.addTagGroupsForAttributes(biobankSampleAttributesToUpdate);
				}
				else
				{
					counter.set(counter.get() + (int) biobankUniverseRepository
							.getBiobankSampleAttributeIdentifiers(biobankSampleCollection).size());
				}
			}

			// Generate matches for all the biobankSampleAttributes in the new members
			for (BiobankSampleCollection biobankSampleCollection : newMembers)
			{
				if (existingMembers.size() > 0)
				{
					List<AttributeMappingCandidate> allCandidates = new ArrayList<>();

					List<OntologyBasedMatcher> matchers = existingMembers.stream()
							.map(collection -> new OntologyBasedMatcher(collection, biobankUniverseRepository,
									queryExpansionService, ontologyService))
							.collect(toList());

					biobankUniverseRepository.getBiobankSampleAttributes(biobankSampleCollection)
							.forEach(biobankSampleAttribute -> {

						List<SemanticType> keyConceptFilter = biobankUniverse.getKeyConcepts();

						Stream<IdentifiableTagGroup> filter = biobankSampleAttribute.getTagGroups().stream().filter(
								tagGroup -> !tagGroup.getSemanticTypes().stream().anyMatch(keyConceptFilter::contains));

						List<TagGroup> tagGroups = filter.map(tag -> TagGroup.create(
								OntologyTerm.and(tag.getOntologyTerms().stream().toArray(OntologyTerm[]::new)),
								tag.getMatchedWords(), (float) tag.getScore())).collect(Collectors.toList());

						// SemanticSearch finding all the relevant attributes from existing entities
						SemanticSearchParam semanticSearchParam = SemanticSearchParam.create(
								Sets.newHashSet(biobankSampleAttribute.getLabel()), newArrayList(tagGroups),
								QueryExpansionParam.create(true, true));

						allCandidates.addAll(biobankUniverseService.findCandidateMappingsOntologyBased(biobankUniverse,
								biobankSampleAttribute, semanticSearchParam, matchers));

						// Update the progress only when the progress proceeds the threshold
						if (counter.incrementAndGet() % PROGRESS_UPDATE_BATCH_SIZE == 0)
						{
							progress.progress(counter.get(), "Processed " + counter);
						}
					});

					biobankUniverseRepository.addAttributeMappingCandidates(allCandidates);
				}

				existingMembers.add(biobankSampleCollection);
			}

			progress.progress(totalNumberOfAttributes * 2, "Processed " + totalNumberOfAttributes * 2);

			progress.setResultUrl(menuReaderService.getMenu().findMenuItemPath(BiobankUniverseController.ID)
					+ "/universe/" + biobankUniverse.getIdentifier());
		});
	}
}