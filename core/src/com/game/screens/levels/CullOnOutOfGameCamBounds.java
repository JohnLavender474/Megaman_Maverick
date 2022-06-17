package com.game.screens.levels;

import com.badlogic.gdx.math.Rectangle;
import com.game.entities.Entity;
import com.game.utils.Timer;

/**
 * Implemented by {@link Entity} that should be culled when out of bounds of the game camera.
 */
public interface CullOnOutOfGameCamBounds {

    /**
     * Gets cull timer. Timer is updated when entity is out of game camera bounds and reset if the entity re-enters
     * the bounds. When {@link Timer#isFinished()} returns true, then the entity is marked for removal.
     *
     * @return the cull timer
     */
    Timer getCullTimer();

    /**
     * Gets bounding box. Used to determine if within game camera bounds or not.
     *
     * @return the bounding box
     */
    Rectangle getBoundingBox();

}
