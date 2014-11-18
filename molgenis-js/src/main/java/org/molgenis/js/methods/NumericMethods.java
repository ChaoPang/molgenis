package org.molgenis.js.methods;

import static org.molgenis.js.ScriptableValue.MISSING_VALUE;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.molgenis.js.ScriptHelper;
import org.molgenis.js.ScriptableValue;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * Arithmetic functions
 */
public class NumericMethods
{
	/**
	 * $('test').thenReturn(1,2)
	 * 
	 * @param ctx
	 * @param thisObj
	 * @param args
	 * @param funObj
	 * @return
	 */
	public static Scriptable thenReturn(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		if (args.length != 2)
		{
			throw new IllegalArgumentException("div expects one argument. Example: $('weight').div(10)");
		}

		if (ScriptHelper.isNull(thisObj)) return new ScriptableValue(thisObj, MISSING_VALUE);

		return new ScriptableValue(thisObj, Boolean.parseBoolean(Context.toString(thisObj)) ? args[0] : args[1]);
	}

	public static Scriptable val(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		if (ScriptHelper.isNull(args)) return new ScriptableValue(thisObj, MISSING_VALUE);
		return new ScriptableValue(thisObj, args[0]);
	}

	public static Scriptable asNull(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		return new ScriptableValue(thisObj, MISSING_VALUE);
	}

	public static Scriptable notNull(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		return new ScriptableValue(thisObj, !ScriptHelper.isNull(thisObj));
	}

	public static Scriptable isNull(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		return new ScriptableValue(thisObj, ScriptHelper.isNull(thisObj));
	}

	public static Scriptable whenNull(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		if (args.length != 1)
		{
			throw new IllegalArgumentException("whenNull expects one argument. Example: $('weight').whenNull(10)");
		}
		return new ScriptableValue(thisObj, ScriptHelper.isNull(thisObj) ? args[0] : thisObj);
	}

	/**
	 * $('test').not(5)
	 * 
	 * @param ctx
	 * @param thisObj
	 * @param args
	 * @param funObj
	 * @return
	 */
	public static Scriptable not(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		if (args.length != 1)
		{
			throw new IllegalArgumentException("not expects one argument. Example: $('weight').not(10)");
		}

		if (ScriptHelper.isNull(thisObj) || ScriptHelper.isNull(args[0])) return new ScriptableValue(thisObj,
				MISSING_VALUE);

		String objectValue = ScriptHelper.isValid(thisObj);
		String object2Value = ScriptHelper.isValid(args[0]);

		return new ScriptableValue(thisObj, !objectValue.equals(object2Value));
	}

	/**
	 * $('test').eq(5)
	 * 
	 * @param ctx
	 * @param thisObj
	 * @param args
	 * @param funObj
	 * @return
	 */
	public static Scriptable eq(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		if (args.length != 1)
		{
			throw new IllegalArgumentException("div expects one argument. Example: $('weight').div(10)");
		}

		if (ScriptHelper.isNull(thisObj) || ScriptHelper.isNull(args[0])) return new ScriptableValue(thisObj,
				MISSING_VALUE);

		String objectValue = ScriptHelper.isValid(thisObj);
		String object2Value = ScriptHelper.isValid(args[0]);

		return new ScriptableValue(thisObj, objectValue.equals(object2Value));
	}

	/**
	 * $('test').gt(5)
	 * 
	 * @param ctx
	 * @param thisObj
	 * @param args
	 * @param funObj
	 * @return
	 */
	public static Scriptable gt(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		if (args.length != 1)
		{
			throw new IllegalArgumentException("div expects one argument. Example: $('weight').div(10)");
		}

		if (ScriptHelper.isNull(thisObj) || ScriptHelper.isNull(args[0])) return new ScriptableValue(thisObj,
				MISSING_VALUE);

		BigDecimal lhs = new BigDecimal(Context.toNumber(thisObj));
		BigDecimal rhs = new BigDecimal(Context.toNumber(args[0]));
		return new ScriptableValue(thisObj, lhs.doubleValue() > rhs.doubleValue());
	}

	/**
	 * $('test').lt(5)
	 * 
	 * @param ctx
	 * @param thisObj
	 * @param args
	 * @param funObj
	 * @return
	 */
	public static Scriptable lt(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		if (args.length != 1)
		{
			throw new IllegalArgumentException("div expects one argument. Example: $('weight').div(10)");
		}

		if (ScriptHelper.isNull(thisObj) || ScriptHelper.isNull(args[0])) return new ScriptableValue(thisObj,
				MISSING_VALUE);

		BigDecimal lhs = new BigDecimal(Context.toNumber(thisObj));
		BigDecimal rhs = new BigDecimal(Context.toNumber(args[0]));
		return new ScriptableValue(thisObj, lhs.doubleValue() < rhs.doubleValue());
	}

	/**
	 * $('test').div(5)
	 * 
	 * @param ctx
	 * @param thisObj
	 * @param args
	 * @param funObj
	 * @return
	 */
	public static Scriptable div(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		if (args.length != 1)
		{
			throw new IllegalArgumentException("div expects one argument. Example: $('weight').div(10)");
		}

		if (ScriptHelper.isNull(thisObj) || ScriptHelper.isNull(args[0])) return new ScriptableValue(thisObj,
				MISSING_VALUE);

		BigDecimal lhs = new BigDecimal(Context.toNumber(thisObj));
		BigDecimal rhs = new BigDecimal(Context.toNumber(args[0]));
		BigDecimal result = lhs.divide(rhs, MathContext.DECIMAL128);
		DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.ENGLISH));
		return new ScriptableValue(thisObj, df.format(result.doubleValue()));
	}

	/**
	 * $('test').pow(2)
	 * 
	 * @param ctx
	 * @param thisObj
	 * @param args
	 * @param funObj
	 * @return
	 */
	public static Scriptable pow(Context ctx, Scriptable thisObj, Object[] args, Function funObj)
	{
		if (args.length != 1)
		{
			throw new IllegalArgumentException("pow expects one argument. Example: $('weight').pow(10)");
		}

		if (ScriptHelper.isNull(thisObj) || ScriptHelper.isNull(args[0])) return new ScriptableValue(thisObj,
				MISSING_VALUE);

		BigDecimal lhs = new BigDecimal(Context.toNumber(thisObj));
		int rhs = (int) Context.toNumber(args[0]);
		BigDecimal result = lhs.pow(rhs, MathContext.DECIMAL128);

		return new ScriptableValue(thisObj, result.doubleValue());
	}
}
