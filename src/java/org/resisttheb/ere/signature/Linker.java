package org.resisttheb.ere.signature;


// takes in a bunch of InputFrame
// each InputFrame is a blob graph + signatures for each blob
//
//  create output frames,
//  where each frame is a list of input ids that are the closest matches
//  for each blob id
public class Linker {
	
	private static String COLOR(final Signature sig) {
		if (sig.count <= 0) return "NONE";
		
		final int b = sig.colors[0] & 0xFF;
		final int g = (sig.colors[0] >> 8) & 0xFF;
		final int r = (sig.colors[0] >> 16) & 0xFF;
		
		return "(" + r + ", " + g + ", " + b + ")";
	}
	
	/**************************
	 * STRUCTURES
	 **************************/
	
	public static final class InputFrame {
		public Signature sig;
		public Signature[] sigs;
		
		public InputFrame(final Signature _sig, final Signature[] _sigs) {
			this.sig = _sig;
			this.sigs = _sigs;
		}
	}
	
	public static final class OutputFrame {
		// indexed in the input array
		public int[] closestInputs;
		
		public OutputFrame(final int n) {
			closestInputs = new int[n];
		}
	}
	
	/**************************
	 * END STRUCTURES
	 **************************/
	
	
	private boolean allowSelfLink 	= false;
	private SignatureMetric metric 	= new WeightedInverseMetric();
	
	// DATA
	private InputFrame[] inputs = new InputFrame[0];
	private OutputFrame[] outputs;
	// ::
	
	
	public Linker() {
	}
	
	
	/**************************
	 * CONFIGURATION
	 **************************/
	
	public void setAllowSelfLinks(final boolean _allowSelfLinks) {
		this.allowSelfLink = _allowSelfLinks;
	}
	
	public void setMetric(final SignatureMetric _metric) {
		this.metric = _metric;
	}
	
	/**************************
	 * END CONFIGURATION
	 **************************/
	
	
	public void setInput(final InputFrame[] _inputs) {
		this.inputs = _inputs;
	}
	
	public OutputFrame[] getOutput() {
		return outputs;
	}
	
	
	// links everything together
	// this is done in quadratic time
	// so it could be crazy
	// it depends on the metric, though
	//
	// weightedinverse is fast ...
	//  others may not be
	public void link() {
		final int n = inputs.length;
	
		
		// Reset output frames:
		outputs = new OutputFrame[n];
		for (int i = 0; i < outputs.length; ++i) {
			outputs[i] = new OutputFrame(inputs[i].sigs.length);
		}
		
		
		for (int i = 0; i < n; ++i) {
			final InputFrame input = inputs[i];
			final OutputFrame output = outputs[i];
			
			for (int j = 0; j < input.sigs.length; ++j) {
				final Signature sig = input.sigs[j];
				
				if (sig.count <= 0) {
					output.closestInputs[j] = -1;
					continue;
				}
				
				// Find the closest:
				float mind = Float.MAX_VALUE;
				int mink = -1;
				for (int k = 0; k < n; ++k) {
					if (!allowSelfLink && i == k)
						continue;
					if (inputs[k].sig.count <= 0)
						continue;
					
					final float d = metric.distance(sig, inputs[k].sig);
//					System.out.println(COLOR(sig) + "::" + COLOR(inputs[k].sig) + " = " + d);
					if (d < mind) {
						mind = d;
						mink = k;
					}
				}
				
				if (mink < 0)
					throw new IllegalStateException();
//				System.out.println("matched " + COLOR(inputs[mink].sig) + " to " + COLOR(sig));
				output.closestInputs[j] = mink;
			}
		}
	}
}
