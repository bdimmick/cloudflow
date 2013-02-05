package com.hexagrammatic.cloudflow;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * Type enclosing a single workflow step, consisting of metadata about the step and the logic the step will be taking upon execution.
 * <p>
 * Implementors are only expected to override the <code>execute()</code> method with the logic of the step.  If a step needs to have
 * dependencies injected, implementors can extend a Step, define setters for the appropriate properties, and extend JsonParser to assign
 * the dependencies either during an implementation of <code>pre(Step)</code> or <code>post(Step)</code>.
 * 
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12 
 * @see Workflow
 */
public abstract class Step extends Parameterized {	
	private String name = getClass().getSimpleName();
	private Workflow workflow = null;
	private long timeoutValue = -1;
	private TimeUnit timeoutUnits = TimeUnit.SECONDS;
	private int maxRetries = 0;
	private long waitBetweenTriesValue = -1;
	private TimeUnit waitBetweenTriesUnits = TimeUnit.SECONDS;
	private boolean optional = false;
	private boolean alwaysRun = false;
	private volatile boolean completed = false;
	private volatile boolean skipped = false;
	private volatile long startTime = -1;
	private volatile long endTime = -1;
	private volatile int tries = 0;	
	private AtomicReference<Throwable> failureCause = new AtomicReference<Throwable>();
		
	/**
	 * Gets the name of this step.
	 * @return the name of the step; if unset, the 'simple name' of the class is used
	 */
	public final String getName() {
		return name;
	}
	
	/**
	 * Sets the name of the step
	 * @param name the name of the step - may not be blank or empty
	 */
	public final void setName(final String name) {
		Validate.isTrue(!StringUtils.isBlank(name), "The provided name may not be null or empty.");
		this.name = name;
	}

	/**
	 * Sets the workflow that 'owns' this step instance.
	 * @param workflow the workflow - may be <code>null</code> to indicate an 'unowned' step.
	 */
	protected final void setWorkflow(final Workflow workflow) {
		this.workflow = workflow;
	}
	
	/**
	 * Gets the workflow the 'owns' this step instance.
	 * @return the workflow instance, or <code>null</code> if the step is 'unowned'
	 */
	protected final Workflow getWorkflow() {
		return workflow;
	}

	/**
	 * Gets a parameter value for this step instance.  If the parameter is not found in the step
	 * itself and the step is 'owned' by a workflow, the workflow is also checked for
	 * the parameter.
	 */
	@Override
	public Object getParameter(final String key, final Object defaultValue) {		
		Object result =  super.getParameter(key, defaultValue);
		if (result == defaultValue && workflow!=null) {
			result = workflow.getParameter(key, defaultValue);
		}
		return result;
	}
	
	/**
	 * Adds a parameter to this step instance.  If the step is 'owned' by a workflow,
	 * then the parameter is also added to the workflow, allowing step execution
	 * to pass on parameters to subsequent steps.
	 */
	@Override	
	public void addParameter(final String key, final Object value) {
		super.addParameter(key, value);
		if (workflow != null) {			
			workflow.addParameter(key, value);
		}
	}
	
