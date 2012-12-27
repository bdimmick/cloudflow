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
 * In some cases, this parser will try to ignore or warn on type errors (e.g. expecting a JSON array and finding a JSON string primitive) and 
 * try to continue marshalling nonetheless.  This may result in incomplete or faulty workflows.  If you don't want the parser to ignore these type
 * errors, please feel free to turn on <code>strict</code> mode, which will result in <code>WorkflowCreationException</code>s when such cases are encountered. 
 * <p>
 * In the case that you want to load classes for Steps from another classloader, feel free to use the <code>setClassLoader</code> method to set the specific
 * classloader that loads the Step classes.  This may be useful in some cases where the Step bytecode is defined outside the initial Java classpath and could
 * be dyanmically updated, such as hosting the classes in a version control system or a distributed filesystem, allowing them to be updated without restarting
 * the JVM.
 *   
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12
 */
public class JsonParser {
	//TODO: Enable 'strict' mode.
	
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
	
	Workflow populateWorkflow(final JsonElement root) throws WorkflowCreationException {
		final Workflow workflow = new Workflow(); 
		if (root.isJsonObject()) {
			final JsonObject obj = root.getAsJsonObject();

			final JsonElement name = obj.get("name");
			if (name!=null && name.isJsonPrimitive()) {
				workflow.setName(name.getAsString());
				obj.remove("name");
			}
			
			final JsonElement timeout = obj.get("timeout");
			if (timeout!=null && timeout.isJsonPrimitive()) {
				workflow.setTimeout(timeout.getAsString());
				obj.remove("timeout");
			}
			
			final JsonElement steps = obj.get("steps");
			if (steps!=null && steps.isJsonArray()) {
				populateSteps(steps.getAsJsonArray(), workflow);
				obj.remove("steps");
			}
			
			for (final Map.Entry<String, JsonElement> entry: obj.entrySet()) {						
				JsonElement element = entry.getValue();						
				if (element == null) {
					workflow.addParameter(entry.getKey(), null);
				} else if (element.isJsonNull()) {
					workflow.addParameter(entry.getKey(), null);
				} else if (element.isJsonPrimitive()) {
					workflow.addParameter(entry.getKey(), element.getAsString());
				} else if (element.isJsonArray()) {
					throw new WorkflowCreationException(String.format("Cannot assign JSON value '%s' as a primitive to property '%s' on workflow - element is an array.",
							element.toString(), entry.getKey()));
				}
			}

		} else if (root.isJsonArray()) {
			populateSteps(root.getAsJsonArray(), workflow);
		}
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
		Class<?> source = null;
		try {
			if (obj.has("class") && obj.get("class").isJsonPrimitive()) {
				source = classLoader.loadClass(obj.get("class").getAsString());
			}		
			if (source!=null) {
				if (Step.class.isAssignableFrom(source)) {
					step = (Step)source.newInstance();
				
					if (obj.has("maxTries")) {
						final JsonElement je = obj.get("maxTries");
						if (je.isJsonPrimitive()) {
							step.setMaxTries(je.getAsInt());
						}
						obj.remove("maxTries");
					}
					
					if (obj.has("name")) {
						final JsonElement je = obj.get("name");
						if (je.isJsonPrimitive()) {
							step.setName(je.getAsString());
						}
						obj.remove("name");
					}
					
					if (obj.has("timeout")) {
						final JsonElement je = obj.get("timeout");
						if (je.isJsonPrimitive()) {
							step.setTimeout(je.getAsString());
						}
						obj.remove("timeout");

					}
					
					if (obj.has("waitBetweenTries")) {
						final JsonElement je = obj.get("waitBetweenTries");
						if (je.isJsonPrimitive()) {
							step.setWaitBetweenTries(je.getAsString());
						}
						obj.remove("waitBetweenTries");
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
							throw new WorkflowCreationException(String.format("Cannot assign JSON value '%s' as a primitive to property '%s' on step '%s' - element is an array.",
									element.toString(), entry.getKey(), step.getName()));
						}
					}
				}
			}		
		} catch (final ClassNotFoundException e) {
			throw new WorkflowCreationException(e);
		} catch (InstantiationException e) {
			throw new WorkflowCreationException(e);
		} catch (IllegalAccessException e) {
			throw new WorkflowCreationException(e);
		}
		return step;
	}
	
	public static class WorkflowCreationException extends Exception {
		private static final long serialVersionUID = -4095197911433761427L;

		public WorkflowCreationException(final String message, final Throwable cause) {
			super(message, cause);
		}
		
		public WorkflowCreationException(final Throwable cause) {
			super(cause);
		}
		
		public WorkflowCreationException(final String message) {
			super(message);
		}
	}
}
