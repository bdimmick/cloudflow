package com.hexagrammatic.cloudflow;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.Validate;

/**
 * Basic utility class.
 * @author Bill Dimmick <me@billdimmick.com>
 */
public class Utils {
	
	/**
	 * Provides the ability to convert a string, such as "1 SECOND" or "5 SECONDS", to a tuple consisting
	 * of a Long for the first value and a TimeUnit for the second value.  The value to convert must consist of
	 * the following:
	 * <OL>
	 * <LI>Any amount of whitespace at the head of the string
	 * <LI>A numeric value as a string
	 * <LI>Any amount of whitespace in the middle of the string
	 * <LI>An optional timeunit value
	 * <LI>Any amount of whitespace at the end of the string
	 * </OL>
	 * If the timeunit is omitted, it defaults to SECONDS.
	 * @param input the value to convert - may not be null
	 * @return a tuple containing a Long and a TimeUnit.
	 */
	final static Object[] parseTimeTuple(final String input) {
		Validate.notNull(input, "The provided time period string may not be null.");
		final String [] parts = input.toUpperCase().trim().split("\\s+", 2);
		final Object[] results = new Object[2]; 
		try {			
			results[0] = Long.parseLong(parts[0]);			
			if (parts.length > 1) {
				if (parts[1].endsWith("S")) {
					results[1] = TimeUnit.valueOf(parts[1]);
				} else {
					results[1] = TimeUnit.valueOf(parts[1]+'S');
				}
			} else {
				results[1] = TimeUnit.SECONDS;
			}
			return results;
		} catch (final Exception e) {
			throw new IllegalArgumentException(String.format("Time period value '%s' is not in the format 'LONG TIMEUNIT'."));
		}

	}

}
