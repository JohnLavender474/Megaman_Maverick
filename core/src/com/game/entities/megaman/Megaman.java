package com.game.entities.megaman;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.game.GameContext2d;
import com.game.animations.AnimationComponent;
import com.game.animations.TimedAnimation;
import com.game.behaviors.Behavior;
import com.game.behaviors.BehaviorComponent;
import com.game.controllers.ControllerAdapter;
import com.game.controllers.ControllerComponent;
import com.game.damage.DamageNegotiation;
import com.game.damage.Damageable;
import com.game.damage.Damager;
import com.game.entities.Entity;
import com.game.entities.contracts.Faceable;
import com.game.entities.contracts.Facing;
import com.game.entities.decorations.ExplosionOrb;
import com.game.entities.enemies.*;
import com.game.entities.hazards.LaserBeamer;
import com.game.entities.projectiles.Bullet;
import com.game.entities.projectiles.ChargedShot;
import com.game.entities.projectiles.Fireball;
import com.game.health.HealthComponent;
import com.game.levels.CameraFocusable;
import com.game.messages.Message;
import com.game.sounds.SoundComponent;
import com.game.sprites.SpriteComponent;
import com.game.sprites.SpriteProcessor;
import com.game.updatables.UpdatableComponent;
import com.game.utils.DebugLogger;
import com.game.utils.enums.Position;
import com.game.utils.objects.TimeMarkedRunnable;
import com.game.utils.objects.Timer;
import com.game.utils.objects.Wrapper;
import com.game.weapons.WeaponDef;
import com.game.world.BodyComponent;
import com.game.world.Fixture;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.Supplier;

import static com.game.GlobalKeys.CHARGE_STATUS;
import static com.game.GlobalKeys.COLLECTION;
import static com.game.ViewVals.PPM;
import static com.game.assets.SoundAsset.*;
import static com.game.assets.TextureAsset.MEGAMAN;
import static com.game.assets.TextureAsset.MEGAMAN_FIRE;
import static com.game.behaviors.BehaviorType.*;
import static com.game.controllers.ControllerButton.*;
import static com.game.entities.contracts.Facing.F_LEFT;
import static com.game.entities.contracts.Facing.F_RIGHT;
import static com.game.entities.megaman.AButtonTask.*;
import static com.game.entities.megaman.MegamanSpecialAbility.*;
import static com.game.entities.megaman.MegamanVals.MEGAMAN_STATS;
import static com.game.entities.megaman.MegamanWeapon.MEGA_BUSTER;
import static com.game.entities.special.AbstractBounds.ABSTRACT_BOUNDS;
import static com.game.health.HealthVals.MAX_HEALTH;
import static com.game.levels.LevelStatus.PAUSED;
import static com.game.messages.MessageType.PLAYER_DEAD;
import static com.game.utils.UtilMethods.bottomCenterPoint;
import static com.game.world.BodySense.*;
import static com.game.world.BodyType.DYNAMIC;
import static com.game.world.FixtureType.*;

/**
 * Megaman, dah bloo bombah!
 */
@Getter
@Setter
public class Megaman extends Entity implements Damageable, Faceable, CameraFocusable {

    static {
        DebugLogger.getInstance().putDebugClass(Megaman.class, DebugLogger.DebugLevel.NONE);
    }

    private static final float CLAMP_X = 20f;
    private static final float CLAMP_Y = 35f;

    private static final float RUN_SPEED = 4f;
    private static final float WATER_RUN_SPEED = 2f;

    private static final float JUMP_VEL = 17.5f;
    private static final float WATER_JUMP_VEL = 25f;

    private static final float WALL_JUMP_VEL = 30f;
    private static final float WALL_JUMP_HORIZ = 12f;
    private static final float WALL_JUMP_IMPETUS_TIME = .2f;

    private static final float AIR_DASH_VEL = 12f;
    private static final float AIR_DASH_END_BUMP = 3f;
    private static final float WATER_AIR_DASH_VEL = 6f;
    private static final float WATER_AIR_DASH_END_BUMP = 2f;
    private static final float MAX_AIR_DASH_TIME = .25f;

    private static final float GROUND_SLIDE_VEL = 12f;
    private static final float WATER_GROUND_SLIDE_VEL = 6f;
    private static final float MAX_GROUND_SLIDE_TIME = .35f;

    private static final float GROUNDED_GRAVITY = -.125f;
    private static final float UNGROUNDED_GRAVITY = -.5f;
    private static final float WATER_UNGROUNDED_GRAVITY = -.25f;

    private static final float SHOOT_ANIM_TIME = .5f;

    private static final float DAMAGE_DURATION = .75f;
    private static final float DAMAGE_RECOVERY_TIME = 1.5f;
    private static final float DAMAGE_RECOVERY_FLASH_DURATION = .05f;

    private static final float TIME_TO_HALFWAY_CHARGED = .5f;
    private static final float TIME_TO_FULLY_CHARGED = 1.25f;

    private static final float EXPLOSION_ORB_SPEED = 3.5f;

    private static final Map<Class<? extends Damager>, DamageNegotiation> damageNegotiations = new HashMap<>() {{
        put(Bat.class, new DamageNegotiation(5));
        put(Met.class, new DamageNegotiation(5));
        put(MagFly.class, new DamageNegotiation(5));
        put(Bullet.class, new DamageNegotiation(10));
        put(ChargedShot.class, new DamageNegotiation(15));
        put(Fireball.class, new DamageNegotiation(5));
        put(Dragonfly.class, new DamageNegotiation(5));
        put(Matasaburo.class, new DamageNegotiation(5));
        put(SniperJoe.class, new DamageNegotiation(10));
        put(SpringHead.class, new DamageNegotiation(5));
        put(FloatingCan.class, new DamageNegotiation(10));
        put(LaserBeamer.class, new DamageNegotiation(10));
        put(SuctionRoller.class, new DamageNegotiation(10));
        put(GapingFish.class, new DamageNegotiation(5));
    }};

