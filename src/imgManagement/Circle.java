package imgManagement;

import org.opencv.core.Point;

/**
 * A simple class for defining a circle using x, y, r.
 * (x,y) defines the center of the circle, while r is the radius.
 * @author Nichlas N. Pilemand
 *
 */
public class Circle {
	
	public double 
		x,
		y,
		r;
	
	/**
	 * @param x Center of circle x
	 * @param y Center of circle y
	 * @param r Circle radius
	 */
	public Circle(double x, double y, double r){
		this.x = x;
		this.y = y;
		this.r = r;
	}
	
	/**
	 * @see imgManagement.Circle#Circle(double, double, double) Circle
	 */
	public Circle(int x, int y, int r) {
		this.x = (double) x;
		this.y = (double) y;
		this.r = (double) r;
	}
	
	
	/**
	 * Retrieves a point object. x and y are obtainable with getPoint().x and getPoint().y
	 * @return {@link Point}
	 */
	public Point getPoint(){
		return new Point(this.x, this.y);
	}
	
	public double getRadius(){
		return this.r;
	}
	
	public String toString(){
		return "("+(int)this.x+", "+(int) this.y + "), r = " + (int)this.r;
	}
}
