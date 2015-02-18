package com.runescape.r317;

import java.io.*;

public class JagexFileStore {

	private static byte[] buffer = new byte[520];
	public RandomAccessFile data;
	public RandomAccessFile index;
	public int store;

	public JagexFileStore(RandomAccessFile data, RandomAccessFile index, int store) {
		this.store = store;
		this.data = data;
		this.index = index;
	}

	//laziness drove me not to rename
	public synchronized byte[] read(int i) {
		try {
			try {
				index.seek(i * 6);
				int l;

				for (int j = 0; j < 6; j += l) {
					l = index.read(buffer, j, 6 - j);
					if (l == -1) {
						return null;
					}
				}

				int size = ((buffer[0] & 0xff) << 16) + ((buffer[1] & 0xff) << 8) + (buffer[2] & 0xff);
				int sector = ((buffer[3] & 0xff) << 16) + ((buffer[4] & 0xff) << 8) + (buffer[5] & 0xff);

				if (size < 0) {
					return null;
				}

				if (sector <= 0 || sector > data.length() / 520L) {
					return null;
				}

				byte abyte0[] = new byte[size];
				int k1 = 0;
				for (int part = 0; k1 < size; part++) {
					if (sector == 0) {
						return null;
					}
					data.seek(sector * 520);
					int k = 0;
					int i2 = size - k1;
					if (i2 > 512) {
						i2 = 512;
					}
					int j2;
					for (; k < i2 + 8; k += j2) {
						j2 = data.read(buffer, k, (i2 + 8) - k);
						if (j2 == -1) {
							return null;
						}
					}
					int sectorFile = ((buffer[0] & 0xff) << 8) + (buffer[1] & 0xff);
					int sectorPart = ((buffer[2] & 0xff) << 8) + (buffer[3] & 0xff);
					int nextSector = ((buffer[4] & 0xff) << 16) + ((buffer[5] & 0xff) << 8) + (buffer[6] & 0xff);
					int sectorStore = buffer[7] & 0xff;

					if (sectorFile != i) {
						return null;
					}

					if (sectorPart != part) {
						return null;
					}

					if (sectorStore != store) {
						return null;
					}

					if (nextSector < 0 || nextSector > data.length() / 520L) {
						return null;
					}
					for (int k3 = 0; k3 < i2; k3++) {
						abyte0[k1++] = buffer[k3 + 8];
					}
					sector = nextSector;
				}
				return abyte0;
			} catch (IOException _ex) {
				_ex.printStackTrace();
				return null;
			}
		} catch (RuntimeException ex) {
			// ignore
		}
		throw new RuntimeException();
	}

}
