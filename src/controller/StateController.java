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
        //Validate QR method
        System.out.println("QRValidation");
        //TODO: Implement method to validate QR tag

        //Check if validated and transit state
        //TODO: Implement to check if validated and transit state. Otherwise move back to searching
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
        System.out.println("AutoController: Going through port " + controller.getPorts());
        drone.getCommandManager().forward(50).doFor(1500);
        drone.getCommandManager().hover().doFor()

        //Updating port to search for and transit state
        //TODO: Transit state

        System.out.println("AutoController: Going through port " + controller.getPorts());
//		while(true) {
//			if (!isCircleCentered())
//				break;
//			drone.getCommandManager().forward(SPEED);
//			Thread.currentThread();
//			Thread.sleep(SLEEP);
//		}
//		// TODO Here we assume we're so close to the circle that we no longer see it
//		// so fly forward
        drone.getCommandManager().forward(SPEED*8).doFor(doFor*2);
        Thread.currentThread();
        Thread.sleep(SLEEP);





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
