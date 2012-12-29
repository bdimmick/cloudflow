package com.hexagrammatic.cloudflow;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Type to parse and create workflow instances from various input types in JSON format.  The expected JSON data format 
 * may be in one of the following formats, with required keys in <i>italics</i>:
 * <p>
 * A JSON Array, consisting of only the steps in the workflow:
 * <pre>
 * [
 *  {
 *    '<i>class</i>':'some.class.name.that.extends.Step',
 *    'name':'step name',
 *    'timeout':'step timeout as a time tuple',
 *    'maxTries':&lt;max tries as a number&gt;
 *    'waitBetweenTries':'wait time as a time tuple', 
 *    'param1':'paramValue1',
 *    'param2':'paramValue2',
 *    'param3':'paramValue3',
 *    ...
 *    'paramN':'paramValueN',
 *  },
 *  ...
 * ]
 * </pre>
 * <p>
 * A JSON Object, consisting of metadata and configuration for the workflow and its steps:
 * <pre>
 * {
 *   'name':'workflow name',
 *   'timeout':'workflow timeout as a time tuple',
 *   'steps': [
 *              {
 *                '<i>class</i>':'some.class.name.that.extends.Step',
 *                'name':'step name',
 *                'timeout':'step timeout as a time tuple',
 *                'maxTries':&lt;max tries as a number&gt;
 *                'waitBetweenTries':'wait time as a time tuple', 
 *                'step_param1':'paramValue1',
 *                'step_param2':'paramValue2',
 *                'step_param3':'paramValue3',
 *                ...
 *                'step_paramN':'paramValueN',
 *              },
 *              ...
 *            ],
 *  'workflow_param1':'paramValue1',
 *  'workflow_param2':'paramValue2',
 *  'workflow_param3':'paramValue3',
 *  ...
 *  'workflow_paramN':'paramValueN',            
 * }
 * </pre>
 * <p>
 * In the case that you want to load classes for Steps from another classloader, feel free to use the <code>setClassLoader</code> method to set the specific
 * classloader that loads the Step classes.  This may be useful in some cases where the Step bytecode is defined outside the initial Java classpath and could
 * be dyanmically updated, such as hosting the classes in a version control system or a distributed filesystem, allowing them to be updated without restarting
 * the JVM.
 * <p>
 * In the case that dependencies, such as JPA <code>EntityManagerFactory</code>s or other utilities that enable steps to act on external resources, need 
 * to be injected into the steps or workflows created during parsing, implementors can extend this class to contain references to those dependencies, 
 * overriding the <code>pre(Step)</code>, <code>pre(Workflow)</code>, <code>post(Step)</code>, or <code>post(Workflow)</code> methods, and determine 
 * when a step or workflow instance needs to have a dependency set on it.
 *   
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12
 */
public class JsonParser {
	
	private ClassLoader classLoader = JsonParser.class.getClassLoader();
	
	/**
	 * Set the classloader to use when resolving Steps.  If unset,
	 * the same classloader that loaded <code>Parser</code> is used.
	 * @param cl the classloader - may not be null
	 */
	public void setClassLoader(final ClassLoader cl) {
		Validate.notNull(cl, "The provided classloader may not be null.");
		this.classLoader = cl;
	}
	
	/**
	 * Parses a workflow from a Reader. 
	 * @param reader the reader - may not be null
	 * @return the marshalled workflow
	 * @throws WorkflowCreationException thrown if creating the workflow fails
	 */	
	public Workflow parse(final Reader reader) throws WorkflowCreationException {
		Validate.notNull(reader, "The provided reader may not be null.");
		final com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
		return populateWorkflow(parser.parse(reader));
	}

	/**
	 * Parses a workflow from an InputStream. 
	 * @param stream the input stream - may not be null
	 * @return the marshalled workflow
	 * @throws WorkflowCreationException thrown if creating the workflow fails
	 */	
	public Workflow parse(final InputStream stream) throws WorkflowCreationException {
		Validate.notNull(stream, "The provided input stream may not be null.");
		final com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
		return populateWorkflow(parser.parse(new InputStreamReader(stream)));
	}
	
