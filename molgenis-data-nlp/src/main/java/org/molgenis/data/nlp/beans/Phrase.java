package org.molgenis.data.nlp.beans;

import com.google.auto.value.AutoValue;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.gson.AutoGson;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@AutoValue
@AutoGson(autoValueClass = AutoValue_Phrase.class)
public abstract class Phrase
{
	public abstract String getCorePhrase();

	public abstract List<String> getModifiers();

	public abstract List<String> getPrepositionModifiers();

	public static Phrase create(String coreNoun, List<String> modifiers)
	{
		return new AutoValue_Phrase(coreNoun, modifiers, emptyList());
	}

	public static Phrase create(String coreNoun, List<String> modifiers, List<String> prepositionModifiers)
	{
		return new AutoValue_Phrase(coreNoun, modifiers, prepositionModifiers);
	}

	public List<String> getAllPhrases()
	{
		List<String> nounPhrases = new ArrayList<>();

		List<String> modifiers = getModifiers();

		if (modifiers.isEmpty())
		{
			nounPhrases.add(getCorePhrase());
		}
		else
		{
			String[] array = modifiers.stream().filter(StringUtils::isNotBlank).toArray(String[]::new);
			StringBuilder stringBuilder = new StringBuilder();
			StringBuilder accumulativeStringBuilder = new StringBuilder();
			stringBuilder.append(getCorePhrase());
			accumulativeStringBuilder.append(getCorePhrase());
			for (int i = array.length - 1; i >= 0; i--)
			{
				if (accumulativeStringBuilder.length() != 0) accumulativeStringBuilder.append(' ');
				accumulativeStringBuilder.append(array[i]);
				nounPhrases.add(accumulativeStringBuilder.toString());
				nounPhrases.add(stringBuilder.append(' ').append(array[i]).toString());
			}
		}

		return nounPhrases.stream().distinct().collect(toList());
	}
}
