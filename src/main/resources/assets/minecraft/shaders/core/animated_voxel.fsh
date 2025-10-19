#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float Time;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec4 normal;

out vec4 fragColor;

void main() {
    // Calculate frame from Time (64 frames, 2 ticks per frame)
    // Time is already in ticks (we set it as gameTime/20 in Java, so multiply back)
    int frame = int(mod((Time * 20.0) / 4.0, 64.0));
    
    // Each frame is 1/64th of the total texture height
    float frameHeight = 1.0 / 64.0;
    float vOffset = float(frame) * frameHeight;
    
    // Add slow horizontal scrolling (wraps at 1.0)
    float horizontalScroll = mod(Time * 0.02, 1.0);

    // Map model UVs (0-1 for a 16x16 face) to the current frame in the 16x1024 texture
    // Don't multiply texCoord0.y by frameHeight first - scale it within the frame offset
    vec2 animatedUV = vec2(mod(texCoord0.x + horizontalScroll, 1.0), vOffset + texCoord0.y * frameHeight);
    
    // Sample texture with animated UVs
    vec4 color = texture(Sampler0, animatedUV);
    
    if (color.a < 0.1) {
        discard;
    }

    // Apply vertex color + color modulator
    color *= vertexColor * ColorModulator;

    // Fog
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
