
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JApplet;
import javax.swing.JFrame;

public abstract class GameShell extends JApplet implements Runnable, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener, FocusListener {

	private static final long serialVersionUID = 4519439019496437757L;

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

	public int state;
	public int deltime = 20;
	public int minDelay = 1;

	public double ft;
	private final long[] optim = new long[10];
	public int fps;
	public int width;
	public int height;
	public Graphics graphics;
	public JFrame frame;
	public boolean refresh = true;
	public int idleCycles;
	public int dragButton;
	public int mouseX;
	public int mouseY;
	public int mouseButton;
	public int mouseWheel;
	public int clickX;
	public int clickY;
	public boolean[] keyDown = new boolean[128];

	private final int[] keyBuffer = new int[128];
	private int lastKeyBufferPos;
	private int keyBufferPos;

	public void initFrame(int w, int h) {
		width = w;
		height = h;
		frame = new GameFrame(this, width, height);
		graphics = getGraphics();
		startThread(this, 1);
	}

	public void initApplet(int w, int h) {
		width = w;
		height = h;
		graphics = getGraphics();
		startThread(this, 1);
	}

	@Override
	public void run() {
		System.out.println("Registering event listeners");
		setBackground(Color.BLACK);

		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		addFocusListener(this);

		drawProgress("Loading...", 0);
		startup();

		int curFrame = 0;
		int ratio = 256;
		int delay = 1;
		int cycle = 0;

		for (int n = 0; n < 10; n++) {
			optim[n] = System.currentTimeMillis();
		}

		long currentTime;

		while (state >= 0) {
			if (state > 0) {
				state--;
				if (state == 0) {
					forceShutdown();
					return;
				}
			}

			int lastRatio = ratio;
			int lastDelta = delay;

			ratio = 300;
			delay = 1;
			currentTime = System.currentTimeMillis();

			if (optim[curFrame] == 0L) {
				ratio = lastRatio;
				delay = lastDelta;
			} else if (currentTime > optim[curFrame]) {
				ratio = (int) ((long) (deltime * 2560) / (currentTime - optim[curFrame]));
			}

			if (ratio < 25) {
				ratio = 25;
			}

			if (ratio > 256) {
				ratio = 256;
				delay = (int) ((long) deltime - (currentTime - optim[curFrame]) / 10L);
			}

			optim[curFrame] = currentTime;
			curFrame = (curFrame + 1) % 10;

			if (delay > 1) {
				for (int n = 0; n < 10; n++) {
					if (optim[n] != 0L) {
						optim[n] += (long) delay;
					}
				}
			}

			if (delay < minDelay) {
				delay = minDelay;
			}

			try {
				Thread.sleep((long) delay);
			} catch (InterruptedException e) {

			}

			for (/**/; cycle < 256; cycle += ratio) {
				update();

				mouseWheel = 0;
				mouseButton = 0;
				lastKeyBufferPos = keyBufferPos;
			}

			cycle &= 0xFF;

			if (deltime > 0) {
				fps = (ratio * 1000) / (deltime * 256);
			}

			long nano = System.nanoTime();
			draw();
			ft = (System.nanoTime() - nano) / 1_000_000.0;
		}

		if (state == -1) {
			forceShutdown();
		}
	}

	public void forceShutdown() {
		state = -2;
		System.out.println("Closing program");
		shutdown();

		try {
			Thread.sleep(1000L);
		} catch (Exception e) {
			// nothing
		}
		try {
			System.exit(0);
		} catch (Throwable t) {
			// nothing
		}
	}

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
			System.out.println("5 seconds expired, forcing kill");
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

	public void startThread(Runnable r, int i) {
		Thread t = new Thread(r);
		t.start();
		t.setPriority(i);
	}

	public void drawProgress(String caption, int percent) {
		if (graphics == null) {
			return;
		}

		if (refresh) {
			graphics.setColor(Color.BLACK);
			graphics.fillRect(0, 0, width, height);
			refresh = false;
		}

		Font f = new Font("Helvetica", Font.BOLD, 13);
		FontMetrics fm = getFontMetrics(f);

		int centerX = width / 2;
		int centerY = height / 2;

		int w = 304;
		int h = 34;

		int x = centerX - (w / 2);
		int y = centerY - (h / 2);

		graphics.setColor(Color.BLACK);
		graphics.fillRect(x, y, w, h);

		graphics.setColor(new Color(140, 17, 17));
		graphics.drawRect(x, y, w - 1, h - 1);
		graphics.fillRect(x + 2, y + 2, ((w - 4) * percent) / 100, h - 4);

		graphics.setFont(f);
		graphics.setColor(Color.WHITE);
		graphics.drawString(caption, centerX - (fm.stringWidth(caption) / 2), y + 22);
	}
}
