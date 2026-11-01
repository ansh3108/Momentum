package com.momentum.physics;

public class SpringDamperSolver {
    private float position;
    private float velocity;
    private final float omega0;
    private final float zeta;

    public SpringDamperSolver(float angularFrequency, float dampingRatio) {
        this.omega0 = angularFrequency;
        this.zeta = dampingRatio;
    }

    public void update(float target, float tickDelta) {
        float dt = Math.min(tickDelta, 0.1f);
        float f = 1.0f + 2.0f * dt * zeta * omega0;
        float oo = omega0 * omega0;
        float hoo = dt * oo;
        float hhoo = dt * hoo;
        float detInv = 1.0f / (f + hhoo);
        float detX = f * position + dt * velocity + hhoo * target;
        float detV = velocity + hoo * (target - position);
        
        this.position = detX * detInv;
        this.velocity = detV * detInv;
    }

    public float getPosition() {
        return position;
    }
}