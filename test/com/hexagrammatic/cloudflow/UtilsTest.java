/**
 * 
 */
package com.hexagrammatic.cloudflow;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12
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
	
	@Test
	public void testCreateTimeTupleSingular() {
		assertEquals("1 second", Utils.createTimeTuple(1, SECONDS));
	}

	@Test
	public void testCreateTimeTuplePlural() {
		assertEquals("2 seconds", Utils.createTimeTuple(2, SECONDS));
	}

	@Test
	public void testCreateTimeTupleNone() {
		assertEquals("Never", Utils.createTimeTuple(-1, null));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateTimeTupleNullUnitsPositiveValue() {
		Utils.createTimeTuple(1, null);
	}
}
