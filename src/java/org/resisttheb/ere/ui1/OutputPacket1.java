package org.resisttheb.ere.ui1;

import java.io.Serializable;

public class OutputPacket1 implements Serializable {
	public final int[] activeWids;
	
	public OutputPacket1(final int ... _activeWids) {
		this.activeWids = _activeWids;
	}
	
	public OutputPacket1() {
		activeWids = new int[0];
	}
}
