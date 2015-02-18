package com.runescape.r317;

import com.runescape.*;
import java.io.*;
import java.util.zip.*;

public class ModelExtension {

	static ModelExtensionInfo[] headers = new ModelExtensionInfo[2000]; // auto expands
	static JagexFileStore store;
	public static boolean loaded = false;

	public static final Model get(int index) {
		if (headers == null) {
			return null;
		}

		ModelExtensionInfo h = headers[index];

		if (h == null) {
			// fall back
			System.out.println("Using fallback model for " + index);
			return new Model(index);
		}

		Model m = new Model();

		m.vertexCount = h.vertexCount;
		m.triangleCount = h.triangleCount;
		m.texturedCount = h.texturedCount;

		m.vertexX = new int[m.vertexCount];
		m.vertexY = new int[m.vertexCount];
		m.vertexZ = new int[m.vertexCount];

		m.triangleVertexA = new int[m.triangleCount];
		m.triangleVertexB = new int[m.triangleCount];
		m.triangleVertexC = new int[m.triangleCount];

		m.textureVertexA = new int[m.texturedCount];
		m.textureVertexB = new int[m.texturedCount];
		m.textureVertexC = new int[m.texturedCount];

		if (h.vertexLabelDataOffset >= 0) {
			m.vertexLabel = new int[m.vertexCount];
		}

		if (h.triangleInfoDataOffset >= 0) {
			m.triangleInfo = new int[m.triangleCount];
		}

		if (h.trianglePriorityDataOffset >= 0) {
			m.trianglePriorities = new int[m.triangleCount];
		} else {
			m.priority = -h.trianglePriorityDataOffset - 1;
		}

		if (h.triangleAlphaDataOffset >= 0) {
			m.triangleAlpha = new int[m.triangleCount];
		}

		if (h.triangleSkinDataOffset >= 0) {
			m.triangleSkin = new int[m.triangleCount];
		}

		m.unmodifiedTriangleColor = new int[m.triangleCount];

		Buffer b0 = new Buffer(h.data);
		b0.position = h.vertexFlagDataOffset;

		Buffer b1 = new Buffer(h.data);
		b1.position = h.vertexXDataOffset;

		Buffer b2 = new Buffer(h.data);
		b2.position = h.vertexYDataOffset;

		Buffer b3 = new Buffer(h.data);
		b3.position = h.vertexZDataOffset;

		Buffer b4 = new Buffer(h.data);
		b4.position = h.vertexLabelDataOffset;

		int x = 0;
		int y = 0;
		int z = 0;
		for (int v = 0; v < m.vertexCount; v++) {
			int flags = b0.read();

			int x0 = 0;
			if ((flags & 1) != 0) {
				x0 = b1.readSmart();
			}

			int y0 = 0;
			if ((flags & 2) != 0) {
				y0 = b2.readSmart();
			}

			int z0 = 0;
			if ((flags & 4) != 0) {
				z0 = b3.readSmart();
			}

			m.vertexX[v] = x + x0;
			m.vertexY[v] = y + y0;
			m.vertexZ[v] = z + z0;

			x = m.vertexX[v];
			y = m.vertexY[v];
			z = m.vertexZ[v];

			if (m.vertexLabel != null) {
				m.vertexLabel[v] = b4.read();
			}
		}

		b0.position = h.triangleColorDataOffset;
		b1.position = h.triangleInfoDataOffset;
		b2.position = h.trianglePriorityDataOffset;
		b3.position = h.triangleAlphaDataOffset;
		b4.position = h.triangleSkinDataOffset;

		for (int k1 = 0; k1 < m.triangleCount; k1++) {
			m.unmodifiedTriangleColor[k1] = b0.readUShort();

			if (m.triangleInfo != null) {
				m.triangleInfo[k1] = b1.read();
			}

			if (m.trianglePriorities != null) {
				m.trianglePriorities[k1] = b2.read();
			}

			if (m.triangleAlpha != null) {
				m.triangleAlpha[k1] = b3.read();
			}

			if (m.triangleSkin != null) {
				m.triangleSkin[k1] = b4.read();
			}
		}

		b0.position = h.vertexIndexDataOffset;
		b1.position = h.triangleTypeDataOffset;

		int a = 0;
		int b = 0;
		int c = 0;
		int last = 0;

		for (int t = 0; t < m.triangleCount; t++) {
			int type = b1.read();

			if (type == 1) {
				a = b0.readSmart() + last;
				last = a;

				b = b0.readSmart() + last;
				last = b;

				c = b0.readSmart() + last;
				last = c;

				m.triangleVertexA[t] = a;
				m.triangleVertexB[t] = b;
				m.triangleVertexC[t] = c;
			}

			if (type == 2) {
				b = c;

				c = b0.readSmart() + last;
				last = c;

				m.triangleVertexA[t] = a;
				m.triangleVertexB[t] = b;
				m.triangleVertexC[t] = c;
			}

			if (type == 3) {
				a = c;

				c = b0.readSmart() + last;
				last = c;

				m.triangleVertexA[t] = a;
				m.triangleVertexB[t] = b;
				m.triangleVertexC[t] = c;
			}

			if (type == 4) {
				int a0 = a;
				a = b;
				b = a0;

				c = b0.readSmart() + last;
				last = c;

				m.triangleVertexA[t] = a;
				m.triangleVertexB[t] = b;
				m.triangleVertexC[t] = c;
			}
		}

		b0.position = h.triangleTextureDataOffset;

		for (int t = 0; t < m.texturedCount; t++) {
			m.textureVertexA[t] = b0.readUShort();
			m.textureVertexB[t] = b0.readUShort();
			m.textureVertexC[t] = b0.readUShort();
		}

		return m;
	}

