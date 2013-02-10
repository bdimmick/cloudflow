package com.hexagrammatic.cloudflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.Validate;

/**
 * Type enclosing a single workflow, consisting of metadata about the workflow and the steps the workflow will be taking upon
 * execution.
 * <p>
 * Workflows are configured with the following properties:
 * <ul>
 * 	<li><b>name</b> - the name of the workflow.  May be blank or null
 *  <li><b>timeout</b> - how long the workflow has to complete <i>all</i> steps before it is considered timed out.
 * </ul>
 * Steps may be created manually or via the Parser and added to the workflow via a call to <code>add(Step)</code>.
 *
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12
 * @see JsonParser
 * @see Step
 */
public class Workflow extends Parameterized {
	//TODO: Add pause method?
	//TODO: Add insertAfter methods?
	private final ArrayList<Step> steps = new ArrayList<Step>();
	private String name = null;
	private long timeoutValue = -1;
	private TimeUnit timeoutUnits = TimeUnit.SECONDS;
	private ExecutorService executor = null;
	private AtomicBoolean executing = new AtomicBoolean(false);
	private AtomicBoolean successful = new AtomicBoolean(false);
	private AtomicReference<Future<Void>> workflowFuture = new AtomicReference<Future<Void>>();
	private AtomicReference<Future<Void>> currstepFuture = new AtomicReference<Future<Void>>();
	private AtomicReference<Step> currstep = new AtomicReference<Step>();
	
	/**
	 * Gets the name of this workflow.
	 * @return the name of the workflow, as a String; may be <code>null</code>, but not blank.
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Sets the name of the workflow.  If the provided name is <code>null</code>, blank, or all whitespace,
	 * <code>null</code> is assigned as the name.
	 * @param name the name to assign - may be null.
	 */
	public final void setName(final String name) {
		if (name==null) {
			this.name = null;
			return;
		}
		this.name = name.trim();		
		if (this.name.length()==0) this.name=null;
	}

	/**
	 * Sets the timeout for the entire workflow.  This value consists of a String that contains
	 * a time tuple, such as "1 SECOND" or "5 HOUR", which is then parsed by <code>Utils.parseTimeTuple</code>
	 * to set the timeout value and units, respectively.
	 * @param timeout the timeout value, as a String; providing a null, empty, or unparsable String is considered an error condition.
	 * @throws IllegalArgumentException if the provided Stirng is null, empty, or unparsable. 
	 * @see Utils#parseTimeTuple(String)
	 */
	protected final void setTimeout(final String timeout) {
		final Object[] parsed = Utils.parseTimeTuple(timeout);
		setTimeoutValue((Long)parsed[0]);
		setTimeoutUnits((TimeUnit)parsed[1]);
	}
	
	/**
	 * Gets the timeout value, which is the numeric component of the overall workflow timeout.
	 * @return the timeout value, or -1 if the workflow never times out.
	 */
	public long getTimeoutValue() {
		return timeoutValue;
	}

	/**
	 * Sets the timeout value, which is the numeric component of the overall workflow timeout.
	 * Providing a negative value indicates that the workflow should never timeout and workflows
	 * that never time out will always have a timeout value of -1.
	 * @param timeout the timeout value (See above for special casing about negative values.) 
	 */
	public void setTimeoutValue(final long timeout) {
		this.timeoutValue = timeout;
		if (this.timeoutValue < -1) this.timeoutValue = -1; 

	}

	/**
	 * Gets the timeout units, which is the units component of the overall workflow timeout.
	 * @return the timeout units as a TimeUnit or <code>null</code> if this workflow has no timeout.
	 */
	public TimeUnit getTimeoutUnits() {
		if (timeoutValue < 0) return null;
		return timeoutUnits;
	}

	/**
	 * Sets the timeout units, which is the units component of the overall workflow timeout.
	 * The parameter to this method may never be null; if you want to disable the timeout, use
	 * <code>setTimeoutValue(-1)</code> instead.
	 * @param timeoutUnits the timeout units as a TimeUnit; may never be <code>null</code>.
	 * @see Workflow#setTimeoutValue(long)
	 * @throws IllegalArgumentException if the provided argument is <code>null</code>.
	 */
	public void setTimeoutUnits(final TimeUnit timeoutUnits) {
		Validate.notNull(timeoutUnits, "The provided timeout units may not be null.");
		this.timeoutUnits = timeoutUnits;
	}
	