    private static MegamanWeaponDefs megamanWeaponDefs;

    private final MegamanStats megamanStats;

    private final Timer damageTimer = new Timer(DAMAGE_DURATION, true);
    private final Timer airDashTimer = new Timer(MAX_AIR_DASH_TIME);
    private final Timer shootAnimationTimer = new Timer(SHOOT_ANIM_TIME, true);
    private final Timer groundSlideTimer = new Timer(MAX_GROUND_SLIDE_TIME);
    private final Timer damageRecoveryTimer = new Timer(DAMAGE_RECOVERY_TIME, true);
    private final Timer wallJumpImpetusTimer = new Timer(WALL_JUMP_IMPETUS_TIME, true);
    private final Timer damageRecoveryBlinkTimer = new Timer(DAMAGE_RECOVERY_FLASH_DURATION);
    private final Timer chargingTimer = new Timer(TIME_TO_FULLY_CHARGED, new TimeMarkedRunnable(TIME_TO_HALFWAY_CHARGED,
            () -> getComponent(SoundComponent.class).requestSound(MEGA_BUSTER_CHARGING_SOUND, true)));

    private boolean recoveryBlink;
    private Facing facing = F_RIGHT;
    private MegamanWeapon currentWeapon;
    private AButtonTask aButtonTask = _JUMP;

    public Megaman(GameContext2d gameContext, Vector2 spawn) {
        super(gameContext);
        megamanStats = gameContext.getBlackboardObject(MEGAMAN_STATS, MegamanStats.class);
        megamanStats.setWeaponSetter(weapon -> currentWeapon = weapon);
        if (megamanWeaponDefs == null) {
            megamanWeaponDefs = new MegamanWeaponDefs(gameContext);
        }
        megamanWeaponDefs.setMegaman(this);
        setCurrentWeapon(MEGA_BUSTER);
        addComponent(healthComponent(MAX_HEALTH));
        addComponent(updatableComponent());
        addComponent(bodyComponent(spawn));
        addComponent(behaviorComponent());
        addComponent(spriteComponent());
        addComponent(animationComponent());
        addComponent(new SoundComponent());
        addComponent(controllerComponent());
        // shape component
        /*
        BodyComponent bodyComponent = getComponent(BodyComponent.class);
        ShapeComponent shapeComponent = new ShapeComponent();
        ShapeHandle shapeHandle = new ShapeHandle();
        shapeHandle.setShapeSupplier(bodyComponent::getCollisionBox);
        shapeHandle.setColorSupplier(() -> RED);
        shapeComponent.addShapeHandle(shapeHandle);
        bodyComponent.getFixturesOfType(WATER_LISTENER).forEach(waterListener -> {
            ShapeHandle waterListenerHandle = new ShapeHandle();
            waterListenerHandle.setShapeSupplier(waterListener::getFixtureShape);
            waterListenerHandle.setColorSupplier(() -> PURPLE);
            shapeComponent.addShapeHandle(waterListenerHandle);
        });
        addComponent(shapeComponent);
         */
        // set timers to end
        /*
        damageTimer.setToEnd();
        shootAnimationTimer.setToEnd();
        damageRecoveryTimer.setToEnd();
        wallJumpImpetusTimer.setToEnd();
         */
    }

    @Override
    public void onDeath() {
        super.onDeath();
        megamanStats.setWeaponSetter(null);
    }

    @Override
    public Set<Class<? extends Damager>> getDamagerMaskSet() {
        return damageNegotiations.keySet();
    }

    @Override
    public void takeDamageFrom(Damager damager) {
        DamageNegotiation damageNegotiation = damageNegotiations.get(damager.getClass());
        damageTimer.reset();
        damageNegotiation.runOnDamage();
        getComponent(HealthComponent.class).sub(damageNegotiation.getDamage(damager));
        getComponent(SoundComponent.class).requestSound(MEGAMAN_DAMAGE_SOUND);
    }

    @Override
    public Vector2 getFocus() {
        return bottomCenterPoint(getComponent(BodyComponent.class).getCollisionBox());
    }

    @Override
    public boolean isInvincible() {
        return !damageTimer.isFinished() || !damageRecoveryTimer.isFinished();
    }

    /**
     * Is damaged.
     *
     * @return if damaged
     */
    public boolean isDamaged() {
        return !damageTimer.isFinished();
    }

    /**
     * Is shooting.
     *
     * @return if shooting
     */
    public boolean isShooting() {
        return !shootAnimationTimer.isFinished();
    }

    /**
     * Return if charging.
     *
     * @return if charging
     */
    public boolean isCharging() {
        return megamanStats.isWeaponsChargeable() && chargingTimer.getTime() >= TIME_TO_HALFWAY_CHARGED;
    }

    /**
     * Return if charging fully.
     *
     * @return if charging fully
     */
    public boolean isChargingFully() {
        return megamanStats.isWeaponsChargeable() && chargingTimer.isFinished();
    }

    /**
     * Get weapon def.
     *
     * @param megamanWeapon the weapon
     * @return the weapon def
     */
    public WeaponDef getWeaponDef(MegamanWeapon megamanWeapon) {
        return megamanWeaponDefs.get(megamanWeapon);
    }

