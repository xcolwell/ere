package org.resisttheb.ere.blobs;

import java.awt.geom.GeneralPath;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Combines a sequence of directions into a path that is rooted at some point in
 * the plane. No restrictions are placed on paths; they may be zero length,
 * open/closed, self-intersecting. Path objects are immutable.
 * 
 * @author Tom Gibara
 * 
 */

public class xMS_Path {

	// statics

	private static final double ADJ_LEN = Math.sqrt(2.0) / 2.0 - 1;

	// fields

	private final xMS_Direction[] directions;


	private final double length;

	private final int originX;

	private final int originY;

	private final int terminalX;

	private final int terminalY;

	// constructors

	private xMS_Path(xMS_Path that, int deltaX, int deltaY) {
		this.directions = that.directions;
		this.length = that.length;
		this.originX = that.originX + deltaX;
		this.originY = that.originY + deltaY;
		this.terminalX = that.terminalX + deltaX;
		this.terminalY = that.terminalY + deltaY;
	}

	/**
	 * Constructs a path which starts at the specified point in the plane. The
	 * array may be zero length.
	 * 
	 * @param startX
	 *            the x coordinate of the path's origin in the plane
	 * @param startY
	 *            the y coordinate of the path's origin in the plane
	 * @param directions
	 *            an array of directions, never null
	 */

	public xMS_Path(int startX, int startY, xMS_Direction[] directions) {
		this.originX = startX;
		this.originY = startY;
		this.directions = directions.clone();

		int endX = startX;
		int endY = startY;
		int diagonals = 0;
		for (xMS_Direction direction : directions) {
			endX += direction.screenX;
			endY += direction.screenY;
			if (direction.screenX != 0 && direction.screenY != 0) {
				diagonals++;
			}
		}

		this.terminalX = endX;
		this.terminalY = endY;

		this.length = directions.length + diagonals * ADJ_LEN;
	}

	// accessors



	/**
	 * @return the x coordinate in the plane at which the path begins
	 */

	public int getOriginX() {
		return originX;
	}

	/**
	 * @return the y coordinate in the plane at which the path begins
	 */

	public int getOriginY() {
		return originY;
	}

	/**
	 * @return the x coordinate in the plane at which the path ends
	 */

	public int getTerminalX() {
		return terminalX;
	}

	/**
	 * @return the y coordinate in the plane at which the path ends
	 */

	public int getTerminalY() {
		return terminalY;
	}

	/**
	 * @return the length of the path using the standard euclidean metric
	 */

	public double getLength() {
		return length;
	}

	/**
	 * @return true if and only if the path's point of origin is the same as
	 *         that of its point of termination
	 */

	public boolean isClosed() {
		return originX == terminalX && originY == terminalY;
	}

	// methods

	/**
	 * Creates a new path by translating this path in the plane.
	 * @param deltaX the change in the path's x coordinate
	 * @param deltaY the change in the path's y coordinate
	 * @return a new path whose origin has been translated
	 */
	
	public xMS_Path translate(int deltaX, int deltaY) {
		return new xMS_Path(this, deltaX, deltaY);
	}

	// TODO add rotate, mirror and reverse methods
	
	
	
	public void toGeneralPath(final GeneralPath gpath) {
		int x = originX;
		int y = originY;
		gpath.moveTo(x, -y);
		for (int i = 0; i < directions.length; ++i) {
			final xMS_Direction dir = directions[i];
			x += dir.planeX;
			y += dir.planeY;
			gpath.lineTo(x, -y);
		}
		if (x != originX && y != originY) {
			assert false;
			gpath.closePath();
		}
	}
	
	public void toReverseGeneralPath(final GeneralPath gpath) {
		int x = originX;
		int y = originY;
		gpath.moveTo(x, -y);
		for (int i = directions.length - 1; 0 <= i; --i) {
			final xMS_Direction dir = directions[i];
			x -= dir.planeX;
			y -= dir.planeY;
			gpath.lineTo(x, -y);
		}
		if (x != originX && y != originY) {
			assert false;
			gpath.closePath();
		}
	}
	
	

	// object methods

	/**
	 * Two paths are equal if they have the same origin and the same directions.
	 */
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof xMS_Path))
			return false;
		xMS_Path that = (xMS_Path) obj;

		if (this.originX != that.originX)
			return false;
		if (this.originY != that.originY)
			return false;
		if (this.terminalX != that.terminalX)
			return false; // optimization
		if (this.terminalY != that.terminalY)
			return false; // optimization
		if (!Arrays.equals(this.directions, that.directions))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return originX ^ 7 * originY ^ directions.hashCode();
	}

	@Override
	public String toString() {
		return "X: " + originX + ", Y: " + originY + " "
				+ Arrays.toString(directions);
	}

}
