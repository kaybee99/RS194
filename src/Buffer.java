
import java.math.BigInteger;

public final class Buffer extends CacheLink {

	public byte[] data;
	public int pos;
	public int bitPos;
	public ISAAC isaac;

	private int start, varSize;

	private static final int[] BITMASK;

	private static int poolSize1;
	private static int poolSize2;
	private static int poolSize3;
	private static LinkedList pool1;
	private static LinkedList pool2;
	private static LinkedList pool3;

	public static Buffer get(int type) {
		synchronized (pool2) {
			Buffer b = null;

			if (type == 0 && poolSize1 > 0) {
				poolSize1--;
				b = (Buffer) pool1.poll();
			} else if (type == 1 && poolSize2 > 0) {
				poolSize2--;
				b = (Buffer) pool2.poll();
			} else if (type == 2 && poolSize3 > 0) {
				poolSize3--;
				b = (Buffer) pool3.poll();
			}

			if (b != null) {
				b.pos = 0;
				return b;
			}
		}

		Buffer b = new Buffer();
		b.pos = 0;

		if (type == 0) {
			b.data = new byte[100];
		} else if (type == 1) {
			b.data = new byte[5000];
		} else {
			b.data = new byte[30000];
		}

		return b;
	}

	private Buffer() {
	}

	public Buffer(int size) {
		this(new byte[size]);
	}

	public Buffer(byte[] src) {
		data = src;
		pos = 0;
	}

	public void startVarSize(int opcode, int bytes) {
		writeOpcode(opcode);
		pos += bytes;
		start = pos;
		varSize = bytes;
	}

	public void endVarSize() {
		final int length = pos - start;
		final int bytes = varSize + 1;

		for (int i = 1; i < bytes; i++) {
			data[pos - length - i] = (byte) (length >> ((i - 1) * 8));
		}
	}

	public void writeOpcode(int opcode) {
		if (isaac != null) {
			data[pos++] = (byte) (opcode + isaac.nextInt());
		} else {
			write(opcode);
		}
	}

	public void write(int i) {
		data[pos++] = (byte) i;
	}

	public void writeShort(int i) {
		data[pos++] = (byte) (i >> 8);
		data[pos++] = (byte) i;
	}

	public void writeInt(int i) {
		data[pos++] = (byte) (i >> 24);
		data[pos++] = (byte) (i >> 16);
		data[pos++] = (byte) (i >> 8);
		data[pos++] = (byte) i;
	}

	public void writeLong(long l) {
		data[pos++] = (byte) (int) (l >> 56);
		data[pos++] = (byte) (int) (l >> 48);
		data[pos++] = (byte) (int) (l >> 40);
		data[pos++] = (byte) (int) (l >> 32);
		data[pos++] = (byte) (int) (l >> 24);
		data[pos++] = (byte) (int) (l >> 16);
		data[pos++] = (byte) (int) (l >> 8);
		data[pos++] = (byte) (int) l;
	}

	public void writeString(String s) {
		System.arraycopy(s.getBytes(), 0, data, pos, s.length());
		pos += s.length();
		data[pos++] = (byte) 10;
	}

	public void write(byte[] src, int off, int len) {
		for (int i = off; i < off + len; i++) {
			data[pos++] = src[i];
		}
	}

	public void writeLength(int length) {
		data[pos - length - 1] = (byte) length;
	}

	public int read() {
		return data[pos++] & 0xff;
	}

	public byte readByte() {
		return data[pos++];
	}

	public int readUShort() {
		pos += 2;
		return (((data[pos - 2] & 0xff) << 8) + (data[pos - 1] & 0xff));
	}

	public int readShort() {
		pos += 2;
		int i = (((data[pos - 2] & 0xff) << 8) + (data[pos - 1] & 0xff));
		if (i > 32767) {
			i -= 65536;
		}
		return i;
	}

	public int readInt24() {
		pos += 3;
		return (((data[pos - 3] & 0xff) << 16) + ((data[pos - 2] & 0xff) << 8) + (data[pos - 1] & 0xff));
	}

	public int readInt() {
		pos += 4;
		return (((data[pos - 4] & 0xff) << 24) + ((data[pos - 3] & 0xff) << 16) + ((data[pos - 2] & 0xff) << 8) + (data[pos - 1] & 0xff));
	}

	public long readLong() {
		long a = (long) readInt() & 0xffffffffL;
		long b = (long) readInt() & 0xffffffffL;
		return (a << 32) + b;
	}

	public String readString() {
		int start = pos;
		while (data[pos++] != 10) {
			/* empty */
		}
		return new String(data, start, pos - start - 1);
	}

	public byte[] readStringBytes() {
		int start = pos;
		while (data[pos++] != 10) {
			/* empty */
		}
		byte[] bytes = new byte[pos - start - 1];
		for (int i = start; i < pos - 1; i++) {
			bytes[i - start] = data[i];
		}
		return bytes;
	}

	public void read(byte[] dst, int off, int len) {
		for (int i = off; i < off + len; i++) {
			dst[i] = data[pos++];
		}
	}

	public void startBitAccess(int i) {
		bitPos = pos * 8;
	}

	public int readBits(int count) {
		int bytePos = bitPos >> 3;
		int mask = 8 - (bitPos & 0x7);
		int i = 0;

		bitPos += count;

		for (/**/; count > mask; mask = 8) {
			i += ((data[bytePos++] & BITMASK[mask]) << count - mask);
			count -= mask;
		}

		if (count == mask) {
			i += data[bytePos] & BITMASK[mask];
		} else {
			i += (data[bytePos] >> mask - count & BITMASK[count]);
		}

		return i;
	}

	public void startByteAccess() {
		pos = (bitPos + 7) / 8;
	}

	public int readSmart() {
		int i = data[pos] & 0xff;
		if (i < 128) {
			return read() - 64;
		}
		return readUShort() - 49152;
	}

	public int readUSmart() {
		int i = data[pos] & 0xff;
		if (i < 128) {
			return read();
		}
		return readUShort() - 32768;
	}

	public void encode(BigInteger exponent, BigInteger modulus) {
		byte[] tmp = new byte[pos];
		System.arraycopy(data, 0, tmp, 0, pos);
		byte[] encoded = new BigInteger(tmp).toByteArray();// .modPow(exponent,
		// modulus).toByteArray();
		pos = 0;
		write(encoded.length);
		write(encoded, 0, encoded.length);
	}

	static {
		BITMASK = new int[]{0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535, 131071, 262143, 524287, 1048575, 2097151, 4194303, 8388607, 16777215, 33554431, 67108863, 134217727, 268435455, 536870911, 1073741823, 2147483647, -1};
		pool1 = new LinkedList();
		pool2 = new LinkedList();
		pool3 = new LinkedList();
	}
}
