package com.hexagrammatic.cloudflow;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * 
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12 
 * @see Workflow
 */
public abstract class Step extends Parameterized {	
	//TODO: Enable 'skip on failure' mode
	private String name = getClass().getSimpleName();
	private Workflow workflow = null;
	private long timeout = -1;
	private TimeUnit timeoutUnits = TimeUnit.SECONDS;
	private int maxRetries = 0;
	private long waitBetweenTries = -1;
	private TimeUnit waitBetweenTriesUnits = TimeUnit.SECONDS;

	/**
	 * Gets the name of this step.
	 * @return the name of the step; if unset, the 'simple name' of the class is used
	 */
	protected final String getName() {
		return name;
	}
	
	/**
	 * Sets the name of the step
	 * @param name the name of the step - may not be blank or empty
	 */
	protected final void setName(final String name) {
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
	protected Object getParameter(final String key, final Object defaultValue) {		
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
	protected void addParameter(final String key, final Object value) {
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
	protected boolean hasParameter(final String key) {
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
	protected void snapshot() {
		super.snapshot();
		if (workflow!=null) workflow.snapshot();
	}

	/**
	 * Rolls back the parameters in this step instance to a previous snapshot.  If the step is 'owned' by a workflow,
	 * the workflow are also rolled back.
	 */
	@Override
	protected void rollback() {		
		super.rollback();
		if (workflow!=null) workflow.rollback();
	}
	
	/**
	 * Removes a parameter from this step instance.  If the step is 'owned' by a workflow,
	 * the parameter is also removed from the workflow.
	 */
	@Override
	protected void removeParameter(final String key) {
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
		return timeout;
	}
		
	/**
	 * Sets the timeout value, which is the numeric component of the overall step timeout.
	 * Providing a negative value indicates that the step should never timeout and steps
	 * that never time out will always have a timeout value of -1.
	 * @param timeout the timeout value (See above for special casing about negative values.) 
	 */
	protected void setTimeoutValue(final long timeout) {
		this.timeout = timeout;
		if (this.timeout < -1) this.timeout = -1; 

	}
	
	/**
	 * Gets the timeout units, which is the units component of the overall step timeout.
	 * @return the timeout units as a TimeUnit or <code>null</code> if this step has no timeout.
	 */
	protected final TimeUnit getTimeoutUnits() {
		if (timeout < 0) return null;
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
	 * to less than 1, this step is never retried in workflow execution.  If unset, the default value
	 * is 0.
	 * @param maxRetries the number of times to try to execute this step - if less than 1, the step is never retried 
	 */
	protected final void setMaxRetries(final int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public final void setWaitBetweenTries(final String wait) {
		final Object[] parsed = Utils.parseTimeTuple(wait);
		setWaitBetweenTries((Long)parsed[0]);
		setWaitBetweenTriesUnits((TimeUnit)parsed[1]);
	}
	
	protected final long getWaitBetweenTries() {
		return waitBetweenTries;
	}
	
	protected final void setWaitBetweenTries(long waitBetweenTries) {
		this.waitBetweenTries = waitBetweenTries;
	}
	
	protected final TimeUnit getWaitBetweenTriesUnits() {
		return waitBetweenTriesUnits;
	}
	
	protected final void setWaitBetweenTriesUnits(final TimeUnit waitBetweenTriesUnits) {
		Validate.notNull(waitBetweenTriesUnits, "The provided wait between retries units may not be null.");
		this.waitBetweenTriesUnits = waitBetweenTriesUnits;
	}
	
	/**
	 * Body of step execution logic.  Implementors must override this method and implement whatever
	 * the step has to do.  Implementors may feel free to let unchecked exceptions
	 * be thrown outside of this class, as the framework will pick them up and handle them properly
	 * for retries by ignoring them and, when the retries are exhausted, will throw the instance of the
	 * unchecked exception out of the body of the workflow's <code>execute()</code> call to let callers handle.
	 */
	protected abstract void execute();
}