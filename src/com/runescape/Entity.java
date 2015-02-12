package com.runescape;

public class Entity extends Renderable {

	int sceneX;
	int sceneZ;
	int yaw;

	int renderPadding;
	int size = 1;

	int animWalkIndex = -1;
	int animRunIndex = -1;
	int animTurnRightIndex = -1;
	int animTurnLeftIndex = -1;
	int animStand = -1;
	int animTurnIndex = -1;

	String spoken;
	int spokenLife = 100;
	int spokenColor;
	int spokenEffect;

	int damageTaken;
	int damageType;

	int lastCombatCycle = -1000;

	int health;
	int maxHealth;

	int targetEntity = -1;

	int focusX;
	int focusY;

	int secondaryAnimIndex = -1;
	int secondaryAnimFrame;
	int secondaryAnimCycle;

	int primaryAnimIndex = -1;
	int primaryAnimFrame;
	int primaryAnimCycle;
	int primaryAnimDelay;
	int primaryAnimDelta;

	int spotanimIndex = -1;
	int spotanimFrame;
	int spotanimCycle;
	int lastSpotanimCycle;
	int spotanimOffsetY;

	int srcTileX;
	int dstTileX;
	int srcTileY;
	int dstTileY;

	int firstMoveCycle;
	int lastMoveCycle;

	int faceDirection;
	boolean remove = false;
	int height;
	int dstYaw;

	int pathStepCount;
	int[] pathX = new int[10];
	int[] pathY = new int[10];

	int catchupCycles;

	public final void setPosition(int x, int y) {
		if (x != pathX[0] || y != pathY[0]) {
			if (primaryAnimIndex != -1 && Animation.instance[primaryAnimIndex].priority <= 1) {
				primaryAnimIndex = -1;
			}

			if (pathStepCount < 9) {
				pathStepCount++;
				for (int n = pathStepCount; n > 0; n--) {
					pathX[n] = pathX[n - 1];
					pathY[n] = pathY[n - 1];
				}
			} else {
				for (int n = 8; n > 0; n--) {
					pathX[n] = pathX[n - 1];
					pathY[n] = pathY[n - 1];
				}
			}
			pathX[0] = x;
			pathY[0] = y;
		}
	}

	public final void moveBy(boolean bool, int dy, int dx) {
		if (dx != 0 || dy != 0) {
			int x = pathX[0] + dx;
			int y = pathY[0] + dy;

			if (primaryAnimIndex != -1 && Animation.instance[primaryAnimIndex].priority <= 1) {
				primaryAnimIndex = -1;
			}

			if (pathStepCount < 9) {
				pathStepCount++;
				for (int n = pathStepCount; n > 0; n--) {
					pathX[n] = pathX[n - 1];
					pathY[n] = pathY[n - 1];
				}
			} else {
				for (int n = 8; n > 0; n--) {
					pathX[n] = pathX[n - 1];
					pathY[n] = pathY[n - 1];
				}
			}
			pathX[0] = x;
			pathY[0] = y;
		}
	}
}