	/**
	 * Parses a workflow from a String. 
	 * @param data the input string - may not be null
	 * @return the marshalled workflow
	 * @throws WorkflowCreationException thrown if creating the workflow fails
	 */		
	public Workflow parse(final String data) throws WorkflowCreationException {
		Validate.notNull(data, "The provided data may not be null.");
		final com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
		return populateWorkflow(parser.parse(data));		
	}
	
	
	/**
	 * Performs any post-workflow creation actions, called after all parameters and steps have been added.
	 * This method does nothing in this implementation, but implementors may feel free to extend this class 
	 * and add their own hooks to modify workflows after the parser has finished creating them.
	 * @param workflow the workflow instance- never null.
	 */
	protected void post(final Workflow workflow) {};
	
	/**
	 * Performs any pre-workflow creation actions, called before any parameters and steps have been added.
	 * This method does nothing in this implementation, but implementors may feel free to extend this class 
	 * and add their own hooks to modify workflows before the parser has started populating them.
	 * @param workflow the workflow instance- never null.
	 */
	protected void pre(final Workflow workflow) {};
	
	/**
	 * Performs any post-step creation actions, called before any parameters have been added.
	 * This method does nothing in this implementation, but implementors may feel free to extend this class 
	 * and add their own hooks to modify steps after the parser has finished creating them.
	 * @param step the step instance - never null.
	 */
	protected void post(final Step step) {};
	
	/**
	 * Performs any pre-step creation actions, called before any parameters have been added.
	 * This method does nothing in this implementation, but implementors may feel free to extend this class 
	 * and add their own hooks to modify steps before the parser has started populating them.
	 * @param step the step instance - never null.
	 */
	protected void pre(final Step step) {};
	
	
	Workflow populateWorkflow(final JsonElement root) throws WorkflowCreationException {
		final Workflow workflow = new Workflow(); 
		pre(workflow);
		if (root.isJsonObject()) {
			final JsonObject obj = root.getAsJsonObject();

			final JsonElement name = obj.get("name");
			if (name!=null) {
				if (name.isJsonPrimitive()) {
					workflow.setName(name.getAsString());
					obj.remove("name");
				} else {
					throw new WorkflowCreationException(String.format("Workflow name value '%s' is not a JSON primitive.", name.toString()));
				}
			}
			
			final JsonElement timeout = obj.get("timeout");
			if (timeout!=null) {
				if (timeout.isJsonPrimitive()) {
					workflow.setTimeout(timeout.getAsString());
					obj.remove("timeout");
				} else {
					throw new WorkflowCreationException(String.format("Workflow timeout value '%s' is not a JSON primitive.", timeout.toString()));
				}
			}
			
			final JsonElement steps = obj.get("steps");
			if (steps!=null) {
				if (steps.isJsonArray()) {
					populateSteps(steps.getAsJsonArray(), workflow);
					obj.remove("steps");
				} else {
					throw new WorkflowCreationException(String.format("Workflow steps value '%s' is not a JSON array.", steps.toString()));
				}
			}
			
			for (final Map.Entry<String, JsonElement> entry: obj.entrySet()) {						
				JsonElement element = entry.getValue();						
				if (element == null) {
					workflow.addParameter(entry.getKey(), null);
				} else if (element.isJsonNull()) {
					workflow.addParameter(entry.getKey(), null);
				} else if (element.isJsonPrimitive()) {
					workflow.addParameter(entry.getKey(), element.getAsString());
				} else {
					throw new WorkflowCreationException(String.format("Cannot assign JSON value '%s' as a primitive to property '%s' on workflow - element is a %s.",
							element.toString(), entry.getKey(), element.getClass().getSimpleName()));
				}
			}

		} else if (root.isJsonArray()) {
			populateSteps(root.getAsJsonArray(), workflow);
		} else throw new WorkflowCreationException("Root element of JSON document is neither an array nor an object.");
		post(workflow);
		return workflow;
	}
	
	void populateSteps(final JsonArray steps, final Workflow workflow) throws WorkflowCreationException {
		for (final JsonElement step: steps) {
			if (step.isJsonObject()) {
				workflow.add(populateStep(step.getAsJsonObject()));
			}
		}
	}
	
