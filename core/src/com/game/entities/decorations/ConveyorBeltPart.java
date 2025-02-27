package com.game.entities.decorations;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.game.GameContext2d;
import com.game.animations.AnimationComponent;
import com.game.animations.TimedAnimation;
import com.game.entities.Entity;
import com.game.sprites.SpriteComponent;

import static com.game.ViewVals.PPM;
import static com.game.assets.TextureAsset.CONVEYOR_BELT;
import static com.game.utils.UtilMethods.equalsAny;

public class ConveyorBeltPart extends Entity {

    public ConveyorBeltPart(GameContext2d gameContext, Vector2 pos, String part, boolean isMovingLeft) {
        super(gameContext);
        Rectangle bounds = new Rectangle(pos.x, pos.y, PPM, PPM / 2f);
        addComponent(spriteComponent(bounds));
        addComponent(animationComponent(part, isMovingLeft));
    }

    protected SpriteComponent spriteComponent(Rectangle bounds) {
        Sprite sprite = new Sprite();
        sprite.setBounds(bounds.x, bounds.y, PPM, PPM);
        return new SpriteComponent(sprite);
    }

    protected AnimationComponent animationComponent(String part, boolean isMovingLeft) {
        TextureAtlas conveyorAtlas = gameContext.getAsset(CONVEYOR_BELT.getSrc(), TextureAtlas.class);
        TextureRegion region;
        if (equalsAny(part, "left", "right")) {
            if (part.equals("left")) {
                region = conveyorAtlas.findRegion(isMovingLeft ? "LeftPart-MoveLeft" : "LeftPart-MoveRight");
            } else {
                region = conveyorAtlas.findRegion(isMovingLeft ? "RightPart-MoveLeft" : "RightPart-MoveRight");
            }
        } else {
            region = conveyorAtlas.findRegion("MiddlePart");
        }
        TimedAnimation animation = new TimedAnimation(region, 2, .15f);
        return new AnimationComponent(animation);
    }

}
