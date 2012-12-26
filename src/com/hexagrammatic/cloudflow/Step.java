package com.hexagrammatic.cloudflow;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * 
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12 
 */
public abstract class Step extends Parameterized {	
	//TODO: Enable 'skip on failure' mode
	private String name = getClass().getSimpleName();
	private Workflow workflow = null;
	private long timeout = -1;
	private TimeUnit timeoutUnits = TimeUnit.SECONDS;
	private int maxTries = 1;
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
	 * @param workflow the workflow - may not be null
	 */
	protected final void setWorkflow(final Workflow workflow) {
		Validate.isTrue(workflow==null || this.workflow==null,"Attempting to assign a workflow to a step that already has a workflow assigned.");		
		this.workflow = workflow;
	}
	
	protected final Workflow getWorkflow() {
		return workflow;
	}

	@Override
	protected Object getParameter(final String key, final Object defaultValue) {		
		Object result =  super.getParameter(key, defaultValue);
		if (result == defaultValue && workflow!=null) {
			result = workflow.getParameter(key, defaultValue);
		}
		return result;
	}
	
	@Override	
	protected void addParameter(final String key, final Object value) {
		super.addParameter(key, value);
		if (workflow != null) {			
			workflow.addParameter(key, value);
		}
	}
	
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
	
	@Override
	protected void snapshot() {
		super.snapshot();
		if (workflow!=null) workflow.snapshot();
	}
	
	@Override
	protected void rollback() {		
		super.rollback();
		if (workflow!=null) workflow.rollback();
	}
	
	@Override
	protected void removeParameter(final String key) {
		super.removeParameter(key);
		if (workflow!=null) workflow.removeParameter(key);
	}
	
	public final void setTimeout(final String timeout) {		
		final Object[] parsed = Utils.parseTimeTuple(timeout);
		setTimeout((Long)parsed[0]);
		setTimeoutUnits((TimeUnit)parsed[1]);
	}
	
	protected final long getTimeout() {
		return timeout;
	}
		
	protected final void setTimeout(final long timeout) {
		this.timeout = timeout;
	}
	
	protected final TimeUnit getTimeoutUnits() {
		return timeoutUnits;
	}
	
	protected final void setTimeoutUnits(final TimeUnit timeoutUnits) {
		Validate.notNull(timeoutUnits, "The provided timeout units may not be null.");
		this.timeoutUnits = timeoutUnits;
	}
	
	protected final int getMaxTries() {
		return maxTries;
	}
	
	protected final void setMaxTries(final int maxTries) {
		this.maxTries = maxTries;
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
	
	protected abstract void execute();
}