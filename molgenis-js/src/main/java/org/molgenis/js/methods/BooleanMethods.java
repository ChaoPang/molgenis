package org.molgenis.js.methods;

import static org.molgenis.js.ScriptableValue.MISSING_VALUE;

import java.util.HashSet;
import java.util.Set;

import org.molgenis.js.ScriptHelper;
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
		sets.add((ScriptHelper.isNull(thisObj) ? null : Boolean.parseBoolean(Context.toString(thisObj))));
		// as long as one of the arguments and object is true the whole block is
		// true
		for (Object arg : args)
		{
			sets.add((ScriptHelper.isNull(arg) ? null : Boolean.parseBoolean(Context.toString(arg))));
		}
		if (sets.contains(true))
		{
			return new ScriptableValue(thisObj, true);
		}
		return new ScriptableValue(thisObj, sets.contains(null) ? MISSING_VALUE : false);
	}

	public static ScriptableValue and(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		Boolean result = (ScriptHelper.isNull(thisObj) ? null : Boolean.parseBoolean(Context.toString(thisObj)));

		if (result != null)
		{
			for (Object arg : args)
			{
				Boolean truthValue = ScriptHelper.isNull(arg) ? null : Boolean.parseBoolean(Context.toString(arg));
				if (truthValue == null)
				{
					return new ScriptableValue(thisObj, MISSING_VALUE);
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
