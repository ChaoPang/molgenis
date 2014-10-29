package org.molgenis.js.methods;

import java.util.HashSet;
import java.util.Set;

import org.molgenis.js.ScriptableValue;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * Arithmetic functions
 */
public class BooleanMethods
{
	public static ScriptableValue or(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		Set<Boolean> sets = new HashSet<Boolean>();
		// TODO : need a check on whether thisObj is convertible to boolean
		sets.add((thisObj == null ? null : Boolean.parseBoolean(Context.toString(thisObj))));
		// as long as one of the arguments and object is true the whole block is
		// true
		for (Object arg : args)
		{
			sets.add((arg == null ? null : Boolean.parseBoolean(Context.toString(arg))));
		}
		if (sets.contains(true))
		{
			return new ScriptableValue(thisObj, true);
		}
		return new ScriptableValue(thisObj, sets.contains(null) ? null : false);
	}

	public static ScriptableValue and(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		// TODO : need a check on whether thisObj is convertible to boolean
		Boolean result = (thisObj == null ? null : Boolean.parseBoolean(Context.toString(thisObj)));

		if (result != null)
		{
			for (Object arg : args)
			{
				Boolean truthValue = arg == null ? null : Boolean.parseBoolean(Context.toString(arg));
				if (truthValue == null)
				{
					result = null;
					break;
				}
				else
				{
					result = result && truthValue;
				}
			}
		}
		return new ScriptableValue(thisObj, result);
	}
}
