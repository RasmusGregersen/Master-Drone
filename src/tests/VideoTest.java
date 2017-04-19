package tests;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import controller.MainDroneController;
import de.yadrone.base.ARDrone;

public class VideoTest extends Thread {
	private static String source = "/assets/vid1.mp4";
	private static String source2 = "C:/Users/evil_/git/Master-Drone/assets/vid1.mp4";

	public static void main(String[] args) throws InterruptedException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		//source = System.getProperty("user.dir") + source;
		System.out.println(source2);
		VideoCapture cap = new VideoCapture();
		cap.open(source2);
		MainDroneController con = new MainDroneController(new ARDrone());
		//QRCodeScanner con = new QRCodeScanner();
		Mat image = new Mat();
		//System.out.println(cap.retrieve(image));
		System.out.println();
		int i = 0;
		while(cap.read(image)){
			System.out.println("loop " + i++);
			
			con.imageUpdated(image);
			sleep(25);	
		}
	}

}
