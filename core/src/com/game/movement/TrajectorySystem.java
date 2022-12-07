package com.game.movement;

import com.game.entities.Entity;
import com.game.System;
import com.game.world.BodyComponent;

public class TrajectorySystem extends System {

    public TrajectorySystem() {
        super(TrajectoryComponent.class, BodyComponent.class);
    }

    @Override
    protected void processEntity(Entity entity, float delta) {
        TrajectoryComponent trajectoryComponent = entity.getComponent(TrajectoryComponent.class);
        BodyComponent bodyComponent = entity.getComponent(BodyComponent.class);
        bodyComponent.setVelocity(trajectoryComponent.getVelocity());
        trajectoryComponent.update(delta);
        if (trajectoryComponent.isFinished()) {
            trajectoryComponent.reset();
        }
    }

}
