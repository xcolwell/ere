package org.resisttheb.ere.ui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class FontUtilities {
	public final static Font ERAS_LIGHT;
	public final static Font ERAS_MEDIUM;
	public final static Font ERAS_BOLD;
	
	
	static {
		// TODO: security issues with loading our own font --
		// requires privileges, which we don't want to sign for
		ERAS_LIGHT 		= 
			new Font("lucida sans", Font.PLAIN, 1);
			//createFont("ERASLGHT.TTF");
		ERAS_BOLD 		= new Font("lucida sans", Font.BOLD, 1);
			//createFont("ERASBD.TTF");
		ERAS_MEDIUM 	= ERAS_LIGHT;
			//createFont("ERASMD.TTF");
	}
	
	
	private static Font createFont(final String name) {
		try {
			final InputStream is = FontUtilities.class.getResourceAsStream(name);
			try {
				return Font.createFont(Font.TRUETYPE_FONT, is);
			}
			finally {
				is.close();
			}
		}
		catch (FontFormatException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	private FontUtilities() {
	}
}
