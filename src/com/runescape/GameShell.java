package com.runescape;

import dane.runescape.mapeditor.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.event.*;

public abstract class GameShell extends JApplet implements Runnable, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener, FocusListener {

	private static final long serialVersionUID = 4519439019496437757L;

	private static final Logger logger = Logger.getLogger(GameShell.class.getName());

	public static final int KEY_LEFT = 1;
	public static final int KEY_RIGHT = 2;
	public static final int KEY_UP = 3;
	public static final int KEY_DOWN = 4;
	public static final int KEY_CTRL = 5;
	public static final int KEY_SHIFT = 6;
	public static final int KEY_BACKSPACE = 8;
	public static final int KEY_TAB = 9;
	public static final int KEY_ENTER = 10;
	public static final int KEY_ALT = 11;

	public static final int KEY_F1 = 1008;
	public static final int KEY_F2 = KEY_F1 + 1;
	public static final int KEY_F3 = KEY_F2 + 1;
	public static final int KEY_F4 = KEY_F3 + 1;
	public static final int KEY_F5 = KEY_F4 + 1;
	public static final int KEY_F6 = KEY_F5 + 1;
	public static final int KEY_F7 = KEY_F6 + 1;
	public static final int KEY_F8 = KEY_F7 + 1;
	public static final int KEY_F9 = KEY_F8 + 1;
	public static final int KEY_F10 = KEY_F9 + 1;
	public static final int KEY_F11 = KEY_F10 + 1;
	public static final int KEY_F12 = KEY_F11 + 1;

	/**
	 * The state of the game loop. List of meanings and values:<br/>
	 * <b>-2</b> Has been shutdown<br/>
	 * <b>-1</b> Shutting down<br/>
	 * <b>0</b>	Running<br/>
	 * <b>>0</b> Shutdown in x cycles
	 */
	public int state;
	/**
	 * The goal amount of time the game loop wants to sleep.
	 */
	public int deltime = 20;
	/**
	 * The minimum amount of time the game loop can sleep.
	 */
	public int minDelay = 1;
	/**
	 * Used to track listeners.
	 */
	protected final EventListenerList listeners = new EventListenerList();
	/**
	 * How long it took for the last frame to draw in milliseconds.
	 */
	public double frameTime;
	/**
	 * An array of operation times in milliseconds for the last 10 frames.
	 */
	private final long[] optim = new long[10];
	/**
	 * How many frames were drawn in the last second.
	 */
	public int fps;
	/**
	 * The width of the shell.
	 */
	public int width;
	/**
	 * The height of the shell.
	 */
	public int height;
	/**
	 * The destination graphics object all image producers will draw to.
	 */
	public Graphics graphics;
	/**
	 * The frame used if the shell is initialized via <code>initFrame</code>.
	 */
	public JFrame frame;
	/**
	 * Set to true when focus is gained, or either of these methods are called:
	 * <code>update(g)</code>, <code>paint(g)</code>.
	 */
	public boolean refresh = true;
	/**
	 * How many cycles the game loop has gone through since the last input
	 * event.
	 */
	public int idleCycles;
	/**
	 * The mouse button currently being held down.
	 */
	public int dragButton;
	/**
	 * The current horizontal mouse position.
	 */
	public int mouseX;
	/**
	 * The current vertical mouse position.
	 */
	public int mouseY;
	/**
	 * The mouse button pressed in the last frame.
	 */
	public int mouseButton;
	/**
	 * The amount the mouse wheel rotated in the last frame.
	 */
	public int mouseWheel;
	/**
	 * The x position the mouse was pressed in the last frame.
	 */
	public int clickX;
	/**
	 * The y position the mouse was pressed in the last frame.
	 */
	public int clickY;
	/**
	 * An array of booleans describing whether an action key is pressed.
	 */
	public boolean[] keyDown = new boolean[128];
	/**
	 * A buffer of pressed keys.
	 */
	private final int[] keyBuffer = new int[128];
	/**
	 * The last position the key buffer has reached.
	 */
	private int lastKeyBufferPos;
	/**
	 * The current position the key buffer is at.
	 */
	private int keyBufferPos;