	public static final void unpack() {
		try {
			RandomAccessFile data = new RandomAccessFile(Signlink.getFile("main_file_cache.dat"), "r");
			RandomAccessFile index = new RandomAccessFile(Signlink.getFile("main_file_cache.idx1"), "r");

			store = new JagexFileStore(data, index, 2);

			System.out.println("Model indices: " + store.index.length() / 6);

			int failed = 0;
			for (int n = 0; n < store.index.length() / 6; n++) {
				if (!unpack(n, store.read(n))) {
					failed++;
				}
			}

			System.out.println(failed + " models failed to load");

			loaded = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static final boolean unpack(int index, byte[] src) {
		if (index >= headers.length) {
			ModelExtensionInfo[] h = headers;
			headers = new ModelExtensionInfo[index + 100];
			System.arraycopy(h, 0, headers, 0, h.length);
		}

		if (src == null) {
			ModelExtensionInfo h = headers[index] = new ModelExtensionInfo();
			h.vertexCount = 0;
			h.triangleCount = 0;
			h.texturedCount = 0;
			return false;
		}

		try {
			byte[] buffer = new byte[1024];
			int read;

			try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); GZIPInputStream gzin = new GZIPInputStream(new ByteArrayInputStream(src))) {
				while ((read = gzin.read(buffer)) != -1) {
					baos.write(buffer, 0, read);
				}
				src = baos.toByteArray();
			}
		} catch (Exception e) {
			return false;
		}

		Buffer b = new Buffer(src);
		b.position = src.length - 18;

		ModelExtensionInfo h = headers[index] = new ModelExtensionInfo();
		h.data = src;
		h.vertexCount = b.readUShort();
		h.triangleCount = b.readUShort();
		h.texturedCount = b.read();

		int hasInfo = b.read();
		int priority = b.read();
		int hasAlpha = b.read();
		int hasSkins = b.read();
		int hasLabels = b.read();
		int xDataLen = b.readUShort();
		int yDataLen = b.readUShort();
		int zDataLen = b.readUShort();
		int vertexIndexDataLen = b.readUShort();

		int i = 0;

		h.vertexFlagDataOffset = i;
		i += h.vertexCount;

		h.triangleTypeDataOffset = i;
		i += h.triangleCount;

		h.trianglePriorityDataOffset = i;

		if (priority == 255) {
			i += h.triangleCount;
		} else {
			h.trianglePriorityDataOffset = -priority - 1;
		}

		h.triangleSkinDataOffset = i;

		if (hasSkins == 1) {
			i += h.triangleCount;
		} else {
			h.triangleSkinDataOffset = -1;
		}

		h.triangleInfoDataOffset = i;

		if (hasInfo == 1) {
			i += h.triangleCount;
		} else {
			h.triangleInfoDataOffset = -1;
		}

		h.vertexLabelDataOffset = i;

		if (hasLabels == 1) {
			i += h.vertexCount;
		} else {
			h.vertexLabelDataOffset = -1;
		}

		h.triangleAlphaDataOffset = i;

		if (hasAlpha == 1) {
			i += h.triangleCount;
		} else {
			h.triangleAlphaDataOffset = -1;
		}

		h.vertexIndexDataOffset = i;
		i += vertexIndexDataLen;

		h.triangleColorDataOffset = i;
		i += h.triangleCount * 2;

		h.triangleTextureDataOffset = i;
		i += h.texturedCount * 6;

		h.vertexXDataOffset = i;
		i += xDataLen;

		h.vertexYDataOffset = i;
		i += yDataLen;

		h.vertexZDataOffset = i;
		i += zDataLen;
		return true;
	}

}
