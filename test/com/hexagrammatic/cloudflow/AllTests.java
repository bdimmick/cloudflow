package com.hexagrammatic.cloudflow;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Bill Dimmick <me@billdimmick.com>
 * @since 2012.12
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	UtilsTest.class,
	ParameterizedTest.class,
	StepTest.class,
	WorkflowTest.class,
	JsonParserTest.class
})
public class AllTests {}
