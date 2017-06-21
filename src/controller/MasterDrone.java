package controller;

import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.FlyingMode;
import de.yadrone.base.command.VideoBitRateMode;
import de.yadrone.base.command.VideoChannel;
import de.yadrone.base.command.VideoCodec;
import de.yadrone.base.navdata.AttitudeListener;
import imgManagement.CircleFinder;
import imgManagement.QRCodeScanner;
import org.opencv.core.Core;
import java.io.FileNotFoundException;

public class MasterDrone {

	public final static int IMAGE_WIDTH = 1280 /2;
	public final static int IMAGE_HEIGHT = 720 /2;

	public final static int TOLERANCE = 35;

	private IARDrone drone = null;
	private MainDroneController droneController;
	private QRCodeScanner scanner = null;

	private boolean autoControlEnabled = false;

	public MasterDrone() {

		drone = new ARDrone();
		droneController = new MainDroneController(drone);
		KeyboardController keyboardController = new KeyboardController(this, drone);
		drone.start();
		keyboardController.start();
		drone.getCommandManager().setVideoCodec(VideoCodec.H264_360P);
		drone.getCommandManager().setVideoBitrateControl(VideoBitRateMode.MANUAL);
		drone.getCommandManager().setVideoBitrate(4000);
		drone.getCommandManager().setVideoChannel(VideoChannel.HORI);
		drone.getCommandManager().setVideoCodecFps(30);

		drone.getNavDataManager().addAttitudeListener(new AttitudeListener() {
			public void attitudeUpdated(float pitch, float roll, float yaw) {
			}

			@Override
			public void attitudeUpdated(float pitch, float roll) {
			}

			@Override
			public void windCompensation(float pitch, float roll) {
			}
		});
		GUI gui = new GUI(drone, this);

		scanner = new QRCodeScanner();
		scanner.addListener(gui);
		CircleFinder cf = new CircleFinder();

		cf.addListener(droneController);
		cf.addListener(gui);

		drone.getCommandManager().setFlyingMode(FlyingMode.HOVER_ON_TOP_OF_ROUNDEL);

		drone.getVideoManager().addImageListener(droneController);
		drone.getVideoManager().addImageListener(gui);
		drone.getVideoManager().addImageListener(cf);
		drone.getVideoManager().addImageListener(scanner);
	}

	public MainDroneController getDroneController() {
		return droneController;
	}

	public void enableAutoControl(boolean enable) {
		System.out.println("MasterDrone enableAutoControler: " + enable);
		if (enable) {
			scanner.addListener(droneController);
			new Thread(droneController).start(); // Seems to be better to force a new thread
		} else {
			droneController.stopController();
			scanner.removeListener(droneController);
		}
		this.autoControlEnabled = enable;
	}

	// Main program start
	public static void main(String[] args) throws FileNotFoundException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Load OpenCV
		new MasterDrone();
	}

	public boolean getAutoControlEnabled() {
		return autoControlEnabled;
	}

	public int getAltitude() {
		return this.droneController.getAltitude();
	}
}
