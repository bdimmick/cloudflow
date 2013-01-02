/**
 * 
 */
package com.hexagrammatic.cloudflow;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12
 */
public class JsonParserTest {

	private JsonParser parser;
	
	@Before
	public void setUp() throws Exception {
		parser = new JsonParser();
	}
	
	@Test
	public void testStepCreationClassOnly() throws Exception {		
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		final Step step = parser.populateStep(obj);
		assertNotNull(step);
		assertEquals(SimpleStep.class, step.getClass());
	}
	
	@Test
	public void testStepCreationWithNullAssignment() throws Exception {		
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("parameter", null);
		final Step step = parser.populateStep(obj);
		assertNotNull(step);
		assertEquals(SimpleStep.class, step.getClass());
		assertFalse(step.hasParameter("parameter"));
		assertNull(step.getParameter("parameter"));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testStepCreationWithJsonNullAssignment() throws Exception {		
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("parameter", new JsonNull());
		final Step step = parser.populateStep(obj);
		assertNotNull(step);
		assertEquals(SimpleStep.class, step.getClass());
		assertFalse(step.hasParameter("parameter"));
		assertNull(step.getParameter("parameter"));
	}

	@Test
	public void testStepCreationWithValueAssignment() throws Exception {	
		final String value = "value";
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("parameter", new JsonPrimitive(value));
		final Step step = parser.populateStep(obj);
		assertNotNull(step);
		assertEquals(SimpleStep.class, step.getClass());
		assertTrue(step.hasParameter("parameter"));
		assertEquals(value, step.getParameter("parameter"));
	}

	//JSON Arrays are unsupported currently.
	@Test(expected=WorkflowCreationException.class)
	public void testStepCreationWithAssignment() throws Exception {	
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("parameter", new JsonArray());
		parser.populateStep(obj);
	}
	
	@Test
	public void testStepCreationWithTimeoutAssignment() throws Exception {	
		final String value = "5 MINUTES";
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("timeout", new JsonPrimitive(value));
		final Step step = parser.populateStep(obj);
		assertNotNull(step);
		assertEquals(SimpleStep.class, step.getClass());
		assertEquals(5, step.getTimeoutValue());
		assertEquals(TimeUnit.MINUTES, step.getTimeoutUnits());
	}

	@Test
	public void testStepCreationWithWaitBetweenTriesAssignment() throws Exception {	
		final String value = "5 MINUTES";
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("waitBetweenTries", new JsonPrimitive(value));
		final Step step = parser.populateStep(obj);
		assertNotNull(step);
		assertEquals(SimpleStep.class, step.getClass());
		assertEquals(5, step.getWaitBetweenTriesValue());
		assertEquals(TimeUnit.MINUTES, step.getWaitBetweenTriesUnits());
	}

	@Test
	public void testStepCreationWithMaxRetriesAssignment() throws Exception {	
		final int value = 5;
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("maxRetries", new JsonPrimitive(value));
		final Step step = parser.populateStep(obj);
		assertNotNull(step);
		assertEquals(SimpleStep.class, step.getClass());
		assertEquals(value, step.getMaxRetries());		
	}

	@Test
	public void testStepCreationWithNameAssignment() throws Exception {	
		final String value = UUID.randomUUID().toString();
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("name", new JsonPrimitive(value));
		final Step step = parser.populateStep(obj);
		assertNotNull(step);
		assertEquals(SimpleStep.class, step.getClass());
		assertEquals(value, step.getName());		
	}
	
	@Test(expected=WorkflowCreationException.class)
	public void testNonPrimitiveNameStepCreation() throws Exception {	
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("name", new JsonArray());
		parser.populateStep(obj);
	}

	@Test(expected=WorkflowCreationException.class)
	public void testNonPrimitiveTimeoutStepCreation() throws Exception {	
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("timeout", new JsonArray());
		parser.populateStep(obj);
	}

	@Test
	public void testStepCreationWithOptionalAssignment() throws Exception {	
		final boolean value = true;
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("optional", new JsonPrimitive(value));
		final Step step = parser.populateStep(obj);
		assertNotNull(step);
		assertEquals(SimpleStep.class, step.getClass());
		assertEquals(value, step.isOptional());		
	}

	@Test(expected = WorkflowCreationException.class)
	public void testStepCreationWithNonBooleanPrimitiveOptionalAssignment() throws Exception {	
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("optional", new JsonPrimitive("huh huh"));
		parser.populateStep(obj);
	}	

	@Test(expected = WorkflowCreationException.class)
	public void testStepCreationWithNonPrimitiveOptionalAssignment() throws Exception {	
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("optional", new JsonArray());
		parser.populateStep(obj);
	}	

	@Test(expected=WorkflowCreationException.class)
	public void testPrivateStepCreation() throws Exception {	
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(PrivateStep.class.getName()));
		parser.populateStep(obj);
	}