    private void stopCharging() {
        chargingTimer.reset();
        getComponent(SoundComponent.class).stopLoopingSound(MEGA_BUSTER_CHARGING_SOUND);
    }

    private boolean shoot() {
        WeaponDef weaponDef = getWeaponDef(currentWeapon);
        if (weaponDef == null || isDamaged() || megamanStats.getWeaponAmmo(currentWeapon) <= 0 ||
                getComponent(BehaviorComponent.class).is(GROUND_SLIDING, AIR_DASHING) ||
                !weaponDef.isCooldownTimerFinished()) {
            return false;
        }
        int chargeStatus = 0;
        if (isChargingFully()) {
            chargeStatus = 2;
        } else if (isCharging()) {
            chargeStatus = 1;
        }
        Wrapper<Integer> nextBestChargeStatus = Wrapper.of(-1);
        boolean canShoot = megamanStats.canUseWeapon(currentWeapon, chargeStatus, nextBestChargeStatus);
        if (!canShoot) {
            if (nextBestChargeStatus.getData() == -1) {
                return false;
            }
            chargeStatus = nextBestChargeStatus.getData();
        }
        Map<String, Object> m = new HashMap<>();
        m.put(CHARGE_STATUS, chargeStatus);
        weaponDef.getWeaponsInstances(m).forEach(gameContext::addEntity);
        weaponDef.resetCooldownTimer();
        shootAnimationTimer.reset();
        weaponDef.runOnShoot(m);
        return true;
    }

    private HealthComponent healthComponent(int maxHealth) {
        return new HealthComponent(maxHealth, () -> {
            List<Vector2> trajectories = new ArrayList<>() {{
                add(new Vector2(-EXPLOSION_ORB_SPEED, 0f));
                add(new Vector2(-EXPLOSION_ORB_SPEED, EXPLOSION_ORB_SPEED));
                add(new Vector2(0f, EXPLOSION_ORB_SPEED));
                add(new Vector2(EXPLOSION_ORB_SPEED, EXPLOSION_ORB_SPEED));
                add(new Vector2(EXPLOSION_ORB_SPEED, 0f));
                add(new Vector2(EXPLOSION_ORB_SPEED, -EXPLOSION_ORB_SPEED));
                add(new Vector2(0f, -EXPLOSION_ORB_SPEED));
                add(new Vector2(-EXPLOSION_ORB_SPEED, -EXPLOSION_ORB_SPEED));
            }};
            trajectories.forEach(trajectory -> gameContext.addEntity(new ExplosionOrb(
                    gameContext, getComponent(BodyComponent.class).getCenter(), trajectory)));
            gameContext.sendMessage(new Message(PLAYER_DEAD));
        });
    }

    private UpdatableComponent updatableComponent() {
        return new UpdatableComponent(delta -> {
            // charging timer
            if (!megamanStats.isWeaponsChargeable()) {
                chargingTimer.reset();
            }
            // damage timer
            damageTimer.update(delta);
            if (isDamaged()) {
                chargingTimer.reset();
                getComponent(SoundComponent.class).stopLoopingSound(MEGA_BUSTER_CHARGING_SOUND);
                getComponent(BodyComponent.class).translateVelocity((isFacing(F_LEFT) ? .15f : -.15f) * PPM, 0f);
            }
            if (damageTimer.isJustFinished()) {
                damageRecoveryTimer.reset();
            }
            if (damageTimer.isFinished() && !damageRecoveryTimer.isFinished()) {
                damageRecoveryTimer.update(delta);
                damageRecoveryBlinkTimer.update(delta);
                if (damageRecoveryBlinkTimer.isFinished()) {
                    recoveryBlink = !recoveryBlink;
                    damageRecoveryBlinkTimer.reset();
                }
            }
            if (damageRecoveryTimer.isJustFinished()) {
                setRecoveryBlink(false);
            }
            // anim and wall jump impetus timers
            shootAnimationTimer.update(delta);
            wallJumpImpetusTimer.update(delta);
            // update weapon cool down timer
            megamanWeaponDefs.get(currentWeapon).updateCooldownTimer(delta);
        });
    }

