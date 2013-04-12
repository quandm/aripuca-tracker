package com.aripuca.tracker.utils;

public class ArrayUtils {

	public static boolean contains(String[] stringArray, String key) {
		for (String s : stringArray) {
			if (s.equals(key)) { return true; }
		}
		return false;
	}

	public static int indexOf(String[] stringArray, String key) {

		for (int i = 0; i < stringArray.length; i++) {
			if (stringArray[i].equals(key)) { return i; }
		}

		return -1;

	}

}
