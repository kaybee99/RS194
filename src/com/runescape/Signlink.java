package com.runescape;

import java.applet.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;
import java.util.zip.*;
import javax.swing.*;

public final class Signlink implements Runnable {

	private static final Logger logger = Logger.getLogger(Signlink.class.toString());

	public static Applet mainapp;
	public static boolean sunjava;
	private static boolean active;
	private static InetAddress socketip;
	private static int socketreq;
	private static Socket socket = null;
	private static int threadreqpri = 1;
	private static Runnable threadreq = null;
	private static String dnsreq = null;
	private static String loadreq = null;
	private static byte[] loadbuf = null;
	private static String savereq = null;
	private static byte[] savebuf = null;
	private static String urlreq = null;
	private static DataInputStream urlstream = null;
	public static String midi = null;
	public static String jingle = null;
	public static int jinglelen = 3500;
	public static int looprate = 100;
	public static File cacheDirectory;

	static {
		// default it
		cacheDirectory = findCachePath();
	}

	/**
	 * Used to allow the user to manually select the path to load their cache
	 * from.
	 *
	 * @param parent the parent component for the chooser.
	 */
	public static final void openCacheChooser(Component parent) {
		JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setMultiSelectionEnabled(false);
		fc.setDialogType(JFileChooser.OPEN_DIALOG);

		if (fc.showDialog(parent, "Select Cache") == JFileChooser.APPROVE_OPTION) {
			cacheDirectory = fc.getSelectedFile();
		}
	}

	public static final void startPrivate(InetAddress ip) {
		if (!active) {
			socketip = ip;
			Thread t = new Thread(new Signlink());
			t.setDaemon(true);
			t.start();
		}
	}

	@Override
	public final void run() {
		if (!active) {
			active = true;

			File path = findCachePath();

			logger.log(Level.INFO, "Cache path: {0}", path);

			for (;;) {
				if (socketreq != 0) {
					try {
						socket = new Socket(socketip, socketreq);
					} catch (Exception e) {
						socket = null;
						logger.log(Level.WARNING, "Error opening socket", e);
					}
					socketreq = 0;
				}

				if (threadreq != null) {
					Thread t = new Thread(threadreq);
					t.setDaemon(true);
					t.start();
					t.setPriority(threadreqpri);
					threadreq = null;
				}

				if (dnsreq != null) {
					try {
						InetAddress.getByName(dnsreq).getHostName();
					} catch (Exception e) {
						logger.log(Level.WARNING, "Error getting host name", e);
					}
					dnsreq = null;
				}

				if (loadreq != null) {
					loadbuf = null;

					// attempt to load file relative to classpath (includes within .jar)
					try (InputStream is = ClassLoader.getSystemResourceAsStream(loadreq)) {
						loadbuf = new byte[is.available()];
						is.read(loadbuf, 0, loadbuf.length);
					} catch (Exception e) {
						// ignore: logger.log(Level.WARNING, "Error loading file as stream", e);
					}

					File f = new File(path, loadreq);

					if (f.exists()) {
						int i = (int) f.length();
						loadbuf = new byte[i];
						try (DataInputStream dis = new DataInputStream(new FileInputStream(f))) {
							dis.readFully(loadbuf, 0, i);
						} catch (Exception e) {
							logger.log(Level.WARNING, "Error loading file", e);
						}
					}

					loadreq = null;
				}

				if (savereq != null) {
					try (FileOutputStream fos = new FileOutputStream(new File(path, savereq))) {
						fos.write(savebuf, 0, savebuf.length);
					} catch (Exception e) {
						logger.log(Level.WARNING, "Error saving file", e);
					}
					savereq = null;
				}

				if (urlreq != null) {
					try {
						urlstream = new DataInputStream(new URL(mainapp.getCodeBase(), urlreq).openStream());
					} catch (Exception e) {
						urlstream = null;
						logger.log(Level.WARNING, "Error opening url", e);
					}
					urlreq = null;
				}

				try {
					Thread.sleep((long) looprate);
				} catch (Exception e) {
					/* empty */
				}
			}
		}
	}

	public static final File findCachePath() {
		if (cacheDirectory != null) {
			return cacheDirectory;
		}

		String[] paths = {"c:/windows/", "c:/winnt/", "d:/windows/", "d:/winnt/", "e:/windows/", "e:/winnt/", "f:/windows/", "f:/winnt/", "c:/", "~/", "/tmp/", ""};
		String folder = ".file_store_32/";

		for (String path : paths) {
			try {
				if (path.length() > 0) {
					if (!new File(path).exists()) {
						continue;
					}
				}

				File f = new File(path + folder);

				if ((f.exists() && f.isDirectory()) || f.mkdir()) {
					return f;
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "Error finding cache path", e);
			}
		}
		return null;
	}

	public static final void setLoopRate(int rate) {
		looprate = rate;
	}

	public static final File getFile(String s) {
		URL url = getURL(s);
		if (url != null) {
			return new File(url.getFile());
		}
		return null;
	}

	public static final URL getURL(String s) {
		File f = new File(cacheDirectory, s);

		if (f.exists()) {
			try {
				return f.toURI().toURL();
			} catch (MalformedURLException e) {
				logger.log(Level.WARNING, "Malformed url", e);
			}
		}

		return ClassLoader.getSystemResource(s);
	}

	public static final byte[] getCompressed(byte[] src) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
			gzos.write(src);
			return baos.toByteArray();
		}
	}

	public static final byte[] getDecompressed(byte[] src) throws IOException {
		byte[] buffer = new byte[4 * 1024];// 4kb
		int read;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(src))) {
			while ((read = gzis.read(buffer)) > 0) {
				baos.write(buffer, 0, read);
			}
			return baos.toByteArray();
		}
	}

	public static final boolean filesExist(String... s) {
		for (String path : s) {
			if (!getFile(path).exists()) {
				return false;
			}
		}
		return true;
	}

	public static final byte[] loadFile(String s) {
		if (!active) {
			return null;
		}

		if (loadreq != null) {
			return null;
		}

		// TODO: loadreq = String.valueOf(gethash(s));
		loadreq = s;
		while (loadreq != null) {
			try {
				Thread.sleep(1L);
			} catch (Exception exception) {
				/* empty */
			}
		}
		return loadbuf;
	}

	public static final void saveFile(String string, byte[] is) {
		if (active && savereq == null && is.length <= 2000000) {
			savebuf = is;
			savereq = String.valueOf(StringUtil.toBase37(string));
			savereq = string;
			while (savereq != null) {
				try {
					Thread.sleep(1L);
				} catch (Exception e) {
					/* empty */
				}
			}
		}
	}

	public static final Socket openSocket(int i) throws IOException {
		socketreq = i;
		while (socketreq != 0) {
			try {
				Thread.sleep(50L);
			} catch (Exception e) {
				/* empty */
			}
		}
		if (socket == null) {
			throw new IOException("could not open socket");
		}
		return socket;
	}

	public static final String lookupDNS(String string) {
		return "unknown";
	}

	public static final void startThread(Runnable runnable, int i) {
		threadreqpri = i;
		threadreq = runnable;
	}

	public static final DataInputStream openURL(String string) throws IOException {
		urlreq = string;
		while (urlreq != null) {
			try {
				Thread.sleep(50L);
			} catch (Exception exception) {
				/* empty */
			}
		}
		if (urlstream == null) {
			throw new IOException("could not open: " + string);
		}
		return urlstream;
	}
}
