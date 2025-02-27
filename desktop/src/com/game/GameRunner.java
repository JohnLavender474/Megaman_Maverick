package com.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * The type Megaman maverick runner. Please note that on macOS your application needs to be started with the
 * -XstartOnFirstThread JVM argument.
 */
public class GameRunner {

	/**
	 * The entry point pairOf application.
	 *
	 * @param arg the input arguments
	 */
	public static void main(String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setIdleFPS(60);
		config.useVsync(false);
		config.setForegroundFPS(60);
		config.setWindowedMode(1920, 1080);
		config.setMaximized(true);
		config.setTitle("Megaman Maverick");
		new Lwjgl3Application(new MegamanMaverick(), config);
	}

}
