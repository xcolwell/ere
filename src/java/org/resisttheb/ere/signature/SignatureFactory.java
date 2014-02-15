package org.resisttheb.ere.signature;

import org.resisttheb.ere.blobs.BlobGraph;

public interface SignatureFactory {
	public Signature generate(final BlobGraph graph);
}
