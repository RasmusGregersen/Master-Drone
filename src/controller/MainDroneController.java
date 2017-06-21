package controller;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import controller.StateController.Command;
import de.yadrone.base.IARDrone;
import de.yadrone.base.exception.ARDroneException;
import de.yadrone.base.exception.IExceptionListener;
import de.yadrone.base.exception.VideoException;
import de.yadrone.base.navdata.Altitude;
import de.yadrone.base.navdata.AltitudeListener;
import de.yadrone.base.video.ImageListener;
import imgManagement.Circle;
import imgManagement.CircleFinder;
import imgManagement.CircleListener;
import imgManagement.TagListener;
import org.opencv.core.Point;
import utils.WallCoordinatesReader;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Main drone controller.
 * @author Nichlas N. Pilemand
 *
 */
public class MainDroneController extends AbstractController implements TagListener, CircleListener, ImageListener {
	protected Result tag;
	private HashMap<String, Point> wallMarks;
	private Circle[] circles;
	private int altitude;

	protected double latestImgTime;
	protected int circleRadius = 160;
	private ArrayList<String> ports = new ArrayList<String>();
	private StateController sc;


	public StateController getSc() {
		return sc;
	}


	public MainDroneController(IARDrone drone) {
		super(drone);
		// Init port names list
		for (int i = 0; i <= 7; i++)
			ports.add("P.0" + i);
//		ports.add("W02.02"); // Test room
		
		wallMarks = WallCoordinatesReader.read();
		setupAltitudeListener();

		drone.addExceptionListener(new ExeptionListener());
	}

	@Override
	public void run() {
		this.doStop = false;
		sc = new StateController(this, drone, drone.getCommandManager());
		sc.state = Command.Centralize;
		while (!doStop) // control loop
		{
			try {
				// reset if too old (and not updated)
				if ((tag != null) && (System.currentTimeMillis() - tag.getTimestamp() > 2000)) {
					System.out.println("Resetting tag");
					tag = null;
				}
				sc.commands(sc.state);
				this.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	Result getTag() {
		return tag;
	}

	ArrayList<String> getPorts() {
		return ports;
	}

	HashMap<String, Point> getWallMarks() {
		return wallMarks;
	}

	Circle[] getCircles() {
		return circles;
	}

	public void onTag(Result result, float orientation) {
		if (result == null)
			return;

		tag = result;
	}

	/**
	 * A circle is centered if it's within the tolerance of the center of the image,
	 * and has a predefined {@link MainDroneController#circleRadius radius}. 
	 * Of all the potential circles detected, only the first with a radius greater
	 * than the image width / 4 is considered.
	 * @return True if centered, otherwise false.
	 */
	Boolean isCircleCentered() {
		Boolean ret = false;
		int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;

		if (circles.length > 0) // Same deal as centerCircle()
			for (Circle c : circles)
				if (c.getRadius() >= MasterDrone.IMAGE_HEIGHT / 4)
					return ret = ((c.x > (imgCenterX - MasterDrone.TOLERANCE))
							&& (c.x < (imgCenterX + MasterDrone.TOLERANCE))
							&& (c.y > (imgCenterY - MasterDrone.TOLERANCE))
							&& (c.y < (imgCenterY + MasterDrone.TOLERANCE)) && (c.r >= circleRadius));
		return ret;
	}
	
	/**
	 * A tag is centered if it's {@link MainDroneController#getTagCenter center} is within the tolerance 
	 * of the center of the image, and it's {@link MainDroneController#getTagSize size} is at least the 
	 * image width / 14.
	 * @return True if tag is centered, otherwise false
	 */
	public boolean isTagCentered(){
		if (tag == null)
			return false;
		
		Point center = getTagCenter(this.tag);
		
		int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;
		
		return (( center.x > (imgCenterX - MasterDrone.TOLERANCE))
				&& (center.x < (imgCenterX + MasterDrone.TOLERANCE))
				&& (center.y > (imgCenterY - MasterDrone.TOLERANCE))
				&& (center.y < (imgCenterY + MasterDrone.TOLERANCE)
				&& (getTagSize() < (MasterDrone.IMAGE_WIDTH / 14))));
	}
	
	@Override
	public void imageUpdated(BufferedImage image) {
		this.latestImgTime = System.currentTimeMillis();
	}
	
	/**
	 * Receives circles from {@link CircleFinder}.
	 */
	@Override
	public void circlesUpdated(Circle[] circles) {
		this.circles = circles;
	}
	
	/**
	 * Calculates the size of the current scanned QR, or 0.0 if not available.
	 * The size is defined as the difference in x-values between the two top marks on the QR.
	 * @return double The size.
	 */
	public double getTagSize() {
		if (tag != null){
			ResultPoint[] points = tag.getResultPoints();
			return points[2].getX() - points[1].getX();
		}			
		else
			return 0.0;
	}

	/**
	 * Calculates the center of the tag according to it's position on the image.
	 * @param tag The tag to get the center from
	 * @return The center point of the tag. 
	 */
	private Point getTagCenter(Result tag) {
		ResultPoint[] points = tag.getResultPoints();
		double dy = (points[0].getY() + points[1].getY()) / 2; // bottom-left,
																// top-left
		double dx = (points[1].getX() + points[2].getX()) / 2; // Top-left,
																// top-right
		return new Point(dx, dy);
	}

	/**
	 * Guesstimates the angle to the QR from the center of the image.
	 * 
	 * @return double angle. The angle is negative if the QR is to the left of
	 *         the image center.
	 */
	public double getQRRelativeAngle(Result tag) {
		final double cameraAngle = 92;
		final double imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		double degPerPx = cameraAngle / MasterDrone.IMAGE_WIDTH;

		synchronized (tag) {
			// TODO Consider if we should handle the Y offset
			if (tag == null)
				return 0.0;
			Point qrCenter = getTagCenter(tag);
			return (qrCenter.x - imgCenterX) * degPerPx;
		}
	}

	/**
	 * Delivers the currently stored tag to the angle function:
	 * @see controller.MainDroneController#getQRRelativeAngle(Result) getQRRelativeAngle(Result tag)
	 */
	public double getQRRelativeAngle() {
		return getQRRelativeAngle(this.tag);
	}

	/**
	 * Setups an AltitudeListener so we can extract the altitude when received.
	 */
	private void setupAltitudeListener() {
		drone.getNavDataManager().addAltitudeListener(new AltitudeListener() {
			@Override
			public void receivedAltitude(int a) {
				altitude = a;
			}

			@Override
			public void receivedExtendedAltitude(Altitude d) {
			}
		});
	}

	/**
	 * @return The drone's altitude
	 */
	public int getAltitude() {
		return this.altitude;
	}

	/**
	 * Class for catching and handling specific exceptions.
	 * 
	 * @author Nichlas N. Pilemand
	 */
	class ExeptionListener implements IExceptionListener {
		@Override
		public void exeptionOccurred(ARDroneException exc) {
			if (exc.getClass().equals(VideoException.class)) {
				System.out.println("Got VideoException, trying to restart");
				drone.getVideoManager().reinitialize();
			}
		}
	}
}
