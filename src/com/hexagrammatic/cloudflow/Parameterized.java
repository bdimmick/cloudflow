package com.hexagrammatic.cloudflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.Validate;

/**
 * Base type for "paramaterized" types - those types that have properties about themselves that they use during the workflow.
 * This class differs from a <code>java.util.Properties</code>, <code>java.util.HashMap&lt;String,Object&gt;</code>, or similar
 * in the fact that it includes methods for 'snapshotting' and rolling back changes to the parameters, which is important when
 * striving to not have 'half-changes' propagated.
 * <p>
 * Note: This type provides very crude thread-safety using read-write locks.  This is probably overkill, and implementors may 
 * coose to abandon thread-safety if they can ensure that only one thread will be accessing an object of this type at a time.
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12
 */
public abstract class Parameterized {
	private HashMap<String, LiteLinkedList> values = new HashMap<String, LiteLinkedList>();
	private ReadWriteLock rwLock = new ReentrantReadWriteLock();
	
    /**
     * Snapshots the current parameters.  
     * <p> 
     * Note: Implementors may override this method to put in hooks
     * for before or after snapshotting; such implementations <i>must</i> contain a call to 
     * <code>super.snapshot()</code>.
     */
	public void snapshot() {
    	rwLock.writeLock().lock();
    	try {
	    	for (final Map.Entry<String, LiteLinkedList> entry: values.entrySet()) {
	    		final LiteLinkedList current = entry.getValue();
	    		final LiteLinkedList next = new LiteLinkedList(current);
	    		next.setValue(current.getValue());
	    		entry.setValue(next);
	    	}
    	} finally {
    		rwLock.writeLock().unlock();
    	}
    }
    
    /**
     * Rolls back the current parameters to the previous snapshot.  If no previous snapshot exists, nothing happens.
     * <p>
     * Note: Implementors may override this method to put in hooks for before or after rolling back; 
     * such implementations <i>must</i> contain a call to <code>super.rollback()</code>.
     */
	public void rollback() {
    	rwLock.writeLock().lock();
    	try {
	    	final ArrayList<String> removals = new ArrayList<String>();
	    	for (final Map.Entry<String, LiteLinkedList> entry: values.entrySet()) {
	    		final LiteLinkedList prev = entry.getValue().next();
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
    	} finally {
    		rwLock.writeLock().unlock();
    	}
    }

    /**
     * Adds a parameter to this object.
     * @param key the parameter key - may not be <code>null</code>
     * @param value the parameter value = may be <code>null</code>
     */
	public void addParameter(final String key, final Object value) {
		Validate.notNull(key, "The provided key may not be null.");
		rwLock.writeLock().lock();
		try {
			LiteLinkedList stack = values.get(key);
			if (stack == null) {
				stack = new LiteLinkedList();
				values.put(key, stack);
			}
			stack.setValue(value);
		} finally {
			rwLock.writeLock().unlock();
		}
	}
	
	/**
	 * Removes a parameter from this object.
	 * @param key the parameter key - may be <code>null</code>
	 */
	public void removeParameter(final String key) {
		if (key == null) return;
		rwLock.writeLock().lock();
		try {
			final LiteLinkedList stack = values.get(key);
			if (stack != null) {
				stack.setValue(null);
			}
		} finally {
			rwLock.writeLock().unlock();
		}
	}
	
	/**
	 * Determines if this object has a specific parameter.
	 * @param key the paremeter key to check - may be <code>null</code>.
	 * @return <code>true</code> if this object contains a parameter with this key, <code>false</code> otherwise.
	 */
	public boolean hasParameter(final String key) {
		if (key == null) return false;
		rwLock.readLock().lock();
		try {
			final LiteLinkedList stack = values.get(key);
			if (stack == null) return false;
			if (stack.getValue() == null) return false;
			return true;
		} finally {
			rwLock.readLock().unlock();
		}
	}

	
	/**
	 * Get the value for a specific parameter.
	 * @param key the parameter key to find - may be <code>null</code>
	 * @return the value of the parameter or <code>null</code> if not found
	 */
	public Object getParameter(final String key) {		
		return getParameter(key, null);
	}

	/**
	 * Get the value for a specific parameter.
	 * @param key the parameter key to find - may be <code>null</code>
	 * @param defaultValue the default value to return if the parameter is not found - may be <code>null</code>
	 * @return the value of the parameter or <code>defaultValue</code> if not found
	 */
	public Object getParameter(final String key, final Object defaultValue) {		
		if (key == null) return defaultValue;
		rwLock.readLock().lock();
		try {
			final LiteLinkedList stack = values.get(key);
			return stack == null || stack.getValue() == null ? defaultValue : stack.getValue();
		} finally {
			rwLock.readLock().unlock();
		}
	}
	
	/**
	 * Determines the number of versions of a given parameter.
	 * @param key the parameter key to check - may be <code>null</code>
	 * @return the number of versions snapshotted
	 */
	public int numVersions(final String key) {
		if (key == null) return 0;
		rwLock.readLock().lock();
		try {
			final LiteLinkedList stack = values.get(key);
			if (stack == null) return 0;
			return stack.length();
		} finally {
			rwLock.readLock().unlock();
		}
	}
	
	private static final class LiteLinkedList {
		private Object value;
		private final LiteLinkedList next;
		
		public LiteLinkedList() {
			this(null);
		}
		
		public LiteLinkedList(final LiteLinkedList prev) {
			this.next = prev;
		}
	
		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public LiteLinkedList next() {
			return next;
		}
		
		public int length() {
			final int k = getValue() == null ? 0 : 1;
			if (next == null) {
				return k;
			}
			return k + next.length();
		}
	}
}
