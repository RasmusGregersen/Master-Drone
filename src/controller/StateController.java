package controller;

import de.yadrone.base.IARDrone;

import javax.xml.bind.SchemaOutputResolver;

/**
 * Created by Dave on 07/06/2017.
 */
public class StateController {


    public enum Command {
        ReadyForTakeOff,Hover,QRSearching,QRFound,QRValidated,QRCentralized,CircleFound,DroneCentralized,FlownThrough, Finished
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
            case QRFound: qRValidate(); // Nichlas
                break;
            case QRValidated: qRCentralizing();
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


    public void qRCentralizing() {
        //Centralize QR tag method
        System.out.println("QRcentralizing");
        //TODO: Implement method to centralize QR tag

        //If succesfully centralized, transit otherwise move back to searching
        //TODO: Implement check otherwise move back to search
    }

    public void searchForCircle() {
        //Increase altitude and search for the circle
        System.out.println("SearchForCircle");
        if (controller.getCircles().length > 1) {
            System.out.println("Circle found!");
            this.state = Command.CircleFound;
        }
        else {
            System.out.println("Returning TO QR SEARCH!");
            this.state = Command.QRSearching;
        }



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
        System.out.println("AutoController: Going through port " + nextPort);
        drone.getCommandManager().forward(50).doFor(1500);
        drone.getCommandManager().hover().doFor(1200);
        Thread.currentThread();
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Returning to Hover State");
        state = Command.Hover;
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
