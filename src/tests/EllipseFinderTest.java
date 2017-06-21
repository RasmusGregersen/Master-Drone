package tests;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * A currently nonfunctional test to find ellipses.
 * @author Nichlas N. Pilemand
 *
 */
public class EllipseFinderTest {

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Mat image;
		image = loadImg("assets/ny_demo1.jpg");
		OpencvTest.imshow(findEllipse(image));

	}
	private static final double DP = 1.1; // Basicly tolerence
	private static final int MIN_DIST = 50; // Minimum distance between center points
	private static final int BLUR = 9; // Blur amount, removes noise - must be uneven


	
	/**
	 * Detects and shows circles in an image
	 * @param image The image to detect circles on and display.
	 * @return The image with found circles drawn onto it.
	 */
	public static Mat findEllipse(Mat image) {
		// TODO: Should probably return data instead of manipulated image
		
		System.out.println(image.size());
		Mat output = image.clone();
		Mat gray = image.clone();
		// Get the gray img
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		
		// Detect circles
		Mat circles = new Mat();
		Size s = new Size(BLUR,BLUR);
		Imgproc.blur(gray, gray, s);
		
		Mat threshold_output = new Mat();
		List<MatOfPoint> contours = new ArrayList();
		Mat hierarchy = new Mat();
		Imgproc.threshold(gray, output, 100, 255, Imgproc.THRESH_BINARY);
		Imgproc.findContours(threshold_output, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0,0));
		
		RotatedRect[] minEllipse = new RotatedRect[contours.size()];
		
		Object[] f =  contours.toArray();
		System.out.println("Size: " + contours.size());
		
		for (int i = 0; i < contours.size(); i++) {
			if (contours.size() > 5){
				minEllipse[i] = Imgproc.fitEllipse(new MatOfPoint2f(contours.get(i)));
				System.out.println("Ellipse "+i);
			}
		}
		for (int i = 0; i < contours.size(); i++){
			Imgproc.ellipse(output, minEllipse[i], new Scalar(0,0,255), 2, 8);
		}
		
		return output;
	}
	
	/**
	 *	Loads an image based on imgLoc
	 *	@param imgLoc Location of the image
	 *	@return The image loaded of type Mat
	*/
	public static Mat loadImg(String imgLoc){
		Mat image;
		image = Imgcodecs.imread(imgLoc, Imgcodecs.IMREAD_COLOR);
		if (image.empty()){
			System.out.println("Couldn't load image!");
			return null;
		} else
		return image;
	}

}
