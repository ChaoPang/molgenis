package org.molgenis.data.discovery.job;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
import org.molgenis.security.core.runas.RunAsSystemProxy;
import org.molgenis.ui.menu.MenuReaderService;

import com.google.common.collect.Sets;

public class BiobankUniverseJobProcessor
{
	private static final int PROGRESS_UPDATE_BATCH_SIZE = 50;

	private final BiobankUniverse biobankUniverse;
	private final List<BiobankSampleCollection> newMembers;
	private final BiobankUniverseService biobankUniverseService;
	private final BiobankUniverseRepository biobankUniverseRepository;
	private final QueryExpansionService queryExpansionService;
	private final AtomicInteger counter;
	private final Map<BiobankSampleCollection, OntologyBasedMatcher> ontologyBasedMatcherRegistry;

	private final Progress progress;
	private final MenuReaderService menuReaderService;

	public BiobankUniverseJobProcessor(BiobankUniverse biobankUniverse,
			List<BiobankSampleCollection> biobankSampleCollections, BiobankUniverseService biobankUniverseService,
			BiobankUniverseRepository biobankUniverseRepository, QueryExpansionService queryExpansionService,
			Progress progress, MenuReaderService menuReaderService)
	{
		this.biobankUniverse = requireNonNull(biobankUniverse);
		this.newMembers = requireNonNull(biobankSampleCollections);
		this.biobankUniverseService = requireNonNull(biobankUniverseService);
		this.biobankUniverseRepository = requireNonNull(biobankUniverseRepository);
		this.queryExpansionService = requireNonNull(queryExpansionService);
		this.progress = requireNonNull(progress);
		this.counter = new AtomicInteger(0);
		this.menuReaderService = requireNonNull(menuReaderService);
		this.ontologyBasedMatcherRegistry = new HashMap<>();
	}

	public void process()
	{
		RunAsSystemProxy.runAsSystem(() -> {

			List<BiobankSampleCollection> existingMembers = biobankUniverse.getMembers().stream()
					.filter(member -> !newMembers.contains(member)).collect(Collectors.toList());

			int totalNumberOfAttributes = newMembers.stream()
					.map(member -> biobankUniverseRepository.getBiobankSampleAttributeIdentifiers(member).size())
					.mapToInt(Integer::intValue).sum();

			// The process includes tagging and matching, therefore the total number is multiplied by 2
			progress.setProgressMax(totalNumberOfAttributes * 2);

			// Tag all the biobankSampleAttributes in the new members
			tagBiobankSampleAttributes();

			// Generate matches for all the biobankSampleAttributes in the new members
			generateMatches(existingMembers);

			// Update the vector representations of biobankSampleCollections
			biobankUniverseService.updateBiobankUniverseMemberVectors(biobankUniverse);

			progress.progress(totalNumberOfAttributes * 2, "Processed " + totalNumberOfAttributes * 2);

			progress.setResultUrl(menuReaderService.getMenu().findMenuItemPath(BiobankUniverseController.ID)
					+ "/universe/" + biobankUniverse.getIdentifier());
		});
	}

	private void generateMatches(List<BiobankSampleCollection> existingMembers)
	{
		for (BiobankSampleCollection target : newMembers)
		{
			if (!existingMembers.isEmpty())
			{
				List<AttributeMappingCandidate> allCandidates = new ArrayList<>();

				List<OntologyBasedMatcher> ontologyBasedMatchers = existingMembers.stream()
						.map(this::getOntologyBasedMatcher).collect(toList());

				for (BiobankSampleAttribute biobankSampleAttribute : biobankUniverseRepository
						.getBiobankSampleAttributes(target))
				{
					List<SemanticType> keyConceptFilter = biobankUniverse.getKeyConcepts();

					Set<TagGroup> tagGroups = new HashSet<>();
					for (IdentifiableTagGroup tagGroup : biobankSampleAttribute.getTagGroups())
					{
						List<OntologyTerm> ontologyTerms = tagGroup.getOntologyTerms().stream().filter(
								ot -> ot.getSemanticTypes().stream().allMatch(st -> !keyConceptFilter.contains(st)))
								.collect(Collectors.toList());
						if (!ontologyTerms.isEmpty())
						{
							tagGroups.add(
									TagGroup.create(ontologyTerms, tagGroup.getMatchedWords(), tagGroup.getScore()));
						}
					}

					// SemanticSearch finding all the relevant attributes from existing entities
					SemanticSearchParam semanticSearchParam = SemanticSearchParam.create(
							Sets.newHashSet(biobankSampleAttribute.getLabel()), newArrayList(tagGroups),
							QueryExpansionParam.create(true, false), true);

					allCandidates.addAll(biobankUniverseService.generateAttributeCandidateMappings(biobankUniverse,
							biobankSampleAttribute, semanticSearchParam, ontologyBasedMatchers));

					// Update the progress only when the progress proceeds the threshold
					if (counter.incrementAndGet() % PROGRESS_UPDATE_BATCH_SIZE == 0)
					{
						progress.progress(counter.get(), "Processed " + counter);
					}
				}

				biobankUniverseRepository.addAttributeMappingCandidates(allCandidates);
			}

			existingMembers.add(target);
		}
	}

	private void tagBiobankSampleAttributes()
	{
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
	}

	private OntologyBasedMatcher getOntologyBasedMatcher(BiobankSampleCollection biobankSampleCollection)
	{
		if (!ontologyBasedMatcherRegistry.containsKey(biobankSampleCollection))
		{
			ontologyBasedMatcherRegistry.put(biobankSampleCollection, new OntologyBasedMatcher(biobankSampleCollection,
					biobankUniverseRepository, queryExpansionService));
		}
		return ontologyBasedMatcherRegistry.get(biobankSampleCollection);
	}
}