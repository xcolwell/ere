package org.resisttheb.ere.blobs;

import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;


// NOTE: blobs must be created with four neighbors.
//       EIGHT neighbor diagonals will not be correctly returned.
//       e.g. right now we assume every path but the first is a hole.
//       which is not the case with eight connect
public class xMarchingSquares {
	private static final Object MARK = new Object();
	
	
	private final List<xMS_Direction> directions 	= new ArrayList<xMS_Direction>(32);
	private final List<xMS_Path> paths 				= new ArrayList<xMS_Path>(8);
	
	private BlobPixels pixels = null;
	

	public xMarchingSquares() {
	}
	
	
	public void reset(final BlobPixels _pixels) {
		if (null == _pixels)
			throw new IllegalArgumentException();
		this.pixels = _pixels;
	}


	
	public xMS_Path identifyPerimeter(int ix, int iy) {
		if (ix < pixels.x0()) 		ix = pixels.x0();
		else if (ix > pixels.x1()) 	ix = pixels.x1();
		if (iy < pixels.y0()) 		iy = pixels.y0();
		else if (iy > pixels.y1()) 	iy = pixels.y1();

		final int initialValue = value(ix, iy);
		if (initialValue == 0 || initialValue == 15)
			throw new IllegalArgumentException(String.format("Supplied initial coordinates (%d, %d) do not lie on a perimeter.", ix, iy));

		directions.clear();
		
		int x = ix;
		int y = iy;
		xMS_Direction previous = null;

		do {
			final xMS_Direction direction;
			switch (value(x, y)) {
				case  1: direction = xMS_Direction.N; break;
				case  2: direction = xMS_Direction.E; break;
				case  3: direction = xMS_Direction.E; break;
				case  4: direction = xMS_Direction.W; break;
				case  5: direction = xMS_Direction.N; break;
				case  6: direction = previous == xMS_Direction.N ? xMS_Direction.W : xMS_Direction.E; break;
				case  7: direction = xMS_Direction.E; break;
				case  8: direction = xMS_Direction.S; break;
				case  9: direction = previous == xMS_Direction.E ? xMS_Direction.N : xMS_Direction.S; break;
				case 10: direction = xMS_Direction.S; break;
				case 11: direction = xMS_Direction.S; break;
				case 12: direction = xMS_Direction.W; break;
				case 13: direction = xMS_Direction.N; break;
				case 14: direction = xMS_Direction.W; break;
				default: throw new IllegalStateException();
			}			
			
			directions.add(direction);
			x += direction.screenX;
			y += direction.screenY; // accomodate change of basis
			previous = direction;
		} while (x != ix || y != iy);

		final xMS_Path path = new xMS_Path(ix, -iy, 
				directions.toArray(new xMS_Direction[directions.size()]));
		directions.clear();
		return path;
	}


	// the first path will always be an outer perimiter, by def. of sort order
	public xMS_Path[] identifyPerimeters() {
		final Column[] cols = pixels.getColumnsModifiable();
		
		for (Column col : cols) {
			col.alg_ref_0 = null;
		}
		
		paths.clear();
		
		for (int i = 0; i < cols.length; ++i) {
			final Column col = cols[i];
			if (null == col.alg_ref_0) {
				// Note -- we don't always store contiguous ...
				final int x = col.x;
				final int y = col.y0;
				assert pixels.px(x, y);
				if (! pixels.px(x, y - 1)) {
					final int value = value(x, y);
					if (0 != value && 15 != value) {
						paths.add(identifyPerimeter(x, y));
					}
				}
			}
		}
		
		final xMS_Path[] a_paths = paths.toArray(new xMS_Path[paths.size()]);
		paths.clear();
		return a_paths;
	}

	
	
	
	
	
	public boolean identifyPerimeter(final GeneralPath gpath, int ix, int iy) {
		if (ix < pixels.x0()) 		ix = pixels.x0();
		else if (ix > pixels.x1()) 	ix = pixels.x1();
		if (iy < pixels.y0()) 		iy = pixels.y0();
		else if (iy > pixels.y1()) 	iy = pixels.y1();

		final int initialValue = value(ix, iy);
		if (initialValue == 0 || initialValue == 15)
			return false;
//			throw new IllegalArgumentException(String.format("Supplied initial coordinates (%d, %d) do not lie on a perimeter.", ix, iy));

		int x = ix;
		int y = iy;

		gpath.moveTo(ix, iy);
		
		xMS_Direction previous = null;
		do {
			final xMS_Direction direction;
			switch (value(x, y)) {
				case  1: direction = xMS_Direction.N; break;
				case  2: direction = xMS_Direction.E; break;
				case  3: direction = xMS_Direction.E; break;
				case  4: direction = xMS_Direction.W; break;
				case  5: direction = xMS_Direction.N; break;
				case  6: direction = previous == xMS_Direction.N ? xMS_Direction.W : xMS_Direction.E; break;
				case  7: direction = xMS_Direction.E; break;
				case  8: direction = xMS_Direction.S; break;
				case  9: direction = previous == xMS_Direction.E ? xMS_Direction.N : xMS_Direction.S; break;
				case 10: direction = xMS_Direction.S; break;
				case 11: direction = xMS_Direction.S; break;
				case 12: direction = xMS_Direction.W; break;
				case 13: direction = xMS_Direction.N; break;
				case 14: direction = xMS_Direction.W; break;
				default: throw new IllegalStateException();
			}			
			
			x += direction.screenX;
			y += direction.screenY; // accomodate change of basis
			
			gpath.lineTo(x, y);
			
			previous = direction;
		} while (x != ix || y != iy);
		
		return true;
	}
	
	public void identifyPerimeters(final GeneralPath gpath) {
		final Column[] cols = pixels.getColumnsModifiable();
		
		for (Column col : cols) {
			col.alg_ref_0 = null;
		}
		
		for (int i = 0; i < cols.length; ++i) {
			final Column col = cols[i];
			if (null == col.alg_ref_0) {
				final int x = col.x;
				final int y = col.y0;
				assert pixels.px(x, y);
				// Note: we now store in longest runs, so this test isn't needed.
//				if (! pixels.px(x, y - 1)) {
					identifyPerimeter(gpath, x, y);
//				}
			}
		}
	}
	
	// Note: this assume the blobs were constructed
	//        using FOUR ADJACENCY.
	//       if not, this may will interpret some boundaries
	public void identifyPerimeter(final GeneralPath gpath) {
		identifyPerimeters(gpath);
	}
	
	
	// private utility methods
	
	private int value(final int x, final int y) {
		int sum = 0;
		if (probe(x, y)) 
			sum |= 1;
		if (probe(x + 1, y)) 
			sum |= 2;
		if (probe(x, y + 1)) 
			sum |= 4;
		if (probe(x + 1, y + 1)) 
			sum |= 8;
		return sum;
	}

	private boolean probe(final int x, final int y) {
		if (pixels.px(x - 1, y - 1)) {
			final Column col = pixels.getLastColumn();
//			col.alg_data_0 |= col.y0 == y - 1 
//				? BOTTOM_FLAG 
//				: TOP_FLAG;
			if (col.y0 == y - 1) {
				// Mark:
				col.alg_ref_0 = MARK;
			}
			return true;
		}
		return false;
	}
}
