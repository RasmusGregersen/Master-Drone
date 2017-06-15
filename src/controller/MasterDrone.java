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
import java.io.FileOutputStream;
import java.io.PrintStream;

public class MasterDrone {

	public final static int IMAGE_WIDTH = 1280/2;
	public final static int IMAGE_HEIGHT = 720/2;

	public final static int TOLERANCE = 40;

	private IARDrone drone = null;
	private MainDroneController droneController;
	private QRCodeScanner scanner = null;
	
	private boolean autoControlEnabled = false;

	public MasterDrone() {
		
		drone = new ARDrone();
		KeyboardController keyboardController = new KeyboardController(this, drone);
		droneController = new MainDroneController(drone);
		drone.start();
		new Thread(keyboardController).start();
		drone.getCommandManager().setVideoCodec(VideoCodec.H264_360P);
		drone.getCommandManager().setVideoBitrateControl(VideoBitRateMode.MANUAL);
		//drone.getCommandManager().setMaxVideoBitrate(4000);
		drone.getCommandManager().setVideoBitrate(1400);
		drone.getCommandManager().setVideoChannel(VideoChannel.HORI);
		drone.getCommandManager().setVideoCodecFps(30);

		
		drone.getNavDataManager().addAttitudeListener(new AttitudeListener() {
			public void attitudeUpdated(float pitch, float roll, float yaw){
				//droneYaw = yaw/1000;
			}
			@Override
			public void attitudeUpdated(float pitch, float roll) {}

			@Override
			public void windCompensation(float pitch, float roll) {}
		});
		GUI gui = new GUI(drone, this);

		
		
		scanner = new QRCodeScanner();
		scanner.addListener(gui);
		CircleFinder cf = new CircleFinder();
		
		cf.addListener(droneController);
		cf.addListener(gui);
		
		drone.getCommandManager().setFlyingMode(FlyingMode.HOVER_ON_TOP_OF_ROUNDEL);
		//drone.getCommandManager().setFlyingMode(FlyingMode.HOVER_ON_TOP_OF_ORIENTED_ROUNDEL);

		Thread g = new Thread() {
			public void run() {
				drone.getVideoManager().addImageListener(gui);
				drone.getVideoManager().addImageListener(droneController);
			}
		};
		g.start();

		Thread q = new Thread() {
			public void run() {
				drone.getVideoManager().addImageListener(scanner);
			}
		};
		q.start();

		Thread c = new Thread() {
			public void run() {
				drone.getVideoManager().addImageListener(cf);
			}
		};
		c.start();
	}

	public MainDroneController getDroneController() {
		return droneController;
	}

	public void enableAutoControl(boolean enable) {
		if (enable) {
			scanner.addListener(droneController);
			new Thread(droneController).start();
		} else {
			droneController.stopController();
			scanner.removeListener(droneController);
		}
	}

	// Main program start
	public static void main(String[] args) throws FileNotFoundException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Load OpenCV
		//PrintStream out = new PrintStream(new FileOutputStream("output.txt"));
		//System.setOut(out);
		new MasterDrone();
	}

	public boolean getAutoControlEnabled() {
		return autoControlEnabled;
	}

}
