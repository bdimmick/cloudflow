package com.hexagrammatic.cloudflow;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12
 */
public class WorkflowTest {

	private Workflow workflow;
	private final AtomicLong totalTimeRetryWaiting = new AtomicLong();
	
	@Before
	public void setUp() throws Exception {
		workflow = new Workflow() {
			@Override
			protected void waitBeforeRetry(final Step step) throws InterruptedException {
				if (step.getWaitBetweenTriesValue() > 0) {
					totalTimeRetryWaiting.addAndGet(step.getWaitBetweenTriesUnits().toMillis(step.getWaitBetweenTriesValue()));
				}		
				super.waitBeforeRetry(step);
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
	public void testSuccessfulOneStepWorkflow() throws TimeoutException, InterruptedException {
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

	@Test
	public void testSuccessfulSimpleWorkflow() throws TimeoutException, InterruptedException {
		final Workflow workflow = new Workflow();
		final AtomicBoolean[] ran = new AtomicBoolean[20];
		for (int i=0; i<ran.length; i++) {
			final int j = i;
			ran[j]=new AtomicBoolean();
			final Step step = new Step() {			
				@Override
				public void execute() { ran[j].set(true); }
			};
			workflow.add(step);
		}
		workflow.execute();
		for (int i=0; i<ran.length; i++) {
			assertTrue(String.format("Step #%d did not run.", i), ran[i].get());
		}
	}

	@Test
	public void testUnsuccessfulSimpleWorkflow() throws TimeoutException, InterruptedException {
		final Workflow workflow = new Workflow();
		final AtomicBoolean[] ran = new AtomicBoolean[20];
		final int failOn = 11;
		for (int i=0; i<ran.length; i++) {
			final int j = i;
			ran[j]=new AtomicBoolean();
			final Step step = new Step() {			
				@Override
				public void execute() {
					if (j < failOn) {
						ran[j].set(true);
					} else {
						throw new IllegalStateException();
					}
				}
			};
			workflow.add(step);
		}
		
		try {		
			workflow.execute();
			fail("Expected exception not thrown during execution.");
		} catch (IllegalStateException ise) {}
			
		for (int i=0; i<ran.length; i++) {
			if (i < failOn) {
				assertTrue(String.format("Step #%d did not run.", i), ran[i].get());
			} else {
				assertFalse(String.format("Step #%d did ran when it was not supposed to.", i), ran[i].get());
			}
		}
	}

	@Test
	public void testUnsuccessfulSimpleWorkflowWithAlwaysRunSteps() throws TimeoutException, InterruptedException {
		final Workflow workflow = new Workflow();
		final AtomicBoolean[] ran = new AtomicBoolean[20];
		final int failOn = 11;
		for (int i=0; i<ran.length; i++) {
			final int j = i;
			ran[j]=new AtomicBoolean();
			final Step step = new Step() {			
				@Override
				public void execute() {
					if (j < failOn) {
						ran[j].set(true);
					} else {
						throw new IllegalStateException();
					}
				}
			};
			workflow.add(step);
		}

		final AtomicBoolean alwaysRan = new AtomicBoolean(false);
		final Step always = new Step() {			
			@Override
			protected void execute() throws InterruptedException {
				alwaysRan.set(true);
			}
		};
		always.setAlwaysRun(true);
		workflow.add(always);
		
		try {		
			workflow.execute();
			fail("Expected exception not thrown during execution.");
		} catch (IllegalStateException ise) {}

		for (int i=0; i<ran.length; i++) {
			if (i < failOn) {
				assertTrue(String.format("Step #%d did not run.", i), ran[i].get());
			} else {
				assertFalse(String.format("Step #%d did ran when it was not supposed to.", i), ran[i].get());
			}
		}
		
		assertTrue("The always-run steop did not run.", alwaysRan.get());
	}

	
	
	@Test(expected=TimeoutException.class)
	public void testStepTimeout() throws TimeoutException, InterruptedException {
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
	public void testWorkflowTimeoutNoName() throws TimeoutException, InterruptedException {
		final Step step = new Step() {			
			@Override
			public void execute() {
				try { Thread.sleep(1000); } catch (final InterruptedException ie) {}
			}
		};
		workflow.setTimeoutValue(100);
		workflow.setTimeoutUnits(MILLISECONDS);
		workflow.add(step);
		workflow.execute();
		assertEquals(step, workflow.getCurrentStep());
		assertEquals(1, workflow.getCurrentStepTries());		
	}
	
	@Test(expected=TimeoutException.class)
	public void testWorkflowTimeoutWithName() throws TimeoutException, InterruptedException {
		final Step step = new Step() {			
			@Override
			public void execute() {
				try { Thread.sleep(1000); } catch (final InterruptedException ie) {}
			}
		};
		workflow.setName("workflow");
		workflow.setTimeoutValue(100);
		workflow.setTimeoutUnits(MILLISECONDS);
		workflow.add(step);
		workflow.execute();
		assertEquals(step, workflow.getCurrentStep());
		assertEquals(1, workflow.getCurrentStepTries());
	}

	@Test(expected=NullPointerException.class)
	public void testStepException() throws TimeoutException, InterruptedException {
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
	public void testStepIsOptionalWithException() throws TimeoutException, InterruptedException {
		final Step optional = new Step() {			
			@Override
			public void execute() {
				throw new NullPointerException();
			}
		};
		optional.setOptional(true);
		
		final AtomicBoolean ran = new AtomicBoolean(false);
		final Step step = new Step() {			
			@Override
			public void execute() {
				ran.set(true);
			}
		};
		
		workflow.add(optional);
		workflow.add(step);
		workflow.execute();
		assertTrue(ran.get());
	}

	@Test
	public void testStepIsOptionalWithTimeout() throws TimeoutException, InterruptedException {
		final Step optional = new Step() {			
			@Override
			public void execute() {
				try { Thread.sleep(1000); } catch (InterruptedException ie) {};
			}
		};
		optional.setTimeoutValue(10);
		optional.setTimeoutUnits(MILLISECONDS);		
		optional.setOptional(true);
		
		final AtomicBoolean ran = new AtomicBoolean(false);
		final Step step = new Step() {			
			@Override
			public void execute() {
				ran.set(true);
			}
		};
		
		workflow.add(optional);
		workflow.add(step);
		workflow.execute();
		assertTrue(ran.get());
		assertTrue(step.isCompleted());
	}
	
	@Test
	public void testStepExceptionRetryNoWait() throws TimeoutException, InterruptedException {
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
		assertTrue(step.isCompleted());
	}

	@Test
	public void testStepExceptionRetryWithWait() throws TimeoutException, InterruptedException {
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
		assertTrue(step.isCompleted());
	}

	@Test
	public void testStepTimeoutRetryWithWait() throws TimeoutException, InterruptedException, InterruptedException {
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
		assertTrue(step.isCompleted());
	}
	
	@Test
	public void testWorkflowHalt() throws Exception {
		final AtomicBoolean finished = new AtomicBoolean(false);
		final Step step = new Step() {			
			@Override
			public void execute() throws InterruptedException {
				Thread.sleep(10000);
				finished.set(true);
			}
		};
		workflow.add(step);
		
		final ExecutorService pool = Executors.newCachedThreadPool();
		try {
			pool.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					workflow.execute();
					return null;
				}
			});			
			Thread.sleep(100);
			assertTrue(workflow.isExecuting());
			assertFalse(step.isCompleted());
			workflow.halt();
			Thread.sleep(100);
			assertFalse(workflow.isExecuting());
			assertTrue(step.isCompleted());
			assertFalse(finished.get());			
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	public void testStepParametersRollback() throws TimeoutException, InterruptedException {
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
