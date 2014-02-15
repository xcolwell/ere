package org.resisttheb.ere.ui0;

import java.awt.Color;


// TODO: contains global id, ... that's really all we need
// TODO: we should actually just pre-allocate these
public class OutPacket0 {
	public final Color color;
	public final Prompt prompt;
	public final String text;
	
	public OutPacket0(final Color _color, final Prompt _prompt, final String _text) {
		this.color = _color;
		this.prompt = _prompt;
		this.text = _text;
	}
}
