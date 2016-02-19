package org.trax;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public abstract class TraxRegion {

	public static enum RegionFormat {RECTANGLE, POLYGON, MASK};

	public static class RectangleRegion extends TraxRegion {

		Rectangle2D rect;

		public RectangleRegion(double x, double y, double width, double height) {
			
			rect = new Rectangle2D.Double(x, y, width, height);
			
		}
		
		@Override
		public Rectangle2D getBoundingBox() {

			return (Rectangle2D) rect.clone();
			
		}

		@Override
		public String toString() {

			return String.format("%.3f,%.3f,%.3f,%.3f", rect.getMinX(),
					rect.getMinY(), rect.getWidth(), rect.getHeight());
			
		}

		public List<Point2D> getPolygon() {
			
			ArrayList<Point2D> points = new ArrayList<Point2D>(4);
			
			points.add(new Point2D.Double(rect.getMinX(), rect.getMinY()));
			points.add(new Point2D.Double(rect.getMaxX(), rect.getMinY()));
			points.add(new Point2D.Double(rect.getMaxX(), rect.getMaxY()));
			points.add(new Point2D.Double(rect.getMinX(), rect.getMaxY()));
			
			return points;
			
		}
		
		@Override
		public TraxRegion convertTo(RegionFormat type) {
			switch (type) {
				case RECTANGLE: {
					return this;
					
				}
				case POLYGON: {
					return new PolygonRegion(getPolygon());
				}
				case MASK: {
					// TODO
				}
			}
			
			return null;
		}
		
		@Override
		public RegionFormat getFormat() {
			return RegionFormat.RECTANGLE;
		}
	}

	public static class PolygonRegion extends TraxRegion {

		Vector<Point2D.Double> points = new Vector<Point2D.Double>();

		public PolygonRegion(List<Double> points) {
			
			if (points.size() < 6) throw new IllegalArgumentException("Less than three points given");
			
			for (int i = 0; i < points.size(); i+=2) {
				this.points.add(new Point2D.Double(points.get(i), points.get(i+1))); 

			}

		}
		
		public PolygonRegion(Iterable<Point2D> points) {
			
			for (Point2D p : points)
				this.points.add(new Point2D.Double(p.getX(), p.getY()));
			
			if (this.points.size() < 3) throw new IllegalArgumentException("Less than three points given");

		}

		@Override
		public Rectangle2D getBoundingBox() {

			double minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
			
			for (Point2D.Double point : points) {
				minX = Math.min(minX, point.x);
				minY = Math.min(minY, point.y);
				maxX = Math.max(maxX, point.x);
				maxY = Math.max(maxY, point.y);
				
			}
			
			return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
			
		}

		@Override
		public String toString() {

			StringBuilder builder = new StringBuilder();
			
			for (int i = 0; i < points.size(); i++) {
				
				if (i > 0) builder.append(",");
				
				builder.append(String.format("%.3f,%.3f", points.get(i).x, points.get(i).y));
				
			}
			
			return builder.toString();
			
		}

		public List<Point2D> getPolygon() {
			
			return new Vector<Point2D>(points);
			
		}

		@Override
		public TraxRegion convertTo(RegionFormat type) {
			switch (type) {
				case RECTANGLE: {
					Rectangle2D rect = getBoundingBox();
					return new RectangleRegion(rect.getX(), rect.getY(),
							rect.getWidth(), rect.getHeight());
					
				}
				case POLYGON: {
					return this;
				}
				
				case MASK: {
					// TODO
				}
			
			}
			
			return null;
		}

		@Override
		public RegionFormat getFormat() {
			return RegionFormat.POLYGON;
		}
		
	}
	
	public static TraxRegion parseRegion(String data) {
		
		String[] tokens = data.split(",");
		
		Vector<Double> numbers = new Vector<Double>();
		
		for (String token : tokens) {
			try {
				numbers.add(Double.parseDouble(token));
			} catch (NumberFormatException e) {
				
			}
		}
		
		if (numbers.size() == 4) {
			
			return new RectangleRegion(numbers.get(0), numbers.get(1), numbers.get(2), numbers.get(3));
			
		} else if (numbers.size() > 5 && numbers.size() % 2 == 0) {
			
			return new PolygonRegion(numbers);
			
		} else {
			return null;
		}
		
	}
	
	public abstract Rectangle2D getBoundingBox();
	
	public abstract List<Point2D> getPolygon();
	
	public abstract TraxRegion convertTo(RegionFormat type);
	
	public abstract RegionFormat getFormat();
	
}
