package imgManagement;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * Finds circles in an image.
 * @author Nichlas N. Pilemand 
 */
public class CircleFinder {
	
	private static final double DP = 1.1; // Basicly tolerence
	private static final int MIN_DIST = 50; // Minimum distance between center points
	private static final int BLUR = 9; // Blur amount, removes noise - must be uneven
	
	public CircleFinder() {
		// Probably not needed
	}

	
	/**
	 * Detects circles in an image
	 * @param image The image to detect circles on and display.
	 * @return Circle[] An array containing the data from the found circles.
	 */
	public static Circle[] findCircles(Mat image) {
		Size imgSize = new Size(0,0);
		if (image.size().height > 1200)
			Imgproc.resize(image, image, imgSize, 0.5,0.5,1);
		System.out.println(image.size());
		Mat output = image.clone();
		Mat gray = image.clone();
		// Get the gray img
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		
		// Detect circles
		Mat circles = new Mat();
		Size s = new Size(BLUR,BLUR);
		Imgproc.GaussianBlur(gray, gray, s, 2);
		Imgproc.HoughCircles(gray, circles, Imgproc.CV_HOUGH_GRADIENT, DP, MIN_DIST);
		
		Circle[] ret = new Circle[circles.cols()];
		double[] data;
		
		for (int i = 0; i < circles.cols(); i++) {
			data = circles.get(0, i);
			ret[i] = new Circle(data[0], data[1], data[2]);
		}
		
		return ret;
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
