package org.resisttheb.ere.ui0;

// wid, key, [][wid, q, loc]
public final class Prompt {
	public static final class Parent {
		public final int wid;
		public final String text;
		public final String loc;
		public final String name;
		
		public Parent(final int _wid, final String _text, final String _loc, final String _name) {
			this.wid = _wid;
			this.text = _text;
			this.loc = _loc;
			this.name = _name;
		}
	}
	
	
	public final int wid;
	public final String securityKey;
	public Parent[] parents;
	
	public Prompt(final int _wid, final String _securityKey, final Parent[] _parents) {
		this.wid = _wid;
		this.securityKey = _securityKey;
		this.parents = _parents;
	}
}
