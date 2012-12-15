/**
 * 
 */
package com.hexagrammatic.cloudflow;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the <code>com.hexagrammatic.cloudflow.Parameterized</code> type.
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12
 */
public class ParameterizedTest {

	private Parameterized target;
	
	@Before
	public void setUp() throws Exception {
		target = new Parameterized() {};
	}

	@Test
	public void testBasicGet() {
		assertNull(target.getParameter("key"));
		
		final Object nv = new Object();
		assertEquals(nv, target.getParameter("key", nv));
	}
	
	@Test
	public void testBasicAdd() {
		final Object value = new Object();
		target.addParameter("key", value);
		assertEquals(value, target.getParameter("key"));		
	}

	@Test
	public void testSnapshotAndRollback() {
		final Object value1 = new Object();
		target.addParameter("key", value1);
		target.snapshot();
		
		final Object value2 = new Object();
		final Object value3 = new Object();
		target.addParameter("key", value2);
		target.addParameter("null", value3);
		
		assertEquals(value2, target.getParameter("key"));
		assertEquals(value3, target.getParameter("null"));

		target.rollback();
		
		assertEquals(value1, target.getParameter("key"));
		assertNull(target.getParameter("null"));
	}
}
