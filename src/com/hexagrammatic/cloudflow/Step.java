package com.hexagrammatic.cloudflow;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

public abstract class Step extends Parameterized {

	private String name = getClass().getSimpleName();
	private Workflow workflow = null;
	private long timeout = -1;
	private TimeUnit timeoutUnits = TimeUnit.SECONDS;
	private int maxTries = 1;
	private long waitBetweenTries = -1;
	private TimeUnit waitBetweenTriesUnits = TimeUnit.SECONDS;

	public final String getName() {
		return name;
	}
	
	public final void setName(final String name) {
		Validate.isTrue(!StringUtils.isBlank(name), "The provided name may not be null or empty.");
		this.name = name;
	}

	final void setWorkflow(final Workflow workflow) {
		this.workflow = workflow;
	}
	
	protected final Workflow getWorkflow() {
		return workflow;
	}

	@Override
	protected Object getParameter(final String key, final Object defaultValue) {		
		Object result =  super.getParameter(key, defaultValue);
		if (result == defaultValue) {
			result = workflow.getParameter(key, defaultValue);
		}
		return result;
	}
	
	@Override	
	protected void addParameter(String key, Object value) {
		super.addParameter(key, value);
		if (workflow != null) {			
			workflow.addParameter(key, value);
		}
	}
	
	public final void setTimeout(final String timeout) {		
		final Object[] parsed = Utils.parseTimeTuple(timeout);
		setTimeout((Long)parsed[0]);
		setTimeoutUnits((TimeUnit)parsed[1]);
	}
	
	public final long getTimeout() {
		return timeout;
	}
		
	public final void setTimeout(final long timeout) {
		this.timeout = timeout;
	}
	
	public final TimeUnit getTimeoutUnits() {
		return timeoutUnits;
	}
	
	public final void setTimeoutUnits(final TimeUnit timeoutUnits) {
		Validate.notNull(timeoutUnits, "The provided timeout units may not be null.");
		this.timeoutUnits = timeoutUnits;
	}
	
	public final int getMaxTries() {
		return maxTries;
	}
	
	public final void setMaxTries(final int maxTries) {
		this.maxTries = maxTries;
	}

	public final void setWaitBetweenTries(final String wait) {
		final Object[] parsed = Utils.parseTimeTuple(wait);
		setWaitBetweenTries((Long)parsed[0]);
		setWaitBetweenTriesUnits((TimeUnit)parsed[1]);
	}
	
	public final long getWaitBetweenTries() {
		return waitBetweenTries;
	}
	
	public final void setWaitBetweenTries(long waitBetweenTries) {
		this.waitBetweenTries = waitBetweenTries;
	}
	
	public final TimeUnit getWaitBetweenTriesUnits() {
		return waitBetweenTriesUnits;
	}
	
	public final void setWaitBetweenTriesUnits(final TimeUnit waitBetweenTriesUnits) {
		Validate.notNull(waitBetweenTriesUnits, "The provided wait between retries units may not be null.");
		this.waitBetweenTriesUnits = waitBetweenTriesUnits;
	}
	
	public abstract void execute();
}