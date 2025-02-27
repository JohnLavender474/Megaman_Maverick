package com.game.entities.special;

import com.badlogic.gdx.math.Rectangle;
import com.game.GameContext2d;
import com.game.entities.Entity;
import com.game.world.BodyComponent;

import static com.game.world.BodyType.*;

public class AbstractBounds extends Entity {

    public static final String ABSTRACT_BOUNDS = "AbstractBounds";

    public AbstractBounds(GameContext2d gameContext, Rectangle bounds) {
        super(gameContext);
        addComponent(bodyComponent(bounds));
    }

    private BodyComponent bodyComponent(Rectangle bounds) {
        BodyComponent bodyComponent = new BodyComponent(ABSTRACT);
        bodyComponent.setCustomCollisionBit(ABSTRACT_BOUNDS);
        bodyComponent.set(bounds);
        return bodyComponent;
    }

}
