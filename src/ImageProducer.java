
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

public class ImageProducer extends Link {

	public int[] pixels;
	public int width;
	public int height;
	public BufferedImage image;

	public ImageProducer(int width, int height) {
		this.width = width;
		this.height = height;
		this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		this.prepare();
	}

	public void clear(int rgb) {
		Arrays.fill(pixels, rgb);
	}

	public final void prepare() {
		Canvas2D.prepare(pixels, width, height);
	}

	public void draw(Graphics g, int x, int y) {
		g.drawImage(image, x, y, null);
	}

}
