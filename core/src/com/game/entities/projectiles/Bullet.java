package com.game.entities.projectiles;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.game.core.Entity;
import com.game.core.GameContext2d;
import com.game.entities.decorations.Disintegration;
import com.game.entities.enemies.AbstractEnemy;
import com.game.sounds.SoundComponent;
import com.game.sprites.SpriteAdapter;
import com.game.sprites.SpriteComponent;
import com.game.utils.enums.Position;
import com.game.utils.objects.Wrapper;
import com.game.world.BodyComponent;
import com.game.world.Fixture;
import lombok.Getter;
import lombok.Setter;

import static com.game.core.constants.SoundAsset.*;
import static com.game.core.constants.TextureAsset.OBJECTS;
import static com.game.core.constants.ViewVals.PPM;
import static com.game.utils.enums.Position.*;
import static com.game.world.BodyType.*;
import static com.game.world.FixtureType.*;
import static com.game.world.FixtureType.BLOCK;
import static com.game.world.FixtureType.SHIELD;

@Getter
@Setter
public class Bullet extends AbstractProjectile {

    public Bullet(GameContext2d gameContext, Entity owner, Vector2 trajectory, Vector2 spawn) {
        super(gameContext, owner, .15f);
        addComponent(new SoundComponent());
        addComponent(defineSpriteComponent());
        addComponent(defineBodyComponent(spawn, trajectory));
    }

    public void disintegrate() {
        gameContext.addEntity(new Disintegration(gameContext, getComponent(BodyComponent.class).getCenter()));
        if (isInGameCamBounds()) {
            getComponent(SoundComponent.class).requestSound(THUMP_SOUND);
        }
        setDead(true);
    }

    @Override
    public void hit(Fixture fixture) {
        if (fixture.getEntity().equals(owner) || (owner instanceof AbstractEnemy &&
                fixture.getEntity() instanceof AbstractEnemy)) {
            return;
        }
        if (fixture.isAnyFixtureType(BLOCK, DAMAGEABLE)) {
            disintegrate();
        } else if (fixture.isFixtureType(SHIELD)) {
            setOwner(fixture.getEntity());
            Vector2 velocity = getComponent(BodyComponent.class).getVelocity();
            velocity.x *= -1f;
            String reflectDir = fixture.getUserData("reflectDir", String.class);
            if (reflectDir == null || reflectDir.equals("straight")) {
                velocity.y = 0f;
            } else {
                velocity.y = 5f * PPM * (reflectDir.equals("down") ? -1f : 1f);
            }
            getComponent(SoundComponent.class).requestSound(DINK_SOUND);
        }
    }

    private SpriteComponent defineSpriteComponent() {
        TextureRegion textureRegion = gameContext.getAsset(OBJECTS.getSrc(), TextureAtlas.class)
                .findRegion("YellowBullet");
        Sprite sprite = new Sprite();
        sprite.setRegion(textureRegion);
        sprite.setSize(PPM * 1.25f, PPM * 1.25f);
        return new SpriteComponent(sprite, new SpriteAdapter() {
            @Override
            public boolean setPositioning(Wrapper<Rectangle> bounds, Wrapper<Position> position) {
                bounds.setData(getComponent(BodyComponent.class).getCollisionBox());
                position.setData(CENTER);
                return true;
            }
        });
    }

    private BodyComponent defineBodyComponent(Vector2 spawn, Vector2 trajectory) {
        BodyComponent bodyComponent = new BodyComponent(DYNAMIC);
        bodyComponent.setVelocity(trajectory);
        bodyComponent.setSize(.1f * PPM, .1f * PPM);
        bodyComponent.setCenter(spawn.x, spawn.y);
        bodyComponent.setAffectedByResistance(false);
        // projectile
        Fixture projectile = new Fixture(this, new Rectangle(0f, 0f, .1f * PPM, .1f * PPM), HITTER_BOX);
        bodyComponent.addFixture(projectile);
        // force listener
        Fixture forceListener = new Fixture(this, new Rectangle(0f, 0f, .1f * PPM, .1f * PPM), FORCE_LISTENER);
        bodyComponent.addFixture(forceListener);
        // damager box
        Fixture damageBox = new Fixture(this, new Rectangle(0f, 0f, .2f * PPM, .2f * PPM), DAMAGER);
        bodyComponent.addFixture(damageBox);
        return bodyComponent;
    }

}
