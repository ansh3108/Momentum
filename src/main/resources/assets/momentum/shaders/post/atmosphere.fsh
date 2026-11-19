#version 330

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(InSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;

    // Isolate distant terrain using a power curve on the depth buffer
    float fogFactor = pow(depth, 24.0);

    // Deep dark atmospheric fog color
    vec3 fogColor = vec3(0.05, 0.07, 0.09);

    // Tactical color grade: desaturate and apply a subtle terminal green tint
    vec3 luma = vec3(dot(color.rgb, vec3(0.299, 0.587, 0.114)));
    vec3 graded = mix(color.rgb, luma, 0.3);
    graded *= vec3(0.9, 1.0, 0.95);

    // Blend the depth fog over the graded color
    vec3 finalColor = mix(graded, fogColor, fogFactor * 0.85);

    fragColor = vec4(finalColor, 1.0);
}