    private ControllerComponent controllerComponent() {
        ControllerComponent controllerComponent = new ControllerComponent();
        BodyComponent bodyComponent = getComponent(BodyComponent.class);
        BehaviorComponent behaviorComponent = getComponent(BehaviorComponent.class);
        // left dpad
        controllerComponent.addControllerAdapter(DPAD_LEFT, new ControllerAdapter() {

            @Override
            public void onPressContinued(float delta) {
                if (isDamaged() || gameContext.isControllerButtonPressed(DPAD_RIGHT)) {
                    return;
                }
                setFacing(behaviorComponent.is(WALL_SLIDING) ? F_RIGHT : F_LEFT);
                behaviorComponent.set(RUNNING, !behaviorComponent.is(WALL_SLIDING));
                float threshold = -RUN_SPEED * PPM * (bodyComponent.is(IN_WATER) ? .65f : 1f);
                if (bodyComponent.getVelocity().x > threshold) {
                    bodyComponent.translateVelocity(-PPM * 50f * delta, 0f);
                }
            }

            @Override
            public void onJustReleased() {
                if (!gameContext.isControllerButtonPressed(DPAD_RIGHT)) {
                    getComponent(BehaviorComponent.class).setIsNot(RUNNING);
                }
            }

            @Override
            public void onReleaseContinued() {
                if (!gameContext.isControllerButtonPressed(DPAD_RIGHT)) {
                    getComponent(BehaviorComponent.class).setIsNot(RUNNING);
                }
            }

        });
        // right dpad
        controllerComponent.addControllerAdapter(DPAD_RIGHT, new ControllerAdapter() {

            @Override
            public void onPressContinued(float delta) {
                if (isDamaged() || gameContext.isControllerButtonPressed(DPAD_LEFT)) {
                    return;
                }
                setFacing(behaviorComponent.is(WALL_SLIDING) ? F_LEFT : F_RIGHT);
                behaviorComponent.set(RUNNING, !behaviorComponent.is(WALL_SLIDING));
                float threshold = RUN_SPEED * PPM * (bodyComponent.is(IN_WATER) ? .65f : 1f);
                if (bodyComponent.getVelocity().x < threshold) {
                    bodyComponent.translateVelocity(PPM * 50f * delta, 0f);
                }
            }

            @Override
            public void onJustReleased() {
                if (!gameContext.isControllerButtonPressed(DPAD_LEFT)) {
                    getComponent(BehaviorComponent.class).setIsNot(RUNNING);
                }
            }

            @Override
            public void onReleaseContinued() {
                if (!gameContext.isControllerButtonPressed(DPAD_LEFT)) {
                    getComponent(BehaviorComponent.class).setIsNot(RUNNING);
                }
            }

        });
        // x
        controllerComponent.addControllerAdapter(X, new ControllerAdapter() {

            @Override
            public void onPressContinued(float delta) {
                if (!megamanStats.isWeaponsChargeable()) {
                    return;
                }
                chargingTimer.update(delta);
                if (isDamaged()) {
                    stopCharging();
                }
            }

            @Override
            public void onJustReleased() {
                if (!shoot()) {
                    // TODO: play error sound
                }
                stopCharging();
            }

        });
        return controllerComponent;
    }

