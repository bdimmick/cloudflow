package com.hexagrammatic.cloudflow;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.Validate;

public class Workflow extends Parameterized {
	private ArrayList<Step> steps = new ArrayList<Step>();
	private String name = null;
	private long timeout = -1;
	private TimeUnit timeoutUnits = TimeUnit.SECONDS;
	
	public final String getName() {
		return name;
	}

	public final void setName(final String name) {		
		this.name = name.trim();
		if (this.name.length()==0) this.name=null;
	}

	
	public final void setTimeout(final String timeout) {
		final Object[] parsed = Utils.parseTimeTuple(timeout);
		setTimeout((Long)parsed[0]);
		setTimeoutUnits((TimeUnit)parsed[1]);
	}
	
	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(final long timeout) {
		this.timeout = timeout;
	}

	public TimeUnit getTimeoutUnits() {
		return timeoutUnits;
	}

	public void setTimeoutUnits(final TimeUnit timeoutUnits) {
		Validate.notNull(timeoutUnits, "The provided timeout units may not be null.");
		this.timeoutUnits = timeoutUnits;
	}
	
	public void add(final Step e) {
		if (steps.add(e)) {
			e.setWorkflow(this);
		}
	}
	
	public final void execute() throws TimeoutException {
		final ExecutorService executor = Executors.newFixedThreadPool(2);
		final Callable<Void> call = new Callable<Void>() {			
			@Override
			public Void call() throws Exception {
				executeSteps(executor);
				return null;
			}
		};
		
		final Future<Void> result = executor.submit(call);
		
		try {
			if (getTimeout() > 0) {
				result.get(getTimeout(), getTimeoutUnits());
			} else {
				result.get();
			}
		} catch (TimeoutException te) {
			String message;
			if (getName() == null) {
				message = String.format("Execution of workflow timed out after %s", timevalueString(getTimeout(), getTimeoutUnits()));
			} else {
				message = String.format("Execution of workflow '%s' timed out after %s", getName(), timevalueString(getTimeout(), getTimeoutUnits()));
			}
			throw new TimeoutException(message);
		} catch (ExecutionException ee) {
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
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}
	
	private final void executeSteps(final ExecutorService executor) throws TimeoutException, ExecutionException, InterruptedException {
		for (final Step step: steps) {
			int trynum = 0;
			step.snapshot();
			this.snapshot();
			while (trynum < step.getMaxTries()) {
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
					if (step.getTimeout() > 0) {
						result.get(step.getTimeout(), step.getTimeoutUnits());
					} else {
						result.get();
					}
					return;
				} catch (TimeoutException te) {
					result.cancel(true);
					if (trynum == step.getMaxTries()) {
						throw new TimeoutException(String.format("Execution of workflow step '%s' timed out after %s", step.getName(), 
																	timevalueString(step.getTimeout(), step.getTimeoutUnits())));
					}
				} catch (ExecutionException ee) {
					if (trynum == step.getMaxTries()) throw ee;
				}				
				retryWait(step);
				step.rollback();
				this.rollback();
			}
		}
	}
	
	protected void retryWait(final Step step) throws InterruptedException {
		if (step.getWaitBetweenTries() > 0) {
			step.getWaitBetweenTriesUnits().sleep(step.getWaitBetweenTries());
		}		
	}
	
	final String timevalueString(final long t, final TimeUnit u) {
		if (t < 1) return "none";
		final StringBuilder builder = new StringBuilder();
		builder.append(t);
		builder.append(" ");
		builder.append(u.toString().toLowerCase());
		if (t == 1) {
			return builder.substring(0, builder.length()-1);
		} else {
			return builder.toString();
		}
	}
}
