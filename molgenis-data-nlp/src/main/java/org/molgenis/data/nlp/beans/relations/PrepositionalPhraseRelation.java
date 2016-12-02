package org.molgenis.data.nlp.beans.relations;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.molgenis.data.nlp.beans.core.PhraseObject;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class PrepositionalPhraseRelation
{
	private final Multimap<PhraseObject, PhraseObject> nounModifierMap;

	public PrepositionalPhraseRelation(List<PhraseObject> nounModifiers, List<PhraseObject> nounPhraseNodes)
	{
		this.nounModifierMap = LinkedHashMultimap.create();
		for (PhraseObject nounPhraseNode : nounPhraseNodes)
		{
			for (PhraseObject modifier : nounModifiers)
			{
				nounModifierMap.put(nounPhraseNode, modifier);
			}
		}
	}

	public boolean contains(PhraseObject nounPhraseNode)
	{
		return nounModifierMap.containsKey(nounPhraseNode);
	}

	public List<PhraseObject> getNounModifiers(PhraseObject nounPhraseNode)
	{
		return Lists.newArrayList(nounModifierMap.get(nounPhraseNode));
	}

	public List<PhraseObject> getAllModifiers()
	{
		return nounModifierMap.values().stream().distinct().collect(toList());
	}

	public String toString()
	{
		return String.format("Noun modifiers: %s; NounPhrases: %s", nounModifierMap.values(), nounModifierMap.keySet());
	}
}