	/**
	 * Adds a Step to this workflow, including making this Workflow the owner of the provided step.
	 * (Note: this method is not final so implementors may override it for any other special behaviors they 
	 *  wish to have executed when a step is added to a workflow.  Such implementations should always call
	 *  <code>super.add(s)</code> somewhere in their implementation.)  
	 * @param s the Step to add.  If <code>null</code>, no exception is thrown and the call is treated as a no-op.
	 * @throws IllegalArgumentException if the provided step already belongs to a workflow.
	 */
	public void add(final Step s) {
		if (s!=null) {
			if (steps.add(s)) {
				s.setWorkflow(this);
			}
		}
	}
	
	/**
	 * Gets the steps in this workflow.
	 * @return the steps, as an unmodifiable collection.
	 */
	public final Collection<Step> getSteps() {
		return Collections.unmodifiableCollection(steps);
	}
	
	/**
	 * Determines if this workflow is currently executing.
	 * @return <code>true</code> if it <code>execute()</code> has been called and is in progress, <code>false</code> otherwise.
	 */
	public final boolean isExecuting() {
		return this.executing.get();
	}
	
	/**
	 * Determines if the execution of this workflow was successful.
	 * @return <code>true</code> if the workflow has executed and was successful, <code>false</code> otherwise.
	 */
	public final boolean isSuccessful() {
		return this.successful.get();
	}
	
	/**
	 * Gets a reference to the current step being executed.
	 * @return the current step or <code>null</code> if no step is being executed.
	 */
	public final Step getCurrentStep() {
		return currstep.get();
	}
	
	/**
	 * Gets the number of tries the current step has executed.
	 * @return the number of tries the step has executed, or <code>0</code> if no tries for this step have been executed.
	 */
	public final int getCurrentStepTries() {
		return getCurrentStep().getTimesTried();
	}
	
	/**
	 * Executes the workflow.  Executes each step in order, including managing timeouts and retries for the
	 * steps and workflow.  
	 * <p>
	 * Note: Threads executing this workflow and its steps are <i>always</i> daemon threads. 
	 * @throws TimeoutException if a single step times out without any remaining retries or the whole workflow times out
	 * @throws RuntimeException thrown if a step decides to 'leak' a RuntimeException out of its <code>execute()</code> method.  
	 */
	public final void execute() throws TimeoutException, InterruptedException {
		executor = Executors.newCachedThreadPool(
					new ThreadFactory() {
						@Override
						public Thread newThread(final Runnable r) {
							final Thread t = new Thread(r);
							t.setDaemon(true);
							t.setName(String.format("Workflow %s(Thread#%s)", 
													getName() == null ? "" : String.format("'%s' ", getName()), t.getId()));
							return t;
						}
					}
				);
		
		final Callable<Void> call = new Callable<Void>() {			
			@Override
			public Void call() throws Exception {
				executeSteps();
				return null;
			}
		};
		
		final Future<Void> result = executor.submit(call);
		executing.set(true);
		workflowFuture.set(result);
		
		try {
			if (getTimeoutValue() > 0) {
				result.get(getTimeoutValue(), getTimeoutUnits());
			} else {
				result.get();
			}
		} catch (final TimeoutException te) {
			String message;
			if (getName() == null) {
				message = String.format("Execution of workflow timed out after %s", Utils.createTimeTuple(getTimeoutValue(), getTimeoutUnits()));
			} else {
				message = String.format("Execution of workflow '%s' timed out after %s", getName(), Utils.createTimeTuple(getTimeoutValue(), getTimeoutUnits()));
			}
			throw new TimeoutException(message);
		} catch (final ExecutionException ee) {
			Throwable cause = ee.getCause();
			while (cause instanceof ExecutionException) {
				cause = cause.getCause();
			}			
			if (cause instanceof RuntimeException) {
				throw (RuntimeException)cause;
			} else if (cause instanceof TimeoutException) {
				throw (TimeoutException)cause;				
			} else {
				throw new IllegalStateException("Unexpected non-runtime exception encountered.", cause);
			}
		} finally {
			result.cancel(true);
			workflowFuture.set(null);
			executing.set(false);
		}
	}
	
