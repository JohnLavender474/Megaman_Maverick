package com.game.debugging;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.game.core.Entity;
import com.game.core.System;
import com.game.core.MegaTextHandle;

import java.util.*;

public class DebugMessageSystem extends System {

    private final Camera camera;
    private final SpriteBatch spriteBatch;
    private final List<MegaTextHandle> debugMessageMegaTextHandles = new ArrayList<>();

    public DebugMessageSystem(Camera camera, SpriteBatch spriteBatch,
                              String fontSrc, int fontSize, Vector2... positions) {
        super(DebugMessageComponent.class);
        this.camera = camera;
        this.spriteBatch = spriteBatch;
        for (Vector2 position : positions) {
            debugMessageMegaTextHandles.add(new MegaTextHandle(fontSrc, fontSize, position));
        }
    }

    @Override
    protected void processEntity(Entity entity, float delta) {
        DebugMessageComponent debugMessageComponent = entity.getComponent(DebugMessageComponent.class);
        Iterator<Map.Entry<Integer, String>> msgIter = debugMessageComponent.getDebugMessages().entrySet().iterator();
        while (msgIter.hasNext()) {
            Map.Entry<Integer, String> msg = msgIter.next();
            msgIter.remove();
            if (msg.getKey() >= debugMessageMegaTextHandles.size()) {
                java.lang.System.err.println("ERROR: Debug Message System int key is out of bounds: " + msg.getKey());
                java.lang.System.err.println("CULPRIT: " + entity);
                continue;
            }
            debugMessageMegaTextHandles.get(msg.getKey()).setText(msg.getValue());
        }
        Queue<Integer> clearQueue = debugMessageComponent.getClearQueue();
        while (!clearQueue.isEmpty()) {
            int indexToClear = clearQueue.poll();
            if (indexToClear >= debugMessageMegaTextHandles.size()) {
                java.lang.System.err.println("ERROR: Debug Message System clear key is out of bounds: " + indexToClear);
                java.lang.System.err.println("CULPRIT: " + entity);
                continue;
            }
            debugMessageMegaTextHandles.get(indexToClear).clearText();
        }
    }

    @Override
    protected void postProcess(float delta) {
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        debugMessageMegaTextHandles.forEach(debugMessage -> debugMessage.draw(spriteBatch));
        spriteBatch.end();
    }

}
