#version 150

#moj_import <fog.glsl>
#moj_import <light.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float Time;
uniform float CompressionLevel;
uniform float EmissiveIntensity;
uniform float VoxelScale;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec2 texCoord2;
in vec4 normal;

out vec4 fragColor;

void main() {
    int frame = int(mod((Time * 20.0) / 4.0, 64.0));
    float frameHeight = 1.0 / 64.0;
    float vOffset = float(frame) * frameHeight;
    float horizontalScroll = mod(Time * 0.02, 1.0);
    
    vec2 scaledTexCoord = texCoord0 * (VoxelScale / 4.0);
    vec2 animatedUV = vec2(mod(scaledTexCoord.x + horizontalScroll, 1.0), vOffset + mod(scaledTexCoord.y, 1.0) * frameHeight);
    
    vec4 color = texture(Sampler0, animatedUV);
    
    if (color.a < 0.1) {
        discard;
    }

    color *= vertexColor;
    
    vec4 lightmapColor = texture(Sampler2, texCoord2);
    color *= lightmapColor;
    
    if (CompressionLevel > 0.25) {
        float normalizedCompression = clamp((CompressionLevel - 0.25) / 0.45, 0.0, 1.0);
        
        vec3 whiteColor = vec3(1.0, 1.0, 1.0);
        vec3 emissionBoost = whiteColor * (EmissiveIntensity * normalizedCompression * 2.0);
        
        color.rgb = mix(color.rgb, whiteColor + emissionBoost, normalizedCompression);
        color.a = min(1.0, color.a + normalizedCompression * (1.0 - color.a));
    }

    color *= ColorModulator;

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