    private BehaviorComponent behaviorComponent() {
        BehaviorComponent behaviorComponent = new BehaviorComponent();
        BodyComponent bodyComponent = getComponent(BodyComponent.class);
        // wall slide
        Behavior wallSlide = new Behavior() {

            @Override
            protected boolean evaluate(float delta) {
                if (isDamaged() || !megamanStats.canUseSpecialAbility(WALL_JUMP)) {
                    return false;
                }
                return wallJumpImpetusTimer.isFinished() && !bodyComponent.is(FEET_ON_GROUND) &&
                        ((bodyComponent.is(TOUCHING_WALL_SLIDE_LEFT) &&
                                gameContext.isControllerButtonPressed(DPAD_LEFT)) ||
                                (bodyComponent.is(TOUCHING_WALL_SLIDE_RIGHT) &&
                                        gameContext.isControllerButtonPressed(DPAD_RIGHT)));
            }

            @Override
            protected void init() {
                behaviorComponent.setIs(WALL_SLIDING);
                setAButtonTask(bodyComponent.is(IN_WATER) ? _SWIM : _JUMP);
            }

            @Override
            protected void act(float delta) {
                bodyComponent.applyResistanceY(1.25f);
            }

            @Override
            protected void end() {
                behaviorComponent.setIsNot(WALL_SLIDING);
                if (!bodyComponent.is(IN_WATER)) {
                    setAButtonTask(_AIR_DASH);
                }
            }

        };
        behaviorComponent.addBehavior(wallSlide);
        // swim
        Behavior swim = new Behavior() {

            @Override
            protected boolean evaluate(float delta) {
                if (isDamaged() || !bodyComponent.is(IN_WATER)) {
                    return false;
                }
                if (behaviorComponent.is(SWIMMING)) {
                    return bodyComponent.getVelocity().y > 0f;
                }
                return gameContext.isControllerButtonJustPressed(A) && aButtonTask == _SWIM;
            }

            @Override
            protected void init() {
                float x = 0f;
                float y = 18f * PPM;
                if (behaviorComponent.is(WALL_SLIDING)) {
                    x = WALL_JUMP_HORIZ * PPM * 1.15f;
                    if (isFacing(F_LEFT)) {
                        x *= -1f;
                    }
                    y *= 2f;
                }
                bodyComponent.translateVelocity(x, y);
                behaviorComponent.setIs(SWIMMING);
                Sound swimSound = gameContext.getAsset(SWIM_SOUND.getSrc(), Sound.class);
                gameContext.playSound(swimSound);
            }

            @Override
            protected void act(float delta) {}

            @Override
            protected void end() {
                behaviorComponent.setIsNot(SWIMMING);
            }

        };
        behaviorComponent.addBehavior(swim);
        // jump
        Behavior jump = new Behavior() {

            @Override
            protected boolean evaluate(float delta) {
                if (isDamaged() || gameContext.isControllerButtonPressed(DPAD_DOWN) ||
                        bodyComponent.is(HEAD_TOUCHING_BLOCK) || behaviorComponent.is(SWIMMING)) {
                    return false;
                }
                return behaviorComponent.is(JUMPING) ?
                        // case 1
                        bodyComponent.getVelocity().y >= 0f && gameContext.isControllerButtonPressed(A) :
                        // case 2
                        aButtonTask == _JUMP && gameContext.isControllerButtonJustPressed(A) &&
                                (bodyComponent.is(FEET_ON_GROUND) || behaviorComponent.is(WALL_SLIDING));
            }

            @Override
            protected void init() {
                behaviorComponent.setIs(JUMPING);
                boolean wallSliding = behaviorComponent.is(WALL_SLIDING);
                Vector2 vel = new Vector2();
                // float x = wallSliding ? WALL_JUMP_HORIZ : 0f;
                float x;
                if (wallSliding) {
                    x = WALL_JUMP_HORIZ * PPM;
                    if (isFacing(F_LEFT)) {
                        x *= -1f;
                    }
                } else {
                    x = bodyComponent.getVelocity().x;
                }
                float y = PPM * (wallSliding ? WALL_JUMP_VEL : JUMP_VEL);
                // vel.set(isFacing(F_LEFT)? -x : x, y).scl(PPM);
                vel.set(x, y);
                /*
                if (bodyComponent.is(IN_WATER)) {
                    vel.scl(.75f, .9f);
                }
                 */
                bodyComponent.setVelocity(vel);
                if (wallSliding) {
                    wallJumpImpetusTimer.reset();
                }
                /*
                if (behaviorComponent.is(WALL_SLIDING)) {
                    bodyComponent.translateVelocity((isFacing(F_LEFT) ? -1f : 1f) * WALL_JUMP_HORIZ * PPM,
                            WALL_JUMP_VEL * PPM);
                    wallJumpImpetusTimer.reset();
                } else {
                    bodyComponent.setVelocityY(JUMP_VEL * PPM * (bodyComponent.is(IN_WATER) ? 1.25f : 1f));
                }
                 */
            }

            @Override
            protected void act(float delta) {
            }

            @Override
            protected void end() {
                behaviorComponent.setIsNot(JUMPING);
                getComponent(BodyComponent.class).setVelocityY(0f);
            }

        };
        behaviorComponent.addBehavior(jump);
        // air dash
        Behavior airDash = new Behavior() {

            @Override
            protected boolean evaluate(float delta) {
                if (isDamaged() || !megamanStats.canUseSpecialAbility(AIR_DASH) || behaviorComponent.is(WALL_SLIDING) ||
                        bodyComponent.is(FEET_ON_GROUND) || airDashTimer.isFinished()) {
                    return false;
                }
                return behaviorComponent.is(AIR_DASHING) ? gameContext.isControllerButtonPressed(A) :
                        gameContext.isControllerButtonJustPressed(A) && getAButtonTask() == _AIR_DASH;
            }

            @Override
            protected void init() {
                getComponent(SoundComponent.class).requestSound(WHOOSH_SOUND);
                bodyComponent.setGravityOn(false);
                setAButtonTask(_JUMP);
                behaviorComponent.setIs(AIR_DASHING);
            }

            @Override
            protected void act(float delta) {
                airDashTimer.update(delta);
                bodyComponent.setVelocityY(0f);
                if ((isFacing(F_LEFT) && bodyComponent.is(TOUCHING_BLOCK_LEFT)) ||
                        (isFacing(F_RIGHT) && bodyComponent.is(TOUCHING_BLOCK_RIGHT))) {
                    return;
                }
                float x = AIR_DASH_VEL * PPM;
                if (bodyComponent.is(IN_WATER)) {
                    x /= 2f;
                }
                if (isFacing(F_LEFT)) {
                    x *= -1f;
                }
                bodyComponent.setVelocityX(x);
            }

            @Override
            protected void end() {
                airDashTimer.reset();
                bodyComponent.setGravityOn(true);
                behaviorComponent.setIsNot(AIR_DASHING);
                bodyComponent.translateVelocity((isFacing(F_LEFT) ? -AIR_DASH_END_BUMP : AIR_DASH_END_BUMP) * PPM, 0f);
            }
        };
        behaviorComponent.addBehavior(airDash);
        // ground slide
        Behavior groundSlide = new Behavior() {

            @Override
            protected boolean evaluate(float delta) {
                if (!megamanStats.canUseSpecialAbility(GROUND_SLIDE)) {
                    return false;
                }
                if (behaviorComponent.is(GROUND_SLIDING) && bodyComponent.is(HEAD_TOUCHING_BLOCK)) {
                    return true;
                }
                if (isDamaged() || !bodyComponent.is(FEET_ON_GROUND) || groundSlideTimer.isFinished()) {
                    return false;
                }
                if (!behaviorComponent.is(GROUND_SLIDING)) {
                    return gameContext.isControllerButtonPressed(DPAD_DOWN) &&
                            gameContext.isControllerButtonJustPressed(A);
                } else {
                    return gameContext.isControllerButtonPressed(DPAD_DOWN) &&
                            gameContext.isControllerButtonPressed(A);
                }
            }

            @Override
            protected void init() {
                behaviorComponent.setIs(GROUND_SLIDING);
            }

            @Override
            protected void act(float delta) {
                groundSlideTimer.update(delta);
                if (isDamaged() || (isFacing(F_LEFT) && bodyComponent.is(TOUCHING_BLOCK_LEFT)) ||
                        (isFacing(F_RIGHT) && bodyComponent.is(TOUCHING_BLOCK_RIGHT))) {
                    return;
                }
                float x = GROUND_SLIDE_VEL * PPM * (bodyComponent.is(IN_WATER) ? .5f : 1f);
                if (isFacing(F_LEFT)) {
                    x *= -1f;
                }
                bodyComponent.setVelocityX(x);
            }

            @Override
            protected void end() {
                groundSlideTimer.reset();
                behaviorComponent.setIsNot(GROUND_SLIDING);
                float endDash = (bodyComponent.is(IN_WATER) ? 2f : 5f) * PPM;
                if (isFacing(F_LEFT)) {
                    endDash *= -1;
                }
                bodyComponent.translateVelocity(endDash, 0f);
            }
        };
        behaviorComponent.addBehavior(groundSlide);
        return behaviorComponent;
    }

