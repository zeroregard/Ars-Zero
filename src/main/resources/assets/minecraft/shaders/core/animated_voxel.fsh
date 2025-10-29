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
uniform float VoxelScale;
uniform float CompressionLevel;
uniform float EmissiveIntensity;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec2 texCoord2;
in vec4 normal;
in vec4 overlayColor;
in vec3 morphedNormal;

out vec4 fragColor;

void main() {
    float animationSpeed = max(0.0, 1.0 - CompressionLevel);
    
    int frame = int(mod((Time * 20.0 * animationSpeed) / 4.0, 64.0));
    float frameHeight = 1.0 / 64.0;
    float vOffset = float(frame) * frameHeight;
    float horizontalScroll = mod(Time * 0.02 * animationSpeed, 1.0);
    
    vec2 scaledTexCoord = texCoord0 * (VoxelScale / 4.0);
    vec2 animatedUV = vec2(mod(scaledTexCoord.x + horizontalScroll, 1.0), vOffset + mod(scaledTexCoord.y, 1.0) * frameHeight);
    
    vec4 texColor = texture(Sampler0, animatedUV);
    
    if (texColor.a < 0.1) {
        discard;
    }
    
    vec4 lightmapColor = texture(Sampler2, texCoord2);
    
    float normalizedCompression = 0.0;
    if (CompressionLevel > 0.25) {
        normalizedCompression = clamp((CompressionLevel - 0.25) / 0.75, 0.0, 1.0);
    }
    
    vec4 color;
    
    vec3 tintedTexture = texColor.rgb * vertexColor.rgb;
    
    if (normalizedCompression > 0.0) {
        vec3 litTexture = tintedTexture * lightmapColor.rgb;
        vec3 whiteEmissive = vec3(1.0, 1.0, 1.0) * (1.0 + EmissiveIntensity * normalizedCompression * 2.0);
        
        color.rgb = mix(litTexture, whiteEmissive, normalizedCompression);
        color.a = texColor.a * vertexColor.a;
    } else {
        color.rgb = tintedTexture * lightmapColor.rgb;
        color.a = texColor.a * vertexColor.a;
    }

    color *= ColorModulator;

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
