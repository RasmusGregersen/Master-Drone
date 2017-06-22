package tests;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

import controller.MainDroneController;
import de.yadrone.base.ARDrone;
import imgManagement.QRCodeScanner;
import imgManagement.TagListener;

class Controller3 implements TagListener {
	public Controller3(){
		
	}
	public void onTag(Result result, float orientation){
		if (result == null) {
			System.out.println("No tag result!");
			return; 
		}
		System.out.print("orientation: "+orientation + ". ");
		System.out.print("Tag found: "+ result.getText());	
		for (ResultPoint p : result.getResultPoints())
			System.out.print(" " + p);
		System.out.println();
	}
}

/**
 * A currently non functioning class that tries to emulate a drone video stream
 * in order to test various image methods locally.
 * @author Nichlas N. Pilemand
 *
 */
public class VideoTest extends Thread {
	private static String source = "\\assets\\vid1.mp4";
	private static String source2 = "C:/Users/evil_/git/Master-Drone/assets/vid1.mp4";

	public static void main(String[] args) throws InterruptedException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		source = System.getProperty("user.dir") + source;
		System.out.println(source);
		VideoCapture cap = new VideoCapture();
		cap.open(source2);
		QRCodeScanner scanner = new QRCodeScanner();
		Controller3 con = new Controller3();
		scanner.addListener(con);
		//QRCodeScanner con = new QRCodeScanner();
		Mat image = new Mat();
		//System.out.println(cap.retrieve(image));
		System.out.println();
		int i = 0;
		while(cap.read(image)){
			cap.retrieve(image);
			System.out.println("loop " + i++);
			
			scanner.imageUpdated(mat2BufImg(image));
			sleep(25);	
		}
	}
	
	private static BufferedImage mat2BufImg(Mat src){
		 BufferedImage bufImage = null;
		 try {
	        MatOfByte matOfByte = new MatOfByte();
	        Imgcodecs.imencode(".jpg", src, matOfByte);
	        byte[] byteArray = matOfByte.toArray();
	        InputStream in = new ByteArrayInputStream(byteArray);	        
			bufImage = ImageIO.read(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
		 return bufImage;
	}
	
	public static Mat imshow(Mat src){
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
	    return src;
	}

}
