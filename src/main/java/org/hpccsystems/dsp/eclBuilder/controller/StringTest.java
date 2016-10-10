package org.hpccsystems.dsp.eclBuilder.controller;

public class StringTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		System.out.println("Word#$#$% Word 1234".replaceAll("[^A-Za-z]+", ""));
		
		System.out.println("Word\n\n\n Word\n1234".replaceAll("[\n]+", "\n"));

	}

}
