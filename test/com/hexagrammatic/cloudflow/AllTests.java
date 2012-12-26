package com.hexagrammatic.cloudflow;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	UtilsTest.class,
	ParameterizedTest.class,
	StepTest.class,
	WorkflowTest.class,
	ParserTest.class
})
public class AllTests {}
