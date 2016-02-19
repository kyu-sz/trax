package org.trax;

import java.util.Hashtable;
import java.util.Map;

public class TraxStatus {

	private Hashtable<String, String> parameters = new Hashtable<String, String>();

	private TraxRegion region;
	
	public TraxStatus(TraxRegion region, Map<String, String> parameters) {
		this.region = region;
		this.parameters.putAll(parameters);
	}

	public Map<String, String> getParameters() {
		
		return parameters;
		
	}
	
	public TraxRegion getRegion() {
		return region;
	}
	
}
