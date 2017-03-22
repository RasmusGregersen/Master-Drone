package imgManagement;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class OpencvTest {
	
	private static String imgLoc = "assets/circles.jpg";

	public static void main(String[] args) {
		// Load libs
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		// Load image
		Mat img = loadImg();
		circleTest(img);
	}
	
	
	/**
	 *	Loads an image based on the static imgLoc
	 *	@return The image loaded of type Mat
	*/
	public static Mat loadImg(){
		Mat image;
		image = Imgcodecs.imread(imgLoc, Imgcodecs.IMREAD_COLOR);
		if (image.empty()){
			System.out.println("Empty image!");
			return null;
		} else
		return image;
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
	

	public static void circleTest(Mat image) {
		Mat output = image.clone();
		Mat gray = image.clone();
		// Get the gray img
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		
		// Detect circles
		Mat circles = new Mat();
		Imgproc.HoughCircles(gray, circles, Imgproc.CV_HOUGH_GRADIENT,1.3, 50);
		if (!circles.empty()){
			System.out.println("#rows "+ circles.rows() +", #cols: "+ circles.cols());
			Point point;
			Scalar red = new Scalar(0,0,255);
			Scalar blue = new Scalar(255,0,0);
			
			//convert the (x, y) coordinates and radius of the circles to integers
			for (int i = 0; i < circles.cols(); i++) {
//				for (int i = 0; i < 1; i++) {
				double[] data = circles.get(0, i);
				point = new Point(data[0],data[1]);
				double radius = Math.round(data[2]);
				System.out.printf("Col[%d]: (%d;%d):%d\n", i, (int)point.x, (int)point.y, (int)radius);
				Imgproc.circle(output, point, (int)data[2], red, 4);
				Point point2 = point.clone();
				point.x -= 10; point.y -= 10;
				point2.x += 10; point2.y += 10;
				Imgproc.rectangle(output, point, point2, blue,-1);
			}
			imshow(output);
			
		} else System.out.println("No circles");
		
		
	}
	
	

}