	/**
	 * Initializes and wraps the shell in a JFrame.
	 *
	 * @param width the width.
	 * @param height the height.
	 */
	public void initFrame(int width, int height) {
		this.width = width;
		this.height = height;
		this.frame = new GameFrame(this, this.width, this.height);
		this.graphics = this.getGraphics();
		this.startThread(this, 1);
	}

	/**
	 * Initializes the shell.
	 *
	 * @param width the width.
	 * @param height the height.
	 */
	public void initApplet(int width, int height) {
		this.width = width;
		this.height = height;
		this.graphics = getGraphics();
		this.startThread(this, 1);
	}

	@Override
	public void addNotify() {
		super.addNotify();
		this.requestFocus();
	}

	@Override
	public void run() {
		this.setBackground(Color.BLACK);
		this.setFocusable(true);

		logger.log(Level.INFO, "Registering event listeners");
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addMouseWheelListener(this);
		this.addKeyListener(this);
		this.addFocusListener(this);

		this.firePreStartup();
		this.drawProgress("Loading...", 0);
		this.startup();
		this.firePostStartup();

		int currentFrame = 0;
		int ratio = 256;
		int delay = 1;
		int cycle = 0;

		for (int n = 0; n < 10; n++) {
			this.optim[n] = System.currentTimeMillis();
		}

		long currentTime;

		while (this.state >= 0) {
			if (this.state > 0) {
				this.state--;
				if (this.state == 0) {
					forceShutdown();
					return;
				}
			}

			int lastRatio = ratio;
			int lastDelta = delay;

			ratio = 300;
			delay = 1;
			currentTime = System.currentTimeMillis();

			if (this.optim[currentFrame] == 0L) {
				ratio = lastRatio;
				delay = lastDelta;
			} else if (currentTime > this.optim[currentFrame]) {
				ratio = (int) ((long) (this.deltime * 2560) / (currentTime - this.optim[currentFrame]));
			}

			if (ratio < 25) {
				ratio = 25;
			}

			if (ratio > 256) {
				ratio = 256;
				delay = (int) ((long) this.deltime - (currentTime - this.optim[currentFrame]) / 10L);
			}

			this.optim[currentFrame] = currentTime;
			currentFrame = (currentFrame + 1) % 10;

			if (delay > 1) {
				for (int n = 0; n < 10; n++) {
					if (this.optim[n] != 0L) {
						this.optim[n] += (long) delay;
					}
				}
			}

			if (delay < this.minDelay) {
				delay = this.minDelay;
			}

			try {
				Thread.sleep((long) delay);
			} catch (InterruptedException e) {

			}

			for (/**/; cycle < 256; cycle += ratio) {
				this.update();
				this.fireUpdate(); // send listeners notification

				this.mouseWheel = 0;
				this.mouseButton = 0;
				this.lastKeyBufferPos = this.keyBufferPos;
			}

			cycle &= 0xFF;

			if (this.deltime > 0) {
				this.fps = (ratio * 1000) / (this.deltime * 256);
			}

			long nano = System.nanoTime();
			this.draw();
			this.fireDraw(); // send listeners notification
			this.frameTime = (System.nanoTime() - nano) / 1_000_000.0;
		}

		if (this.state == -1) {
			this.forceShutdown();
		}
	}

	/**
	 * Returns an array list of shell listeners.
	 *
	 * @return the shell listeners.
	 */
	public ShellListener[] getShellListeners() {
		return this.listeners.getListeners(ShellListener.class);
	}

	/**
	 * Notifies all shell listeners that startup is about to be ran.
	 */
	protected void firePreStartup() {
		for (ShellListener l : this.getShellListeners()) {
			l.onPreShellStartup();
		}
	}

	/**
	 * Notifies all shell listeners that startup has been ran.
	 */
	protected void firePostStartup() {
		for (ShellListener l : this.getShellListeners()) {
			l.onPostShellStartup();
		}
	}

	/**
	 * Notifies all shell listeners of an update.
	 */
	protected void fireUpdate() {
		for (ShellListener l : this.getShellListeners()) {
			l.onShellUpdate();
		}
	}

	/**
	 * Notifies all shell listeners of a draw.
	 */
	protected void fireDraw() {
		for (ShellListener l : this.getShellListeners()) {
			l.onShellDraw();
		}
	}

	/**
	 * Notifies all shell listeners of a shutdown.
	 */
	protected void fireShutdown() {
		for (ShellListener l : this.getShellListeners()) {
			l.onShellShutdown();
		}
	}

