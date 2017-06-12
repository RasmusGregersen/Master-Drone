package controller;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import controller.StateController.Command;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.LEDAnimation;
import imgManagement.Circle;
import imgManagement.CircleFinder;
import imgManagement.CircleListener;
import imgManagement.TagListener;
import org.opencv.core.Point;
import utils.WallCoordinatesReader;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Main drone controller.
 * Translates read tags into commands for the drone.
 * @author Nichlas N. Pilemand
 *
 */
public class MainDroneController extends AbstractController implements TagListener, CircleListener {

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
	private int nextPort = 3;

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
	}

	@Override
	public void run() {
		sc = new StateController(this, drone);
		sc.state = Command.TakeOff;
		while (!doStop) // control loop
		{
			try {
				// reset if too old (and not updated)
				if ((tag != null) && (System.currentTimeMillis() - tag.getTimestamp() > 500)){
					tag = null;
				}
				sc.commands(sc.state);
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
}