	/**
	 * Executes the steps in order, inlcuding handling timeouts and retries.
	 * @throws TimeoutException if a step times out without remaining retries.
	 * @throws ExecutionException if a step lets an unhandled exeception leak out of its <code>execute()</code> method.
	 * @throws InterruptedException if the step is interrupted while being executed.
	 */
	private final void executeSteps() throws TimeoutException, ExecutionException, InterruptedException {
		TimeoutException te = null;
		ExecutionException ee = null;

		boolean go = true;
		for (final Step step: steps) {
			try {
				if (go || step.isAlwaysRun()) {
					executeStep(step);
				} else {
					step.skip();
				}
			} catch (final ExecutionException e) {
				successful.set(false);
				go = false;
				ee = e;
			} catch (final TimeoutException t) {
				successful.set(false);
				go = false;
				te = t;
			}
		}
		
		if (ee != null) {			
			throw ee;
		} else if (te != null) {			
			throw te;
		} else {
			successful.set(true);
		}
	}

	/**
	 * Executes and individual step, inlcuding handling timeouts and retries.
	 * @param step the step itself; if <code>null</code>, this method instantly returns
	 * @throws TimeoutException if the step times out without remaining retries.
	 * @throws ExecutionException if the step lets an unhandled exeception leak out of its <code>execute()</code> method.
	 * @throws InterruptedException if the step is interrupted while being executed.
	 */
	private final void executeStep(final Step step)  throws TimeoutException, ExecutionException, InterruptedException {
		if (step == null) return;
		while (step.getTimesTried() <= step.getMaxRetries()) {			
			step.start();
			step.snapshot();
			final Callable<Void> call = new Callable<Void>() {			
				@Override
				public Void call() throws Exception {						
					step.execute();
					return null;
				}
			};
			
			final Future<Void> result = executor.submit(call);
			currstepFuture.set(result);
			currstep.set(step);
			try {
				if (step.getTimeoutValue() > 0) {
					result.get(step.getTimeoutValue(), step.getTimeoutUnits());
				} else {
					result.get();
				}
				step.complete();
				return;
			} catch (InterruptedException ie) {
				step.complete(ie);
				throw ie;
			} catch (TimeoutException te) {
				result.cancel(true);
				if (step.getTimesTried() > step.getMaxRetries() && !step.isOptional()) {
					step.complete(te);
					throw new TimeoutException(String.format("Execution of workflow step '%s' timed out after %s", step.getName(), 
																Utils.createTimeTuple(step.getTimeoutValue(), step.getTimeoutUnits())));
				}
			} catch (ExecutionException ee) {
				if (step.getTimesTried() > step.getMaxRetries() && !step.isOptional()) {
					step.complete(ee.getCause());
					throw ee;
				}
			} finally {
				result.cancel(true);
				currstepFuture.set(null);
				currstep.set(null);
			}
			waitBeforeRetry(step);
			step.rollback();
		}
	}
	
	/**
	 * Halts the execution of a workflow that is in progress.  Makes a best-faith effort at halting
	 * execution: if a task handles and swallows <code>InterruptedException</code>s, then it is highly
	 * unlikely that halting execution will be performed properly.
	 * @since 2013.01
	 */
	public void halt() {
		if (executing.get()) {
			if (currstepFuture.get() != null) {
				currstepFuture.get().cancel(true);
			}

			if (workflowFuture.get() != null) {
				workflowFuture.get().cancel(true);
			}

			if (executor!=null && !executor.isShutdown()) {
				executor.shutdownNow();
			}
		}
	}
	
	/**
	 * Perform any required sleeping between a failed execution of a step and its retry.  Implementors
	 * may choose to override this method to inject hooks that may happen before or after waiting, but
	 * such implementations should always call <code>super.retryWait(Step)</code> unless they want to handle
	 * the wait logic itself.
	 * <p>
	 * The wait logic, by default, is a contant-time wait.
	 * @param step the step that will be retried; never <code>null</code>
	 * @throws InterruptedException if the sleep is interrupted before it completes.
	 */
	protected void waitBeforeRetry(final Step step) throws InterruptedException {
		if (step.getWaitBetweenTriesValue() > 0) {
			step.getWaitBetweenTriesUnits().sleep(step.getWaitBetweenTriesValue());
		}		
	}	
}