    private BodyComponent bodyComponent(Vector2 spawn) {
        BodyComponent bodyComponent = new BodyComponent(DYNAMIC);
        bodyComponent.setClamp(CLAMP_X * PPM, CLAMP_Y * PPM);
        bodyComponent.maskForCustomCollisions(ABSTRACT_BOUNDS);
        bodyComponent.setPosition(spawn);
        bodyComponent.setWidth(.8f * PPM);
        // model 1
        Rectangle model1 = new Rectangle(0f, 0f, .575f * PPM, PPM / 16f);
        // standard on bounce
        Runnable onBounce = () -> {
            if (!bodyComponent.is(IN_WATER)) {
                setAButtonTask(_AIR_DASH);
            }
        };
        // feet and bounceable
        Fixture feet = new Fixture(this, new Rectangle(model1), FEET);
        bodyComponent.addFixture(feet);
        Fixture feetBounceable = new Fixture(this, new Rectangle(model1), BOUNCEABLE);
        bodyComponent.addFixture(feetBounceable);
        feetBounceable.putUserData("onBounce", onBounce);
        // head
        Fixture head = new Fixture(this, new Rectangle(model1), HEAD);
        head.setOffset(0f, PPM / 2f);
        bodyComponent.addFixture(head);
        Fixture headBounceable = new Fixture(this, new Rectangle(model1), BOUNCEABLE);
        headBounceable.setOffset(0f, PPM / 2f);
        headBounceable.putUserData("onBounce", onBounce);
        bodyComponent.addFixture(headBounceable);
        // model 2
        Rectangle model2 = new Rectangle(0f, 0f, PPM / 16f, PPM / 16f);
        // left
        Fixture left = new Fixture(this, new Rectangle(model2), LEFT);
        left.setOffset(-.4f * PPM, .15f * PPM);
        bodyComponent.addFixture(left);
        Fixture leftBounceable = new Fixture(this, new Rectangle(model2), BOUNCEABLE);
        leftBounceable.setOffset(-.25f * PPM, .15f * PPM);
        leftBounceable.putUserData("onBounce", onBounce);
        bodyComponent.addFixture(leftBounceable);
        // right
        Fixture right = new Fixture(this, new Rectangle(model2), RIGHT);
        right.setOffset(.4f * PPM, .15f * PPM);
        bodyComponent.addFixture(right);
        Fixture rightBounceable = new Fixture(this, new Rectangle(model2), BOUNCEABLE);
        rightBounceable.setOffset(.25f * PPM, .15f * PPM);
        rightBounceable.putUserData("onBounce", onBounce);
        bodyComponent.addFixture(rightBounceable);
        // gate listeners
        Fixture leftGateListener = new Fixture(this, new Rectangle(0f, 0f, .2f * PPM, .2f * PPM), GATE_LISTENER);
        leftGateListener.setOffset(-.55f * PPM, PPM / 2f);
        bodyComponent.addFixture(leftGateListener);
        Fixture rightGateListener = new Fixture(this, new Rectangle(0f, 0f, .2f * PPM, .2f * PPM), GATE_LISTENER);
        rightGateListener.setOffset(.55f * PPM, PPM / 2f);
        bodyComponent.addFixture(rightGateListener);
        // hitbox
        Fixture hitBox = new Fixture(this, new Rectangle(0f, 0f, .8f * PPM, .75f * PPM), DAMAGEABLE);
        bodyComponent.addFixture(hitBox);
        // force listener
        Fixture forceListener = new Fixture(this, bodyComponent.getCollisionBox(), FORCE_LISTENER);
        forceListener.putUserData(COLLECTION, new HashSet<>() {{
            add(MagFly.class);
            add(Matasaburo.class);
        }});
        bodyComponent.addFixture(forceListener);
        // water listener
        /*
        Rectangle waterListenerModel = new Rectangle(0f, 0f, .625f * PPM, PPM / 5f);
        Fixture upperWaterListener = new Fixture(this, new Rectangle(waterListenerModel), WATER_LISTENER);
        upperWaterListener.setOffset(0f, PPM / 2.5f);
        bodyComponent.addFixture(upperWaterListener);
        Fixture lowerWaterListener = new Fixture(this, new Rectangle(waterListenerModel), WATER_LISTENER);
        lowerWaterListener.setOffset(0f, -PPM / 3f);
        bodyComponent.addFixture(lowerWaterListener);
         */
        Fixture waterListener = new Fixture(this, new Rectangle(0f, 0f, PPM, PPM / 2f), WATER_LISTENER);
        bodyComponent.addFixture(waterListener);
        // pre-process
        bodyComponent.setPreProcess(delta -> {
            BehaviorComponent behaviorComponent = getComponent(BehaviorComponent.class);
            if (behaviorComponent.is(GROUND_SLIDING)) {
                bodyComponent.setHeight(.45f * PPM);
                feet.setOffset(0f, -PPM / 4f);
                feetBounceable.setOffset(0f, -PPM / 5f);
                ((Rectangle) right.getFixtureShape()).setHeight(.15f * PPM);
                ((Rectangle) left.getFixtureShape()).setHeight(.15f * PPM);
            } else {
                bodyComponent.setHeight(.95f * PPM);
                feet.setOffset(0f, -PPM / 2f);
                feetBounceable.setOffset(0f, -PPM / 4f);
                ((Rectangle) right.getFixtureShape()).setHeight(.35f * PPM);
                ((Rectangle) left.getFixtureShape()).setHeight(.35f * PPM);
            }
            if (bodyComponent.getVelocity().y < 0f && !bodyComponent.is(FEET_ON_GROUND)) {
                bodyComponent.setGravity(UNGROUNDED_GRAVITY * PPM * (bodyComponent.is(IN_WATER) ? .5f : 1f));
            } else {
                bodyComponent.setGravity(GROUNDED_GRAVITY * PPM * (bodyComponent.is(IN_WATER) ? 1.75f : 1f));
            }
        });
        return bodyComponent;
    }

