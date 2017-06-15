package controller;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import controller.StateController.Command;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.LEDAnimation;
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
 * Translates read tags into commands for the drone.
 * @author Nichlas N. Pilemand
 *
 */
public class MainDroneController extends AbstractController implements TagListener, CircleListener, ImageListener {

	private final static int SPEED = 4;
	private final static int SLEEP = 500;
	private final static int doFor = 20; // How long (ms) to run commands for.
	private static int leftRightDiv = 10;
	private static int leftRightAdd = 5;

	/*
	 * This list holds tag-IDs for all tags which have successfully been visited
	 */
	private ArrayList<String> tagVisitedList = new ArrayList<String>();

	private Result tag;
	private Result lastTag;
	private float tagOrientation;
	private ArrayList<String> ports = new ArrayList<String>();
	private HashMap<String, Point> wallMarks;
	private Circle[] circles;
	private int nextPort = 1;
	private int altitude;
	
	protected double latestImgTime;

	public StateController getSc() {
		return sc;
	}

	private StateController sc;

	public MainDroneController(IARDrone drone) {
		super(drone);
		//drone.getCommandManager().setMaxAltitude(maxHeight);
		//drone.getCommandManager().setMinAltitude(minHeight);
		// Init ports list
		for (int i = 0; i <= 7; i++)
			ports.add("P.0" + i);
		wallMarks = WallCoordinatesReader.read();
		setupAltitudeListener();
		
		drone.addExceptionListener(new ExeptionListener());
	}

	@Override
	public void run() {
		sc = new StateController(this, drone);
		sc.state = Command.TakeOff;
		while (!doStop) // control loop
		{
			try {
				// reset if too old (and not updated)
				if ((tag != null) && (System.currentTimeMillis() - tag.getTimestamp() > 1000)){
					System.out.println("Resetting tag");
					tag = null;
				}
				sc.commands(sc.state);
				Thread.currentThread().sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	ArrayList<String> getTagVisitedList() {
		return tagVisitedList;
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
		if (result == null) // ToDo: do not call if no tag is present
			return;

		//System.out.println("AutoController: Tag found " + result.getText() + ", " + orientation);

		tag = result;
		tagOrientation = orientation;
	}

	Boolean isCircleCentered() {
		Boolean ret = false;
		int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;
		
		if (circles.length > 0)  // Same deal as centerCircle()
			for (Circle c : circles)
				if (c.getRadius() >= MasterDrone.IMAGE_HEIGHT / 10) 
					return ret = ((c.x > (imgCenterX - MasterDrone.TOLERANCE))
							&& (c.x < (imgCenterX + MasterDrone.TOLERANCE))
							&& (c.y > (imgCenterY - MasterDrone.TOLERANCE))
							&& (c.y < (imgCenterY + MasterDrone.TOLERANCE))
							&& (c.r >= 160));
		return ret;
	}


	/**
	 * Takes circles from {@link CircleFinder}.
	 */
	@Override
	public void circlesUpdated(Circle[] circles) {
		this.circles = circles;		
	}
	
	private Point getTagCenter(Result tag) {
		ResultPoint[] points = tag.getResultPoints();
		double dy = (points[0].getY() + points[1].getY()) / 2; // bottom-left, top-left
		double dx = (points[1].getX() + points[2].getX()) / 2; // Top-left, top-right
		return new Point(dx, dy);		
	}
	
	/**
	 * Guesstimates the angle to the QR from the center of the image.
	 * @return double angle. The angle is negative if the QR is to the left of the image center.
	 */
	public double getQRRelativeAngle(Result tag) {
		final double cameraAngle = 92;
		final double imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		double degPerPx = cameraAngle/MasterDrone.IMAGE_WIDTH; 
		
		synchronized(tag){
			// TODO Consider if we should handle the Y offset
			if (tag == null)
				return 0.0;
			Point qrCenter = getTagCenter(tag);
			return (qrCenter.x - imgCenterX) * degPerPx;
		}
	}
	public double getQRRelativeAngle() {
		return getQRRelativeAngle(this.tag);
	}

	@Override
	public void imageUpdated(BufferedImage image) {
		this.latestImgTime = System.currentTimeMillis();		
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
	
	public int getAltitude() {
		return this.altitude;
	}
	
	/**
	 * Class for catching and handling specific exceptions.
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
