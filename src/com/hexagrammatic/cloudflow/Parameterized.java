package com.hexagrammatic.cloudflow;

import java.util.HashMap;

/**
 * Base type for "paramaterized" types - those types that have properties about themselves that they use during the workflow.
 * This class differs from a <code>java.util.Properties</code>, <code>java.util.HashMap&lt;String,Object&gt;</code>, or similar
 * in the fact that it includes methods for 'snapshotting' and rolling back changes to the parameters, which is important when
 * striving to not have 'half-changes' propagated.
 *   
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12
 */
public abstract class Parameterized {

	private HashMap<String, Object> current = new HashMap<String, Object>();
    private HashMap<String, Object> snapshot = null;
    
    protected void snapshot() {
    	snapshot = new HashMap<String, Object>();
    	snapshot.putAll(current);
    }
    
    protected void rollback() {	    	
    	current.clear();
    	if (snapshot!=null) {
    		current.putAll(snapshot);
    	}
    }

	protected void addParameter(final String key, final Object value) {
		current.put(key, value);
	}
	
	protected boolean hasParameter(final String key) {
		return current.containsKey(key);
	}

	protected final Object getParameter(final String key) {
		return getParameter(key, null);
	}
	
	protected Object getParameter(final String key, final Object defaultValue) {
		final Object result = current.get(key);
		return result == null ? defaultValue : result;
	}
}
