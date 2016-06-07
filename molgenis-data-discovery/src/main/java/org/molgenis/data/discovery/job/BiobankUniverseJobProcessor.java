package org.molgenis.data.discovery.job;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.molgenis.ontology.core.model.OntologyTerm.and;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.molgenis.data.discovery.controller.BiobankUniverseController;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.model.matching.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.matching.IdentifiableTagGroup;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.jobs.Progress;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParam;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;
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
	private final AtomicInteger counter;

	private final Progress progress;
	private final MenuReaderService menuReaderService;

	public BiobankUniverseJobProcessor(BiobankUniverse biobankUniverse,
			List<BiobankSampleCollection> biobankSampleCollections, BiobankUniverseService biobankUniverseService,
			BiobankUniverseRepository biobankUniverseRepository, Progress progress, MenuReaderService menuReaderService)
	{
		this.biobankUniverse = requireNonNull(biobankUniverse);
		this.newMembers = requireNonNull(biobankSampleCollections);
		this.biobankUniverseService = requireNonNull(biobankUniverseService);
		this.biobankUniverseRepository = requireNonNull(biobankUniverseRepository);
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
							.getBiobankSampleAttributes(biobankSampleCollection).size());
				}
			}

			// Generate matches for all the biobankSampleAttributes in the new members
			for (BiobankSampleCollection biobankSampleCollection : newMembers)
			{
				if (existingMembers.size() > 0)
				{
					List<AttributeMappingCandidate> allCandidates = new ArrayList<>();

					biobankUniverseRepository.getBiobankSampleAttributes(biobankSampleCollection)
							.forEach(biobankSampleAttribute -> {

						List<SemanticType> keyConceptFilter = biobankUniverse.getKeyConcepts();

						Stream<IdentifiableTagGroup> filter = biobankSampleAttribute.getTagGroups().stream().filter(
								tagGroup -> !tagGroup.getSemanticTypes().stream().anyMatch(keyConceptFilter::contains));

						List<TagGroup> tagGroups = filter.map(tag -> TagGroup.create(
								and(tag.getOntologyTerms().stream().toArray(OntologyTerm[]::new)),
								tag.getMatchedWords(), (float) tag.getScore())).collect(toList());

						// SemanticSearch finding all the relevant attributes from existing entities
						SemanticSearchParam semanticSearchParam = SemanticSearchParam.create(
								Sets.newHashSet(biobankSampleAttribute.getLabel()), tagGroups,
								QueryExpansionParam.create(true, true));

						allCandidates.addAll(biobankUniverseService.findCandidateMappings(biobankUniverse,
								biobankSampleAttribute, semanticSearchParam, existingMembers));

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