    private SpriteComponent spriteComponent() {
        Sprite sprite = new Sprite();
        sprite.setSize(1.65f * PPM, 1.35f * PPM);
        return new SpriteComponent(sprite, new SpriteProcessor() {

            @Override
            public int getSpriteRenderPriority() {
                return 3;
            }

            @Override
            public boolean setPositioning(Wrapper<Rectangle> bounds, Wrapper<Position> position) {
                bounds.setData(getComponent(BodyComponent.class).getCollisionBox());
                position.setData(Position.BOTTOM_CENTER);
                return true;
            }

            @Override
            public float getAlpha() {
                return recoveryBlink ? 0f : 1f;
            }

            @Override
            public boolean isFlipX() {
                return getComponent(BehaviorComponent.class).is(WALL_SLIDING) ? isFacing(F_RIGHT) : isFacing(F_LEFT);
            }

            @Override
            public float getOffsetY() {
                return getComponent(BehaviorComponent.class).is(GROUND_SLIDING) ? .1f * -PPM : 0f;
            }

        });
    }

    private AnimationComponent animationComponent() {
        Supplier<String> keySupplier = () -> {
            if (gameContext.isLevelStatus(PAUSED)) {
                return null;
            }
            BodyComponent bodyComponent = getComponent(BodyComponent.class);
            BehaviorComponent behaviorComponent = getComponent(BehaviorComponent.class);
            if (isDamaged()) {
                return behaviorComponent.is(GROUND_SLIDING) ? "LayDownDamaged" : "Damaged";
            } else if (behaviorComponent.is(AIR_DASHING)) {
                if (isChargingFully()) {
                    return "AirDashCharging";
                } else if (isCharging()) {
                    return "AirDashHalfCharging";
                } else {
                    return "AirDash";
                }
            } else if (behaviorComponent.is(GROUND_SLIDING)) {
                if (isChargingFully()) {
                    return "GroundSlideCharging";
                } else if (isCharging()) {
                    return "GroundSlideHalfCharging";
                } else {
                    return "GroundSlide";
                }
            } else if (behaviorComponent.is(WALL_SLIDING)) {
                if (isShooting()) {
                    return "WallSlideShoot";
                } else if (isChargingFully()) {
                    return "WallSlideCharging";
                } else if (isCharging()) {
                    return "WallSlideHalfCharging";
                } else {
                    return "WallSlide";
                }
            } else if (behaviorComponent.is(SWIMMING)) {
                if (isShooting()) {
                    return "SwimShoot";
                } else if (isChargingFully()) {
                    return "SwimCharging";
                } else if (isCharging()) {
                    return "SwimHalfCharging";
                } else {
                    return "Swim";
                }
            } else if (behaviorComponent.is(JUMPING) || !bodyComponent.is(FEET_ON_GROUND)) {
                if (isShooting()) {
                    return "JumpShoot";
                } else if (isChargingFully()) {
                    return "JumpCharging";
                } else if (isCharging()) {
                    return "JumpHalfCharging";
                } else {
                    return "Jump";
                }
            } else if (bodyComponent.is(FEET_ON_GROUND) && behaviorComponent.is(RUNNING)) {
                if (isShooting()) {
                    return "RunShoot";
                } else if (isChargingFully()) {
                    return "RunCharging";
                } else if (isCharging()) {
                    return "RunHalfCharging";
                } else {
                    return "Run";
                }
            } else if (behaviorComponent.is(CLIMBING)) {
                if (isShooting()) {
                    return "ClimbShoot";
                } else if (isChargingFully()) {
                    return "ClimbCharging";
                } else if (isCharging()) {
                    return "ClimbHalfCharging";
                } else {
                    return "Climb";
                }
            } else if (bodyComponent.is(FEET_ON_GROUND) && Math.abs(bodyComponent.getVelocity().x) > 3f) {
                if (isShooting()) {
                    return "SlipSlideShoot";
                } else if (isChargingFully()) {
                    return "SlipSlideCharging";
                } else if (isCharging()) {
                    return "SlipSlideHalfCharging";
                } else {
                    return "SlipSlide";
                }
            } else {
                if (isShooting()) {
                    return "StandShoot";
                } else if (isChargingFully()) {
                    return "StandCharging";
                } else if (isCharging()) {
                    return "StandHalfCharging";
                } else {
                    return "Stand";
                }
            }
        };
        Map<MegamanWeapon, Map<String, TimedAnimation>> weaponToAnimMap = new EnumMap<>(MegamanWeapon.class);
        final float chargingAnimTime = .125f;
        for (MegamanWeapon megamanWeapon : MegamanWeapon.values()) {

            // TODO: Temporary, do not include any but mega buster

            if (megamanWeapon != MEGA_BUSTER) {
                continue;
            }

            String textureAtlasKey;
            switch (megamanWeapon) {
                case MEGA_BUSTER -> textureAtlasKey = MEGAMAN.getSrc();
                case FLAME_TOSS -> textureAtlasKey = MEGAMAN_FIRE.getSrc();
                default -> throw new IllegalStateException();
            }
            TextureAtlas textureAtlas = gameContext.getAsset(textureAtlasKey, TextureAtlas.class);
            Map<String, TimedAnimation> animations = new HashMap<>();
            // climb
            animations.put("Climb", new TimedAnimation(textureAtlas.findRegion("Climb"), 2, .125f));
            animations.put("ClimbShoot", new TimedAnimation(textureAtlas.findRegion("ClimbShoot")));
            animations.put("ClimbHalfCharging", new TimedAnimation(
                    textureAtlas.findRegion("ClimbHalfCharging"), 2, chargingAnimTime));
            animations.put("ClimbCharging", new TimedAnimation(
                    textureAtlas.findRegion("ClimbCharging"), 2, chargingAnimTime));
            // stand
            animations.put("Stand", new TimedAnimation(textureAtlas.findRegion("Stand"), new float[]{1.5f, .15f}));
            animations.put("StandCharging", new TimedAnimation(
                    textureAtlas.findRegion("StandCharging"), 2, chargingAnimTime));
            animations.put("StandHalfCharging", new TimedAnimation(
                    textureAtlas.findRegion("StandHalfCharging"), 2, chargingAnimTime));
            animations.put("StandShoot", new TimedAnimation(textureAtlas.findRegion("StandShoot")));
            // damaged
            animations.put("Damaged", new TimedAnimation(textureAtlas.findRegion("Damaged"), 3, .05f));
            animations.put("LayDownDamaged", new TimedAnimation(textureAtlas.findRegion("LayDownDamaged"), 3, .05f));
            // run
            animations.put("Run", new TimedAnimation(textureAtlas.findRegion("Run"), 4, .125f));
            animations.put("RunCharging", new TimedAnimation(textureAtlas
                    .findRegion("RunCharging"), 4, chargingAnimTime));
            animations.put("RunHalfCharging", new TimedAnimation(
                    textureAtlas.findRegion("RunHalfCharging"), 4, chargingAnimTime));
            animations.put("RunShoot", new TimedAnimation(textureAtlas.findRegion("RunShoot"), 4, .125f));
            // jump
            animations.put("Jump", new TimedAnimation(textureAtlas.findRegion("Jump")));
            animations.put("JumpCharging", new TimedAnimation(
                    textureAtlas.findRegion("JumpCharging"), 2, chargingAnimTime));
            animations.put("JumpHalfCharging", new TimedAnimation(
                    textureAtlas.findRegion("JumpHalfCharging"), 2, chargingAnimTime));
            animations.put("JumpShoot", new TimedAnimation(textureAtlas.findRegion("JumpShoot")));
            // swim
            animations.put("Swim", new TimedAnimation(textureAtlas.findRegion("Swim")));
            animations.put("SwimAttack", new TimedAnimation(textureAtlas.findRegion("SwimAttack")));
            animations.put("SwimCharging", new TimedAnimation(
                    textureAtlas.findRegion("SwimCharging"), 2, chargingAnimTime));
            animations.put("SwimHalfCharging", new TimedAnimation(
                    textureAtlas.findRegion("SwimHalfCharging"), 2, chargingAnimTime));
            animations.put("SwimShoot", new TimedAnimation(textureAtlas.findRegion("SwimShoot")));
            // wall slide
            animations.put("WallSlide", new TimedAnimation(textureAtlas.findRegion("WallSlide")));
            animations.put("WallSlideCharging", new TimedAnimation(
                    textureAtlas.findRegion("WallSlideCharging"), 2, chargingAnimTime));
            animations.put("WallSlideHalfCharging", new TimedAnimation(
                    textureAtlas.findRegion("WallSlideHalfCharging"), 2, chargingAnimTime));
            animations.put("WallSlideShoot", new TimedAnimation(textureAtlas.findRegion("WallSlideShoot")));
            // ground slide
            animations.put("GroundSlide", new TimedAnimation(textureAtlas.findRegion("GroundSlide")));
            animations.put("GroundSlideCharging", new TimedAnimation(
                    textureAtlas.findRegion("GroundSlideCharging"), 2, chargingAnimTime));
            animations.put("GroundSlideHalfCharging", new TimedAnimation(
                    textureAtlas.findRegion("GroundSlideHalfCharging"), 2, chargingAnimTime));
            // air dash
            animations.put("AirDash", new TimedAnimation(textureAtlas.findRegion("AirDash")));
            animations.put("AirDashCharging", new TimedAnimation(
                    textureAtlas.findRegion("AirDashCharging"), 2, chargingAnimTime));
            animations.put("AirDashHalfCharging", new TimedAnimation(
                    textureAtlas.findRegion("AirDashHalfCharging"), 2, chargingAnimTime));
            // slip slide
            animations.put("SlipSlide", new TimedAnimation(textureAtlas.findRegion("SlipSlide")));
            animations.put("SlipSlideCharging", new TimedAnimation(
                    textureAtlas.findRegion("SlipSlideCharging"), 2, chargingAnimTime));
            animations.put("SlipSlideHalfCharging", new TimedAnimation(
                    textureAtlas.findRegion("SlipSlideHalfCharging"), 2, chargingAnimTime));
            animations.put("SlipSlideShoot", new TimedAnimation(textureAtlas.findRegion("SlipSlideShoot")));
            weaponToAnimMap.put(megamanWeapon, animations);
        }
        return new AnimationComponent(keySupplier, key -> weaponToAnimMap.get(currentWeapon).get(key));
    }

}
