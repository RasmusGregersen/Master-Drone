package controller;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.CommandManager;
import imgManagement.Circle;

/**
 * Created by Dave on 07/06/2017..
 */
public class StateController {

	private enum Mode {
		Normal, Continous
	}

	private Mode currentMode;

	public enum Command {
		TakeOff, Hover, QRSearch, QRLost, QRValidate, QRCentralizing, SearchForCircle, Centralize, FlyThrough, UpdateGate, Finished
	}

	public Command state;

	private IARDrone drone;
	private CommandManager cmd;
	private MainDroneController controller;
	int strayModeCircle = 0;


	private int nextPort = 2; // Consider handling this in MainDronController
	private final int maxPorts = 5;

	public StateController(MainDroneController mc, IARDrone drone, CommandManager cmd) {
		this.controller = mc;
		this.cmd = cmd;
		this.drone = drone;
		this.currentMode = Mode.Continous;
	}

	public void commands(Command command) throws InterruptedException {
		if (System.currentTimeMillis() - this.controller.latestImgTime > 150) {
			System.out.println("Image lag, delaying commands...");
			drone.hover();
			MainDroneController.sleep(150);
		}
		switch (command) {
		case TakeOff:
			takeOff();
			break;
		case Hover:
			hover();
			break;
		case QRSearch:
			qRSearch();// qRSearch(); // Hannibal
			break;
		case QRLost:
			qRLost(); // Rasmus og Lars
			break;
		case QRValidate:
			qRValidate(); // Nichlas
			break;
		case QRCentralizing:
			qRCentralizing(); // Nichlas
			break;
		case SearchForCircle:
			searchForCircle(); // David
			break;
		case Centralize:
			centralize(); // Lars
			break;
		case FlyThrough:
			flyThrough(); // David
			break;
		case UpdateGate:
			updateGate();
			break;
		case Finished:
			finish();
			break;
		}
	}

	public void takeOff() throws InterruptedException {
		// Takeoff
		System.out.println("State: ReadyForTakeOff");
		cmd.takeOff();
		flyToHeight(1000);
		Thread.currentThread().sleep(500);
		// //MainDroneController.sleep(1250);
		// cmd.up(15).doFor(1250);
		// MainDroneController.sleep(1250);
		// cmd.landing();
		// MainDroneController.sleep(3000);
		// Check conditions and transit to next state

		state = Command.Hover;
	}

	public void hover() throws InterruptedException {
		// Hover method
		System.out.println("State: Hover");
		cmd.hover().doFor(500);
		// Thread.currentThread().sleep(5000);
		// MainDroneController.sleep(550);
		// Check conditions and transit to next state
		state = Command.QRValidate;
	}

	int strayMode = 0;

	public void qRSearch() throws InterruptedException {

		// Searching method
		System.out.print("State: QRSearch - ");

		int SPEEDSpin = 80;
		int SPEEDMove = 4;
		int doFor = 30;

		Result tag = controller.getTag();
		if (tag != null) {
			System.out.println("Tag found");
			this.state = Command.QRValidate;
			return;
		}
		flyToHeight(1000);
		System.out.println("Spin right");
		spinRight(SPEEDSpin, doFor);
		controller.sleep(800);
	}

	int lostMode = 0;

	public void qRLost() throws InterruptedException {
		System.out.print("State: QR Lost: ");

		int Speed = 5;
		int doFor = 200;

		Result tag = controller.getTag();
		if (tag != null) {
			System.out.println("Tag found");
			this.state = Command.QRValidate;
			return;
		}
		switch (lostMode) {
		case 0:
			System.out.println("Fly backwards");
			cmd.backward(Speed).doFor(doFor);
			lostMode = 1;
			break;
		case 1:
			System.out.println("Look right");
			//spinRight(Speed, doFor);
			cmd.goRight(4).doFor(40);
			lostMode = 2;
			break;
		case 2:
			System.out.println("Look left");
			//spinLeft(Speed*2, doFor);
			cmd.goLeft(4).doFor(40);
			lostMode = 1;
			break;
		}
		Thread.currentThread().sleep(200);
	}

	private boolean firstTag = false;

	public void qRValidate() throws InterruptedException {
		System.out.print("State: QRValidate: ");
		Result tag = controller.getTag();
		if (tag == null) {
			if (firstTag) {
				System.out.println("Tag Lost");
				firstTag = false;
				this.state = Command.QRLost;
				return;
			}
			this.state = Command.QRSearch;
			return;

		}
		firstTag = true;
		// The scanned QR is the next port we need
		if (controller.getPorts().get(nextPort).equals(tag.getText())) {
			System.out.println("Validated port: " + tag.getText());
			this.state = Command.QRCentralizing;
		} else {
			System.out.println("Not validated port: " + tag.getText());
			this.state = Command.QRSearch;
		}
		Thread.currentThread().sleep(10);
	}

