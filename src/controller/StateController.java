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


    public enum Command {
        TakeOff,Hover,QRSearch, QRLost, QRValidate,QRCentralizing,SearchForCircle,Centralize,FlyThrough,UpdateGate, Finished
    }

    public Command state;
    
    private IARDrone drone;
    private MainDroneController controller;
    private CommandManager cmd;
    int strayModeCircle = 0;
    
    private int nextPort = 1; // Consider handling this in MainDronController
    private final int maxPorts = 5;
    
    public StateController(MainDroneController mc, IARDrone drone) {
    	this.controller = mc;
        this.drone = drone;
        this.cmd = drone.getCommandManager();

    }

	public void commands(Command command) throws InterruptedException {
		if (System.currentTimeMillis() - this.controller.latestImgTime < 100) {
			System.out.println("Image lag, delaying commands...");
			drone.hover();
			MainDroneController.sleep(500);
		}
        switch(command){
            case TakeOff: takeOff();
                break;
            case Hover: hover();
                break;
            case QRSearch: qRSearch();//qRSearch(); // Hannibal
                break;
            case QRLost: qRLost(); //Rasmus og Lars
                break;
            case QRValidate: qRValidate(); // Nichlas
                break;
            case QRCentralizing: qRCentralizing(); // Nichlas
                break;
            case SearchForCircle: searchForCircle(); //David
                break;
            case Centralize: centralize(); // Lars
                break;
            case FlyThrough: flyThrough(); // David
                break;
            case UpdateGate: updateGate();
                break;
            case Finished: finish();
                break;
        }
    }


    public void takeOff() throws InterruptedException{
        //Takeoff
        System.out.println("State: ReadyForTakeOff");
        cmd.takeOff();
        Thread.currentThread().sleep(4000);
        //MainDroneController.sleep(1250);
        cmd.up(15).doFor(1250);
        //MainDroneController.sleep(1250);
        //drone.getCommandManager().landing();
        //MainDroneController.sleep(3000);
        //Check conditions and transit to next state

        state = Command.Hover;
    }


    public void hover() throws InterruptedException {
        //Hover method
        System.out.println("State: Hover");
        cmd.hover().doFor(5000);
//		Thread.currentThread().sleep(5000);
        //MainDroneController.sleep(550);
        //Check conditions and transit to next state
        state = Command.QRValidate;
    }

    int strayMode = 0;

    public void qRSearch() throws InterruptedException {

        int SPEEDSpin = 10;
        int SPEEDMove = 4;
        int doFor = 50;

        //Searching method
        System.out.print("State: QRSearch - ");

        Result tag = controller.getTag();
        if (tag != null ) {
            System.out.println("Tag found");
            this.state = Command.QRValidate;
            return;
        }

        System.out.println("Spin right");
        cmd.spinRight(SPEEDSpin * 3).doFor(doFor);
        cmd.spinLeft(1).doFor(1);
        Thread.currentThread().sleep(20);
        cmd.spinLeft(SPEEDSpin * 3).doFor(doFor);
        cmd.spinLeft(1).doFor(1);
    }

    int lostMode = 0;

    public void qRLost() throws InterruptedException {
        System.out.println("QR Lost");
        Result tag = controller.getTag();
        if (tag != null ) {
            System.out.println("Tag found");
            this.state = Command.QRValidate;
            return;
        }
        switch(lostMode) {
            case 0:

                System.out.println("Fly backwards");
                cmd.backward(4).doFor(200);
                lostMode = 1;
                break;
            case 1:
                System.out.println("Look right");
                cmd.spinRight(10).doFor(200);
                cmd.spinLeft(1).doFor(5);
                //Thread.currentThread().sleep(50);
                lostMode = 2;
                break;
            case 2:
                System.out.println("Look left");
                cmd.spinLeft(10).doFor(200);
                cmd.spinRight(1).doFor(5);
                lostMode = 1;
                break;
        }
    }
