package tests;

import java.util.HashMap;

import org.opencv.core.Point;

import utils.WallCoordinatesReader;

public class WallCoordinatesTest {

	public static void main(String[] args) {
		HashMap<String, Point> map = WallCoordinatesReader.read();
		System.out.println(map.get("W00.00"));
		System.out.println(map.get("W03.04"));
	}
}
