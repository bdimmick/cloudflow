package com.hexagrammatic.cloudflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;

/**
 * Base type for "paramaterized" types - those types that have properties about themselves that they use during the workflow.
 * This class differs from a <code>java.util.Properties</code>, <code>java.util.HashMap&lt;String,Object&gt;</code>, or similar
 * in the fact that it includes methods for 'snapshotting' and rolling back changes to the parameters, which is important when
 * striving to not have 'half-changes' propagated.
 * <p>
 * Note: This type provides very little to no thread-safety.  Please don't use it in multiple threads simultaneously. 
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12
 */
public abstract class Parameterized {
	//TODO: Refactor this class to make it thread-safe	
	private HashMap<String, LiteStack> values = new HashMap<String, LiteStack>();
    
    /**
     * Snapshots the current parameters.  
     * <p> 
     * Note: Implementors may override this method to put in hooks
     * for before or after snapshotting; such implementations <i>must</i> contain a call to 
     * <code>super.snapshot()</code>.
     */
    protected void snapshot() {
    	for (final Map.Entry<String, LiteStack> entry: values.entrySet()) {
    		final LiteStack current = entry.getValue();
    		final LiteStack next = new LiteStack(current);
    		next.setValue(current.getValue());
    		entry.setValue(next);
    	}
    }
    
    /**
     * Rolls back the current parameters to the previous snapshot.  If no previous snapshot exists, nothing happens.
     * <p>
     * Note: Implementors may override this method to put in hooks for before or after rolling back; 
     * such implementations <i>must</i> contain a call to <code>super.rollback()</code>.
     */
    protected void rollback() {
    	final ArrayList<String> removals = new ArrayList<String>();
    	for (final Map.Entry<String, LiteStack> entry: values.entrySet()) {
    		final LiteStack prev = entry.getValue().prev();
    		if (prev == null) {
    			removals.add(entry.getKey());
    		} else {
    			entry.getValue().setValue(null);
    			entry.setValue(prev);
    		}
    	}
    	for (final String removal: removals) {
    		values.remove(removal);
    	}
    }

    /**
     * Adds a parameter to this object.
     * @param key the parameter key - may not be <code>null</code>
     * @param value the parameter value = may be <code>null</code>
     */
	protected void addParameter(final String key, final Object value) {
		Validate.notNull(key, "The provided key may not be null.");
		LiteStack stack = values.get(key);
		if (stack == null) {
			stack = new LiteStack();
			values.put(key, stack);
		}
		stack.setValue(value);
	}
	
	/**
	 * Removes a parameter from this object.
	 * @param key the parameter key - may be <code>null</code>
	 */
	protected void removeParameter(final String key) {
		if (key == null) return;
		final LiteStack stack = values.get(key);
		if (stack != null) {
			stack.setValue(null);
		}		
	}
	
	/**
	 * Determines if this object has a specific parameter.
	 * @param key the paremeter key to check - may be <code>null</code>.
	 * @return <code>true</code> if this object contains a parameter with this key, <code>false</code> otherwise.
	 */
	protected boolean hasParameter(final String key) {
		if (key == null) return false;
		final LiteStack stack = values.get(key);
		if (stack == null) return false;
		if (stack.getValue() == null) return false;
		return true;
	}

	
	/**
	 * Get the value for a specific parameter.
	 * @param key the parameter key to find - may be <code>null</code>
	 * @return the value of the parameter or <code>null</code> if not found
	 */
	protected Object getParameter(final String key) {		
		return getParameter(key, null);
	}

	/**
	 * Get the value for a specific parameter.
	 * @param key the parameter key to find - may be <code>null</code>
	 * @param defaultValue the default value to return if the parameter is not found - may be <code>null</code>
	 * @return the value of the parameter or <code>defaultValue</code> if not found
	 */
	protected Object getParameter(final String key, final Object defaultValue) {
		if (key == null) return defaultValue;
		final LiteStack stack = values.get(key);
		return stack == null || stack.getValue() == null ? defaultValue : stack.getValue();
	}
	
	/**
	 * Determines the number of versions of a given parameter.
	 * @param key the parameter key to check - may be <code>null</code>
	 * @return the number of versions snapshotted
	 */
	protected int numVersions(final String key) {
		if (key == null) return 0;
		final LiteStack stack = values.get(key);
		if (stack == null) return 0;
		return stack.depth();
	}
	
	private static final class LiteStack {
		private Object value;
		private final LiteStack prev;
		
		public LiteStack() {
			this(null);
		}
		
		public LiteStack(final LiteStack prev) {
			this.prev = prev;
		}
	
		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public LiteStack prev() {
			return prev;
		}
		
		public int depth() {
			final int k = getValue() == null ? 0 : 1;
			if (prev == null) {
				return k;
			}
			return k + prev.depth();
		}
	}
}
