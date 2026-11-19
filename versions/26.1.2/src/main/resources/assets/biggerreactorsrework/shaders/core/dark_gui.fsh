#version 330
#extension GL_ARB_separate_shader_objects : require

// Modified 2026-11-01: match the native 26.1.2 uniform layout shared by the vertex shader.
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

// Dark look of the machine screens: gray pixels keep only 0.32 of their brightness, colored pixels keep their
// color, with a smooth step between the two so tinted parts never lose their hue.
void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a == 0.0) {
        discard;
    }
    float chroma = max(color.r, max(color.g, color.b)) - min(color.r, min(color.g, color.b));
    color.rgb *= mix(0.32, 1.0, smoothstep(0.12, 0.35, chroma));
    fragColor = color * ColorModulator;
}