	/**
	 * Adds a shell listener.
	 *
	 * @param l the listener.
	 */
	public void addShellListener(ShellListener l) {
		this.listeners.add(ShellListener.class, l);
	}

	/**
	 * Removes a shell listener.
	 *
	 * @param l the listener.
	 */
	public void removeShellListener(ShellListener l) {
		this.listeners.remove(ShellListener.class, l);
	}

	/**
	 * Notifies all listeners of a shutdown, invokes the shutdown method, and
	 * forces the application to shut down after one second.
	 */
	public void forceShutdown() {
		this.state = -2;
		logger.log(Level.INFO, "Closing program");

		this.fireShutdown();
		this.shutdown();

		try {
			Thread.sleep(1000L);
		} catch (Exception e) {

		}
		try {
			System.exit(0);
		} catch (Throwable t) {
			// nothing
		}
	}

	/**
	 * Sets the goal delta.
	 *
	 * @param fps the goal frames per second.
	 */
	public void setLoopRate(int fps) {
		deltime = 1000 / fps;
	}

	@Override
	public void start() {
		if (state >= 0) {
			state = 0;
		}
	}

	@Override
	public void stop() {
		if (state >= 0) {
			state = 4000 / deltime;
		}
	}

	@Override
	public void destroy() {
		state = -1;

		try {
			Thread.sleep(5000L);
		} catch (Exception exception) {
		}

		if (state == -1) {
			// means that we didn't shut down fast enough. we should always
			// shut down before 5 seconds pass.
			logger.log(Level.WARNING, "5 seconds expired, forcing kill.");
			forceShutdown();
		}
	}

	@Override
	public void update(Graphics g) {
		refresh = true;
		refresh();
	}

	@Override
	public void paint(Graphics g) {
		refresh = true;
		refresh();
	}

	@Override
	public void setSize(int w, int h) {
		super.setSize(w, h);
		setPreferredSize(getSize());
		width = w;
		height = h;
	}

