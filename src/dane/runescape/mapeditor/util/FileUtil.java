package dane.runescape.mapeditor.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

public class FileUtil {

	public static final URL getURL(String s) {
		URL url = ClassLoader.getSystemResource(s);
		if (url != null) {
			return url;
		}
		File file = new File(s);
		if (file.exists()) {
			try {
				return file.toURI().toURL();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static final BufferedImage readImage(String s) throws IOException {
		return ImageIO.read(getURL(s));
	}

}
