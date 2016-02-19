package org.trax;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TraxTests {

	public static void main(String[] args) throws IOException {
		
		if (args.length < 1) return;
		
		if (args[0].compareToIgnoreCase("parsing") == 0) {
			
			if (args.length < 2) return;
			
			BufferedReader reader = new BufferedReader(new FileReader(new File(args[1])));
			
			TraxMessage.testParsing(reader);
			
		}
		
		
	}
	
}