	/**
	 * Gives this component an awt event to process.
	 *
	 * @param e the event.
	 */
	public void consumeEvent(AWTEvent e) {
		this.processEvent(e);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int x = e.getX();
		int y = e.getY();

		idleCycles = 0;
		mouseX = x;
		mouseY = y;
		mouseWheel = e.getWheelRotation();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();

		idleCycles = 0;
		clickX = x;
		clickY = y;

		if (e.isMetaDown()) {
			mouseButton = 2;
			dragButton = 2;
		} else {
			mouseButton = 1;
			dragButton = 1;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		idleCycles = 0;
		dragButton = 0;
	}

	@Override
	public void mouseClicked(MouseEvent mouseevent) {

	}

	@Override
	public void mouseEntered(MouseEvent mouseevent) {

	}

	@Override
	public void mouseExited(MouseEvent mouseevent) {

	}

	@Override
	public void mouseDragged(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();

		idleCycles = 0;
		mouseX = x;
		mouseY = y;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();

		idleCycles = 0;
		mouseX = x;
		mouseY = y;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		idleCycles = 0;

		int i = e.getKeyCode();
		int c = e.getKeyChar();

		if (c < 30) {
			c = 0;
		}

		if (i == KeyEvent.VK_LEFT) {
			c = KEY_LEFT;
		} else if (i == KeyEvent.VK_RIGHT) {
			c = KEY_RIGHT;
		} else if (i == KeyEvent.VK_UP) {
			c = KEY_UP;
		} else if (i == KeyEvent.VK_DOWN) {
			c = KEY_DOWN;
		} else if (i == KeyEvent.VK_CONTROL) {
			c = KEY_CTRL;
		} else if (i == KeyEvent.VK_SHIFT) {
			c = KEY_SHIFT;
		} else if (i == KeyEvent.VK_BACK_SPACE) {
			c = KEY_BACKSPACE;
		} else if (i == KeyEvent.VK_DELETE) {
			c = KEY_BACKSPACE;
		} else if (i == KeyEvent.VK_TAB) {
			c = KEY_TAB;
		} else if (i == KeyEvent.VK_ENTER) {
			c = KEY_ENTER;
		} else if (i == KeyEvent.VK_ALT) {
			c = KEY_ALT;
		} else if (i >= KeyEvent.VK_F1 && i <= KeyEvent.VK_F12) {
			c = (i + 1008) - KeyEvent.VK_F1;
		} else if (i == KeyEvent.VK_HOME) {
			c = 1000;
		} else if (i == KeyEvent.VK_END) {
			c = 1001;
		} else if (i == KeyEvent.VK_PAGE_UP) {
			c = 1002;
		} else if (i == KeyEvent.VK_PAGE_DOWN) {
			c = 1003;
		}

		if (c > 0 && c < 128) {
			keyDown[c] = true;
		}

		if (c > 6) {
			keyBuffer[keyBufferPos] = c;
			keyBufferPos = keyBufferPos + 1 & 0x7f;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		idleCycles = 0;

		int i = e.getKeyCode();
		char c = e.getKeyChar();

		if (c < 30) {
			c = 0;
		}

		if (i == KeyEvent.VK_LEFT) {
			c = KEY_LEFT;
		} else if (i == KeyEvent.VK_RIGHT) {
			c = KEY_RIGHT;
		} else if (i == KeyEvent.VK_UP) {
			c = KEY_UP;
		} else if (i == KeyEvent.VK_DOWN) {
			c = KEY_DOWN;
		} else if (i == KeyEvent.VK_CONTROL) {
			c = KEY_CTRL;
		} else if (i == KeyEvent.VK_SHIFT) {
			c = KEY_SHIFT;
		} else if (i == KeyEvent.VK_BACK_SPACE) {
			c = KEY_BACKSPACE;
		} else if (i == KeyEvent.VK_DELETE) {
			c = KEY_BACKSPACE;
		} else if (i == KeyEvent.VK_TAB) {
			c = KEY_TAB;
		} else if (i == KeyEvent.VK_ENTER) {
			c = KEY_ENTER;
		} else if (i == KeyEvent.VK_ALT) {
			c = KEY_ALT;
		}

		if (c > 0 && c < 128) {
			keyDown[c] = false;
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void focusGained(FocusEvent e) {
		refresh = true;
		refresh();
	}

	@Override
	public void focusLost(FocusEvent e) {

	}

	/**
	 * Polls the next available character in the key buffer.
	 *
	 * @return the key or -1 if no new keys have been pushed.
	 */
	public int pollKey() {
		int i = -1;
		if (keyBufferPos != lastKeyBufferPos) {
			i = keyBuffer[lastKeyBufferPos];
			lastKeyBufferPos = lastKeyBufferPos + 1 & 0x7f;
		}
		return i;
	}

	public abstract void startup();

	public abstract void update();

	public abstract void shutdown();

	public abstract void draw();

	public abstract void refresh();

	/**
	 * Starts a thread.
	 *
	 * @param r the runnable.
	 * @param pri the priority.
	 */
	public void startThread(Runnable r, int pri) {
		Thread t = new Thread(r);
		t.start();
		t.setPriority(pri);
	}

	/**
	 * Draws the progress.
	 *
	 * @param caption the caption.
	 * @param percent the percent (0-100).
	 */
	public void drawProgress(String caption, int percent) {
		if (this.graphics == null) {
			return;
		}

		if (this.refresh) {
			this.graphics.setColor(Color.BLACK);
			this.graphics.fillRect(0, 0, this.width, this.height);
			this.refresh = false;
		}

		Font f = new Font("Helvetica", Font.BOLD, 13);
		FontMetrics fm = getFontMetrics(f);

		int cx = this.width / 2; //center x
		int cy = this.height / 2;// center y

		int w = 304;
		int h = 34;

		int x = cx - (w / 2);
		int y = cy - (h / 2);

		this.graphics.setColor(Color.BLACK);
		this.graphics.fillRect(x, y, w, h);

		this.graphics.setColor(new Color(140, 17, 17));
		this.graphics.drawRect(x, y, w - 1, h - 1);
		this.graphics.fillRect(x + 2, y + 2, ((w - 4) * percent) / 100, h - 4);

		this.graphics.setFont(f);
		this.graphics.setColor(Color.WHITE);
		this.graphics.drawString(caption, cx - (fm.stringWidth(caption) / 2), y + 22);

		this.fireDraw(); // let listeners know we've drawn something
	}
}
