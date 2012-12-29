/**
 * 
 */
package com.hexagrammatic.cloudflow;

/**
 * @author Bill Dimmick <me@billdimmick.com>
 */
/**
 * Exception type thrown in the case that workflow creation fails.  Implementors of parsers may feel free to
 * use this exception to signal universal failure.
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12
 */
public class WorkflowCreationException extends Exception {
	private static final long serialVersionUID = -4095197911433761427L;

	public WorkflowCreationException(final String message, final Throwable cause) {
		super(message, cause);
	}
	
	public WorkflowCreationException(final Throwable cause) {
		super(cause);
	}
	
	public WorkflowCreationException(final String message) {
		super(message);
	}
}
