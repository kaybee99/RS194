package dane.runescape.mapeditor.util;

public class MathUtil {

	private static final double TWO_PI = Math.PI * 2;

	public static final int toRuneDegree(double a) {
		return (int) ((a * 2048) / TWO_PI) & 0x7FF;
	}

	public static final double toRadian(int a) {
		return (a * TWO_PI) / 2048.0;
	}

	public static final double toDegree(int a) {
		return (a * 360) / 2048.0;
	}
}
