package com.hexagrammatic.cloudflow;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WorkflowTest {

	private Workflow workflow;
	private final AtomicLong totalTimeRetryWaiting = new AtomicLong();
	
	@Before
	public void setUp() throws Exception {
		workflow = new Workflow() {
			@Override
			protected void waitBeforeRetry(final Step step) {
				if (step.getWaitBetweenTriesValue() > 0) {
					totalTimeRetryWaiting.addAndGet(step.getWaitBetweenTriesUnits().toMillis(step.getWaitBetweenTriesValue()));
				}		

			}
		};
	}

	@After
	public void tearDown() {
		totalTimeRetryWaiting.set(0L);
	}
	
	@Test
	public void testUnsetName() {
		assertNull(workflow.getName());
	}

	@Test
	public void testNullName() {
		workflow.setName(null);
		assertNull(workflow.getName());
	}
	
	@Test
	public void testBasicNameSetting() {
		final String name = "name";
		workflow.setName(name);
		assertEquals(name, workflow.getName());
	}
	
	@Test
	public void testBlankNameSetting() {
		workflow.setName("   ");
		assertNull(workflow.getName());
	}
	
	@Test
	public void testTimeoutAsLongWithUnsetUnits() {
		final long value = 1;
		workflow.setTimeoutValue(value);
		assertEquals(value, workflow.getTimeoutValue());
		assertEquals(SECONDS, workflow.getTimeoutUnits());
	}

	@Test
	public void testTimeoutAsLongWithSetUnits() {
		final long value = 1;
		final TimeUnit unit = MINUTES;
		workflow.setTimeoutValue(value);
		workflow.setTimeoutUnits(unit);
		assertEquals(value, workflow.getTimeoutValue());
		assertEquals(unit, workflow.getTimeoutUnits());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullTimeoutUnits() {
		workflow.setTimeoutUnits(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullTimeoutString() {
		workflow.setTimeout(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testEmptyTimeoutString() {
		workflow.setTimeout("   ");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testBadUnitsTimeoutString() {
		workflow.setTimeout("1 LIGHTYEAR");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testBadValueTimeoutString() {
		workflow.setTimeout("Q MINUTES");
	}
	
	@Test
	public void testSignularUnitTimeoutString() {
		workflow.setTimeout("1 MINUTE");
		assertEquals(1, workflow.getTimeoutValue());
		assertEquals(MINUTES, workflow.getTimeoutUnits());
	}

	@Test
	public void testPluralUnitTimeoutString() {
		workflow.setTimeout("5 HOURS");
		assertEquals(5, workflow.getTimeoutValue());
		assertEquals(HOURS, workflow.getTimeoutUnits());
	}

	@Test
	public void testNoUnitTimeoutString() {
		workflow.setTimeout("5");
		assertEquals(5, workflow.getTimeoutValue());
		assertEquals(SECONDS, workflow.getTimeoutUnits());		
	}
	
	@Test
	public void testSuccessfulSimpleWorkflow() throws TimeoutException {
		final AtomicBoolean ran = new AtomicBoolean();
		final Step step = new Step() {			
			@Override
			public void execute() { ran.set(true); }
		};
		final Workflow workflow = new Workflow();
		workflow.add(step);
		workflow.execute();
		assertTrue(ran.get());
	}

	@Test(expected=TimeoutException.class)
	public void testStepTimeout() throws TimeoutException {
		final Step step = new Step() {			
			@Override
			public void execute() {
				try { Thread.sleep(1000); } catch (final InterruptedException ie) {}
			}
		};
		step.setTimeoutValue(10);
		step.setTimeoutUnits(MILLISECONDS);
		workflow.add(step);
		workflow.execute();
	}

	@Test(expected=TimeoutException.class)
	public void testWorkflowTimeout() throws TimeoutException {
		final Step step = new Step() {			
			@Override
			public void execute() {
				try { Thread.sleep(1000); } catch (final InterruptedException ie) {}
			}
		};
		workflow.setTimeoutValue(10);
		workflow.setTimeoutUnits(MILLISECONDS);
		workflow.add(step);
		workflow.execute();
	}
	

	@Test(expected=NullPointerException.class)
	public void testStepException() throws TimeoutException {
		final Step step = new Step() {			
			@Override
			public void execute() {
				throw new NullPointerException();
			}
		};
		workflow.add(step);
		workflow.execute();
	}
	
	@Test
	public void testStepExceptionRetryNoWait() throws TimeoutException {
		final AtomicInteger count = new AtomicInteger();
		final Step step = new Step() {			
			@Override
			public void execute() {
				if (count.incrementAndGet() == 1) {
					throw new NullPointerException();
				}
				
			}
		};
		step.setMaxRetries(2);
		workflow.add(step);
		workflow.execute();
		assertEquals(2, count.get());
		assertEquals(0L, totalTimeRetryWaiting.get());
	}

	@Test
	public void testStepExceptionRetryWithWait() throws TimeoutException {
		final AtomicInteger count = new AtomicInteger();
		final Step step = new Step() {			
			@Override
			public void execute() {
				if (count.incrementAndGet() == 1) {
					throw new NullPointerException();
				}
				
			}
		};
		step.setMaxRetries(2);
		step.setWaitBetweenTriesValue(10);
		step.setWaitBetweenTriesUnits(MILLISECONDS);
		workflow.add(step);
		workflow.execute();		
		assertEquals(2, count.get());
		assertEquals(step.getWaitBetweenTriesValue(), totalTimeRetryWaiting.get());
	}

	@Test
	public void testStepTimeoutRetryWithWait() throws TimeoutException {
		final AtomicInteger count = new AtomicInteger();
		final Step step = new Step() {			
			@Override
			public void execute() {
				if (count.incrementAndGet() == 1) {
					try { Thread.sleep(1000); } catch (final InterruptedException ie) {}
				}
				
			}
		};
		step.setTimeoutValue(10);
		step.setTimeoutUnits(MILLISECONDS);
		step.setMaxRetries(2);
		step.setWaitBetweenTriesValue(10);
		step.setWaitBetweenTriesUnits(MILLISECONDS);
		workflow.add(step);
		workflow.execute();
		assertEquals(2, count.get());
		assertEquals(step.getWaitBetweenTriesValue(), totalTimeRetryWaiting.get());
	}

	@Test
	public void testStepParametersRollback() throws TimeoutException {
		final AtomicInteger count = new AtomicInteger();
		final Step step = new Step() {			
			@Override
			public void execute() {
				if (getParameter("alreadyRun")!=null) {
					throw new IllegalStateException();
				}
				addParameter("alreadyRun", true);
				if (count.incrementAndGet() == 1) {
					try { Thread.sleep(1000); } catch (final InterruptedException ie) {}
				}
			}
		};
		step.setTimeoutValue(10);
		step.setTimeoutUnits(MILLISECONDS);
		step.setMaxRetries(2);
		workflow.add(step);
		workflow.execute();
	}

	
}
