package dane.runescape.mapeditor.event;

import com.runescape.Landscape;
import com.runescape.Scene;
import java.util.EventListener;

public interface GameListener extends EventListener {

	/**
	 * Called when the scene finishes loading.
	 *
	 * @param plane the plane the scene loaded on.
	 * @param s the scene.
	 * @param l the landscape.
	 */
	void onSceneCreation(int plane, Scene s, Landscape l);
}
