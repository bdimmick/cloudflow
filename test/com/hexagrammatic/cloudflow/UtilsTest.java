/**
 * 
 */
package com.hexagrammatic.cloudflow;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for various utilities.
 * 
 * @author Bill Dimmick <me@billdimmick.com>
 */
public class UtilsTest {

	@Test(expected=IllegalArgumentException.class)
	public void testNullInput() {
		Utils.parseTimeTuple(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testEmptyStringInput() {
		Utils.parseTimeTuple("");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testBlankStringInput() {
		Utils.parseTimeTuple("   ");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNonNumericStringInput() {
		Utils.parseTimeTuple("OINK");
	}	
	
	@Test
	public void testNoUnitSupplied() {
		final long timeValue = 1;
		final Object [] values = Utils.parseTimeTuple(Long.toString(timeValue));

		assertEquals(timeValue, values[0]);		
		assertEquals(SECONDS, values[1]);
	}
	
	@Test
	public void testUnitSuppliedNonPlural() {
		Object [] values = Utils.parseTimeTuple("5 HOUR");
		assertEquals(5L, values[0]);		
		assertEquals(HOURS, values[1]);
		
		values = Utils.parseTimeTuple("1 NANOSECOND");
		assertEquals(1L, values[0]);		
		assertEquals(NANOSECONDS, values[1]);
	}
	
	@Test
	public void testUnitSuppliedPlural() {
		Object [] values = Utils.parseTimeTuple("5 HOURS");
		assertEquals(5L, values[0]);		
		assertEquals(HOURS, values[1]);
		
		values = Utils.parseTimeTuple("1 NANOSECONDS");
		assertEquals(1L, values[0]);		
		assertEquals(NANOSECONDS, values[1]);
	}
	
}
