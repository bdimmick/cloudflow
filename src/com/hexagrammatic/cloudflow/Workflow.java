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
	//TODO: Add stop and pause methods?
	//TODO: Add insertAfter methods?
	private ArrayList<Step> steps = new ArrayList<Step>();
	private String name = null;
	private long timeoutValue = -1;
	private TimeUnit timeoutUnits = TimeUnit.SECONDS;
	
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
	protected final void setName(final String name) {
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
	protected long getTimeoutValue() {
		return timeoutValue;
	}

	/**
	 * Sets the timeout value, which is the numeric component of the overall workflow timeout.
	 * Providing a negative value indicates that the workflow should never timeout and workflows
	 * that never time out will always have a timeout value of -1.
	 * @param timeout the timeout value (See above for special casing about negative values.) 
	 */
	protected void setTimeoutValue(final long timeout) {
		this.timeoutValue = timeout;
		if (this.timeoutValue < -1) this.timeoutValue = -1; 

	}

	/**
	 * Gets the timeout units, which is the units component of the overall workflow timeout.
	 * @return the timeout units as a TimeUnit or <code>null</code> if this workflow has no timeout.
	 */
	protected TimeUnit getTimeoutUnits() {
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
	protected void setTimeoutUnits(final TimeUnit timeoutUnits) {
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
	 * Executes the workflow.  Executes each step in order, including managing timeouts and retries for the
	 * steps and workflow.  
	 * <p>
	 * Note: Threads executing this workflow and its steps are <i>always</i> daemon threads. 
	 * @throws TimeoutException if a single step times out without any remaining retries or the whole workflow times out
	 * @throws RuntimeException thrown if a step decides to 'leak' a RuntimeException out of its <code>execute()</code> method.  
	 */
	public final void execute() throws TimeoutException {
		final ExecutorService executor = Executors.newFixedThreadPool(2,
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
				executeSteps(executor);
				return null;
			}
		};
		
		final Future<Void> result = executor.submit(call);
		
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
		} catch (final InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * Executes the steps in order, inlcuding handling timeouts and retries.
	 * @param executor the executor in which to execute the steps; never null.
	 * @throws TimeoutException if a step times out without remaining retries.
	 * @throws ExecutionException if a step lets an unhandled exeception leak out of its <code>execute()</code> method.
	 * @throws InterruptedException if the step is interrupted while being executed.
	 */
	private final void executeSteps(final ExecutorService executor) throws TimeoutException, ExecutionException, InterruptedException {
		for (final Step step: steps) {
			int trynum = -1;
			while (trynum < step.getMaxRetries()) {
				step.snapshot();
				trynum++;
				final Callable<Void> call = new Callable<Void>() {			
					@Override
					public Void call() throws Exception {						
						step.execute();
						return null;
					}
				};
				
				final Future<Void> result = executor.submit(call);
				
				try {
					if (step.getTimeoutValue() > 0) {
						result.get(step.getTimeoutValue(), step.getTimeoutUnits());
					} else {
						result.get();
					}
					return;
				} catch (TimeoutException te) {
					result.cancel(true);
					if (trynum == step.getMaxRetries()) {
						throw new TimeoutException(String.format("Execution of workflow step '%s' timed out after %s", step.getName(), 
																	Utils.createTimeTuple(step.getTimeoutValue(), step.getTimeoutUnits())));
					}
				} catch (ExecutionException ee) {
					if (trynum == step.getMaxRetries()) throw ee;
				}	
				waitBeforeRetry(step);
				step.rollback();
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