private boolean firstTag = false;
    public void qRValidate() {
    	System.out.print("State: QRValidate: ");
    	Result tag = controller.getTag();
        if (tag == null) {
            if (firstTag) {
                System.out.println("Tag Lost");
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
    }


    public void qRCentralizing() throws InterruptedException {
        //Centralize QR tag method
        System.out.print("State: QRcentralizing - ");
        Result tag = controller.getTag();
        if (tag == null) {
        	System.out.println("no tag, back to searching");
        	this.state = Command.QRSearch;
        	return;
        }
        final int SPEED = 5;
        final int SLEEP = 500;
        ResultPoint[] points;
		synchronized (tag) {
			points = tag.getResultPoints();
		}
		int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;

		float x = (points[1].getX() + points[2].getX()) / 2; // True middle
		float y = points[1].getY();

//		if ((tagOrientation > 10) && (tagOrientation < 180)) {
//			System.out.println("AutoController: Spin left");
//			drone.getCommandManager().spinLeft(SPEED * 2);
//			Thread.currentThread();
//			Thread.sleep(SLEEP);
//		} else if ((tagOrientation < 350) && (tagOrientation > 180)) {
//			System.out.println("AutoController: Spin right");
//			drone.getCommandManager().spinRight(SPEED * 2);
//			Thread.currentThread();
//			Thread.sleep(SLEEP);
//		} else
		if (x < (imgCenterX - MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go left");
            cmd.goLeft(SPEED).doFor(30).hover();
			Thread.currentThread().sleep(SLEEP);
		} else if (x > (imgCenterX + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go right");
            cmd.goRight(SPEED).doFor(30).hover();
            Thread.currentThread().sleep(SLEEP);
		} else if (y < (imgCenterY - MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go up");
            cmd.up(SPEED * 2).doFor(60).hover();
            Thread.currentThread().sleep(SLEEP);
		} else if (y > (imgCenterY + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go down");
            cmd.down(SPEED * 2).doFor(60).hover();
            Thread.currentThread().sleep(SLEEP);
		} else {
			System.out.println("AutoController: Tag centered");


            // ADJUSTING TO CIRCLE HEIGHT
            cmd.up(SPEED * 2).doFor(200).hover();
            Thread.currentThread().sleep(200);

            this.state = Command.SearchForCircle;
		}


    }

    public void searchForCircle() throws InterruptedException {
        //Increase altitude and search for the circle
        System.out.print("State: SearchForCircle - ");
        if (controller.getCircles().length >= 1) {
            System.out.println("Circle found!");
            this.state = Command.Centralize;
        }
        else {
        	// TODO Needs to actually FIND the circle
            System.out.println("No circle found");
            int SPEEDSpin = 10;
            int SPEEDMove = 4;
            int doFor = 20;

            cmd.up(20).doFor(30);

            Thread.currentThread().sleep(1500);

            this.state = Command.SearchForCircle;
        }
    }

    String currentCorrect = "";
    String midlertidlig = "";

    public void centralize() throws InterruptedException {
        //Centralize drone in front of circle
        System.out.print("State: Centralize - ");
        int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
        int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;
        if (controller.getCircles().length > 0) {
            // We have more than one circle, figure out which one is correct
            if (controller.getCircles().length > 0) {
                // We have more than one circle, figure out which one is correct
                for (Circle c : controller.getCircles()){
                    if (c.getRadius() >= MasterDrone.IMAGE_HEIGHT / 10) {
                        if (controller.isCircleCentered()) {
                            System.out.println("CENTERED!");
                            this.state = Command.FlyThrough;
                            return;
                        }
                        float leftRightSpeed = (float) ((c.x - imgCenterX) / 30) / 100.0f;

                        float forwardSpeed = (float) ((c.r - 160) / 6 ) / 100.0f;

                        float upDownSpeed = (float) ((imgCenterY - c.y) / 10) / 100.0f;
                        midlertidlig =leftRightSpeed +", " + forwardSpeed +", " + upDownSpeed;
                        if (currentCorrect.equals(midlertidlig)) {
                            System.out.println("Picture FUCKS");
                            cmd.hover().doFor(500);
                            return;
                        }
                        else
                        currentCorrect = midlertidlig;

                        System.out.println("Correcting position, " + leftRightSpeed +", " + forwardSpeed +", " + upDownSpeed);
                        cmd.move(leftRightSpeed, forwardSpeed, upDownSpeed, 0f).doFor(30);
                        cmd.hover();
                        Thread.currentThread().sleep(300);

                        break;
                    }
                }
            }
        }
        else {
            state = Command.SearchForCircle;
        }
    }

    public void flyThrough() throws InterruptedException {
    	System.out.print("State: flyThrough - ");
        System.out.println("AutoController: Going through port " + nextPort);
        cmd.forward(16).doFor(2500);
//        Thread.currentThread().sleep(1200);
        cmd.hover();
        System.out.println("Returning to Hover State");
        state = Command.UpdateGate;
    }

    public void updateGate() {
    	System.out.print("State: updateGate: ");
        //Changing which QR tag to search for next
    	nextPort++;
    	
        System.out.println("Next port is " + nextPort);

        //Changing state to finishing or searching for next tag depending on port state
        if(nextPort>maxPorts) {
        	System.out.println("setting state to finish");
        	this.state=Command.Finished;
        }
        else {
        	System.out.println("setting state hover");
        	this.state=Command.Hover;
        }
    }

    public void finish() {
        System.out.println("State: Finish");
        cmd.landing();
        controller.stopController();
    }
}
