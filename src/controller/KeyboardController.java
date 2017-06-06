package controller;
import de.yadrone.apps.controlcenter.plugins.keyboard.KeyboardCommandManagerAlternative;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.CalibrationCommand;
import de.yadrone.base.command.Device;
import de.yadrone.base.command.HoverCommand;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * More or less a copy of the YA Drone keyboard controller.
 * @see {@link KeyboardCommandManagerAlternative#handleCommand} for inputs
 * @author Nichlas N. Pilemand
 *
 */
public class KeyboardController extends AbstractController
{
	private KeyboardCommandManagerAlternative keyboardCommandManager;
	
	public KeyboardController(IARDrone drone)
	{
		super(drone);
	}
	
	public void run()
	{
        keyboardCommandManager = new KeyboardCommandManagerAlternative(drone);
		
		// CommandManager handles (keyboard) input and dispatches events to the drone
		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(keyEventDispatcher);
	}

	private KeyEventDispatcher keyEventDispatcher = new KeyEventDispatcher() {
    	
    	public boolean dispatchKeyEvent(KeyEvent e)
		{
			if (e.getID() == KeyEvent.KEY_PRESSED) 
			{
				System.out.println("Key registered: " + e.getKeyChar());
				if (e.getKeyCode() == KeyEvent.VK_C){
					drone.getCommandManager().setCommand(new CalibrationCommand(Device.MAGNETOMETER));
					System.out.println("AutoController: Calibrate");
				} else if (e.getKeyCode() == KeyEvent.VK_H) {
					drone.getCommandManager().hover().doFor(500);
					System.out.println("Hover command!!");
				}
					
				else
					keyboardCommandManager.keyPressed(e);
				// TODO For now any key command just kills the drone
				//drone.reset();
            } 
			else if (e.getID() == KeyEvent.KEY_RELEASED) 
            {
                keyboardCommandManager.keyReleased(e);
            }
            return false;
		}
	};
}
