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
import com.hexagrammatic.cloudflow.Parser.WorkflowCreationException;

/**
 * @author Bill Dimmick <me@billdimmick.com>
 */
public class ParserTest {

	private Parser parser;
	
	@Before
	public void setUp() throws Exception {
		parser = new Parser();
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
		assertTrue(step.hasParameter("parameter"));
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
		assertTrue(step.hasParameter("parameter"));
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
		assertEquals(5, step.getTimeout());
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
		assertEquals(5, step.getWaitBetweenTries());
		assertEquals(TimeUnit.MINUTES, step.getWaitBetweenTriesUnits());
	}

	@Test
	public void testStepCreationWithMaxTriesAssignment() throws Exception {	
		final int value = 5;
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(SimpleStep.class.getName()));
		obj.add("maxTries", new JsonPrimitive(value));
		final Step step = parser.populateStep(obj);
		assertNotNull(step);
		assertEquals(SimpleStep.class, step.getClass());
		assertEquals(value, step.getMaxTries());		
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
	public void testPrivateStepCreation() throws Exception {	
		final JsonObject obj = new JsonObject();
		obj.add("class", new JsonPrimitive(PrivateStep.class.getName()));
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
		assertEquals(5, workflow.getTimeout());
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
		assertEquals(5, workflow.getTimeout());
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
		assertEquals(5, workflow.getTimeout());
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
		assertEquals(5, workflow.getTimeout());
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
