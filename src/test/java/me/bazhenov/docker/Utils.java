package me.bazhenov.docker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

class Utils {

	static String readLineFrom(int port) {
		try {
			URL obj = new URL("http://localhost:" + port + "/");
			URLConnection connection = obj.openConnection();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				return in.readLine();
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
