package com.momentum.physics;

public class SpringDamperSolver {
    private final float frequency;
    private final float damping;
    
    private float position = 0.0f;
    private float velocity = 0.0f;

    public SpringDamperSolver(float frequency, float damping) {
        this.frequency = frequency;
        this.damping = damping;
    }

    public void update(float target, float tickDelta) {
        // Convert physical frequency to angular frequency
        float angularFreq = frequency * 2.0f * (float) Math.PI;
        
        // Calculate spring tension (Hooke's Law) and friction damping
        float tension = (target - position) * (angularFreq * angularFreq);
        float damp = velocity * (2.0f * damping * angularFreq);
        
        // Update velocity and position using a fixed delta for stable client rendering
        float dt = 0.016f; // Approximate 60 FPS step
        velocity += (tension - damp) * dt;
        position += velocity * dt;
    }

    public float getPosition() {
        return position;
    }
}