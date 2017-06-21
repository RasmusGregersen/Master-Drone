package tests;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import imgManagement.Circle;
import imgManagement.CircleFinder;

/**
 * Used for testing tolerances on OpenCV method on local images.
 * @author Nichlas N. Pilemand
 *
 */
public class OpencvTest {
	private static final boolean testAll = true;
	
	public static void main(String[] args) {
		// Load libs
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		String[] images = {
//				"assets/demo1.jpg",
//				"assets/demo2.jpg",
//				"assets/demo3.jpg",
//				"assets/demo4.jpg",
				"assets/ny_demo1.jpg",
				"assets/ny_demo2.jpg",
				"assets/ny_demo3.jpg",
				"assets/ny_demo4.jpg",
				};
		long time = System.currentTimeMillis();
		if (testAll){
			for (String img : images) 
				findCircleTest(img);
			
		} else
			findCircleTest("assets/circles.jpg");
		System.out.println("Time taken: " + (System.currentTimeMillis() - time) + " ms.");
	}
	
	public static void findCircleTest(String imgLoc){
		Mat img = CircleFinder.loadImg(imgLoc);
		Circle[] circles = CircleFinder.findCircles(img);
		if (circles.length > 0){
			Scalar red = new Scalar(0,0,255);
			Scalar blue = new Scalar(255,0,0);
			//convert the (x, y) coordinates and radius of the circles to integers
			int i = 0;
			for (Circle c : circles) {
				System.out.printf("Circle[%d]: (%d;%d) r=%d\n", i++, (int)c.x, (int)c.y, (int)c.r);
				Imgproc.circle(img, c.getPoint(), (int) c.r, red, 4);
				Point point = c.getPoint().clone();
				Point point2 = c.getPoint().clone();
				point.x -= 10; point.y -= 10;
				point2.x += 10; point2.y += 10;
				Imgproc.rectangle(img, point, point2, blue,-1);
			}
			
		} else System.out.println("No circles");
		imshow(img);
	}
	

	/**
		Shows an image.
		@param src The image to show
	*/
	public static void imshow(Mat src){
	    BufferedImage bufImage = null;
	    try {
	        MatOfByte matOfByte = new MatOfByte();
	        Imgcodecs.imencode(".jpg", src, matOfByte);
	        byte[] byteArray = matOfByte.toArray();
	        InputStream in = new ByteArrayInputStream(byteArray);
	        bufImage = ImageIO.read(in);

	        JFrame frame = new JFrame("Image");
	        frame.getContentPane().setLayout(new FlowLayout());
	        frame.getContentPane().add(new JLabel(new ImageIcon(bufImage)));
	        frame.pack();
	        frame.setVisible(true);
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
}
