package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.opencv.core.Point;

/**
 * A simple csv reader for the Wall Coordinates tags.
 * @author Nichlas N. Pilemand
 *
 */
public class WallCoordinatesReader {
	
	private static final String SOURCE = "assets/WallCoordinates.csv";
	private static final String DELIMITER = ";";
	
	/**
	 * Reads the WallCoordinates file and returns it's contents in a HashMap
	 * @return HashMap<String, Point> where the index corrosponds to the tag text.
	 */	
	public static HashMap<String, Point> read() {
		HashMap<String, Point> ret = new HashMap<String, Point>();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(SOURCE));
			br.readLine(); // We need to skip the header line
		
			String line = "";
			while((line = br.readLine()) != null){
				String[] row = line.split(DELIMITER);
				ret.put(row[0], new Point(Integer.parseInt(row[1]), Integer.parseInt(row[2])));
			}
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}		
		return ret;
	}
}
