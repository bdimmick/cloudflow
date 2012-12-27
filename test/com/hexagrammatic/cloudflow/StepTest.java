package com.hexagrammatic.cloudflow;

import static org.junit.Assert.*;
import static java.util.concurrent.TimeUnit.*;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class StepTest {

	private Step step;
	
	@Before
	public void setUp() throws Exception {
		step = new Step() {
			@Override
			public void execute() {
			}
		};
	}

	@Test
	public void testDefaultName() {
		assertNotNull(step.getName());
	}
	
	@Test
	public void testExplicitName() {
		final String name = "name";
		step.setName(name);
		assertEquals(name, step.getName());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullName() {
		step.setName(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testEmptyName() {
		step.setName("   ");
	}
	
	@Test
	public void testTimeoutAsLongWithUnsetUnits() {
		final long value = 1;
		step.setTimeoutValue(value);
		assertEquals(value, step.getTimeoutValue());
		assertEquals(SECONDS, step.getTimeoutUnits());
	}

	@Test
	public void testTimeoutAsLongWithSetUnits() {
		final long value = 1;
		final TimeUnit unit = MINUTES;
		step.setTimeoutValue(value);
		step.setTimeoutUnits(unit);
		assertEquals(value, step.getTimeoutValue());
		assertEquals(unit, step.getTimeoutUnits());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullTimeoutUnits() {
		step.setTimeoutUnits(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullTimeoutString() {
		step.setTimeout(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testEmptyTimeoutString() {
		step.setTimeout("   ");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBadUnitsTimeoutString() {
		step.setTimeout("1 LIGHTYEAR");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testBadValueTimeoutString() {
		step.setTimeout("Q MINUTES");
	}
	
	@Test
	public void testSignularUnitTimeoutString() {
		step.setTimeout("1 MINUTE");
		assertEquals(1, step.getTimeoutValue());
		assertEquals(MINUTES, step.getTimeoutUnits());
	}

	@Test
	public void testPluralUnitTimeoutString() {
		step.setTimeout("5 HOURS");
		assertEquals(5, step.getTimeoutValue());
		assertEquals(HOURS, step.getTimeoutUnits());
	}

	@Test
	public void testNoUnitTimeoutString() {
		step.setTimeout("5");
		assertEquals(5, step.getTimeoutValue());
		assertEquals(SECONDS, step.getTimeoutUnits());		
	}
	
	@Test
	public void testSetMaxTries() {
		final int value = 12;
		step.setMaxRetries(value);
		assertEquals(value, step.getMaxRetries());
	}
	
	@Test
	public void testSetWaitBeforeRetryAsLongWithUnsetUnits() {
		final long value = 1;
		step.setWaitBetweenTriesValue(value);
		assertEquals(value, step.getWaitBetweenTriesValue());
		assertEquals(SECONDS, step.getWaitBetweenTriesUnits());
	}
	
	@Test
	public void testSetWaitBeforeRetryAsLongWithSetUnits() {
		final long value = 1;
		final TimeUnit unit = MINUTES;
		step.setWaitBetweenTriesValue(value);
		step.setWaitBetweenTriesUnits(unit);
		assertEquals(value, step.getWaitBetweenTriesValue());
		assertEquals(unit, step.getWaitBetweenTriesUnits());
	}

	
	@Test(expected=IllegalArgumentException.class)
	public void testNullWaitBetweenTriesUnits() {
		step.setWaitBetweenTriesUnits(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullWaitBetweenTriesString() {
		step.setWaitBetweenTries(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testEmptyWaitBetweenTriesString() {
		step.setWaitBetweenTries("   ");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBadUnitsWaitBetweenTriesString() {
		step.setWaitBetweenTries("1 LIGHTYEAR");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testBadValueWaitBetweenTriesString() {
		step.setWaitBetweenTries("Q MINUTES");
	}
	
	@Test
	public void testSignularUnitWaitBetweenTriesString() {
		step.setWaitBetweenTries("1 MINUTE");
		assertEquals(1, step.getWaitBetweenTriesValue());
		assertEquals(MINUTES, step.getWaitBetweenTriesUnits());
	}

	@Test
	public void testPluralUnitWaitBetweenTriesString() {
		step.setWaitBetweenTries("5 HOURS");
		assertEquals(5, step.getWaitBetweenTriesValue());
		assertEquals(HOURS, step.getWaitBetweenTriesUnits());
	}

	@Test
	public void testNoUnitWaitBetweenTriesString() {
		step.setWaitBetweenTries("5");
		assertEquals(5, step.getWaitBetweenTriesValue());
		assertEquals(SECONDS, step.getWaitBetweenTriesUnits());		
	}

	@Test
	public void testWorkflowAssignment() {
		final Workflow w = new Workflow();
		step.setWorkflow(w);
		assertEquals(w, step.getWorkflow());
	}
	
	@Test
	public void testParameterizationBasic() {
		final Workflow workflow = new Workflow();
		
		step.addParameter("foo", "baz");
		assertEquals("baz", step.getParameter("foo"));
		assertNull(workflow.getParameter("foo"));
		
	}

	@Test
	public void testParameterizationShadowing() {
		final Workflow workflow = new Workflow();

		step.addParameter("foo", "baz");
		step.setWorkflow(workflow);		
		workflow.addParameter("foo", "bak");
		workflow.addParameter("rock", "roll");
		
		assertEquals("baz", step.getParameter("foo"));
		assertEquals("bak", workflow.getParameter("foo"));
		assertEquals("roll", step.getParameter("rock"));
		assertEquals("roll", workflow.getParameter("rock"));
	}
	
	@Test
	public void testParameterizationBubbling() {
		final Workflow workflow = new Workflow();

		step.setWorkflow(workflow);
		step.addParameter("foo", "bar");
		
		assertEquals("bar", step.getParameter("foo"));
		assertEquals("bar", workflow.getParameter("foo"));
	}
}
