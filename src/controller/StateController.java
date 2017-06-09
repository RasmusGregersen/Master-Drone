package controller;

import de.yadrone.base.IARDrone;
import de.yadrone.base.command.LEDAnimation;

import javax.xml.bind.SchemaOutputResolver;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

/**
 * Created by Dave on 07/06/2017.
 */
public class StateController {


    public enum Command {
        ReadyForTakeOff,Hover,QRSearching,QRCheck,QRValidated,QRCentralized,CircleFound,DroneCentralized,FlownThrough, Finished
    }

    public Command state;
    
    private IARDrone drone;
    private MainDroneController controller;
    
    private int nextPort = 0; // Consider handling this in MainDronController
    
    public StateController(MainDroneController mc, IARDrone drone) {
    	this.controller = mc;
    	this.drone = drone;
    }


    public void commands(Command command){
        switch(command){
            case ReadyForTakeOff: takeOff();
                break;
            case Hover: hover();
                break;
            case QRSearching: qRSearch(); // Hannibal
                break;
            case QRCheck: qRValidate(); // Nichlas
                break;
            case QRValidated: qRCentralizing(); // Nichlas
                break;
            case QRCentralized: searchForCircle();
                break;
            case CircleFound: centralize(); // Lars
                break;
            case DroneCentralized: flyThrough(); // David
                break;
            case FlownThrough: updateGate();
                break;
            case Finished: finish();
                break;
        }
    }


    public void takeOff(){
        //Takeoff
        System.out.println("ReadyForTakeOff");
        drone.takeOff();
        
        //Check conditions and transit to next state
        state = Command.Hover;
    }


    public void hover() {
        //Hover method
        System.out.println("Hover");
        drone.hover();
        //Check conditions and transit to next state
        state = Command.QRSearching;
    }


    public void qRSearch() {
        //Searching method
        System.out.println("QRSearch");
        //TODO: Implement qRSearch method

        //Check conditions
        //TODO: Implement check to see if QR tag is found. Keep looking or transit state
    }
    
    
    
    

    public void qRValidate() {
    	System.out.print("State: QRValidate: ");
    	if (controller.getTag() == null ) {
    		System.out.println("no tag");
    		this.state = Command.QRSearching;
    	}
    	// The scanned QR is the next port we need
    	if (controller.getPorts().get(nextPort).equals(controller.getTag().getText())) {
    		System.out.println("Validated port: " + nextPort);
    		this.state = Command.QRValidated;    		
    	}
    }
    
    
    
    
    

    public void qRCentralizing() throws InterruptedException {
        //Centralize QR tag method
        System.out.print("State: QRcentralizing - ");
        Result tag = controller.getTag();
        if (tag == null) {
        	System.out.println("no tag, back to searching");
        	this.state = Command.QRSearching;
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
		//} else 
		if (x < (imgCenterX - MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go left");
			drone.getCommandManager().goLeft(SPEED).doFor(30);
			Thread.currentThread();
			Thread.sleep(SLEEP);				
		} else if (x > (imgCenterX + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go right");
			drone.getCommandManager().goRight(SPEED).doFor(30);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else if (y < (imgCenterY - MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go up");
			drone.getCommandManager().up(SPEED * 2).doFor(60);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else if (y > (imgCenterY + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go down");
			drone.getCommandManager().down(SPEED * 2).doFor(60);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else {
			System.out.println("AutoController: Tag centered");
			drone.getCommandManager().setLedsAnimation(LEDAnimation.BLINK_GREEN, 10, 5);
			this.state = Command.QRCentralized;
		}
    }

    public void searchForCircle() {
        //Increase altitude and search for the circle
        System.out.println("SearchForCircle");
        //TODO: Implement method to find circle

        //Check if circle found and transit state
        //TODO: Check if circle is found and transit state, otherwise back to search.
    }

    public void centralize() {
        //Centralize drone in front of circle
        System.out.println("Centralize");
        //TODO: Implement centralizing

        //Check if drone is centralized and transit state or move back to searching.
        //TODO: Check if drone i centralized, otherwise change state to circle search
    }

    public void flyThrough() {
        //Flying through the ring
        System.out.println("FlyThrough");
        //TODO: Implement flythrough

        //Updating port to search for and transit state
        //TODO: Transit state
    }

    public void updateGate() {
        //Changing which QR tag to search for next
        System.out.println("UpdateGate");
        //TODO: Implement port state changer

        //Changing state to finishing or searching for next tag depending on port state
        //TODO: Implement update gate and transit state
    }

    public void finish() {
        System.out.println("Finish");
       drone.landing();
    }
}
