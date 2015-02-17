/*
 * The MIT License
 *
 * Copyright 2015 Dane.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package dane.runescape.mapeditor.util;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;
import javax.imageio.*;

/**
 * File shortcuts.
 *
 * @author Dane
 */
public class FileUtil {

	private static final Logger logger = Logger.getLogger(FileUtil.class.getName());

	/**
	 * Searches for system resources first, and defaults to creating a new url.
	 *
	 * @param path the path.
	 * @return the url or <b>null</b> if no file with the specified path exists.
	 */
	public static final URL getURL(String path) {
		URL url = ClassLoader.getSystemResource(path);

		// if we found it as our resource then use that first
		if (url != null) {
			return url;
		}

		File file = new File(path);

		// it's a real file
		if (file.exists()) {
			try {
				return file.toURI().toURL();
			} catch (MalformedURLException e) {
				logger.log(Level.WARNING, "Bad URL", e);
			}
		}

		// no file found :(
		return null;
	}

	/**
	 * Reads an image from the system resources or local directory.
	 *
	 * @param path the path.
	 * @return the image or <b>null</b> if no image with the specified path
	 * exists.
	 * @throws IOException if an error occurs while reading.
	 */
	public static final BufferedImage readImage(String path) throws IOException {
		return ImageIO.read(getURL(path));
	}

}
