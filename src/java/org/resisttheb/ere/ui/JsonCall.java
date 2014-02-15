package org.resisttheb.ere.ui;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jdesktop.swingworker.SwingWorker;
import org.myjson.common.ParserException;
import org.myjson.json.Parser;
import org.myjson.json.Serializer;



// makes a call to a JSON resource,
// reuturns java values
// the call can either be a get or a post
// with the given parameter map
public class JsonCall extends SwingWorker<Object, Void> {
	/**************************
	 * CONVENIENCE
	 **************************/
	
	public static void call(final Callback callback, final String url, 
			final Object ... params) {
		post(callback, url, params);
	}
	
	public static Object inlineCall(final String url, 
			final Object ... params) throws IOException {
		return inlinePost(url, params);
	}
	
	public static void post(final Callback callback, final String url, 
			final Object ... params) {
		final JsonCall call = new JsonCall(callback, url, params);
		call.setMethod(Method.POST);
		call.execute();
	}
	
	public static Object inlinePost(final String url, 
			final Object ... params) throws IOException {
		final JsonCall call = new JsonCall(null, url, params);
		call.setMethod(Method.POST);
		return call.inline();
	}
	
	public static void get(final Callback callback, final String url, 
			final Object ... params) {
		final JsonCall call = new JsonCall(callback, url, params);
		call.setMethod(Method.GET);
		call.execute();
	}
	
	public static Object inlineGet(final String url, 
			final Object ... params) throws IOException {
		final JsonCall call = new JsonCall(null, url, params);
		call.setMethod(Method.GET);
		return call.inline();
	}
	
	/**************************
	 * END CONVENIENCE
	 **************************/
	
	
	/**************************
	 * UTILITY FUNCTIONS
	 **************************/
	
	private static String toParam(final Object obj) {
		if (null == obj) {
			return "";
		}
		if (obj instanceof String || obj instanceof Number) {
			return String.valueOf(obj);
		}
		// JSON:
		final Serializer s = new Serializer();
		s.serialize(obj);
		return s.getResult();
	}
	
	/**************************
	 * END UTILITY FUNCTIONS
	 **************************/
	
	
	
	public static enum Method {
		POST,
		GET
	}
	
	
	public static interface Callback {
		public void run(final Object jsonObj);
	}
	
	
	private Method method 				= Method.POST;
	private String url 					= "";
	private Map<String, Object> params 	= Collections.<String, Object>emptyMap();
	private Callback callback 			= null;
	
	
	public JsonCall() {
	}
	
	public JsonCall(final Callback _callback, final String _url, 
			final Object ... _params
	) {
		setCallback(_callback);
		setUrl(_url);
		setParameters(_params);
	}
	
	
	public void setCallback(final Callback _callback) {
		this.callback = _callback;
	}
	
	public void setUrl(final String _url) {
		this.url = _url;
	}
	
	public void setMethod(final Method _method) {
		this.method = _method;
	}
	
	public void setParameters(Map<String, Object> _params) {
		this.params = new HashMap<String, Object>(_params);
	}
	
	public void setParameters(final Object ... _params) {
		params = new HashMap<String, Object>(_params.length / 2);
		for (int i = 0; i + 1 < _params.length; i += 2) {
			params.put(String.valueOf(_params[i]), _params[i + 1]);
		}
	}
	
	
	private HttpMethod createPost() {
		PostMethod post = new PostMethod(url);
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			post.addParameter(entry.getKey(), 
					toParam(entry.getValue()));
		}
		return post;
	}
	
	private HttpMethod createGet() {
		StringBuffer buffer = new StringBuffer(url);
		buffer.append("?");
		
		boolean first = true;
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			if (! first) {
				buffer.append("&");
			}
			
			buffer.append(URLEncoder.encode(entry.getKey()));
			buffer.append("=");
			buffer.append(URLEncoder.encode(toParam(entry.getValue())));
			
			first = false;
		}
		
		GetMethod get = new GetMethod(buffer.toString());
		return get;
	}
	
	
	
	public Object inline() throws IOException {
		final HttpMethod hm = Method.POST == method
			? createPost()
			: createGet();

			
			// Turn off this warning ...
			// we know this sucks, but our current API needs a string
//			hm.getParams().setParameter(HttpMethodParams.BUFFER_WARN_TRIGGER_LIMIT, 
//					new Integer(1024 * 1024));
			
		if (Ere.DEBUG) {
			System.out.println(hm.getURI());
		}
		
		final HttpClient client = HttpUtilities.getClient();
		client.executeMethod(hm);
	//	final String response = hm.getResponseBodyAsString();
		
		Object jsonObj = null;
	
		final int MAX_RESPONSE_LENGTH = 1024 * 1024;
		int cl = -1;
		final Header clh = hm.getResponseHeader("Content-Length");
		if (null != clh) {
			try {
				cl = Integer.parseInt(clh.getValue());
			}
			catch (NumberFormatException e) {
				// Ignore
			}
		}
		
		final StringBuffer buffer = new StringBuffer(cl < 0 ? 1024 : cl);
		final Reader r = new InputStreamReader(hm.getResponseBodyAsStream());
		try {
			final char[] b = new char[1024];
			for (int rl; 0 < (rl = r.read(b, 0, b.length));) {
				buffer.append(b, 0, rl);
				if (MAX_RESPONSE_LENGTH <= buffer.length()) {
					throw new IllegalStateException("The server is giving back too much data. Something is wrong. " + 
							(MAX_RESPONSE_LENGTH / 1024) + "k cap");
				}
			}
		}
		finally {
			r.close();
		}
		//final String response = hm.getResponseBodyAsString();
		final String response = buffer.toString();
		
		hm.releaseConnection();
		
		if (Ere.DEBUG) {
		System.out.println(response);
		}
		
		final Parser parser = new Parser();
		parser.setInput(response);
		try {
			jsonObj = parser.parse();
		}
		catch (ParserException e) {
			final Map<Object, Object> fail = new HashMap<Object, Object>(2);
			fail.put("success", 0);
			fail.put("reason", "--at the client--");
			jsonObj = fail;
		}
		/*
		final InputStream is = hm.getResponseBodyAsStream();
		try {
			final Parser parser = new Parser();
			parser.setInput(is);
			jsonObj = parser.parse();
		}
		finally {
			is.close();
		}
		*/
		
		return jsonObj;
	}
	
	
	/**************************
	 * SWINGWORKER OVERRIDES
	 **************************/
	
	@Override
	protected Object doInBackground() throws Exception {
		// 1. make http call
		// 2. get result
		// 3. parse json and return
		
		
		return inline();
	}
	
	@Override
	protected void done() {
		if (null != callback) {
			try {
				callback.run(get());
			}
			catch (ExecutionException e) {
				throw new RuntimeException("Could not get results.", e);
			}
			catch (InterruptedException e) {
				throw new RuntimeException("Could not get results.", e);
			}
		}
	}
	
	/**************************
	 * END SWINGWORKER OVERRIDES
	 **************************/
}
