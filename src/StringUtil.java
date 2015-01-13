
public final class StringUtil {

	public static final char[] BASE37_LOOKUP = {'_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
	public static final String ASCII_TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"\u00a3$%^&*()-_=+[{]};:'@#~,<.>/?\\| ";

	public static long toBase37(String s) {
		s = s.trim();
		long l = 0L;

		for (int i = 0; i < s.length() && i < 12; i++) {
			char c = s.charAt(i);
			l *= 37L;

			if (c >= 'A' && c <= 'Z') {
				l += (c + 1) - 'A';
			} else if (c >= 'a' && c <= 'z') {
				l += (c + 1) - 'a';
			} else if (c >= '0' && c <= '9') {
				l += (c + 27) - '0';
			}
		}
		return l;
	}

	public static String fromBase37(long l) {
		// >= 37 to the 12th power
		if (l < 0L || l >= 6582952005840035281L) {
			return "invalid_name";
		}

		int len = 0;
		char[] chars = new char[12];
		while (l != 0L) {
			long l1 = l;
			l /= 37L;
			chars[11 - len++] = BASE37_LOOKUP[(int) (l1 - l * 37L)];
		}
		return new String(chars, 12 - len, len);
	}

	public static long getHash(String s) {
		s = s.toLowerCase();
		long l = 0L;
		for (int n = 0; n < s.length(); n++) {
			l = l * 61L + (long) s.charAt(n) - ' ';
		}
		return l;
	}

	/**
	 * Only allows the string to contain 'a' to 'z', '0' to '9', and '_'.
	 *
	 * @param s the input string.
	 * @return the safe string.
	 */
	public static String getSafe(String s) {
		s = s.toLowerCase().trim();
		StringBuilder sb = new StringBuilder();

		for (int n = 0; n < s.length(); n++) {
			if (n >= 12) {
				break;
			}

			char c = s.charAt(n);

			if (isLowercaseAlpha(c) || isNumeral(c)) {
				sb.append(c);
			} else {
				sb.append('_');
			}
		}

		s = sb.toString();

		while (s.charAt(0) == '_') {
			s = s.substring(1);
		}

		while (s.charAt(s.length() - 1) == '_') {
			s = s.substring(0, s.length() - 1);
		}

		return s;
	}

	public static String getFormatted(String s) {
		if (s.length() > 0) {
			char[] c = s.toCharArray();

			for (int n = 0; n < c.length; n++) {
				if (c[n] == '_') {
					c[n] = ' ';

					// next letter will be upper case
					int m = n + 1;
					if (m < c.length && isLowercaseAlpha(c[m])) {
						c[m] = (char) (c[m] + 'A' - 'a');
					}
				}
			}

			// First letter always upper case
			if (isLowercaseAlpha(c[0])) {
				c[0] = (char) (c[0] + 'A' - 'a');
			}

			return new String(c);
		}
		return s;
	}

	public static String getPunctuated(String s) {
		char[] chars = s.toLowerCase().toCharArray();

		boolean capitalize = true;
		for (int n = 0; n < chars.length; n++) {
			char c = chars[n];

			if (capitalize && isLowercaseAlpha(c)) {
				chars[n] -= ' ';
				capitalize = false;
			}

			if (c == '.' || c == '!') {
				capitalize = true;
			}
		}
		return new String(chars);
	}

	public static String toAsterisks(String s) {
		char[] c = new char[s.length()];
		for (int n = 0; n < c.length; n++) {
			c[n] = '*';
		}
		return new String(c);
	}

	public static boolean isSymbol(char c) {
		return !isAlpha(c) && !isNumeral(c);
	}

	public static boolean isNotLowercaseAlpha(char c) {
		if (c < 'a' || c > 'z') {
			return true;
		}
		return c == 'v' || c == 'x' || c == 'j' || c == 'q' || c == 'z';
	}

	public static boolean isAlpha(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
	}

	public static boolean isNumeral(char c) {
		return c >= '0' && c <= '9';
	}

	public static boolean isLowercaseAlpha(char c) {
		return c >= 'a' && c <= 'z';
	}

	public static boolean isUppercaseAlpha(char c) {
		return c >= 'A' && c <= 'Z';
	}

	public static boolean isASCII(char c) {
		return c >= ' ' && c <= '~';
	}

}
