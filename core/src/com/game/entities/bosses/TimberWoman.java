package com.game.entities.bosses;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.game.animations.AnimationComponent;
import com.game.animations.TimedAnimation;
import com.game.entities.Entity;
import com.game.GameContext2d;
import com.game.assets.TextureAsset;
import com.game.shapes.ShapeComponent;
import com.game.shapes.ShapeHandle;
import com.game.sprites.SpriteProcessor;
import com.game.sprites.SpriteComponent;
import com.game.utils.enums.Position;
import com.game.utils.objects.Wrapper;
import com.game.world.BodyComponent;
import com.game.world.BodyType;

import java.util.Map;
import java.util.function.Supplier;

import static com.badlogic.gdx.graphics.Color.*;
import static com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.*;
import static com.game.entities.bosses.BossEnum.TIMBER_WOMAN;
import static com.game.ViewVals.PPM;

public class TimberWoman extends Entity {

    public TimberWoman(GameContext2d gameContext, Vector2 spawn) {
        super(gameContext);
        addComponent(bodyComponent(spawn));
        addComponent(spriteComponent());
        addComponent(animationComponent(gameContext));
        addComponent(shapeComponent());
    }

    private ShapeComponent shapeComponent() {
        ShapeHandle shapeHandle = new ShapeHandle();
        shapeHandle.setShapeSupplier(() -> getComponent(BodyComponent.class).getCollisionBox());
        shapeHandle.setColorSupplier(() -> RED);
        shapeHandle.setShapeTypeSupplier(() -> Line);
        return new ShapeComponent(shapeHandle);
    }

    private AnimationComponent animationComponent(GameContext2d gameContext) {
        Supplier<String> keySupplier = () -> {
            return "Stand";
        };
        TextureAtlas textureAtlas = gameContext.getAsset(TextureAsset.TIMBER_WOMAN.getSrc(), TextureAtlas.class);
        Map<String, TimedAnimation> timedAnimations = TIMBER_WOMAN.getAnimations(textureAtlas);
        return new AnimationComponent(keySupplier, timedAnimations::get);
    }

    private SpriteComponent spriteComponent() {
        Sprite sprite = new Sprite();
        Vector2 size = TIMBER_WOMAN.getSpriteSize();
        sprite.setSize(size.x * PPM, size.y * PPM);
        return new SpriteComponent(sprite, new SpriteProcessor() {
            @Override
            public boolean setPositioning(Wrapper<Rectangle> bounds, Wrapper<Position> position) {
                bounds.setData(getComponent(BodyComponent.class).getCollisionBox());
                position.setData(Position.BOTTOM_CENTER);
                return true;
            }
        });
    }

    private BodyComponent bodyComponent(Vector2 spawn) {
        BodyComponent bodyComponent = new BodyComponent(BodyType.DYNAMIC);
        bodyComponent.setSize(1.2f * PPM, 2f * PPM);
        bodyComponent.setGravity(-PPM * .5f);
        bodyComponent.setPosition(spawn);
        return bodyComponent;
    }

}
