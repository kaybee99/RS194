package dane.runescape.mapeditor.media;

import java.awt.Polygon;
import java.util.HashMap;
import java.util.Map;

public class TileShape {

	public static final int SHAPE_VERTICES[][] = {{1, 3, 5, 7}, {1, 3, 5, 7}, {1, 3, 5, 7}, {1, 3, 5, 7, 6}, {1, 3, 5, 7, 6}, {1, 3, 5, 7, 6}, {1, 3, 5, 7, 6}, {1, 3, 5, 7, 2, 6}, {1, 3, 5, 7, 2, 8}, {1, 3, 5, 7, 2, 8}, {1, 3, 5, 7, 11, 12}, {1, 3, 5, 7, 11, 12}, {1, 3, 5, 7, 13, 14}};
	public static final int SHAPE_ROTATIONS[][] = {{0, 1, 2, 3, 0, 0, 1, 3}, {1, 1, 2, 3, 1, 0, 1, 3}, {0, 1, 2, 3, 1, 0, 1, 3}, {0, 0, 1, 2, 0, 0, 2, 4, 1, 0, 4, 3}, {0, 0, 1, 4, 0, 0, 4, 3, 1, 1, 2, 4}, {0, 0, 4, 3, 1, 0, 1, 2, 1, 0, 2, 4}, {0, 1, 2, 4, 1, 0, 1, 4, 1, 0, 4, 3}, {0, 4, 1, 2, 0, 4, 2, 5, 1, 0, 4, 5, 1, 0, 5, 3}, {0, 4, 1, 2, 0, 4, 2, 3, 0, 4, 3, 5, 1, 0, 4, 5}, {0, 0, 4, 5, 1, 4, 1, 2, 1, 4, 2, 3, 1, 4, 3, 5}, {0, 0, 1, 5, 0, 1, 4, 5, 0, 1, 2, 4, 1, 0, 5, 3, 1, 5, 4, 3, 1, 4, 2, 3}, {1, 0, 1, 5, 1, 1, 4, 5, 1, 1, 2, 4, 0, 0, 5, 3, 0, 5, 4, 3, 0, 4, 2, 3}, {1, 0, 5, 4, 1, 0, 1, 5, 0, 0, 4, 3, 0, 4, 5, 3, 0, 5, 2, 3, 0, 1, 2, 5}};
	private static final Map<Integer, Polygon> stored = new HashMap<>();

	public static final Polygon getPolygon(int shape, int rotation) {
		int uid = shape | (rotation << 5);

		Polygon p = stored.get(uid);

		if (p != null) {
			return p;
		}

		p = new Polygon();

		int tileSize = 16;
		int halfSize = tileSize / 2;
		int quarterSize = tileSize / 4;
		int threeQuarterSize = (tileSize * 3) / 4;

		int[] vertices = SHAPE_VERTICES[shape];
		int vertexCount = vertices.length;

		int[] vertexX = new int[vertexCount];
		int[] vertexY = new int[vertexCount];

		for (int v = 0; v < vertexCount; v++) {
			int vertex = vertices[v];

			if ((vertex & 0x1) == 0 && vertex <= 8) {
				vertex = (vertex - rotation - rotation - 1 & 0x7) + 1;
			}

			if (vertex > 8 && vertex <= 12) {
				vertex = (vertex - 9 - rotation & 0x3) + 9;
			}

			if (vertex > 12 && vertex <= 16) {
				vertex = (vertex - 13 - rotation & 0x3) + 13;
			}

			int x;
			int y;

			if (vertex == 1) {
				x = 0;
				y = 0;
			} else if (vertex == 2) {
				x = halfSize;
				y = 0;
			} else if (vertex == 3) {
				x = tileSize;
				y = 0;
			} else if (vertex == 4) {
				x = tileSize;
				y = halfSize;
			} else if (vertex == 5) {
				x = tileSize;
				y = tileSize;
			} else if (vertex == 6) {
				x = halfSize;
				y = tileSize;
			} else if (vertex == 7) {
				x = 0;
				y = tileSize;
			} else if (vertex == 8) {
				x = 0;
				y = halfSize;
			} else if (vertex == 9) {
				x = halfSize;
				y = quarterSize;
			} else if (vertex == 10) {
				x = threeQuarterSize;
				y = halfSize;
			} else if (vertex == 11) {
				x = halfSize;
				y = threeQuarterSize;
			} else if (vertex == 12) {
				x = quarterSize;
				y = halfSize;
			} else if (vertex == 13) {
				x = quarterSize;
				y = quarterSize;
			} else if (vertex == 14) {
				x = threeQuarterSize;
				y = quarterSize;
			} else if (vertex == 15) {
				x = threeQuarterSize;
				y = threeQuarterSize;
			} else {
				x = quarterSize;
				y = threeQuarterSize;
			}

			vertexX[v] = x;
			vertexY[v] = y;
		}

		int[] rotations = SHAPE_ROTATIONS[shape];
		int triangleCount = rotations.length / 4;

		int j = 0;
		for (int n = 0; n < triangleCount; n++) {
			int path = rotations[j];
			int a = rotations[j + 1];
			int b = rotations[j + 2];
			int c = rotations[j + 3];

			j += 4;

			if (a < 4) {
				a = a - rotation & 0x3;
			}

			if (b < 4) {
				b = b - rotation & 0x3;
			}

			if (c < 4) {
				c = c - rotation & 0x3;
			}

			if (path != 0) {
				p.addPoint(vertexX[c], tileSize - vertexY[c]);
				p.addPoint(vertexX[b], tileSize - vertexY[b]);
				p.addPoint(vertexX[a], tileSize - vertexY[a]);
			}
		}
		stored.put(uid, p);
		return p;
	}
}