	@Test(expected=WorkflowCreationException.class)
	public void testNoClassStepCreation() throws Exception {	
		parser.populateStep(new JsonObject());
	}
		
	
	@Test(expected=WorkflowCreationException.class)
	public void testNonPrimitiveClassStepCreation() throws Exception {	
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonArray());
		parser.populateStep(obj);
	}

	@Test(expected=WorkflowCreationException.class)
	public void testNonStepClassStepCreation() throws Exception {	
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(String.class.getName()));
		parser.populateStep(obj);
	}

	@Test(expected=WorkflowCreationException.class)
	public void testAbstractStepCreation() throws Exception {	
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(AbstractStep.class.getName()));
		parser.populateStep(obj);
	}

	@Test(expected=WorkflowCreationException.class)
	public void testExceptionStepCreation() throws Exception {	
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(ExceptionStep.class.getName()));
		parser.populateStep(obj);
	}

	@Test(expected=WorkflowCreationException.class)
	public void testMissingClassStepCreation() throws Exception {	
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive("com.missing.class.no.really.it.is.Gone"));
		parser.populateStep(obj);
	}
	
	@Test(expected=WorkflowCreationException.class)
	public void testWorkflowCreationWithPrimitiveRoot() throws Exception {
		parser.populateWorkflow(new JsonPrimitive("say what?"));
	}

	@Test(expected=WorkflowCreationException.class)
	public void testWorkflowCreationWithNameNonPrimitive() throws Exception {
		final JsonObject obj = new JsonObject();
		obj.add("name", new JsonArray());
		parser.populateWorkflow(obj);
	}

	@Test(expected=WorkflowCreationException.class)
	public void testWorkflowCreationWithStepsNonArray() throws Exception {
		final JsonObject obj = new JsonObject();
		obj.add("steps", new JsonPrimitive(true));
		parser.populateWorkflow(obj);
	}

	@Test(expected=WorkflowCreationException.class)
	public void testWorkflowCreationWithTimeoutNonPrimitive() throws Exception {
		final JsonObject obj = new JsonObject();
		obj.add("timeout", new JsonArray());
		parser.populateWorkflow(obj);
	}

	@Test(expected=WorkflowCreationException.class)
	public void testWorkflowCreationWithMaxRetriesNonPrimitive() throws Exception {
		final JsonObject obj = new JsonObject();
		obj.add("maxRetries", new JsonArray());
		parser.populateWorkflow(obj);
	}
	
	@Test(expected=WorkflowCreationException.class)
	public void testWorkflowCreationWithWaitBetweenTriesNonPrimitive() throws Exception {
		final JsonObject obj = new JsonObject();
		obj.add("waitBetweenTries", new JsonArray());
		parser.populateWorkflow(obj);
	}

	@Test
	public void testWorkflowCreationWithPrimitiveParameter() throws Exception {
		final JsonObject obj = new JsonObject();
		final String key = "param";
		final String value = "value";
		obj.add(key, new JsonPrimitive(value));
		final Workflow workflow = parser.populateWorkflow(obj);
		assertTrue(workflow.hasParameter(key));
		assertEquals(value, workflow.getParameter(key));
	}

	@Test
	public void testWorkflowCreationWithNullParameter() throws Exception {
		final JsonObject obj = new JsonObject();
		final String nullkey = "null";
		obj.add(nullkey, null);
		final Workflow workflow = parser.populateWorkflow(obj);
		assertTrue(!workflow.hasParameter(nullkey));
		assertNull(workflow.getParameter(nullkey));
	}

	@Test(expected=WorkflowCreationException.class)
	public void testWorkflowCreationWithNonPrimitiveParameter() throws Exception {
		final JsonObject obj = new JsonObject();
		obj.add("key", new JsonArray());
		parser.populateWorkflow(obj);
	}
	
	@Test
	public void testManyStepsCreation() throws Exception {
		final int n = 3;
		final Workflow workflow = new Workflow();
		final JsonArray array = new JsonArray();
		for (int i=0; i<n; i++) {
			final JsonObject obj = new JsonObject();
			obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
			obj.add("name", new JsonPrimitive(String.format("name%d", i)));
			array.add(obj);
		}
		parser.populateSteps(array, workflow);
		assertEquals(n, workflow.getSteps().size());
		int i = 0;
		for (final Step step: workflow.getSteps()) {
			assertEquals(SimpleStep.class, step.getClass());
			assertEquals(String.format("name%d", i), step.getName());
			assertEquals(workflow, step.getWorkflow());
			i++;
		}		
	}

	@Test(expected=WorkflowCreationException.class)
	public void testNonJSONObjectInStepsCreation() throws Exception {
		final Workflow workflow = new Workflow();
		final JsonArray array = new JsonArray();
		array.add(new JsonPrimitive(true));
		parser.populateSteps(array, workflow);
	}

	
	@Test
	public void testWorkflowCreationWithJsonArray() throws Exception {
		final int n = 3;
		final JsonArray array = new JsonArray();
		for (int i=0; i<n; i++) {
			final JsonObject obj = new JsonObject();
			obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
			obj.add("name", new JsonPrimitive(String.format("name%d", i)));
			array.add(obj);
		}
		final Workflow workflow = parser.populateWorkflow(array);
		assertEquals(n, workflow.getSteps().size());
		int i = 0;
		for (final Step step: workflow.getSteps()) {
			assertEquals(SimpleStep.class, step.getClass());
			assertEquals(String.format("name%d", i), step.getName());
			assertEquals(workflow, step.getWorkflow());
			i++;
		}		
	}

	@Test
	public void testWorkflowCreationWithJsonObject() throws Exception {
		final String name = "some workflow";
		final String timeout = "5 HOURS";
		final int n = 3;
		final JsonObject object = new JsonObject();
		final JsonArray array = new JsonArray();
		for (int i=0; i<n; i++) {
			final JsonObject obj = new JsonObject();
			obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
			obj.add("name", new JsonPrimitive(String.format("name%d", i)));
			array.add(obj);
		}
		object.add("steps", array);
		object.add("timeout", new JsonPrimitive(timeout));
		object.add("name", new JsonPrimitive(name));
		
		
		final Workflow workflow = parser.populateWorkflow(object);
		assertEquals(name, workflow.getName());
		assertEquals(5, workflow.getTimeoutValue());
		assertEquals(TimeUnit.HOURS, workflow.getTimeoutUnits());
		assertEquals(n, workflow.getSteps().size());
		int i = 0;
		for (final Step step: workflow.getSteps()) {
			assertEquals(SimpleStep.class, step.getClass());
			assertEquals(String.format("name%d", i), step.getName());
			assertEquals(workflow, step.getWorkflow());
			i++;
		}		
	}

	@Test
	public void testWorkflowCreationWithString() throws Exception {
		final String name = "some workflow";
		final String timeout = "5 HOURS";
		final int n = 3;
		final JsonObject object = new JsonObject();
		final JsonArray array = new JsonArray();
		for (int i=0; i<n; i++) {
			final JsonObject obj = new JsonObject();
			obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
			obj.add("name", new JsonPrimitive(String.format("name%d", i)));
			array.add(obj);
		}
		object.add("steps", array);
		object.add("timeout", new JsonPrimitive(timeout));
		object.add("name", new JsonPrimitive(name));
		
		final Workflow workflow = parser.parse(object.toString());
		assertEquals(name, workflow.getName());
		assertEquals(5, workflow.getTimeoutValue());
		assertEquals(TimeUnit.HOURS, workflow.getTimeoutUnits());
		assertEquals(n, workflow.getSteps().size());
		int i = 0;
		for (final Step step: workflow.getSteps()) {
			assertEquals(SimpleStep.class, step.getClass());
			assertEquals(String.format("name%d", i), step.getName());
			assertEquals(workflow, step.getWorkflow());
			i++;
		}		
	}

	@Test
	public void testWorkflowCreationWithStream() throws Exception {
		final String name = "some workflow";
		final String timeout = "5 HOURS";
		final int n = 3;
		final JsonObject object = new JsonObject();
		final JsonArray array = new JsonArray();
		for (int i=0; i<n; i++) {
			final JsonObject obj = new JsonObject();
			obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
			obj.add("name", new JsonPrimitive(String.format("name%d", i)));
			array.add(obj);
		}
		object.add("steps", array);
		object.add("timeout", new JsonPrimitive(timeout));
		object.add("name", new JsonPrimitive(name));
		
		
		final Workflow workflow = parser.parse(new ByteArrayInputStream(object.toString().getBytes()));
		assertEquals(name, workflow.getName());
		assertEquals(5, workflow.getTimeoutValue());
		assertEquals(TimeUnit.HOURS, workflow.getTimeoutUnits());
		assertEquals(n, workflow.getSteps().size());
		int i = 0;
		for (final Step step: workflow.getSteps()) {
			assertEquals(SimpleStep.class, step.getClass());
			assertEquals(String.format("name%d", i), step.getName());
			assertEquals(workflow, step.getWorkflow());
			i++;
		}		
	}

	
	@Test
	public void testWorkflowCreationWithReader() throws Exception {
		final String name = "some workflow";
		final String timeout = "5 HOURS";
		final int n = 3;
		final JsonObject object = new JsonObject();
		final JsonArray array = new JsonArray();
		for (int i=0; i<n; i++) {
			final JsonObject obj = new JsonObject();
			obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
			obj.add("name", new JsonPrimitive(String.format("name%d", i)));
			array.add(obj);
		}
		object.add("steps", array);
		object.add("timeout", new JsonPrimitive(timeout));
		object.add("name", new JsonPrimitive(name));
		
		final Reader reader = new InputStreamReader(new ByteArrayInputStream(object.toString().getBytes()));
		final Workflow workflow = parser.parse(reader);
		assertEquals(name, workflow.getName());
		assertEquals(5, workflow.getTimeoutValue());
		assertEquals(TimeUnit.HOURS, workflow.getTimeoutUnits());
		assertEquals(n, workflow.getSteps().size());
		int i = 0;
		for (final Step step: workflow.getSteps()) {
			assertEquals(SimpleStep.class, step.getClass());
			assertEquals(String.format("name%d", i), step.getName());
			assertEquals(workflow, step.getWorkflow());
			i++;
		}		
	}

	@Test
	public void testStepCreationWithCustomClassloader() throws Exception {
		final AtomicInteger count = new AtomicInteger();
		final ClassLoader loader = new ClassLoader() {
			@Override
			protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				count.incrementAndGet();
				if (SimpleStep.class.getName().equals(name)) {
					return SimpleStep.class;
				}
				return super.loadClass(name, resolve);
			}
		};
		parser.setClassLoader(loader);
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		final Step step = parser.populateStep(obj);
		assertNotNull(step);
		assertEquals(SimpleStep.class, step.getClass());
		assertEquals(1, count.get());
	}
	
		
	public static class SimpleStep extends Step {
		@Override
		public void execute() { }		
	}

	
	public static class PrivateStep extends Step {
		private PrivateStep() {}
		@Override
		public void execute() { }		
	}
	
	
	public abstract static class AbstractStep extends Step {
	}

	
	public static class ExceptionStep extends Step {
		private ExceptionStep() { throw new IllegalStateException(); }
		@Override
		public void execute() { }		
	}
}
