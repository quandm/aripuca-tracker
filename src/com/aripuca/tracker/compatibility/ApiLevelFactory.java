package com.aripuca.tracker.compatibility;

import android.os.Build;

/**
 * ApiLevel factory class
 */
public class ApiLevelFactory {

	private static ApiLevel apiLevel;

	/**
	 * 
	 */
	public static ApiLevel getApiLevel() {

		final int level = Build.VERSION.SDK_INT;

		if (apiLevel == null) {

			if (level >= 8) {
				apiLevel = new ApiLevel8();
				return apiLevel;
			} else if (level >= 5) {
				apiLevel = new ApiLevel5();
				return apiLevel;
			} else {
				apiLevel = new ApiLevel4();
				return apiLevel;
			}

		}
		return apiLevel;
	}
}
