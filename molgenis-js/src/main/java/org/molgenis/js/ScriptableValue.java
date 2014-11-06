package org.molgenis.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * {@link ScriptableObject} that holds the value of an interpreted js function.
 * 
 * TODO Null values
 */
public class ScriptableValue extends ScriptableObject
{
	public static final String MISSING_VALUE = "999999";
	private static final String NUMBER_PATTERN = "\\d+\\.{0,1}\\d*";
	private static final long serialVersionUID = 277471335110754837L;
	private static final String CLASS_NAME = "Value";
	private Object value;

	public ScriptableValue()
	{
	}

	public ScriptableValue(Scriptable scope, Object value)
	{
		super(scope, ScriptableObject.getClassPrototype(scope, CLASS_NAME));

		if (value == null)
		{
			value = MISSING_VALUE;
		}
		this.value = value;
	}

	@Override
	public String getClassName()
	{
		return CLASS_NAME;
	}

	public Object getValue()
	{
		return value;
	}

	@Override
	public Object getDefaultValue(Class<?> typeHint)
	{
		// Handle boolean variables
		if (value.toString().equals("true") || value.toString().equals("false")) return Context.toBoolean(value);

		// Handle numeric variables
		if (value.toString().matches(NUMBER_PATTERN)) return Context.toNumber(value);

		return value;
	}

	@Override
	public String toString()
	{
		return value.toString();
	}

}
