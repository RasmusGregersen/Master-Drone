package controller;

import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.VideoChannel;
import imgManagement.QRCodeScanner;

public class MasterDrone {

	public final static int IMAGE_WIDTH = 640;
	public final static int IMAGE_HEIGHT = 360;

	public final static int TOLERANCE = 40;

	private IARDrone drone = null;
	private MainDroneController droneController;
	private QRCodeScanner scanner = null;

	public MasterDrone() {
		drone = new ARDrone();
		drone.start();
		drone.getCommandManager().setVideoChannel(VideoChannel.HORI);

		KeyboardController keyboardController = new KeyboardController(drone);
		keyboardController.start();

		droneController = new MainDroneController(drone);

		scanner = new QRCodeScanner();

		drone.getVideoManager().addImageListener(scanner);
	}

	public void enableAutoControl(boolean enable) {
		if (enable) {
			scanner.addListener(droneController);
			droneController.start();
		} else {
			droneController.stopController();
			scanner.removeListener(droneController); // only auto autoController
													// registers as TagListener
		}
	}

	// Main program start
	public static void main(String[] args) {
		new MasterDrone();
	}

}