	public void qRCentralizing() throws InterruptedException {
		// Centralize QR tag method
		System.out.print("State: QRcentralizing - ");
		Result tag = controller.getTag();
		if (tag == null) {
			System.out.println("no tag, back to searching");
			this.state = Command.QRLost;
			return;
		}

		if (!controller.isTagCentered()) { // Inverted for readability, lol.
			ResultPoint[] points;
			double a;
			if (controller.getTagSize() > MasterDrone.IMAGE_WIDTH / 5) {
				// We don't want to get too close to the tag
				System.out.println("too close, going back");
				cmd.backward(6).doFor(30);
				controller.sleep(100);
				return;
				
			}
			synchronized (tag) {
				points = tag.getResultPoints();
				a = controller.getQRRelativeAngle(tag);
				System.out.println("Angle: " + a);
			}

			int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
			int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;

			float x = (points[1].getX() + points[2].getX()) / 2; // True middle
			float y = points[1].getY();	
			a = a / 2 / 100f;
			
			
			
			float leftRightSpeed = (float) ((x - imgCenterX) / 45) / 100.0f;
			float upDownSpeed = (float) ((imgCenterY - y) / 6) / 100.0f;
			leftRightSpeed = limit(leftRightSpeed, -0.15f, 0.15f);
			upDownSpeed = limit(upDownSpeed, -0.15f, 0.15f);
			System.out.println("adjusting: " + leftRightSpeed + ", 0f, " + upDownSpeed + ", " + (float)a);
			// Left spin needs more speed
			if (a < 0)
				spin((float) (a * 5), 50);
			else
				spin((float)a, 50);
			System.out.println("Spinning");
			MainDroneController.sleep(500);
			System.out.println("Correcting position");
			move(leftRightSpeed, 0f, upDownSpeed, 0f, 80).waitFor(20).hover();
			MainDroneController.sleep(300);
			
		} else {
			System.out.println("AutoController: Tag centered");
			cmd.hover();
			// ADJUSTING TO CIRCLE HEIGHT
			flyToHeight(1700);
			Thread.currentThread().sleep(200);

			this.state = Command.SearchForCircle;
		}

	}
	
	Circle lastCircle;
	int lastCircleCount = 0;

	public void searchForCircle() throws InterruptedException {
		// Increase altitude and search for the circle
		System.out.print("State: SearchForCircle - ");
		if (controller.getCircles().length >= 1) {
			for (Circle c : controller.getCircles()) {
				if (c.r > MasterDrone.IMAGE_HEIGHT / 10) {
					System.out.println("Circle found!");
					this.state = Command.Centralize;
					lastCircle = c;
				}
			}
			
		} else {
			if (lastCircle != null) {
				if (++lastCircleCount > 10) {
					System.out.println("lastCircle for the 10th time!!!");
					state = Command.QRSearch;
					Thread.currentThread().sleep(200);
					lastCircleCount = 0;
					return;
				}
				
				System.out.println("Going back to last circle @ " + lastCircle);
				if (lastCircle.r > controller.circleRadius) {
					// We're probably too close to the circle, so fly back a bit
					cmd.backward(6).doFor(30);
				} else if (lastCircle.x > MasterDrone.IMAGE_WIDTH / 2){
					// go right
					cmd.goRight(5).doFor(80);
						
				} else {
					cmd.goLeft(5).doFor(80);
				}
			} else
				System.out.println("No lastCircle?!");

			Thread.currentThread().sleep(500);

			this.state = Command.SearchForCircle;
		}
	}

	String currentCorrect = "";
	String midlertidlig = "";

	public void centralize() throws InterruptedException {
		// Centralize drone in front of circle
		System.out.print("State: Centralize - ");
		int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;
		// We have more than one circle, figure out which one is correct
		if (controller.getCircles().length > 0) {
			// We have more than one circle, figure out which one is correct
			for (Circle c : controller.getCircles()) {
				if (c.getRadius() >= MasterDrone.IMAGE_HEIGHT / 5) {
					if (controller.isCircleCentered()) {
						System.out.println("CENTERED!");
						cmd.hover().doFor(100);
						this.state = Command.FlyThrough;
						return;
					}
					float leftRightSpeed = (float) ((c.x - imgCenterX) / 10) / 100.0f;

					float forwardSpeed = (float) ((c.r - controller.circleRadius)) / 100.0f;

					float upDownSpeed = (float) ((imgCenterY - c.y) / 10) / 100.0f;
					leftRightSpeed = limit(leftRightSpeed, -0.15f, 0.15f);
					forwardSpeed = limit(forwardSpeed, -0.15f, 0.15f);
					upDownSpeed = limit(upDownSpeed, -0.15f, 0.15f);
					double a = 92;
					double degPerPx = a /MasterDrone.IMAGE_WIDTH;
					double spin = ((c.x - imgCenterX) * degPerPx) / 2 / 100;
					
					midlertidlig = leftRightSpeed + ", " + forwardSpeed + ", " + upDownSpeed;
					if (currentCorrect.equals(midlertidlig)) {
						System.out.println("Picture FUCKS");
						cmd.hover().doFor(500);
						return;
					} else
						currentCorrect = midlertidlig;

					System.out.println(
							"Correcting position, " + leftRightSpeed + ", " + forwardSpeed + ", " + upDownSpeed + ", " + spin);
					spin = limit((float)spin, -0.15f, 0.15f);
					if (spin < 0)
						spin((float)(spin * 5), 50);
					else
						spin((float)spin, 50);
					cmd.hover();
					controller.sleep(400);
					move(leftRightSpeed, forwardSpeed, upDownSpeed, 0f, 150);
//					cmd.move(leftRightSpeed, forwardSpeed, upDownSpeed, 0f).doFor(30);
					drone.hover();
					controller.sleep(200);
					break;
				}
			}
		} else {
			state = Command.SearchForCircle;
		}
	}

