package controller;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import de.yadrone.base.IARDrone;
import de.yadrone.base.navdata.BatteryListener;
import de.yadrone.base.video.ImageListener;
import imgManagement.Circle;
import imgManagement.CircleFinder;
import imgManagement.TagListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class GUI extends JFrame implements ImageListener, TagListener {
	private MasterDrone main;
	private IARDrone drone;

	private BufferedImage image = null;
	private Result result;
	private String orientation;
	private Circle[] circles;

	private JPanel videoPanel;
	private int batterypercentage;

	public GUI(final IARDrone drone, MasterDrone main) {
		super("Master Drone");

		this.main = main;
		this.drone = drone;

		batteryListener();

		createMenuBar();

        setSize(MasterDrone.IMAGE_WIDTH, MasterDrone.IMAGE_HEIGHT);
        setVisible(true);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				drone.stop();
				System.exit(0);
			}
		});

        setLayout(new GridBagLayout());
        
        add(createVideoPanel(), new GridBagConstraints(0, 0, 1, 2, 1, 1, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
        pack(); 
	}

	private void createMenuBar() {
		JMenu options = new JMenu("Options");

		final JCheckBoxMenuItem autoControlMenuItem = new JCheckBoxMenuItem("Start autonomous flight");
		autoControlMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e)
			{
				main.enableAutoControl(autoControlMenuItem.isSelected());
			}
		});

		options.add(autoControlMenuItem);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(options);

		setJMenuBar(menuBar);
	}

	private JPanel createVideoPanel() {
		videoPanel = new JPanel() {

			/**
			 *
			 */
			private static final long serialVersionUID = 1L;
			private Font tagFont = new Font("SansSerif", Font.BOLD, 14);
			private Font timeFont = new Font("SansSerif", Font.BOLD, 18);
			private Font gameOverFont = new Font("SansSerif", Font.BOLD, 36);

        	public void paint(Graphics g)
        	{
        		if (image != null)
        		{
        			// now draw the camera image
        			g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);

        			// draw the battery percentage
					if(batterypercentage>50) {
						g.setColor(Color.GREEN);
						g.setFont(tagFont);
					}
					else {
						g.setColor(Color.RED);
						g.setFont(tagFont);
					}

					g.drawString("Battery: "+batterypercentage+"%", 0, 15);

        			// draw tolerance field (rectangle)
        			g.setColor(Color.RED);

    				int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
    				int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;
    				int tolerance = MasterDrone.TOLERANCE;

    				g.drawPolygon(new int[] {imgCenterX-tolerance, imgCenterX+tolerance, imgCenterX+tolerance, imgCenterX-tolerance},
						      		  new int[] {imgCenterY-tolerance, imgCenterY-tolerance, imgCenterY+tolerance, imgCenterY+tolerance}, 4);

    				// draw triangle if tag is visible
        			if (result != null)
        			{
        				ResultPoint[] points = result.getResultPoints();
        				ResultPoint a = points[1]; // top-left
        				ResultPoint b = points[2]; // top-right
        				ResultPoint c = points[0]; // bottom-left
        				ResultPoint d = points.length == 4 ? points[3] : points[0]; // alignment point (bottom-right)

        				g.setColor(Color.GREEN);

        				g.drawPolygon(new int[] {(int)a.getX(),(int)b.getX(),(int)d.getX(),(int)c.getX()},
  						      new int[] {(int)a.getY(),(int)b.getY(),(int)d.getY(),(int)c.getY()}, 4);

        				g.setColor(Color.RED);
        				g.setFont(tagFont);
        				g.drawString(result.getText(), (int)a.getX(), (int)a.getY());
        				g.drawString(orientation, (int)a.getX(), (int)a.getY() + 20);

        				if ((System.currentTimeMillis() - result.getTimestamp()) > 1000)
        				{
        					result = null;
        				}
        			}

        			// Draw circles
        			circles = CircleFinder.findCircles(image);
        			for(Circle c : circles){
        				g.setColor(Color.RED);
        				g.drawRect((int)c.x, (int)c.y, 10, 10);
        				g.setColor(Color.BLUE);
        				g.drawOval((int)(c.x - c.r), (int) (c.y - c.r), (int)(2*c.r), (int)(2*c.r));
        			}
        		}
        		else
        		{
        			// draw "Waiting for video"
        			g.setColor(Color.RED);
    				g.setFont(tagFont);
        			g.drawString("Waiting for VideoRecognition ...", 10, 20);
        		}
        	}
        };

        // a click on the video shall toggle the camera (from vertical to horizontal and vice versa)
		videoPanel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e)
			{
				drone.toggleCamera();
			}
		});

        videoPanel.setSize(MasterDrone.IMAGE_WIDTH, MasterDrone.IMAGE_HEIGHT);
        videoPanel.setMinimumSize(new Dimension(MasterDrone.IMAGE_WIDTH, MasterDrone.IMAGE_HEIGHT));
        videoPanel.setPreferredSize(new Dimension(MasterDrone.IMAGE_WIDTH, MasterDrone.IMAGE_HEIGHT));
        videoPanel.setMaximumSize(new Dimension(MasterDrone.IMAGE_WIDTH, MasterDrone.IMAGE_HEIGHT));
        
        return videoPanel;
	}
	
	private long imageCount = 0;

	private void batteryListener() {
		drone.getNavDataManager().addBatteryListener(new BatteryListener() {

			public void batteryLevelChanged(int percentage)
			{
				batterypercentage = percentage;
			}

			@Override
			public void voltageChanged(int vbat_raw) {
			}
		});
	}

	public void imageUpdated(BufferedImage newImage) {
		if ((++imageCount % 2) == 0)
			return;
		
    	image = newImage;
		SwingUtilities.invokeLater(new Runnable() {
			public void run()
			{
				videoPanel.repaint();
			}
		});
    }

	@Override
	public void onTag(Result result, float v) {
		if (result != null) {
			this.result = result;
			this.orientation = v + " deg";
		}
	}
}