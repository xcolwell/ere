package org.resisttheb.ere.ui0;

public final class InPacket0 {
//	public final int wid;
//	public final String q;
//	public final String loc;
	// TODO: also need the security key
	
	public final int pid;
	public final String securityKey;
	
	public InPacket0(final int _pid, final String _securityKey) {
		this.pid = _pid;
		this.securityKey = _securityKey;
	}
}
