import de.yadrone.apps.controlcenter.plugins.keyboard.KeyboardCommandManagerAlternative;
import de.yadrone.base.IARDrone;

import java.awt.*;
import java.awt.event.KeyEvent;

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
                keyboardCommandManager.keyPressed(e);
            } 
			else if (e.getID() == KeyEvent.KEY_RELEASED) 
            {
                keyboardCommandManager.keyReleased(e);
            }
            return false;
		}
	};
}
