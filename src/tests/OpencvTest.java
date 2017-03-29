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

import imgManagement.CircleFinder;

public class OpencvTest {
	private static final boolean testAll = true;
	
	public static void main(String[] args) {
		// Load libs
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		String[] images = {
				"assets/demo1.jpg",
				"assets/demo2.jpg",
				"assets/demo3.jpg",
				"assets/demo4.jpg",
				"assets/ny_demo1.jpg",
				"assets/ny_demo2.jpg"
				};
		if (testAll){
			for (String img : images) 
				findCircleTest(img);
			
		} else
			findCircleTest("assets/demo3.jpg");
	}
	
	public static void findCircleTest(String imgLoc){
		Mat img = CircleFinder.loadImg(imgLoc);
		imshow(CircleFinder.findCircles(img));
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
