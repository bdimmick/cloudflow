package com.hexagrammatic.cloudflow;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.Validate;

/**
 * Utility class.  Provides functions used by various classes.
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12
 */
public class Utils {
	
	/**
	 * Provides the ability to convert a String, such as "1 SECOND" or "5 SECONDS", to a tuple consisting
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
		//TODO: SUPPORT 'NEVER', 'FOREVER', AND 'NONE' AS A VALUE
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
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException(String.format("Time period value '%s' is not in the format 'LONG TIMEUNIT'.", input));
		}
	}

	
	/**
	 * Creates a time tuple string out of the provided parameters.
	 * @param timeValue the provided time value - may be zero or negative
	 * @param timeUnit the provided time units - may not be null if timeValue is positive.
	 * @return "None" if timeValue is not positive; otherwise the two arguments are concatenated together to produce the result.
	 */
	final static String createTimeTuple(final long timeValue, final TimeUnit timeUnit) {
		if (timeValue < 1) return "Never";
		Validate.notNull(timeUnit, "The provided time units may not be null if the time value is positive.");
		final StringBuilder builder = new StringBuilder();
		builder.append(timeValue);
		builder.append(" ");
		builder.append(timeUnit.toString().toLowerCase());
		if (timeValue == 1) {
			return builder.substring(0, builder.length()-1);
		} else {
			return builder.toString();
		}
	}

}
