package com.game.entities.projectiles;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.game.core.Entity;
import com.game.core.GameContext2d;
import com.game.animations.AnimationComponent;
import com.game.animations.TimedAnimation;
import com.game.damage.Damageable;
import com.game.damage.Damager;
import com.game.entities.contracts.Faceable;
import com.game.entities.contracts.Facing;
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
import static com.game.core.constants.TextureAsset.MEGAMAN_CHARGED_SHOT;
import static com.game.core.constants.ViewVals.PPM;
import static com.game.entities.contracts.Facing.F_LEFT;
import static com.game.world.BodyType.*;
import static com.game.world.FixtureType.*;

@Getter
@Setter
public class ChargedShot extends AbstractProjectile implements Faceable {

    private final Vector2 trajectory = new Vector2();

    private Facing facing;

    public ChargedShot(GameContext2d gameContext, Entity owner, Vector2 trajectory, Vector2 spawn, Facing facing) {
        super(gameContext, owner, .15f);
        this.trajectory.set(trajectory);
        setFacing(facing);
        addComponent(defineAnimationComponent());
        addComponent(defineBodyComponent(spawn));
        addComponent(defineSpriteComponent());
    }

    @Override
    public boolean canDamage(Damageable damageable) {
        return !owner.equals(damageable) && !(owner instanceof Damager && damageable instanceof Damager);
    }

    @Override
    public void onDamageInflictedTo(Damageable damageable) {
        setDead(true);
        gameContext.addEntity(new ChargedShotDisintegration(
                gameContext, getComponent(BodyComponent.class).getCenter(), isFacing(F_LEFT)));
    }

    @Override
    public void hit(Fixture fixture) {
        if (fixture.getEntity().equals(owner) ||
                (owner instanceof Damager && fixture.getEntity() instanceof Damager)) {
            return;
        }
        if (fixture.isFixtureType(BLOCK)) {
            setDead(true);
            gameContext.addEntity(new ChargedShotDisintegration(
                    gameContext, getComponent(BodyComponent.class).getCenter(), isFacing(F_LEFT)));
        } else if (fixture.isFixtureType(SHIELD)) {
            setOwner(fixture.getEntity());
            swapFacing();
            trajectory.x *= -1f;
            String reflectDir = fixture.getUserData("reflectDir", String.class);
            if (reflectDir == null || reflectDir.equals("straight")) {
                trajectory.y = 0f;
            } else {
                trajectory.y = 5f * PPM * (reflectDir.equals("down") ? -1f : 1f);
            }
            getComponent(SoundComponent.class).requestSound(DINK_SOUND);
        }
    }

    private AnimationComponent defineAnimationComponent() {
        TextureAtlas textureAtlas = gameContext.getAsset(
                MEGAMAN_CHARGED_SHOT.getSrc(), TextureAtlas.class);
        return new AnimationComponent(new TimedAnimation(textureAtlas.findRegion("MegamanChargedShot"), 2, .05f));
    }

    private SpriteComponent defineSpriteComponent() {
        Sprite sprite = new Sprite();
        sprite.setSize(PPM * 1.75f, PPM * 1.75f);
        return new SpriteComponent(sprite, new SpriteAdapter() {

            @Override
            public boolean setPositioning(Wrapper<Rectangle> bounds, Wrapper<Position> position) {
                bounds.setData(getComponent(BodyComponent.class).getCollisionBox());
                position.setData(Position.CENTER);
                return true;
            }

            @Override
            public boolean isFlipX() {
                return isFacing(F_LEFT);
            }

        });
    }

    private BodyComponent defineBodyComponent(Vector2 spawn) {
        BodyComponent bodyComponent = new BodyComponent(DYNAMIC);
        bodyComponent.setPreProcess(delta -> bodyComponent.setVelocity(trajectory));
        bodyComponent.setSize(PPM, PPM);
        bodyComponent.setCenter(spawn.x, spawn.y);
        // model
        Rectangle model = new Rectangle(0f, 0f, PPM, PPM);
        Fixture projectile = new Fixture(this, new Rectangle(model), HITTER_BOX);
        bodyComponent.addFixture(projectile);
        Fixture damageBox = new Fixture(this, new Rectangle(model), DAMAGER);
        bodyComponent.addFixture(damageBox);
        return bodyComponent;
    }

}
