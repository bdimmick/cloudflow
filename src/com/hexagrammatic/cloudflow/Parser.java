package com.hexagrammatic.cloudflow;

public class Parser {

	private ClassLoader classLoader = Parser.class.getClassLoader();
	
	public void setClassLoader(final ClassLoader cl) {
		this.classLoader = cl;
	}
	
}
