package com.runescape;

import java.math.*;
import net.burtleburtle.bob.rand.*;

public final class Buffer extends QueueLink {

	public byte[] data;
	public int position;
	public int bitPos;
	public IsaacRandom isaac;

	private int start, varSize;

	private static final int[] BITMASK;

	private static int poolSize1;
	private static int poolSize2;
	private static int poolSize3;
	private static LinkedQueue pool1;
	private static LinkedQueue pool2;
	private static LinkedQueue pool3;

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
				b.position = 0;
				return b;
			}
		}

		Buffer b = new Buffer();
		b.position = 0;

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
		position = 0;
	}

	public void startVarSize(int opcode, int bytes) {
		writeOpcode(opcode);
		position += bytes;
		start = position;
		varSize = bytes;
	}

	public void endVarSize() {
		final int length = position - start;
		final int bytes = varSize + 1;

		for (int i = 1; i < bytes; i++) {
			data[position - length - i] = (byte) (length >> ((i - 1) * 8));
		}
	}

	public void writeOpcode(int opcode) {
		if (isaac != null) {
			data[position++] = (byte) (opcode);// XXX: + isaac.nextInt());
		} else {
			write(opcode);
		}
	}

	public void write(int i) {
		data[position++] = (byte) i;
	}

	public void writeShort(int i) {
		data[position++] = (byte) (i >> 8);
		data[position++] = (byte) i;
	}

	public void writeInt(int i) {
		data[position++] = (byte) (i >> 24);
		data[position++] = (byte) (i >> 16);
		data[position++] = (byte) (i >> 8);
		data[position++] = (byte) i;
	}

	public void writeLong(long l) {
		data[position++] = (byte) (int) (l >> 56);
		data[position++] = (byte) (int) (l >> 48);
		data[position++] = (byte) (int) (l >> 40);
		data[position++] = (byte) (int) (l >> 32);
		data[position++] = (byte) (int) (l >> 24);
		data[position++] = (byte) (int) (l >> 16);
		data[position++] = (byte) (int) (l >> 8);
		data[position++] = (byte) (int) l;
	}

	public void writeString(String s) {
		System.arraycopy(s.getBytes(), 0, data, position, s.length());
		position += s.length();
		data[position++] = (byte) 10;
	}

	public void write(byte[] src, int off, int len) {
		for (int i = off; i < off + len; i++) {
			data[position++] = src[i];
		}
	}

	public void writeLength(int length) {
		data[position - length - 1] = (byte) length;
	}

	public int read() {
		return data[position++] & 0xff;
	}

	public byte readByte() {
		return data[position++];
	}

	public int readUShort() {
		position += 2;
		return (((data[position - 2] & 0xff) << 8) + (data[position - 1] & 0xff));
	}

	public int readShort() {
		position += 2;
		int i = (((data[position - 2] & 0xff) << 8) + (data[position - 1] & 0xff));
		if (i > 32767) {
			i -= 65536;
		}
		return i;
	}

	public int readInt24() {
		position += 3;
		return (((data[position - 3] & 0xff) << 16) + ((data[position - 2] & 0xff) << 8) + (data[position - 1] & 0xff));
	}

	public int readInt() {
		position += 4;
		return (((data[position - 4] & 0xff) << 24) + ((data[position - 3] & 0xff) << 16) + ((data[position - 2] & 0xff) << 8) + (data[position - 1] & 0xff));
	}

	public long readLong() {
		long a = (long) readInt() & 0xffffffffL;
		long b = (long) readInt() & 0xffffffffL;
		return (a << 32) + b;
	}

	public String readString() {
		int startPosition = position;
		while (data[position++] != 10) {
			/* empty */
		}
		return new String(data, startPosition, position - startPosition - 1);
	}

	public byte[] readStringBytes() {
		int startPosition = position;
		while (data[position++] != 10) {
			/* empty */
		}
		byte[] bytes = new byte[position - startPosition - 1];
		for (int i = startPosition; i < position - 1; i++) {
			bytes[i - startPosition] = data[i];
		}
		return bytes;
	}

	public void read(byte[] dst, int off, int len) {
		for (int i = off; i < off + len; i++) {
			dst[i] = data[position++];
		}
	}

	public void startBitAccess(int i) {
		bitPos = position * 8;
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
		position = (bitPos + 7) / 8;
	}

	public int readSmart() {
		int i = data[position] & 0xff;
		if (i < 128) {
			return read() - 64;
		}
		return readUShort() - 49152;
	}

	public int readUSmart() {
		int i = data[position] & 0xff;
		if (i < 128) {
			return read();
		}
		return readUShort() - 32768;
	}

	public void encode(BigInteger exponent, BigInteger modulus) {
		byte[] tmp = new byte[position];
		System.arraycopy(data, 0, tmp, 0, position);
		byte[] encoded = new BigInteger(tmp).toByteArray();// .modPow(exponent,
		// modulus).toByteArray();
		position = 0;
		write(encoded.length);
		write(encoded, 0, encoded.length);
	}

	static {
		BITMASK = new int[]{0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535, 131071, 262143, 524287, 1048575, 2097151, 4194303, 8388607, 16777215, 33554431, 67108863, 134217727, 268435455, 536870911, 1073741823, 2147483647, -1};
		pool1 = new LinkedQueue();
		pool2 = new LinkedQueue();
		pool3 = new LinkedQueue();
	}
}