	/**
	 * Determines if this step instance has a specific parameter.  If the parameter is not found in the step
	 * itself and the step is 'owned' by a workflow, the workflow is also checked for
	 * the parameter.
	 */
	@Override
	public boolean hasParameter(final String key) {
		if (!super.hasParameter(key)) {
			if (workflow != null) {
				return workflow.hasParameter(key);
			} else {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Snapshots the parameters in this step instance.  If the step is 'owned' by a workflow,
	 * the workflow is also snapshotted.
	 */
	@Override
	public void snapshot() {
		super.snapshot();
		if (workflow!=null) workflow.snapshot();
	}

	/**
	 * Rolls back the parameters in this step instance to a previous snapshot.  If the step is 'owned' by a workflow,
	 * the workflow are also rolled back.
	 */
	@Override
	public void rollback() {		
		super.rollback();
		if (workflow!=null) workflow.rollback();
	}
	
	/**
	 * Removes a parameter from this step instance.  If the step is 'owned' by a workflow,
	 * the parameter is also removed from the workflow.
	 */
	@Override
	public void removeParameter(final String key) {
		super.removeParameter(key);
		if (workflow!=null) workflow.removeParameter(key);
	}
	
	/**
	 * Sets the timeout for this step.  Steps that run longer than
	 * their timeout are aborted and retried, if retries are available.
	 * @param timeout the timeout tuple - may not be <code>null</code> or blank
	 * @see Utils#parseTimeTuple(String)
	 */
	public final void setTimeout(final String timeout) {		
		final Object[] parsed = Utils.parseTimeTuple(timeout);
		setTimeoutValue((Long)parsed[0]);
		setTimeoutUnits((TimeUnit)parsed[1]);
	}
	
	/**
	 * Gets the timeout value, which is the numeric component of the overall step timeout.
	 * @return the timeout value, or -1 if the step never times out.
	 */
	protected final long getTimeoutValue() {
		return timeoutValue;
	}
		
	/**
	 * Sets the timeout value, which is the numeric component of the overall step timeout.
	 * Providing a negative value indicates that the step should never timeout and steps
	 * that never time out will always have a timeout value of -1.
	 * @param timeout the timeout value (See above for special casing about negative values.) 
	 */
	protected void setTimeoutValue(final long timeout) {
		this.timeoutValue = timeout;
		if (this.timeoutValue < -1) this.timeoutValue = -1; 

	}
	
	/**
	 * Gets the timeout units, which is the units component of the overall step timeout.
	 * @return the timeout units as a TimeUnit or <code>null</code> if this step has no timeout.
	 */
	protected final TimeUnit getTimeoutUnits() {
		if (timeoutValue < 0) return null;
		return timeoutUnits;
	}
	
	/**
	 * Sets the timeout units, which is the units component of the overall step timeout.
	 * The parameter to this method may never be null; if you want to disable the timeout, use
	 * <code>setTimeoutValue(-1)</code> instead.
	 * @param timeoutUnits the timeout units as a TimeUnit; may never be <code>null</code>.
	 * @see Step#setTimeoutValue(long)
	 * @throws IllegalArgumentException if the provided argument is <code>null</code>.
	 */
	protected final void setTimeoutUnits(final TimeUnit timeoutUnits) {
		Validate.notNull(timeoutUnits, "The provided timeout units may not be null.");
		this.timeoutUnits = timeoutUnits;
	}
	
	/**
	 * Get the maximum number of times the step will be retried after failure.  If the result
	 * is less than 1, this step is never retried.
	 * @return the number of times the step will be retried after failure - possibly zero or negative.
	 */
	protected final int getMaxRetries() {
		return maxRetries;
	}

	/**
	 * Set the maximum number of times the step will be retried after failure.  If this is set
	 * to 0, this step is never retried in workflow execution.  If unset, the default value
	 * is 0.
	 * @param maxRetries the number of times to try to execute this step - never negative  
	 */
	protected final void setMaxRetries(final int maxRetries) {
		Validate.isTrue(maxRetries >= 0, "The provided maximum retries must not be negative.");
		this.maxRetries = maxRetries;
	}

	/**
	 * Sets the time to wait between retries for this step.  Steps that are retried
	 * call the workflow's <code>waitForRetry</code>, which uses this value to determine
	 * how long to sleep before trying the step again.
	 * @param wait the wait time tuple - may not be <code>null</code> or blank
	 * @see Utils#parseTimeTuple(String)
	 */
	public final void setWaitBetweenTries(final String wait) {
		final Object[] parsed = Utils.parseTimeTuple(wait);
		setWaitBetweenTriesValue((Long)parsed[0]);
		setWaitBetweenTriesUnits((TimeUnit)parsed[1]);
	}
	
	/**
	 * Gets the retry wait value, which is the numeric component of the overall step retry wait.
	 * @return the retry wait value, or -1 if the step never waits between retries.
	 */
	protected final long getWaitBetweenTriesValue() {
		return waitBetweenTriesValue;
	}
	
	/**
	 * Sets the retry wait value, which is the numeric component of the overall step retry wait.
	 * Providing a negative value indicates that the step should never wait between retries and steps
	 * that never wait will always return a value of -1.
	 * @param waitBetweenTriesValue the timeout value (See above for special casing about negative values.) 
	 */
	protected final void setWaitBetweenTriesValue(long waitBetweenTriesValue) {
		this.waitBetweenTriesValue = waitBetweenTriesValue;
	}
	
	/**
	 * Gets the units of the retry wait, which is the units component of the overall retry wait.
	 * @return the retry wait units as a TimeUnit or <code>null</code> if this step has no retry wait.
	 */
	protected final TimeUnit getWaitBetweenTriesUnits() {
		if (waitBetweenTriesValue < 0) return null;
		return waitBetweenTriesUnits;
	}
	
	/**
	 * Sets the retry wait units, which is the units component of the overall step retry wait.
	 * The parameter to this method may never be null; if you want to disable the retry wait, use
	 * <code>setWaitBetweenTriesValue(-1)</code> instead.
	 * @param waitBetweenTriesUnits the retry units as a TimeUnit; may never be <code>null</code>.
	 * @see Step#setWaitBetweenTriesValue(long)
	 * @throws IllegalArgumentException if the provided argument is <code>null</code>.
	 */
	protected final void setWaitBetweenTriesUnits(final TimeUnit waitBetweenTriesUnits) {
		Validate.notNull(waitBetweenTriesUnits, "The provided retry wait units may not be null.");
		this.waitBetweenTriesUnits = waitBetweenTriesUnits;
	}
	
	/**
	 * Determines if this step is optional.  Optional steps are run like any other steps,
	 * but if they fail to complete execution, either through timeout or an exceptional case,
	 * they do not cause the workflow to fail.  Steps are not optional by default.
	 * @return <code>true</code> if the step is optional, <code>false</code> otherwise.
	 */
	public boolean isOptional() {
		return optional;
	}

	/**
	 * Sets if this step is optional.  Optional steps are run like any other steps,
	 * but if they fail to complete execution, either through timeout or an exceptional case,
	 * they do not cause the workflow to fail.
	 * @param optional provide <code>true</code> if the step is optional, <code>false</code> if the step should cause the workflow to fail.
	 */
	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	/**
	 * Determines if this step is <i>always</i> run.  "Always run" steps are run like any other steps,
	 * even if the workflow has already failed, either through timeout or an exceptional case.
	 * Steps are not "always run" by default.
	 * @return <code>true</code> if the step should always be run, <code>false</code> otherwise.
	 */
	public boolean isAlwaysRun() {
		return alwaysRun;
	}

	/**
	 * Sets if this step should <i>always</i> be run.  "Always run" steps are run like any other steps,
	 * even if the workflow has already failed, either through timeout or an exceptional case.
	 */
	public void setAlwaysRun(boolean alwaysRun) {
		this.alwaysRun = alwaysRun;
	}

	/**
	 * Body of step execution logic.  Implementors must override this method and implement whatever
	 * the step has to do.  Implementors may feel free to let unchecked exceptions
	 * be thrown outside of this class, as the framework will pick them up and handle them properly
	 * for retries by ignoring them and, when the retries are exhausted, will throw the instance of the
	 * unchecked exception out of the body of the workflow's <code>execute()</code> call to let callers handle.
	 */
	protected abstract void execute() throws InterruptedException;
	
	/**
	 * Returns if this step has completed.
	 * @return <code>true</code> if the step has completed, <code>false</code> otherwise.
	 */
	public boolean isCompleted() {
		return completed;
	}
	
	/**
	 * Returns if this step was successful.
	 * @return <code>true</code> if the step did not time out or throw and exception, <code>false</code> otherwise.
	 */
	public boolean isSuccessful() {
		return failureCause.get() == null;
	}
	
	/**
	 * Returns if this step was skipped.  Steps are skipped when they are encountered after a previous step
	 * has failed and are not marked as 'always run'.
	 * @return <code>true</code> if this step was skipped, <code>false</code> otherwise.
	 */
	public boolean wasSkipped() {
		return skipped;
	}
	
	/**
	 * Returns how long this step ran or has been running, in milliseconds.  If the step is currently running, 
	 * this returns the current running time of this step; if the step has not started, -1 is returned;
	 * otherwise the total time taken to run the step is returned.  
	 * @return how long the step has run or ran, or -1 if the step has not started.
	 */
	public long getTimeRunning() {
		if (startTime == -1) {
			return -1;
		} else {
			if (completed) {
				return endTime - startTime;
			} else {
				return System.currentTimeMillis() - startTime;
			}
		}
	}
	
	/**
	 * Returns when this step was started.
	 * @return the time this step was started, in milliseconds since the computational epoch, or -1 if this step has not been started.
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * Returns when this step was ended.
	 * @return the time this step was ended, in milliseconds since the computational epoch, or -1 if this step has not been ended.
	 */
	public long getEndTime() {
		return endTime;
	}
	
	/**
	 * Gets the number of times this specific step instance has been tried, which is 
	 * equivalent to the number of times <code>execute()</code> has been called.
	 * @return the number of times <code>execute()</code> has been called.
	 */
	public int getTimesTried() {
		return tries;
	}

	/**
	 * Completes this step.  This marks it as completed, successful, and records the time this step ended.
	 */
	final void complete() {
		complete(null);
	}
	
	/**
	 * Completes this step.  This marks it as completed and records the time this step ended.
	 * @param failure the reason this step has failed; if <code>null</code> this step is marked as successful.
	 */
	final void complete(final Throwable failure) {
		this.endTime = System.currentTimeMillis();
		this.completed = true;
		this.failureCause.set(failure);
	}
	
	/**
	 * Marks this step as skipped.  This marks it as completed and recorded the time the step was skipped as both the start and end time.
	 */
	final void skip() {
		this.completed = true;
		this.skipped = true;
		this.startTime = System.currentTimeMillis();
		this.endTime = this.startTime;
	}
	
	/**
	 * Starts this step.  This marks it as not completed and records the time this step started.
	 */
	final void start() {
		this.completed = false;
		this.startTime = System.currentTimeMillis();
		this.tries++;
	}
}