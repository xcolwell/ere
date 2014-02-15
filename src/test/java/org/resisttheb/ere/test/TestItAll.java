package org.resisttheb.ere.test;

import java.awt.image.BufferedImage;

import org.resisttheb.ere.blobs.BlobGraph;
import org.resisttheb.ere.blobs.BlobNode;
import org.resisttheb.ere.blobs.BlobPixels;
import org.resisttheb.ere.blobs.MaskedPixels;
import org.resisttheb.ere.blobs.Pixels;
import org.resisttheb.ere.blobs.PseudoIndexedBlob;
import org.resisttheb.ere.signature.Signature;
import org.resisttheb.ere.signature.SignatureFactory;
import org.resisttheb.ere.signature.Linker.InputFrame;

public class TestItAll {

	// reads all images ina  dir
	//    - create blob graph (also store blob outline)
	//    - sub creates a signature for each blob
	// once have all input frames,
	// link!
	
	// for the results, need a ui that lets select an image
	// from a list,
	// then click on a blob to show linked image
	
	
	
	// (file, blobgraph, signatures)
	
	
	private SignatureFactory sigf;
	
	
	// analysis environment.
	// each env. contains        
	//   - source buffer, temp buffer,
	//   - source generator, edge generator, sub generator
	//   - 
	//   - #run
	//       - 1. set url for image
	//       - 2. run -> causes both analysis jobs to be put on the queue
	//       - 3. when both jobs are finished
	//              upload to DB.
	//              finish the env   (causes external client to spin back to step 1 witha new url)
	//               
	
	
	
	
	// ebuffer is temp for the edge detector
	private Analysis2 analyze2(final BufferedImage buffer, BufferedImage ebuffer, final BlobGraph egenerator) {
		// TODO:
		// 1. trace the edges
		
		
	}
	
	
	// generator should be configured
	private Analysis1 analyze1(final BlobGraph graph, final BlobGraph generator) {
		// for each blob,
		// mask the pixels, rock another computation

		final BlobNode[] blobs = graph.getBlobsModifiable();
		
		final Signature[] sigs = new Signature[blobs.length];
		final Signature mainSig = sigf.generate(graph);
		
		
		// Populate signatures:
		
		final Pixels pixels = graph.getPixels();
		
		final PseudoIndexedBlob pib = new PseudoIndexedBlob();
		final BlobPixels pfbs = new BlobPixels();
		pfbs.setData(pib);
		
		pib.reserve(pixels.x1() - pixels.x0());
		
		for (int i = 0; i < blobs.length; ++i) {
			pib.reset(blobs[i]);
			
			generator.setPixels(new MaskedPixels(pfbs, pixels));
			generator.rebuild();
			
			sigs[i] = sigf.generate(generator);
		}
		
		// TODO:
		// Trace each of the blobs --
		// we want to have their shapes
		
		
		
		
		return new InputFrame(mainSig, sigs);
	}
}
