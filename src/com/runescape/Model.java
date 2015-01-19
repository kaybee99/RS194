package com.runescape;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Model extends CacheLink {

	/**
	 * Used in the <i>type</i> param for the <code>transform</code> method.
	 */
	public static final int TRANSFORM_CENTRALIZE = 0, TRANSFORM_POSITION = 1,
		TRANSFORM_ROTATION = 2, TRANSFORM_SCALE = 3, TRANSFORM_ALPHA = 5;

	private static final Logger logger = Logger.getLogger(Model.class.getName());

	public static ModelContext[] contexts;

	public static Buffer obhead;
	public static Buffer obface1;
	public static Buffer obface2;
	public static Buffer obface3;
	public static Buffer obface4;
	public static Buffer obface5;
	public static Buffer obpoint1;
	public static Buffer obpoint2;
	public static Buffer obpoint3;
	public static Buffer obpoint4;
	public static Buffer obpoint5;
	public static Buffer obvertex1;
	public static Buffer obvertex2;
	public static Buffer obaxis;

	public static boolean[] verifyTriangleBounds = new boolean[2048];
	public static boolean[] projectTriangle = new boolean[2048];

	public static int[] vertexScreenX = new int[2048];
	public static int[] vertexScreenY = new int[2048];
	public static int[] vertexDepth = new int[2048];

	public static int[] projectSceneX = new int[2048];
	public static int[] projectSceneY = new int[2048];
	public static int[] projectSceneZ = new int[2048];

	public static int[] depthTriangleCount = new int[1500];
	public static int[][] depthTriangles = new int[1500][512];

	public static int[] priorityTriangleCounts = new int[12];
	public static int[][] priorityTriangles = new int[12][2000];

	public static int[] normalTrianglePriority = new int[2000];
	public static int[] highTrianglePriority = new int[2000];
	public static int[] lowPriorityDepth = new int[12];

	public static int[] tmpX = new int[10];
	public static int[] tmpY = new int[10];
	public static int[] tmpColor = new int[10];

	public static int transformX;
	public static int transformY;
	public static int transformZ;

	public static boolean allowInput;
	public static int mouseX;
	public static int mouseY;

	public static int hoverCount;
	public static int[] hoveredBitsets = new int[1000];

	public static int[] sin = Canvas3D.sin;
	public static int[] cos = Canvas3D.cos;
	public static int[] palette = Canvas3D.palette;
	public static int[] oneOverFixed1616 = Canvas3D.oneOverFixed1616;

	public int vertexCount;
	public int[] vertexX;
	public int[] vertexY;
	public int[] vertexZ;

	public int triangleCount;
	public int[] triangleVertexA;
	public int[] triangleVertexB;
	public int[] triangleVertexC;

	public int[] triangleColorA;
	public int[] triangleColorB;
	public int[] triangleColorC;

	public int[] triangleInfo;
	public int[] trianglePriorities;
	public int[] triangleAlpha;

	public int[] unmodifiedTriangleColor;
	public int priority;

	public int texturedCount;
	public int[] textureVertexA;
	public int[] textureVertexB;
	public int[] textureVertexC;

	public int minBoundX;
	public int maxBoundX;
	public int maxBoundZ;
	public int minBoundZ;
	public int boundHeight;

	/**
	 * Seems inverted. Noticed a simularity in the landscape heightmap
	 */
	public int minBoundY, maxBoundY;

	public int maxDepth;
	public int minDepth;
	public int objectOffsetY;
	public int[] vertexLabel;
	public int[] triangleSkin;
	public int[][] labelVertices;
	public int[][] skinTriangle;
	public Normal[] normals;
	public Normal[] unmodifiedNormals;

	public static void unload() {
		contexts = null;
		obhead = null;
		obface1 = null;
		obface2 = null;
		obface3 = null;
		obface4 = null;
		obface5 = null;
		obpoint1 = null;
		obpoint2 = null;
		obpoint3 = null;
		obpoint4 = null;
		obpoint5 = null;
		obvertex1 = null;
		obvertex2 = null;
		obaxis = null;
		verifyTriangleBounds = null;
		projectTriangle = null;
		vertexScreenX = null;
		vertexScreenY = null;
		vertexDepth = null;
		projectSceneX = null;
		projectSceneY = null;
		projectSceneZ = null;
		depthTriangleCount = null;
		depthTriangles = null;
		priorityTriangleCounts = null;
		priorityTriangles = null;
		normalTrianglePriority = null;
		highTrianglePriority = null;
		lowPriorityDepth = null;
		sin = null;
		cos = null;
		palette = null;
		oneOverFixed1616 = null;
	}

	public static void load(Archive a) {
		int lastIndex = -1;

		try {
			obhead = new Buffer(a.get("ob_head.dat"));
			obface1 = new Buffer(a.get("ob_face1.dat"));
			obface2 = new Buffer(a.get("ob_face2.dat"));
			obface3 = new Buffer(a.get("ob_face3.dat"));
			obface4 = new Buffer(a.get("ob_face4.dat"));
			obface5 = new Buffer(a.get("ob_face5.dat"));
			obpoint1 = new Buffer(a.get("ob_point1.dat"));
			obpoint2 = new Buffer(a.get("ob_point2.dat"));
			obpoint3 = new Buffer(a.get("ob_point3.dat"));
			obpoint4 = new Buffer(a.get("ob_point4.dat"));
			obpoint5 = new Buffer(a.get("ob_point5.dat"));
			obvertex1 = new Buffer(a.get("ob_vertex1.dat"));
			obvertex2 = new Buffer(a.get("ob_vertex2.dat"));
			obaxis = new Buffer(a.get("ob_axis.dat"));

			obhead.position = 0;
			obpoint1.position = 0;
			obpoint2.position = 0;
			obpoint3.position = 0;
			obpoint4.position = 0;
			obvertex1.position = 0;
			obvertex2.position = 0;

			int count = obhead.readUShort();
			contexts = new ModelContext[count + 100];

			int vertexTextureDataOffset = 0;
			int labelDataOffset = 0;
			int triangleColorDataOffset = 0;
			int triangleInfoDataOffset = 0;
			int trianglePriorityDataOffset = 0;
			int triangleAlphaDataOffset = 0;
			int triangleSkinDataOffset = 0;

			for (int n = 0; n < count; n++) {
				int index = lastIndex = obhead.readUShort();
				ModelContext c = contexts[index] = new ModelContext();

				c.vertexCount = obhead.readUShort();
				c.triangleCount = obhead.readUShort();
				c.texturedCount = obhead.read();

				c.vertexFlagDataOffset = obpoint1.position;
				c.vertexXDataOffset = obpoint2.position;
				c.vertexYDataOffset = obpoint3.position;
				c.vertexZDataOffset = obpoint4.position;
				c.vertexIndexDataOffset = obvertex1.position;
				c.triangleTypeDataOffset = obvertex2.position;

				int hasInfo = obhead.read();
				int hasPriorities = obhead.read();
				int hasAlpha = obhead.read();
				int hasSkins = obhead.read();
				int hasLabels = obhead.read();

				for (int v = 0; v < c.vertexCount; v++) {
					int flags = obpoint1.read();

					if ((flags & 0x1) != 0) {
						obpoint2.readSmart();
					}

					if ((flags & 0x2) != 0) {
						obpoint3.readSmart();
					}

					if ((flags & 0x4) != 0) {
						obpoint4.readSmart();
					}
				}

				for (int t = 0; t < c.triangleCount; t++) {
					int type = obvertex2.read();

					if (type == 1) {
						obvertex1.readSmart();
						obvertex1.readSmart();
					}

					obvertex1.readSmart();
				}

				c.triangleColorDataOffset = triangleColorDataOffset;
				triangleColorDataOffset += c.triangleCount * 2;

				if (hasInfo == 1) {
					c.triangleInfoDataOffset = triangleInfoDataOffset;
					triangleInfoDataOffset += c.triangleCount;
				} else {
					c.triangleInfoDataOffset = -1;
				}

				if (hasPriorities == 255) {
					c.trianglePriorityDataOffset = trianglePriorityDataOffset;
					trianglePriorityDataOffset += c.triangleCount;
				} else {
					c.trianglePriorityDataOffset = -hasPriorities - 1;
				}

				if (hasAlpha == 1) {
					c.triangleAlphaDataOffset = triangleAlphaDataOffset;
					triangleAlphaDataOffset += c.triangleCount;
				} else {
					c.triangleAlphaDataOffset = -1;
				}

				if (hasSkins == 1) {
					c.triangleSkinDataOffset = triangleSkinDataOffset;
					triangleSkinDataOffset += c.triangleCount;
				} else {
					c.triangleSkinDataOffset = -1;
				}

				if (hasLabels == 1) {
					c.vertexLabelDataOffset = labelDataOffset;
					labelDataOffset += c.vertexCount;
				} else {
					c.vertexLabelDataOffset = -1;
				}

				c.triangleTextureDataOffset = vertexTextureDataOffset;
				vertexTextureDataOffset += c.texturedCount;
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error loading model index " + lastIndex, e);
		}
	}

	public Model() {
	}

	public Model(int index) {
		if (contexts != null) {
			ModelContext h = contexts[index];

			if (h == null) {
				System.out.println("Error model:" + index + " not found!");
			} else {
				vertexCount = h.vertexCount;
				triangleCount = h.triangleCount;
				texturedCount = h.texturedCount;

				vertexX = new int[vertexCount];
				vertexY = new int[vertexCount];
				vertexZ = new int[vertexCount];

				triangleVertexA = new int[triangleCount];
				triangleVertexB = new int[triangleCount];
				triangleVertexC = new int[triangleCount];

				textureVertexA = new int[texturedCount];
				textureVertexB = new int[texturedCount];
				textureVertexC = new int[texturedCount];

				if (h.vertexLabelDataOffset >= 0) {
					vertexLabel = new int[vertexCount];
				}

				if (h.triangleInfoDataOffset >= 0) {
					triangleInfo = new int[triangleCount];
				}

				if (h.trianglePriorityDataOffset >= 0) {
					trianglePriorities = new int[triangleCount];
				} else {
					priority = -h.trianglePriorityDataOffset - 1;
				}

				if (h.triangleAlphaDataOffset >= 0) {
					triangleAlpha = new int[triangleCount];
				}

				if (h.triangleSkinDataOffset >= 0) {
					triangleSkin = new int[triangleCount];
				}

				unmodifiedTriangleColor = new int[triangleCount];

				obpoint1.position = h.vertexFlagDataOffset;
				obpoint2.position = h.vertexXDataOffset;
				obpoint3.position = h.vertexYDataOffset;
				obpoint4.position = h.vertexZDataOffset;
				obpoint5.position = h.vertexLabelDataOffset;

				int x = 0;
				int y = 0;
				int z = 0;

				for (int v = 0; v < vertexCount; v++) {
					int flags = obpoint1.read();
					int x0 = 0, y0 = 0, z0 = 0;

					if ((flags & 0x1) != 0) {
						x0 = obpoint2.readSmart();
					}

					if ((flags & 0x2) != 0) {
						y0 = obpoint3.readSmart();
					}

					if ((flags & 0x4) != 0) {
						z0 = obpoint4.readSmart();
					}

					vertexX[v] = x + x0;
					vertexY[v] = y + y0;
					vertexZ[v] = z + z0;

					x = vertexX[v];
					y = vertexY[v];
					z = vertexZ[v];

					if (vertexLabel != null) {
						vertexLabel[v] = obpoint5.read();
					}
				}

				obface1.position = h.triangleColorDataOffset;
				obface2.position = h.triangleInfoDataOffset;
				obface3.position = h.trianglePriorityDataOffset;
				obface4.position = h.triangleAlphaDataOffset;
				obface5.position = h.triangleSkinDataOffset;

				for (int n = 0; n < triangleCount; n++) {
					unmodifiedTriangleColor[n] = obface1.readUShort();

					if (triangleInfo != null) {
						triangleInfo[n] = obface2.read();
					}

					if (trianglePriorities != null) {
						trianglePriorities[n] = obface3.read();
					}

					if (triangleAlpha != null) {
						triangleAlpha[n] = obface4.read();
					}

					if (triangleSkin != null) {
						triangleSkin[n] = obface5.read();
					}
				}

				obvertex1.position = h.vertexIndexDataOffset;
				obvertex2.position = h.triangleTypeDataOffset;

				int a = 0;
				int b = 0;
				int c = 0;
				int last = 0;

				for (int n = 0; n < triangleCount; n++) {
					int type = obvertex2.read();

					if (type == 1) {
						a = obvertex1.readSmart() + last;
						last = a;

						b = obvertex1.readSmart() + last;
						last = b;

						c = obvertex1.readSmart() + last;
						last = c;

						triangleVertexA[n] = a;
						triangleVertexB[n] = b;
						triangleVertexC[n] = c;
					}

					if (type == 2) {
						b = c;
						c = obvertex1.readSmart() + last;
						last = c;

						triangleVertexA[n] = a;
						triangleVertexB[n] = b;
						triangleVertexC[n] = c;
					}

					if (type == 3) {
						a = c;
						c = obvertex1.readSmart() + last;
						last = c;

						triangleVertexA[n] = a;
						triangleVertexB[n] = b;
						triangleVertexC[n] = c;
					}

					if (type == 4) {
						int b0 = a;
						a = b;
						b = b0;
						c = obvertex1.readSmart() + last;
						last = c;

						triangleVertexA[n] = a;
						triangleVertexB[n] = b;
						triangleVertexC[n] = c;
					}
				}

				obaxis.position = h.triangleTextureDataOffset * 6;

				for (int t = 0; t < texturedCount; t++) {
					textureVertexA[t] = obaxis.readUShort();
					textureVertexB[t] = obaxis.readUShort();
					textureVertexC[t] = obaxis.readUShort();
				}
			}
		}
	}

	public Model(Model[] models, int count) {
		boolean keepInfo = false;
		boolean keepPriorities = false;
		boolean keepAlpha = false;
		boolean keepSkins = false;

		vertexCount = 0;
		triangleCount = 0;
		texturedCount = 0;
		priority = -1;

		for (int n = 0; n < count; n++) {
			Model m = models[n];

			if (m != null) {
				vertexCount += m.vertexCount;
				triangleCount += m.triangleCount;
				texturedCount += m.texturedCount;
				keepInfo = keepInfo | m.triangleInfo != null;

				if (m.trianglePriorities != null) {
					keepPriorities = true;
				} else {
					if (priority == -1) {
						priority = m.priority;
					}

					if (priority != m.priority) {
						keepPriorities = true;
					}
				}

				keepAlpha = keepAlpha | m.triangleAlpha != null;
				keepSkins = keepSkins | m.triangleSkin != null;
			}
		}

		vertexX = new int[vertexCount];
		vertexY = new int[vertexCount];
		vertexZ = new int[vertexCount];
		vertexLabel = new int[vertexCount];
		triangleVertexA = new int[triangleCount];
		triangleVertexB = new int[triangleCount];
		triangleVertexC = new int[triangleCount];
		textureVertexA = new int[texturedCount];
		textureVertexB = new int[texturedCount];
		textureVertexC = new int[texturedCount];

		if (keepInfo) {
			triangleInfo = new int[triangleCount];
		}

		if (keepPriorities) {
			trianglePriorities = new int[triangleCount];
		}

		if (keepAlpha) {
			triangleAlpha = new int[triangleCount];
		}

		if (keepSkins) {
			triangleSkin = new int[triangleCount];
		}

		unmodifiedTriangleColor = new int[triangleCount];
		vertexCount = 0;
		triangleCount = 0;
		texturedCount = 0;

		for (int n = 0; n < count; n++) {
			Model m = models[n];

			if (m != null) {
				for (int t = 0; t < m.triangleCount; t++) {
					if (keepInfo) {
						if (m.triangleInfo == null) {
							triangleInfo[triangleCount] = 0;
						} else {
							triangleInfo[triangleCount] = m.triangleInfo[t];
						}
					}

					if (keepPriorities) {
						if (m.trianglePriorities == null) {
							trianglePriorities[triangleCount] = m.priority;
						} else {
							trianglePriorities[triangleCount] = m.trianglePriorities[t];
						}
					}

					if (keepAlpha) {
						if (m.triangleAlpha == null) {
							triangleAlpha[triangleCount] = 0;
						} else {
							triangleAlpha[triangleCount] = m.triangleAlpha[t];
						}
					}

					if (keepSkins && m.triangleSkin != null) {
						triangleSkin[triangleCount] = m.triangleSkin[t];
					}

					unmodifiedTriangleColor[triangleCount] = m.unmodifiedTriangleColor[t];
					triangleVertexA[triangleCount] = copyVertex(m, (m.triangleVertexA[t]));
					triangleVertexB[triangleCount] = copyVertex(m, (m.triangleVertexB[t]));
					triangleVertexC[triangleCount] = copyVertex(m, (m.triangleVertexC[t]));
					triangleCount++;
				}

				for (int t = 0; t < m.texturedCount; t++) {
					textureVertexA[texturedCount] = copyVertex(m, (m.textureVertexA[t]));
					textureVertexB[texturedCount] = copyVertex(m, (m.textureVertexB[t]));
					textureVertexC[texturedCount] = copyVertex(m, (m.textureVertexC[t]));
					texturedCount++;
				}
			}
		}
	}

	// unable to remove dummies because there would be conflicting constructors.
	// To fix this I could make a static method.
	public Model(Model[] models, int count, boolean dummy0, int dummy1) {
		boolean keepInfo = false;
		boolean keepPriorities = false;
		boolean keepAlpha = false;
		boolean keepColor = false;

		vertexCount = 0;
		triangleCount = 0;
		texturedCount = 0;
		priority = -1;

		for (int n = 0; n < count; n++) {
			Model m = models[n];

			if (m != null) {
				vertexCount += m.vertexCount;
				triangleCount += m.triangleCount;
				texturedCount += m.texturedCount;
				keepInfo = keepInfo | m.triangleInfo != null;

				if (m.trianglePriorities != null) {
					keepPriorities = true;
				} else {
					if (priority == -1) {
						priority = m.priority;
					}
					if (priority != m.priority) {
						keepPriorities = true;
					}
				}

				keepAlpha = keepAlpha | m.triangleAlpha != null;
				keepColor = keepColor | m.unmodifiedTriangleColor != null;
			}
		}

		vertexX = new int[vertexCount];
		vertexY = new int[vertexCount];
		vertexZ = new int[vertexCount];

		triangleVertexA = new int[triangleCount];
		triangleVertexB = new int[triangleCount];
		triangleVertexC = new int[triangleCount];

		triangleColorA = new int[triangleCount];
		triangleColorB = new int[triangleCount];
		triangleColorC = new int[triangleCount];

		textureVertexA = new int[texturedCount];
		textureVertexB = new int[texturedCount];
		textureVertexC = new int[texturedCount];

		if (keepInfo) {
			triangleInfo = new int[triangleCount];
		}

		if (keepPriorities) {
			trianglePriorities = new int[triangleCount];
		}

		if (keepAlpha) {
			triangleAlpha = new int[triangleCount];
		}

		if (keepColor) {
			unmodifiedTriangleColor = new int[triangleCount];
		}

		vertexCount = 0;
		triangleCount = 0;
		texturedCount = 0;

		for (int n = 0; n < count; n++) {
			Model m = models[n];

			if (m != null) {
				int lastVertex = vertexCount;

				for (int v = 0; v < m.vertexCount; v++) {
					vertexX[vertexCount] = m.vertexX[v];
					vertexY[vertexCount] = m.vertexY[v];
					vertexZ[vertexCount] = m.vertexZ[v];
					vertexCount++;
				}

				for (int t = 0; t < m.triangleCount; t++) {
					triangleVertexA[triangleCount] = m.triangleVertexA[t] + lastVertex;
					triangleVertexB[triangleCount] = m.triangleVertexB[t] + lastVertex;
					triangleVertexC[triangleCount] = m.triangleVertexC[t] + lastVertex;

					triangleColorA[triangleCount] = m.triangleColorA[t];
					triangleColorB[triangleCount] = m.triangleColorB[t];
					triangleColorC[triangleCount] = m.triangleColorC[t];

					if (keepInfo) {
						if (m.triangleInfo == null) {
							triangleInfo[triangleCount] = 0;
						} else {
							triangleInfo[triangleCount] = m.triangleInfo[t];
						}
					}

					if (keepPriorities) {
						if (m.trianglePriorities == null) {
							trianglePriorities[triangleCount] = m.priority;
						} else {
							trianglePriorities[triangleCount] = m.trianglePriorities[t];
						}
					}

					if (keepAlpha) {
						if (m.triangleAlpha == null) {
							triangleAlpha[triangleCount] = 0;
						} else {
							triangleAlpha[triangleCount] = m.triangleAlpha[t];
						}
					}

					if (keepColor && m.unmodifiedTriangleColor != null) {
						unmodifiedTriangleColor[triangleCount] = m.unmodifiedTriangleColor[t];
					}

					triangleCount++;
				}

				for (int t = 0; t < m.texturedCount; t++) {
					textureVertexA[texturedCount] = m.textureVertexA[t] + lastVertex;
					textureVertexB[texturedCount] = m.textureVertexB[t] + lastVertex;
					textureVertexC[texturedCount] = m.textureVertexC[t] + lastVertex;
					texturedCount++;
				}
			}
		}

		setupBBoxDepth();
	}

	public Model(Model from, boolean keepVertices, boolean keepColors, boolean keepAlpha, boolean keepInfo) {
		vertexCount = from.vertexCount;
		triangleCount = from.triangleCount;
		texturedCount = from.texturedCount;

		if (keepVertices) {
			vertexX = from.vertexX;
			vertexY = from.vertexY;
			vertexZ = from.vertexZ;
		} else {
			vertexX = new int[vertexCount];
			vertexY = new int[vertexCount];
			vertexZ = new int[vertexCount];
			for (int v = 0; v < vertexCount; v++) {
				vertexX[v] = from.vertexX[v];
				vertexY[v] = from.vertexY[v];
				vertexZ[v] = from.vertexZ[v];
			}
		}

		if (keepColors) {
			unmodifiedTriangleColor = from.unmodifiedTriangleColor;
		} else {
			unmodifiedTriangleColor = new int[triangleCount];
			System.arraycopy(from.unmodifiedTriangleColor, 0, unmodifiedTriangleColor, 0, triangleCount);
		}

		if (keepAlpha) {
			triangleAlpha = from.triangleAlpha;
		} else {
			triangleAlpha = new int[triangleCount];
			if (from.triangleAlpha == null) {
				for (int t = 0; t < triangleCount; t++) {
					triangleAlpha[t] = 0;
				}
			} else {
				System.arraycopy(from.triangleAlpha, 0, triangleAlpha, 0, triangleCount);
			}
		}

		if (keepInfo) {
			triangleInfo = from.triangleInfo;
		} else {
			triangleInfo = new int[triangleCount];

			if (from.triangleInfo == null) {
				for (int t = 0; t < triangleCount; t++) {
					triangleInfo[t] = 0;
				}
			} else {
				System.arraycopy(from.triangleInfo, 0, triangleInfo, 0, triangleCount);
			}
		}

		vertexLabel = from.vertexLabel;
		triangleSkin = from.triangleSkin;
		triangleVertexA = from.triangleVertexA;
		triangleVertexB = from.triangleVertexB;
		triangleVertexC = from.triangleVertexC;
		trianglePriorities = from.trianglePriorities;
		priority = from.priority;
		textureVertexA = from.textureVertexA;
		textureVertexB = from.textureVertexB;
		textureVertexC = from.textureVertexC;
	}

	public Model(Model m, boolean keepAlpha) {
		vertexCount = m.vertexCount;
		triangleCount = m.triangleCount;
		texturedCount = m.texturedCount;
		vertexX = new int[vertexCount];
		vertexY = new int[vertexCount];
		vertexZ = new int[vertexCount];

		for (int i = 0; i < vertexCount; i++) {
			vertexX[i] = m.vertexX[i];
			vertexY[i] = m.vertexY[i];
			vertexZ[i] = m.vertexZ[i];
		}

		if (keepAlpha) {
			triangleAlpha = m.triangleAlpha;
		} else {
			triangleAlpha = new int[triangleCount];
			if (m.triangleAlpha == null) {
				for (int i = 0; i < triangleCount; i++) {
					triangleAlpha[i] = 0;
				}
			} else {
				System.arraycopy(m.triangleAlpha, 0, triangleAlpha, 0, triangleCount);
			}
		}

		triangleInfo = m.triangleInfo;
		unmodifiedTriangleColor = m.unmodifiedTriangleColor;
		trianglePriorities = m.trianglePriorities;
		priority = m.priority;
		skinTriangle = m.skinTriangle;
		labelVertices = m.labelVertices;

		triangleVertexA = m.triangleVertexA;
		triangleVertexB = m.triangleVertexB;
		triangleVertexC = m.triangleVertexC;

		triangleColorA = m.triangleColorA;
		triangleColorB = m.triangleColorB;
		triangleColorC = m.triangleColorC;

		textureVertexA = m.textureVertexA;
		textureVertexB = m.textureVertexB;
		textureVertexC = m.textureVertexC;
	}

	private int copyVertex(Model from, int i) {
		int selected = -1;
		int x = from.vertexX[i];
		int y = from.vertexY[i];
		int z = from.vertexZ[i];

		for (int v = 0; v < vertexCount; v++) {
			if (x == vertexX[v] && y == vertexY[v] && z == vertexZ[v]) {
				selected = v;
				break;
			}
		}

		if (selected == -1) {
			vertexX[vertexCount] = x;
			vertexY[vertexCount] = y;
			vertexZ[vertexCount] = z;

			if (from.vertexLabel != null) {
				vertexLabel[vertexCount] = from.vertexLabel[i];
			}

			selected = vertexCount++;
		}
		return selected;
	}

	public final void setupBBoxDepth() {
		minBoundY = 0;
		boundHeight = 0;
		maxBoundY = 0;

		for (int v = 0; v < vertexCount; v++) {
			int x = vertexX[v];
			int y = vertexY[v];
			int z = vertexZ[v];

			if (-y > minBoundY) {
				minBoundY = -y;
			}

			if (y > maxBoundY) {
				maxBoundY = y;
			}

			int len = x * x + z * z;

			if (len > boundHeight) {
				boundHeight = len;
			}
		}

		boundHeight = (int) Math.sqrt((double) boundHeight);
		minDepth = (int) Math.sqrt((double) (boundHeight * boundHeight + minBoundY * minBoundY));
		maxDepth = minDepth + (int) Math.sqrt((double) (boundHeight * boundHeight + maxBoundY * maxBoundY));
	}

	public void setupBBox() {
		boundHeight = 0;

		minBoundX = 999999;
		maxBoundX = -999999;

		minBoundY = 0;
		maxBoundY = 0;

		maxBoundZ = -99999;
		minBoundZ = 99999;

		for (int v = 0; v < vertexCount; v++) {
			int x = vertexX[v];
			int y = vertexY[v];
			int z = vertexZ[v];

			if (x < minBoundX) {
				minBoundX = x;
			}

			if (x > maxBoundX) {
				maxBoundX = x;
			}

			if (z < minBoundZ) {
				minBoundZ = z;
			}

			if (z > maxBoundZ) {
				maxBoundZ = z;
			}

			if (-y > minBoundY) {
				minBoundY = -y;
			}

			if (y > maxBoundY) {
				maxBoundY = y;
			}

			int h = x * x + z * z;

			if (h > boundHeight) {
				boundHeight = h;
			}
		}

		boundHeight = (int) Math.sqrt((double) boundHeight);
		minDepth = (int) Math.sqrt((double) (boundHeight * boundHeight + minBoundY * minBoundY));
		maxDepth = minDepth + (int) Math.sqrt((double) (boundHeight * boundHeight + maxBoundY * maxBoundY));
	}

	public void applyGroups() {
		if (vertexLabel != null) {
			int[] labelCount = new int[256];
			int topLabel = 0;

			for (int v = 0; v < vertexCount; v++) {
				int lbl = vertexLabel[v];
				labelCount[lbl]++;

				if (lbl > topLabel) {
					topLabel = lbl;
				}
			}

			labelVertices = new int[topLabel + 1][];

			for (int l = 0; l <= topLabel; l++) {
				labelVertices[l] = new int[labelCount[l]];
				labelCount[l] = 0;
			}

			for (int v = 0; v < vertexCount; v++) {
				int lbl = vertexLabel[v];
				labelVertices[lbl][labelCount[lbl]++] = v;
			}

			vertexLabel = null;
		}

		if (triangleSkin != null) {
			int[] skinCount = new int[256];
			int topSkin = 0;

			for (int t = 0; t < triangleCount; t++) {
				int skin = triangleSkin[t];
				skinCount[skin]++;

				if (skin > topSkin) {
					topSkin = skin;
				}
			}

			skinTriangle = new int[topSkin + 1][];

			for (int s = 0; s <= topSkin; s++) {
				skinTriangle[s] = new int[skinCount[s]];
				skinCount[s] = 0;
			}

			for (int t = 0; t < triangleCount; t++) {
				int s = triangleSkin[t];
				skinTriangle[s][skinCount[s]++] = t;
			}

			triangleSkin = null;
		}
	}

	public void applyFrame(int frame) {
		if (labelVertices != null && frame != -1) {
			SeqFrame f = SeqFrame.instance[frame];
			SeqTransform t = f.transform;

			transformX = 0;
			transformY = 0;
			transformZ = 0;

			for (int n = 0; n < f.groupCount; n++) {
				int group = f.groups[n];
				transform(t.groupTypes[group], t.groups[group], f.x[n], f.y[n], f.z[n]);
			}
		}
	}

	public void applyFrames(int primaryFrame, int secondaryFrame, int[] labelGroups) {
		if (primaryFrame != -1) {
			if (labelGroups == null || secondaryFrame == -1) {
				applyFrame(primaryFrame);
			} else {
				SeqFrame primary = SeqFrame.instance[primaryFrame];
				SeqFrame secondary = SeqFrame.instance[secondaryFrame];
				SeqTransform t = primary.transform;

				transformX = 0;
				transformY = 0;
				transformZ = 0;

				int index = 0;
				int current = labelGroups[index++];

				for (int g = 0; g < primary.groupCount; g++) {
					int group;
					for (group = primary.groups[g]; group > current; current = labelGroups[index++]) {
						/* empty */
					}
					if (group != current || t.groupTypes[group] == 0) {
						transform(t.groupTypes[group], t.groups[group], primary.x[g], primary.y[g], primary.z[g]);
					}
				}

				transformX = 0;
				transformY = 0;
				transformZ = 0;

				index = 0;
				current = labelGroups[index++];

				for (int h = 0; h < secondary.groupCount; h++) {
					int group;

					for (group = secondary.groups[h]; group > current; current = labelGroups[index++]) {
						/* empty */
					}

					if (group == current || t.groupTypes[group] == 0) {
						transform(t.groupTypes[group], t.groups[group], secondary.x[h], secondary.y[h], secondary.z[h]);
					}
				}
			}
		}
	}

	private void transform(int type, int[] labels, int x, int y, int z) {
		int labelCount = labels.length;

		if (type == TRANSFORM_CENTRALIZE) {
			int i = 0; // vertex counter

			transformX = 0;
			transformY = 0;
			transformZ = 0;

			for (int n = 0; n < labelCount; n++) {
				int label = labels[n];

				if (label < labelVertices.length) {
					int[] vertices = labelVertices[label];

					for (int m = 0; m < vertices.length; m++) {
						int v = vertices[m];
						transformX += vertexX[v];
						transformY += vertexY[v];
						transformZ += vertexZ[v];
						i++;
					}
				}
			}

			if (i > 0) {
				// average transform x/y/z until we have the center of our label
				transformX = (transformX / i) + x;
				transformY = (transformY / i) + y;
				transformZ = (transformZ / i) + z;
			} else {
				transformX = x;
				transformY = y;
				transformZ = z;
			}
		} else if (type == TRANSFORM_POSITION) {
			for (int n = 0; n < labelCount; n++) {
				int label = labels[n];

				if (label < labelVertices.length) {
					int[] vertices = labelVertices[label];

					// simple translate
					for (int m = 0; m < vertices.length; m++) {
						int v = vertices[m];
						vertexX[v] += x;
						vertexY[v] += y;
						vertexZ[v] += z;
					}
				}
			}
		} else if (type == TRANSFORM_ROTATION) {
			for (int n = 0; n < labelCount; n++) {
				int label = labels[n];

				if (label < labelVertices.length) {
					int[] vertices = labelVertices[label];
					for (int m = 0; m < vertices.length; m++) {
						int v = vertices[m];

						// translate to origin for rotation
						vertexX[v] -= transformX;
						vertexY[v] -= transformY;
						vertexZ[v] -= transformZ;

						int yaw = (x & 0xFF) * 8;
						int roll = (y & 0xFF) * 8;
						int pitch = (z & 0xFF) * 8;

						if (pitch != 0) {
							int s = sin[pitch];
							int c = cos[pitch];
							int x0 = ((vertexY[v] * s + vertexX[v] * c) >> 16);
							vertexY[v] = (vertexY[v] * c - vertexX[v] * s) >> 16;
							vertexX[v] = x0;
						}

						if (yaw != 0) {
							int s = sin[yaw];
							int c = cos[yaw];
							int y0 = ((vertexY[v] * c - vertexZ[v] * s) >> 16);
							vertexZ[v] = (vertexY[v] * s + vertexZ[v] * c) >> 16;
							vertexY[v] = y0;
						}

						if (roll != 0) {
							int s = sin[roll];
							int c = cos[roll];
							int x0 = ((vertexZ[v] * s + vertexX[v] * c) >> 16);
							vertexZ[v] = (vertexZ[v] * c - vertexX[v] * s) >> 16;
							vertexX[v] = x0;
						}

						// put back in position
						vertexX[v] += transformX;
						vertexY[v] += transformY;
						vertexZ[v] += transformZ;
					}
				}
			}
		} else if (type == TRANSFORM_SCALE) {
			for (int n = 0; n < labelCount; n++) {
				int l = labels[n];

				if (l < labelVertices.length) {
					int[] vertices = labelVertices[l];

					for (int m = 0; m < vertices.length; m++) {
						int v = vertices[m];

						// translate to origin for proper scaling
						vertexX[v] -= transformX;
						vertexY[v] -= transformY;
						vertexZ[v] -= transformZ;

						vertexX[v] = (vertexX[v] * x) / 128;
						vertexY[v] = (vertexY[v] * y) / 128;
						vertexZ[v] = (vertexZ[v] * z) / 128;

						// but back in place
						vertexX[v] += transformX;
						vertexY[v] += transformY;
						vertexZ[v] += transformZ;
					}
				}
			}
		} else if (type == TRANSFORM_ALPHA) {
			if (skinTriangle != null && triangleAlpha != null) {
				for (int n = 0; n < labelCount; n++) {
					int l = labels[n];

					if (l < skinTriangle.length) {
						int[] triangles = skinTriangle[l];

						for (int m = 0; m < triangles.length; m++) {
							int t = triangles[m];

							triangleAlpha[t] += x * 8;

							if (triangleAlpha[t] < 0) {
								triangleAlpha[t] = 0;
							}

							if (triangleAlpha[t] > 255) {
								triangleAlpha[t] = 255;
							}
						}
					}
				}
			}
		}
	}

	public void rotateCounterClockwise() {
		for (int v = 0; v < vertexCount; v++) {
			int x = vertexX[v];
			vertexX[v] = vertexZ[v];
			vertexZ[v] = -x;
		}
	}

	public void rotatePitch(int angle) {
		int s = sin[angle];
		int c = cos[angle];
		for (int v = 0; v < vertexCount; v++) {
			int y = ((vertexY[v] * c - vertexZ[v] * s) >> 16);
			vertexZ[v] = (vertexY[v] * s + vertexZ[v] * c) >> 16;
			vertexY[v] = y;
		}
	}

	public void translate(int x, int y, int z) {
		for (int v = 0; v < vertexCount; v++) {
			vertexX[v] += x;
			vertexY[v] += y;
			vertexZ[v] += z;
		}
	}

	public void recolor(int from, int to) {
		for (int t = 0; t < triangleCount; t++) {
			if (unmodifiedTriangleColor[t] == from) {
				unmodifiedTriangleColor[t] = to;
			}
		}
	}

	public void invert() {
		for (int v = 0; v < vertexCount; v++) {
			vertexZ[v] = -vertexZ[v];
		}

		for (int t = 0; t < triangleCount; t++) {
			int a = triangleVertexA[t];
			triangleVertexA[t] = triangleVertexC[t];
			triangleVertexC[t] = a;
		}
	}

	public void scale(int x, int y, int z) {
		for (int v = 0; v < vertexCount; v++) {
			vertexX[v] = (vertexX[v] * x) / 128;
			vertexY[v] = (vertexY[v] * y) / 128;
			vertexZ[v] = (vertexZ[v] * z) / 128;
		}
	}

	public final void applyLighting(int baseLightness, int intensity, int x, int y, int z, boolean calculateLighting) {
		int light_length = (int) Math.sqrt((double) (x * x + y * y + z * z));
		int lightIntensity = intensity * light_length >> 8;

		if (triangleColorA == null) {
			triangleColorA = new int[triangleCount];
			triangleColorB = new int[triangleCount];
			triangleColorC = new int[triangleCount];
		}

		if (normals == null) {
			normals = new Normal[vertexCount];

			for (int v = 0; v < vertexCount; v++) {
				normals[v] = new Normal();
			}
		}

		for (int t = 0; t < triangleCount; t++) {
			int a = triangleVertexA[t];
			int b = triangleVertexB[t];
			int c = triangleVertexC[t];

			int deltaBAX = vertexX[b] - vertexX[a];
			int deltaBAY = vertexY[b] - vertexY[a];
			int deltaBAZ = vertexZ[b] - vertexZ[a];

			int deltaCAX = vertexX[c] - vertexX[a];
			int deltaCAY = vertexY[c] - vertexY[a];
			int deltaCAZ = vertexZ[c] - vertexZ[a];

			// crummy naming. They're just the differences between each point of
			// the triangle.
			int x0 = deltaBAY * deltaCAZ - deltaCAY * deltaBAZ;
			int y0 = deltaBAZ * deltaCAX - deltaCAZ * deltaBAX;
			int z0 = deltaBAX * deltaCAY - deltaCAX * deltaBAY;

			// while it's too large, shrink it by half
			for (; (x0 > 8192 || y0 > 8192 || z0 > 8192 || x0 < -8192 || y0 < -8192 || z0 < -8192);) {
				x0 >>= 1;
				y0 >>= 1;
				z0 >>= 1;
			}

			int length = (int) Math.sqrt((double) (x0 * x0 + y0 * y0 + z0 * z0));

			if (length <= 0) {
				length = 1;
			}

			// normalizing
			x0 = x0 * 256 / length;
			y0 = y0 * 256 / length;
			z0 = z0 * 256 / length;

			if (triangleInfo == null || (triangleInfo[t] & 0x1) == 0) {
				Normal v = normals[a];
				v.x += x0;
				v.y += y0;
				v.z += z0;
				v.magnitude++;

				v = normals[b];
				v.x += x0;
				v.y += y0;
				v.z += z0;
				v.magnitude++;

				v = normals[c];
				v.x += x0;
				v.y += y0;
				v.z += z0;
				v.magnitude++;
			} else {
				int lightness = baseLightness + (x * x0 + y * y0 + z * z0) / (lightIntensity + lightIntensity / 2);
				triangleColorA[t] = adjustHSLLightness(unmodifiedTriangleColor[t], lightness, triangleInfo[t]);
			}
		}

		if (calculateLighting) {
			calculateLighting(baseLightness, lightIntensity, x, y, z);
		} else {
			unmodifiedNormals = new Normal[vertexCount];

			for (int v = 0; v < vertexCount; v++) {
				Normal current = normals[v];
				Normal original = unmodifiedNormals[v] = new Normal();
				original.x = current.x;
				original.y = current.y;
				original.z = current.z;
				original.magnitude = current.magnitude;
			}
		}

		if (calculateLighting) {
			setupBBoxDepth();
		} else {
			setupBBox();
		}
	}

	public final void calculateLighting(int minLightness, int intensity, int x, int y, int z) {
		for (int t = 0; t < triangleCount; t++) {
			int a = triangleVertexA[t];
			int b = triangleVertexB[t];
			int c = triangleVertexC[t];

			if (triangleInfo == null) {
				int color = unmodifiedTriangleColor[t];

				Normal n = normals[a];
				int lightness = minLightness + ((x * n.x + y * n.y + z * n.z) / (intensity * n.magnitude));
				triangleColorA[t] = adjustHSLLightness(color, lightness, 0);

				n = normals[b];
				lightness = minLightness + ((x * n.x + y * n.y + z * n.z) / (intensity * n.magnitude));
				triangleColorB[t] = adjustHSLLightness(color, lightness, 0);

				n = normals[c];
				lightness = minLightness + ((x * n.x + y * n.y + z * n.z) / (intensity * n.magnitude));
				triangleColorC[t] = adjustHSLLightness(color, lightness, 0);
			} else if ((triangleInfo[t] & 0x1) == 0) {
				int color = unmodifiedTriangleColor[t];
				int info = triangleInfo[t];

				Normal v = normals[a];
				int lightness = minLightness + ((x * v.x + y * v.y + z * v.z) / (intensity * v.magnitude));
				triangleColorA[t] = adjustHSLLightness(color, lightness, info);

				v = normals[b];
				lightness = minLightness + ((x * v.x + y * v.y + z * v.z) / (intensity * v.magnitude));
				triangleColorB[t] = adjustHSLLightness(color, lightness, info);

				v = normals[c];
				lightness = minLightness + ((x * v.x + y * v.y + z * v.z) / (intensity * v.magnitude));
				triangleColorC[t] = adjustHSLLightness(color, lightness, info);
			}
		}

		normals = null;
		unmodifiedNormals = null;
		vertexLabel = null;
		triangleSkin = null;

		if (triangleInfo != null) {
			for (int t = 0; t < triangleCount; t++) {
				if ((triangleInfo[t] & 0x2) == 2) {
					return;
				}
			}
		}

		unmodifiedTriangleColor = null;
	}

	public static final int adjustHSLLightness(int hsl, int lightness, int type) {
		if ((type & 0x2) == 2) {
			if (lightness < 0) {
				lightness = 0;
			} else if (lightness > 127) {
				lightness = 127;
			}
			lightness = 127 - lightness;
			return lightness;
		}

		lightness = lightness * (hsl & 0x7f) >> 7;

		if (lightness < 2) {
			lightness = 2;
		} else if (lightness > 126) {
			lightness = 126;
		}

		return (hsl & 0xff80) + lightness;
	}

	public final void draw(int pitch, int yaw, int roll, int cameraX, int cameraY, int cameraZ, int cameraPitch) {
		final int centerX = Canvas3D.centerX;
		final int centerY = Canvas3D.centerY;

		int pitchsin = sin[pitch];
		int pitchcos = cos[pitch];

		int yawsin = sin[yaw];
		int yawcos = cos[yaw];

		int rollsin = sin[roll];
		int rollcos = cos[roll];

		int cpitchsin = sin[cameraPitch];
		int cpitchcos = cos[cameraPitch];

		int depth = cameraY * cpitchsin + cameraZ * cpitchcos >> 16;

		for (int v = 0; v < vertexCount; v++) {
			int x = vertexX[v];
			int y = vertexY[v];
			int z = vertexZ[v];

			if (roll != 0) {
				int z0 = y * rollsin + x * rollcos >> 16;
				y = y * rollcos - x * rollsin >> 16;
				x = z0;
			}

			if (pitch != 0) {
				int x0 = y * pitchcos - z * pitchsin >> 16;
				z = y * pitchsin + z * pitchcos >> 16;
				y = x0;
			}

			if (yaw != 0) {
				int y0 = z * yawsin + x * yawcos >> 16;
				z = z * yawcos - x * yawsin >> 16;
				x = y0;
			}

			x += cameraX;
			y += cameraY;
			z += cameraZ;

			int x0 = y * cpitchcos - z * cpitchsin >> 16;
			z = y * cpitchsin + z * cpitchcos >> 16;
			y = x0;

			vertexDepth[v] = z - depth;
			vertexScreenX[v] = centerX + (x << 9) / z;
			vertexScreenY[v] = centerY + (y << 9) / z;

			if (texturedCount > 0) {
				projectSceneX[v] = x;
				projectSceneY[v] = y;
				projectSceneZ[v] = z;
			}
		}
		draw(0, false, false);
	}

	public final void draw(int yaw, int cameraPitchSine, int cameraPitchCosine, int cameraYawSine, int cameraYawCosine, int sceneX, int sceneY, int sceneZ, int bitset) {
		int cameraY = sceneZ * cameraYawCosine - sceneX * cameraYawSine >> 16;
		int farZ = sceneY * cameraPitchSine + cameraY * cameraPitchCosine >> 16;
		int distanceZ = boundHeight * cameraPitchCosine >> 16;
		int nearZ = farZ + distanceZ;

		if (nearZ > Scene.NEAR_Z && farZ < Scene.FAR_Z) {
			int i_254_ = sceneZ * cameraYawSine + sceneX * cameraYawCosine >> 16;
			int minX = i_254_ - boundHeight << 9;

			if (minX / nearZ < Canvas2D.centerX) {
				int maxX = i_254_ + boundHeight << 9;

				if (maxX / nearZ > -Canvas2D.centerX) {
					int i_257_ = sceneY * cameraPitchCosine - cameraY * cameraPitchSine >> 16;
					int i_258_ = boundHeight * cameraPitchSine >> 16;
					int maxY = i_257_ + i_258_ << 9;

					if (maxY / nearZ > -Canvas2D.centerY) {
						int i_260_ = i_258_ + (minBoundY * cameraPitchCosine >> 16);
						int minY = i_257_ - i_260_ << 9;

						if (minY / nearZ < Canvas2D.centerY) {
							int i_262_ = distanceZ + (minBoundY * cameraPitchSine >> 16);
							boolean project = false;

							if (farZ - i_262_ <= Scene.NEAR_Z) {
								project = true;
							}

							boolean hasInput = false;

							if (bitset > 0 && allowInput) {
								int zd = farZ - distanceZ;

								if (zd <= Scene.NEAR_Z) {
									zd = Scene.NEAR_Z;
								}

								if (i_254_ > 0) {
									minX /= nearZ;
									maxX /= zd;
								} else {
									maxX /= nearZ;
									minX /= zd;
								}

								if (i_257_ > 0) {
									minY /= nearZ;
									maxY /= zd;
								} else {
									maxY /= nearZ;
									minY /= zd;
								}

								int x = mouseX - Canvas3D.centerX;
								int y = mouseY - Canvas3D.centerY;

								if (x > minX && x < maxX && y > minY && y < maxY) {
									hasInput = true;
								}
							}

							int cx = Canvas3D.centerX;
							int cy = Canvas3D.centerY;

							int yawsin = 0;
							int yawcos = 0;

							if (yaw != 0) {
								yawsin = sin[yaw];
								yawcos = cos[yaw];
							}

							for (int v = 0; v < vertexCount; v++) {
								int x = vertexX[v];
								int y = vertexY[v];
								int z = vertexZ[v];

								if (yaw != 0) {
									int w = z * yawsin + x * yawcos >> 16;
									z = z * yawcos - x * yawsin >> 16;
									x = w;
								}

								x += sceneX;
								y += sceneY;
								z += sceneZ;

								int w = z * cameraYawSine + x * cameraYawCosine >> 16;
								z = z * cameraYawCosine - x * cameraYawSine >> 16;
								x = w;

								w = y * cameraPitchCosine - z * cameraPitchSine >> 16;
								z = y * cameraPitchSine + z * cameraPitchCosine >> 16;
								y = w;

								vertexDepth[v] = z - farZ;

								if (z >= Scene.NEAR_Z) {
									vertexScreenX[v] = cx + (x << 9) / z;
									vertexScreenY[v] = cy + (y << 9) / z;
								} else {
									vertexScreenX[v] = -5000;
									project = true;
								}

								if (project || texturedCount > 0) {
									projectSceneX[v] = x;
									projectSceneY[v] = y;
									projectSceneZ[v] = z;
								}
							}

							try {
								draw(bitset, project, hasInput);
							} catch (Exception e) {

							}
						}
					}
				}
			}
		}
	}

	private void draw(int bitset, boolean projected, boolean hasInput) {
		for (int d = 0; d < maxDepth; d++) {
			depthTriangleCount[d] = 0;
		}

		for (int t = 0; t < triangleCount; t++) {
			if (triangleInfo == null || triangleInfo[t] != -1) {
				int a = triangleVertexA[t];
				int b = triangleVertexB[t];
				int c = triangleVertexC[t];
				int x0 = vertexScreenX[a];
				int x1 = vertexScreenX[b];
				int x2 = vertexScreenX[c];

				if (projected && (x0 == -5000 || x1 == -5000 || x2 == -5000)) {
					projectTriangle[t] = true;
					int depth = ((vertexDepth[a] + vertexDepth[b] + vertexDepth[c]) / 3 + minDepth);
					depthTriangles[depth][depthTriangleCount[depth]++] = t;
				} else {
					if (hasInput && withinTriangle(mouseX, mouseY, vertexScreenY[a], vertexScreenY[b], vertexScreenY[c], x0, x1, x2)) {
						hoveredBitsets[hoverCount++] = bitset;
						hasInput = false;
					}

					if (((x0 - x1) * (vertexScreenY[c] - vertexScreenY[b]) - ((vertexScreenY[a] - vertexScreenY[b]) * (x2 - x1))) > 0) {
						projectTriangle[t] = false;
						verifyTriangleBounds[t] = x0 < 0 || x1 < 0 || x2 < 0 || x0 > Canvas2D.dstXBound || x1 > Canvas2D.dstXBound || x2 > Canvas2D.dstXBound;
						int depth = ((vertexDepth[a] + vertexDepth[b] + vertexDepth[c]) / 3 + minDepth);
						depthTriangles[depth][depthTriangleCount[depth]++] = t;
					}
				}
			}
		}

		if (trianglePriorities == null) {
			for (int d = maxDepth - 1; d >= 0; d--) {
				int n = depthTriangleCount[d];
				if (n > 0) {
					int[] triangles = depthTriangles[d];
					for (int t = 0; t < n; t++) {
						drawTriangle(triangles[t]);
					}
				}
			}
		} else {
			for (int p = 0; p < 12; p++) {
				priorityTriangleCounts[p] = 0;
				lowPriorityDepth[p] = 0;
			}

			for (int d = maxDepth - 1; d >= 0; d--) {
				int n = depthTriangleCount[d];
				if (n > 0) {
					int[] triangles = depthTriangles[d];
					for (int m = 0; m < n; m++) {
						int t = triangles[m];
						int trianglePriority = trianglePriorities[t];
						int priorityTriangle = priorityTriangleCounts[trianglePriority]++;
						priorityTriangles[trianglePriority][priorityTriangle] = t;

						if (trianglePriority < 10) {
							lowPriorityDepth[trianglePriority] += d;
						} else if (trianglePriority == 10) {
							normalTrianglePriority[priorityTriangle] = d;
						} else {
							highTrianglePriority[priorityTriangle] = d;
						}
					}
				}
			}

			int minPriority = 0;
			if (priorityTriangleCounts[1] > 0 || priorityTriangleCounts[2] > 0) {
				minPriority = ((lowPriorityDepth[1] + lowPriorityDepth[2]) / (priorityTriangleCounts[1] + priorityTriangleCounts[2]));
			}

			int halfPriority = 0;
			if (priorityTriangleCounts[3] > 0 || priorityTriangleCounts[4] > 0) {
				halfPriority = ((lowPriorityDepth[3] + lowPriorityDepth[4]) / (priorityTriangleCounts[3] + priorityTriangleCounts[4]));
			}

			int maxPriority = 0;
			if (priorityTriangleCounts[6] > 0 || priorityTriangleCounts[8] > 0) {
				maxPriority = ((lowPriorityDepth[6] + lowPriorityDepth[8]) / (priorityTriangleCounts[6] + priorityTriangleCounts[8]));
			}

			int t = 0;
			int priorityTriangleCount = priorityTriangleCounts[10];
			int[] triangles = priorityTriangles[10];
			int[] priorities = normalTrianglePriority;

			if (t == priorityTriangleCount) {
				t = 0;
				priorityTriangleCount = priorityTriangleCounts[11];
				triangles = priorityTriangles[11];
				priorities = highTrianglePriority;
			}

			int pri;

			if (t < priorityTriangleCount) {
				pri = priorities[t];
			} else {
				pri = -1000;
			}

			for (int p = 0; p < 10; p++) {
				while (p == 0) {
					if (pri <= minPriority) {
						break;
					}

					drawTriangle(triangles[t++]);
					if (t == priorityTriangleCount && triangles != priorityTriangles[11]) {
						t = 0;
						priorityTriangleCount = priorityTriangleCounts[11];
						triangles = priorityTriangles[11];
						priorities = highTrianglePriority;
					}
					if (t < priorityTriangleCount) {
						pri = priorities[t];
					} else {
						pri = -1000;
					}
				}

				while (p == 3) {
					if (pri <= halfPriority) {
						break;
					}

					drawTriangle(triangles[t++]);

					if (t == priorityTriangleCount && triangles != priorityTriangles[11]) {
						t = 0;
						priorityTriangleCount = priorityTriangleCounts[11];
						triangles = priorityTriangles[11];
						priorities = highTrianglePriority;
					}

					if (t < priorityTriangleCount) {
						pri = priorities[t];
					} else {
						pri = -1000;
					}
				}

				while (p == 5 && pri > maxPriority) {
					drawTriangle(triangles[t++]);

					if (t == priorityTriangleCount && triangles != priorityTriangles[11]) {
						t = 0;
						priorityTriangleCount = priorityTriangleCounts[11];
						triangles = priorityTriangles[11];
						priorities = highTrianglePriority;
					}

					if (t < priorityTriangleCount) {
						pri = priorities[t];
					} else {
						pri = -1000;
					}
				}

				int n = priorityTriangleCounts[p];
				int[] tris = priorityTriangles[p];

				for (int m = 0; m < n; m++) {
					drawTriangle(tris[m]);
				}
			}

			while (pri != -1000) {
				drawTriangle(triangles[t++]);

				if (t == priorityTriangleCount && triangles != priorityTriangles[11]) {
					t = 0;
					triangles = priorityTriangles[11];
					priorityTriangleCount = priorityTriangleCounts[11];
					priorities = highTrianglePriority;
				}

				if (t < priorityTriangleCount) {
					pri = priorities[t];
				} else {
					pri = -1000;
				}
			}
		}
	}

	private void drawTriangle(int index) {
		if (projectTriangle[index]) {
			drawProjectedTriangle(index);
		} else {
			int a = triangleVertexA[index];
			int b = triangleVertexB[index];
			int c = triangleVertexC[index];

			Canvas3D.verifyBounds = verifyTriangleBounds[index];

			if (triangleAlpha == null) {
				Canvas3D.alpha = 0;
			} else {
				Canvas3D.alpha = triangleAlpha[index];
			}

			int type;

			if (triangleInfo == null) {
				type = 0;
			} else {
				type = triangleInfo[index] & 0x3;
			}

			if (type == 0) {
				Canvas3D.fillShadedTriangle(vertexScreenX[a], vertexScreenY[a], vertexScreenX[b], vertexScreenY[b], vertexScreenX[c], vertexScreenY[c], triangleColorA[index], triangleColorB[index], triangleColorC[index]);
			} else if (type == 1) {
				Canvas3D.fillTriangle(vertexScreenX[a], vertexScreenY[a], vertexScreenX[b], vertexScreenY[b], vertexScreenX[c], vertexScreenY[c], palette[triangleColorA[index]]);
			} else if (type == 2) {
				// texture triangle
				int t = triangleInfo[index] >> 2;
				int ta = textureVertexA[t];
				int tb = textureVertexB[t];
				int tc = textureVertexC[t];
				Canvas3D.fillTexturedTriangle(vertexScreenY[a], vertexScreenY[b], vertexScreenY[c], vertexScreenX[a], vertexScreenX[b], vertexScreenX[c], triangleColorA[index], triangleColorB[index], triangleColorC[index], projectSceneX[ta], projectSceneX[tb], projectSceneX[tc], projectSceneY[ta], projectSceneY[tb], projectSceneY[tc], projectSceneZ[ta], projectSceneZ[tb], projectSceneZ[tc], unmodifiedTriangleColor[index]);
			} else if (type == 3) {
				// texture triangle
				int t = triangleInfo[index] >> 2;
				int ta = textureVertexA[t];
				int tb = textureVertexB[t];
				int tc = textureVertexC[t];
				Canvas3D.fillTexturedTriangle(vertexScreenY[a], vertexScreenY[b], vertexScreenY[c], vertexScreenX[a], vertexScreenX[b], vertexScreenX[c], triangleColorA[index], triangleColorA[index], triangleColorA[index], projectSceneX[ta], projectSceneX[tb], projectSceneX[tc], projectSceneY[ta], projectSceneY[tb], projectSceneY[tc], projectSceneZ[ta], projectSceneZ[tb], projectSceneZ[tc], unmodifiedTriangleColor[index]);
			}
		}
	}

	private void drawProjectedTriangle(int index) {
		int centerX = Canvas3D.centerX;
		int centerY = Canvas3D.centerY;
		int n = 0;

		int a = triangleVertexA[index];
		int b = triangleVertexB[index];
		int c = triangleVertexC[index];

		int zA = projectSceneZ[a];
		int zB = projectSceneZ[b];
		int zC = projectSceneZ[c];

		// TODO: document properly
		if (zA >= Scene.NEAR_Z) {
			tmpX[n] = vertexScreenX[a];
			tmpY[n] = vertexScreenY[a];
			tmpColor[n++] = triangleColorA[index];
		} else {
			int x = projectSceneX[a];
			int y = projectSceneY[a];
			int color = triangleColorA[index];

			if (zC >= Scene.NEAR_Z) {
				int interpolant = (Scene.NEAR_Z - zA) * oneOverFixed1616[zC - zA];
				tmpX[n] = centerX + ((x + (((projectSceneX[c] - x) * interpolant) >> 16)) << 9) / Scene.NEAR_Z;
				tmpY[n] = centerY + ((y + (((projectSceneY[c] - y) * interpolant) >> 16)) << 9) / Scene.NEAR_Z;
				tmpColor[n++] = color + ((triangleColorC[index] - color) * interpolant >> 16);
			}

			if (zB >= Scene.NEAR_Z) {
				int interpolant = (Scene.NEAR_Z - zA) * oneOverFixed1616[zB - zA];
				tmpX[n] = (centerX + (x + ((projectSceneX[b] - x) * interpolant >> 16) << 9) / Scene.NEAR_Z);
				tmpY[n] = (centerY + (y + ((projectSceneY[b] - y) * interpolant >> 16) << 9) / Scene.NEAR_Z);
				tmpColor[n++] = color + ((triangleColorB[index] - color) * interpolant >> 16);
			}
		}

		if (zB >= Scene.NEAR_Z) {
			tmpX[n] = vertexScreenX[b];
			tmpY[n] = vertexScreenY[b];
			tmpColor[n++] = triangleColorB[index];
		} else {
			int x = projectSceneX[b];
			int y = projectSceneY[b];
			int color = triangleColorB[index];

			if (zA >= Scene.NEAR_Z) {
				int mul = (Scene.NEAR_Z - zB) * oneOverFixed1616[zA - zB];
				tmpX[n] = (centerX + (x + ((projectSceneX[a] - x) * mul >> 16) << 9) / Scene.NEAR_Z);
				tmpY[n] = (centerY + (y + ((projectSceneY[a] - y) * mul >> 16) << 9) / Scene.NEAR_Z);
				tmpColor[n++] = color + ((triangleColorA[index] - color) * mul >> 16);
			}

			if (zC >= Scene.NEAR_Z) {
				int mul = (Scene.NEAR_Z - zB) * oneOverFixed1616[zC - zB];
				tmpX[n] = (centerX + (x + ((projectSceneX[c] - x) * mul >> 16) << 9) / Scene.NEAR_Z);
				tmpY[n] = (centerY + (y + ((projectSceneY[c] - y) * mul >> 16) << 9) / Scene.NEAR_Z);
				tmpColor[n++] = color + ((triangleColorC[index] - color) * mul >> 16);
			}
		}

		if (zC >= Scene.NEAR_Z) {
			tmpX[n] = vertexScreenX[c];
			tmpY[n] = vertexScreenY[c];
			tmpColor[n++] = triangleColorC[index];
		} else {
			int x = projectSceneX[c];
			int y = projectSceneY[c];
			int color = triangleColorC[index];

			if (zB >= Scene.NEAR_Z) {
				int mul = (Scene.NEAR_Z - zC) * (oneOverFixed1616[zB - zC]);

				tmpX[n] = (centerX + (x + (((projectSceneX[b] - x) * mul) >> 16) << 9) / Scene.NEAR_Z);
				tmpY[n] = (centerY + (y + (((projectSceneY[b] - y) * mul) >> 16) << 9) / Scene.NEAR_Z);
				tmpColor[n++] = color + ((triangleColorB[index] - color) * mul >> 16);
			}
			if (zA >= Scene.NEAR_Z) {
				int mul = (Scene.NEAR_Z - zC) * oneOverFixed1616[zA - zC];
				tmpX[n] = (centerX + (x + (((projectSceneX[a] - x) * mul) >> 16) << 9) / Scene.NEAR_Z);
				tmpY[n] = (centerY + (y + (((projectSceneY[a] - y) * mul) >> 16) << 9) / Scene.NEAR_Z);
				tmpColor[n++] = color + ((triangleColorA[index] - color) * mul >> 16);
			}
		}

		int xA = tmpX[0];
		int xB = tmpX[1];
		int xC = tmpX[2];

		int yA = tmpY[0];
		int yB = tmpY[1];
		int yC = tmpY[2];

		if (((xA - xB) * (yC - yB) - (yA - yB) * (xC - xB)) > 0) {
			Canvas3D.verifyBounds = false;

			if (n == 3) {
				if (xA < 0 || xB < 0 || xC < 0 || xA > Canvas2D.dstXBound || xB > Canvas2D.dstXBound || xC > Canvas2D.dstXBound) {
					Canvas3D.verifyBounds = true;
				}

				int type;

				if (triangleInfo == null) {
					type = 0;
				} else {
					type = triangleInfo[index] & 0x3;
				}

				if (type == 0) {
					Canvas3D.fillShadedTriangle(xA, yA, xB, yB, xC, yC, tmpColor[0], tmpColor[1], tmpColor[2]);
				} else if (type == 1) {
					Canvas3D.fillTriangle(xA, yA, xB, yB, xC, yC, (palette[(triangleColorA[index])]));
				} else if (type == 2) {
					int t = triangleInfo[index] >> 2;
					int tA = textureVertexA[t];
					int tB = textureVertexB[t];
					int tC = textureVertexC[t];
					Canvas3D.fillTexturedTriangle(yA, yB, yC, xA, xB, xC, tmpColor[0], tmpColor[1], tmpColor[2], projectSceneX[tA], projectSceneX[tB], projectSceneX[tC], projectSceneY[tA], projectSceneY[tB], projectSceneY[tC], projectSceneZ[tA], projectSceneZ[tB], projectSceneZ[tC], unmodifiedTriangleColor[index]);
				} else if (type == 3) {
					int t = triangleInfo[index] >> 2;
					int tA = textureVertexA[t];
					int tB = textureVertexB[t];
					int tC = textureVertexC[t];
					Canvas3D.fillTexturedTriangle(yA, yB, yC, xA, xB, xC, triangleColorA[index], triangleColorA[index], triangleColorA[index], projectSceneX[tA], projectSceneX[tB], projectSceneX[tC], projectSceneY[tA], projectSceneY[tB], projectSceneY[tC], projectSceneZ[tA], projectSceneZ[tB], projectSceneZ[tC], unmodifiedTriangleColor[index]);
				}
			}

			if (n == 4) {
				if (xA < 0 || xB < 0 || xC < 0 || xA > Canvas2D.dstXBound || xB > Canvas2D.dstXBound || xC > Canvas2D.dstXBound || tmpX[3] < 0 || tmpX[3] > Canvas2D.dstXBound) {
					Canvas3D.verifyBounds = true;
				}

				int type;

				if (triangleInfo == null) {
					type = 0;
				} else {
					type = triangleInfo[index] & 0x3;
				}

				if (type == 0) {
					Canvas3D.fillShadedTriangle(xA, yA, xB, yB, xC, yC, tmpColor[0], tmpColor[1], tmpColor[2]);
					Canvas3D.fillShadedTriangle(xA, yA, xC, yC, tmpX[3], tmpY[3], tmpColor[0], tmpColor[2], tmpColor[3]);
				} else if (type == 1) {
					int rgb = palette[triangleColorA[index]];
					Canvas3D.fillTriangle(xA, yA, xB, yB, xC, yC, rgb);
					Canvas3D.fillTriangle(xA, yA, xC, yC, tmpX[3], tmpY[3], rgb);
				} else if (type == 2) {
					int t = triangleInfo[index] >> 2;
					int tA = textureVertexA[t];
					int tB = textureVertexB[t];
					int tC = textureVertexC[t];
					Canvas3D.fillTexturedTriangle(yA, yB, yC, xA, xB, xC, tmpColor[0], tmpColor[1], tmpColor[2], projectSceneX[tA], projectSceneX[tB], projectSceneX[tC], projectSceneY[tA], projectSceneY[tB], projectSceneY[tC], projectSceneZ[tA], projectSceneZ[tB], projectSceneZ[tC], unmodifiedTriangleColor[index]);
					Canvas3D.fillTexturedTriangle(yA, yC, tmpY[3], xA, xC, tmpX[3], tmpColor[0], tmpColor[2], tmpColor[3], projectSceneX[tA], projectSceneX[tB], projectSceneX[tC], projectSceneY[tA], projectSceneY[tB], projectSceneY[tC], projectSceneZ[tA], projectSceneZ[tB], projectSceneZ[tC], unmodifiedTriangleColor[index]);
				} else if (type == 3) {
					int t = triangleInfo[index] >> 2;
					int tA = textureVertexA[t];
					int tB = textureVertexB[t];
					int tC = textureVertexC[t];
					Canvas3D.fillTexturedTriangle(yA, yB, yC, xA, xB, xC, triangleColorA[index], triangleColorA[index], triangleColorA[index], projectSceneX[tA], projectSceneX[tB], projectSceneX[tC], projectSceneY[tA], projectSceneY[tB], projectSceneY[tC], projectSceneZ[tA], projectSceneZ[tB], projectSceneZ[tC], unmodifiedTriangleColor[index]);
					Canvas3D.fillTexturedTriangle(yA, yC, tmpY[3], xA, xC, tmpX[3], triangleColorA[index], triangleColorA[index], triangleColorA[index], projectSceneX[tA], projectSceneX[tB], projectSceneX[tC], projectSceneY[tA], projectSceneY[tB], projectSceneY[tC], projectSceneZ[tA], projectSceneZ[tB], projectSceneZ[tC], unmodifiedTriangleColor[index]);
				}
			}
		}
	}

	public static final boolean withinTriangle(int x, int y, int y0, int y1, int y2, int x0, int x1, int x2) {
		if (y < y0 && y < y1 && y < y2) {
			return false;
		}
		if (y > y0 && y > y1 && y > y2) {
			return false;
		}
		if (x < x0 && x < x1 && x < x2) {
			return false;
		}
		return !(x > x0 && x > x1 && x > x2);
	}
}
