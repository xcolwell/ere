package org.resisttheb.ere.ui;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpMethodParams;

public class HttpUtilities {
	private static HttpClient httpClient;
	
	static {
		httpClient = new HttpClient();
		httpClient.setHttpConnectionManager(
				new MultiThreadedHttpConnectionManager());
		
//		disableLogging();
	}
	
	private static void disableLogging() {
		System.setProperty("org.apache.commons.logging.Log", 
		"org.apache.commons.logging.impl.SimpleLog");
		System.setProperty("org.apache.commons.logging.simplelog.showdatetime",
		"true");
		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient",
		"error");
	}
	
	
	public static HttpClient getClient() {
		return httpClient;
	}
	
	
	private HttpUtilities() {
	}
}
