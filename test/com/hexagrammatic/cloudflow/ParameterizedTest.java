/**
 * 
 */
package com.hexagrammatic.cloudflow;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
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
	public void testMissingGet() {
		assertNull(target.getParameter("key"));
	}
	
	@Test
	public void testMissingGetWithDefault() {		
		final Object nv = new Object();
		assertEquals(nv, target.getParameter("key", nv));
	}
	
	@Test
	public void testNullGet() {
		assertNull(target.getParameter(null));
	}
	
	@Test
	public void testNullGetWithDefault() {		
		final Object nv = new Object();
		assertEquals(nv, target.getParameter(null, nv));
	}	
	
	@Test
	public void testBasicAdd() {
		final Object value = new Object();
		target.addParameter("key", value);
		assertTrue(target.hasParameter("key"));
		assertEquals(value, target.getParameter("key"));		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullKeyAdd() {
		target.addParameter(null, "something");
	}
	
	@Test
	public void testNullHas() {
		assertFalse(target.hasParameter(null));
	}
	
	@Test
	public void testRemoval() {
		final String key = "key";
		final Object value = new Object();
		target.addParameter(key, value);
		assertTrue(target.hasParameter(key));
		assertEquals(value, target.getParameter(key));
		assertEquals(1, target.numVersions(key));
		target.removeParameter(key);
		assertTrue(!target.hasParameter(key));
		assertNull(target.getParameter(key));
		assertEquals(0, target.numVersions(key));
		final Object defaultValue = new Object();
		assertEquals(defaultValue, target.getParameter(key, defaultValue));
	}
	
	@Test
	public void testSnapshotAndRollback() {
		final Object value1 = new Object();
		target.addParameter("key", value1);
		assertEquals(1, target.numVersions("key"));
		assertEquals(0, target.numVersions("null"));
		target.snapshot();
		
		assertEquals(2, target.numVersions("key"));
		assertEquals(0, target.numVersions("null"));
		
		final Object value2 = new Object();
		final Object value3 = new Object();
		target.addParameter("key", value2);
		target.addParameter("null", value3);

		assertEquals(value2, target.getParameter("key"));
		assertEquals(value3, target.getParameter("null"));
		assertEquals(2, target.numVersions("key"));
		assertEquals(1, target.numVersions("null"));

		target.rollback();
		
		assertEquals(value1, target.getParameter("key"));
		assertNull(target.getParameter("null"));
		assertEquals(1, target.numVersions("key"));
		assertEquals(0, target.numVersions("null"));
	}
	
}
