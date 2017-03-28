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
 * 
 * @TODO Check Todo's. Consider if this should contain println's.
 * 
 */
public class CircleFinder {
	
	private static final double DP = 1.7; // Basicly tolerence
	private static final int MIN_DIST = 50; // Minimum distance between center points
	private static final int BLUR = 9; // Blur amount, removes noise - must be uneven
	
	public CircleFinder() {
		// Probably not needed
	}

	
	/**
	 * Detects and shows circles in an image
	 * @param image The image to detect circles on and display.
	 * @return The image with found circles drawn onto it.
	 */
	public static Mat findCircles(Mat image) {
		// TODO: Should probably return data instead of manipulated image
		Size imgSize = new Size(0,0);
		if (image.size().height > 1200)
			Imgproc.resize(image, image, imgSize, 0.3,0.3,1);
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
			
		} else System.out.println("No circles");	
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
