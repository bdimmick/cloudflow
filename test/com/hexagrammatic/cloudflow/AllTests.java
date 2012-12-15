package com.hexagrammatic.cloudflow;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	ParameterizedTest.class,
	StepTest.class,
	WorkflowTest.class
})
public class AllTests {}