	public void flyThrough() throws InterruptedException {
		System.out.print("State: flyThrough - ");
		System.out.println("AutoController: Going through port " + nextPort);
		cmd.forward(16).doFor(2500);
		// Thread.currentThread().sleep(1200);
		System.out.println("Returning to Hover State");
		state = Command.UpdateGate;
	}

	public void updateGate() {
		System.out.print("State: updateGate: ");
		// Changing which QR tag to search for next
		nextPort++;

		// Changing state to finishing or searching for next tag depending on
		// port state
		if (nextPort > maxPorts) {
			switch (this.currentMode) {
			case Continous:
				System.out.println("\n  Starting over as mode is continous.");
				nextPort = 0;
				this.state = Command.Hover;
			case Normal:
				System.out.println("\n  Last port reached.");
				this.state = Command.Finished;
				break;
			default:
				this.state = Command.Hover; // Redundant
				break;
			}
		} else {
			this.state = Command.Hover;
		}
		System.out.println("Next port is " + nextPort);
	}

	public void finish() {
		System.out.println("State: Finish");
		cmd.landing();
		controller.stopController();
	}

	/**
	 * Puts the drone to a specific height. This will run until the height is
	 * reached. Has a tolerance of 10 cms in each direction.
	 * 
	 * @param height
	 *            The requested height in millimeters.
	 */
	private void flyToHeight(int height) {
		System.out.println("StateController: flyToHeight: " + height);
		while (true) {
			if (height + 100 < this.controller.getAltitude()) { // fly down
				cmd.down(30).doFor(30).hover();
				// Sleep maybe?
			} else if (height - 100 > this.controller.getAltitude()) { // fly up
				cmd.up(30).doFor(30).hover();
			} else {
				System.out.println("Reached height: " + this.controller.getAltitude()); // done
				return;
			}
		}
	}

	/**
	 * Separate method for spinning to compensate for stability issues.
	 * 
	 * @param speed
	 *            Int 0 - 100: Percent of max speed
	 * @param doFor
	 *            Integer: How long in ms to run the command
	 * @return {@link CommandManager} for chaining commands
	 * @see StateController#spinLeft spinLeft().
	 */
	private CommandManager spinRight(int speed, int doFor) {
		return cmd.spinRight(speed).doFor(doFor).spinLeft(1);
	}

	/**
	 * SpinLeft
	 * 
	 * @see StateController#spinRight(int, int) spinRight()
	 */
	private CommandManager spinLeft(int speed, int doFor) {
		return cmd.spinLeft(speed).doFor(doFor);
	}
	
	private CommandManager spin(float speed, int doFor) {
		return cmd.move(0f, 0f, 0f, speed);
	}
	
	private int limit(int i, int min, int max) {
		return (i > max ? max : (i < min ? min : i));
	}

	private float limit(float f, float min, float max) {
		return (f > max ? max : (f < min ? min : f));
	}
	
	private CommandManager goLeft(int speed, int doFor) throws InterruptedException {
		for (int i = 1; i < doFor/10; i++) {
			cmd.goLeft(speed);
			controller.sleep(10);
		}
		return cmd.goLeft(speed);
	}
	
	private CommandManager goRight(int speed, int doFor) throws InterruptedException {
		for (int i = 1; i < doFor/10; i++) {
			cmd.goRight(speed);
			controller.sleep(10);
		}
		return cmd.goRight(speed);
	}
	
	private CommandManager move(float lrtilt, float fbtilt, float vspeed, float aspeed, int doFor) throws InterruptedException {
		for (int i = 1; i < doFor/7; i++) {
			cmd.move(lrtilt, fbtilt, vspeed, aspeed).doFor(25);
		}
		return cmd.move(lrtilt, fbtilt, vspeed, aspeed);
	}
	
}