	Step populateStep(final JsonObject obj) throws WorkflowCreationException {
		Step step = null;
		JsonElement classname = obj.get("class");
		if (classname == null) 
			throw new WorkflowCreationException(String.format("Provided JSON step definition '%s' does not supply a class declaration.", obj.toString()));
		if (!classname.isJsonPrimitive()) 
			throw new WorkflowCreationException(String.format("Provided JSON step class declaration '%s' is not a JSON primitive.", classname.toString()));
		
		
		try {
			final Class<?> source = classLoader.loadClass(classname.getAsString());
			if (Step.class.isAssignableFrom(source)) {
				step = (Step)source.newInstance();
				pre(step);
				
				JsonElement je = obj.get("maxRetries");
				if (je!=null && !je.isJsonNull()) {
					if (je.isJsonPrimitive() && je.getAsJsonPrimitive().isNumber()) {
						step.setMaxRetries(je.getAsInt());
					} else throw new WorkflowCreationException(String.format("Provided JSON step definition '%s' supplies a non-primitive 'maxRetries' value.", je.toString()));
					obj.remove("maxRetries");
				}
				
				je = obj.get("name");
				if (je!=null && !je.isJsonNull()) {
					if (je.isJsonPrimitive()) {
						step.setName(je.getAsString());					
					} else throw new WorkflowCreationException(String.format("Provided JSON step definition '%s' supplies a non-primitive 'name' value.", je.toString()));
					obj.remove("name");
				}

				je = obj.get("timeout");
				if (je!=null && !je.isJsonNull()) {
					if (je.isJsonPrimitive()) {
						step.setTimeout(je.getAsString());
					} else throw new WorkflowCreationException(String.format("Provided JSON step definition '%s' supplies a non-primitive 'timeout' value.", je.toString()));
					obj.remove("timeout");
				}

				je = obj.get("waitBetweenTries");
				if (je!=null && !je.isJsonNull()) {
					if (je.isJsonPrimitive()) {
						step.setWaitBetweenTries(je.getAsString());
					} else throw new WorkflowCreationException(String.format("Provided JSON step definition '%s' supplies a non-primitive 'waitBetweenTries' value.", je.toString()));
					obj.remove("waitBetweenTries");
				}

				je = obj.get("optional");
				if (je!=null && !je.isJsonNull()) {
					if (je.isJsonPrimitive() && je.getAsJsonPrimitive().isBoolean()) {						
						step.setOptional(je.getAsBoolean());
					} else throw new WorkflowCreationException(String.format("Provided JSON step definition '%s' supplies a non-primitive 'optional' value.", je.toString()));					
				}
				
				for (final Map.Entry<String, JsonElement> entry: obj.entrySet()) {						
					JsonElement element = entry.getValue();						
					if (element == null) {
						step.addParameter(entry.getKey(), null);
					} else if (element.isJsonNull()) {
						step.addParameter(entry.getKey(), null);
					} else if (element.isJsonPrimitive()) {
						step.addParameter(entry.getKey(), element.getAsString());
					} else if (element.isJsonArray()) {
						throw new WorkflowCreationException(String.format("Cannot assign JSON value '%s' as a primitive to property '%s' on step '%s' - element is a %s.",
								element.toString(), entry.getKey(), step.getName(), element.getClass().getSimpleName()));
					}
				}
				post(step);
			} else {
				throw new WorkflowCreationException(String.format("Provided step class definition '%s' does not extend '%s'", source.getName(), Step.class.getName()));
			}
		} catch (final ClassNotFoundException e) {
			throw new WorkflowCreationException(e);
		} catch (final InstantiationException e) {
			throw new WorkflowCreationException(e);
		} catch (final IllegalAccessException e) {
			throw new WorkflowCreationException(e);
		}
		return step;
	}
	
	/**
	 * Exception type thrown in the case that workflow creation fails.
	 * @author Bill Dimmick <me@billdimmick.com>
	 * @since 2012.12
	 */
	public static class WorkflowCreationException extends Exception {
		private static final long serialVersionUID = -4095197911433761427L;

		public WorkflowCreationException(final Throwable cause) {
			super(cause);
		}
		
		public WorkflowCreationException(final String message) {
			super(message);
		}
	}
}
