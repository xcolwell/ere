package org.resisttheb.ere.test;

import org.resisttheb.ere.ui.JsonCall;
import org.resisttheb.ere.ui.JsonCall.Callback;

public class JsonCallTest {

	public static void main(final String[] in) {
		JsonCall.call(
				new Callback() {
					public void run(final Object jsonObj) {
						System.out.println(jsonObj);
					}
				},
//				"http://resisttheb.org/ere/core/create_person.php"
				"http://resisttheb.org/ere/core/alloc_prompts.php",
				"pid", 33,
				"p_key", "ucMCbcS8FPgxtFHj8eG9sp5ykvx8xG7q",
				"p_offs", "[[1],[2],[3]]"
				
		);
		
	}
	
	
}
