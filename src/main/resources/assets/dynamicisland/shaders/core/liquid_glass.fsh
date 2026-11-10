#version 330
#extension GL_ARB_separate_shader_objects : require
// Adapted from Jacquesqwq/LiquidGlassShader, liquidGlassV2Tinted.frag.
// Copyright (c) 2026 Jack. MIT; see licenses/LiquidGlassShader-MIT.txt.
// Port: GLSL 330 explicit IO, independent scene snapshot, local 9-tap diffusion,
// pixel-space rounded rectangle, bounded refraction and alpha-controlled composition.
uniform sampler2D Sampler0;
layout(location = 0) in vec2 localUV;
layout(location = 1) in vec4 material;
layout(location = 2) in vec2 screenUV;
layout(location = 0) out vec4 fragColor;
const float M_E = 2.718281828459045;
float f(float x) { return 1.0 - 2.3 * pow(5.2 * M_E, -6.9 * x - 0.7); }
vec3 sampleScene(vec2 uv) { return texture(Sampler0, clamp(uv, vec2(0.001), vec2(0.999))).rgb; }
void main() {
    vec2 size = 1.0 / max(vec2(length(dFdx(localUV)), length(dFdy(localUV))), vec2(0.00001));
    vec2 p = (localUV - 0.5) * size;
    float radius = clamp(material.r * min(size.x, size.y), 1.0, min(size.x, size.y) * 0.5);
    vec2 q = abs(p) - size * 0.5 + radius;
    float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float aa = max(fwidth(d), 1.0);
    float coverage = 1.0 - smoothstep(-aa, aa, d);
    if (coverage <= 0.0) discard;
    float dist = clamp(-d / max(radius, 1.0), 0.0, 1.0);
    float fresnel = pow(1.0 - dist, 3.0);
    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 normal = normalize(p / max(size, vec2(1.0)) + vec2(0.00001));
    // Keep the upstream refraction curve but limit its displacement to a local edge band.
    float refractCurve = pow(max(f(dist), 0.001), 0.35);
    vec2 sampleUV = screenUV + normal * (1.0 - refractCurve) * 8.0 * texel;
    vec2 blur = texel * 2.6;
    vec3 color = sampleScene(sampleUV) * 0.25;
    color += (sampleScene(sampleUV + vec2(blur.x, 0.0)) + sampleScene(sampleUV - vec2(blur.x, 0.0))
           + sampleScene(sampleUV + vec2(0.0, blur.y)) + sampleScene(sampleUV - vec2(0.0, blur.y))) * 0.125;
    color += (sampleScene(sampleUV + blur) + sampleScene(sampleUV - blur)
           + sampleScene(sampleUV + vec2(blur.x, -blur.y)) + sampleScene(sampleUV + vec2(-blur.x, blur.y))) * 0.0625;
    vec2 chromaOffset = normal * texel * fresnel * 1.4;
    color.r = mix(color.r, sampleScene(sampleUV + chromaOffset).r, 0.4);
    color.b = mix(color.b, sampleScene(sampleUV - chromaOffset).b, 0.4);
    // Upstream low-frequency diffusion, adaptive tint, saturation and Fresnel material shaping.
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(vec3(luma), color, mix(0.45, 0.75, luma));
    vec3 tint = vec3(0.075, 0.095, 0.15);
    color = mix(color, tint, 0.33 * (1.0 - luma * 0.5));
    color += vec3(0.68, 0.79, 1.0) * fresnel * mix(0.12, 0.06, luma);
    color *= 0.96 + smoothstep(0.0, 1.0, dist) * 0.04;
    float rim = 1.0 - smoothstep(0.5, 1.7, abs(d));
    color += vec3(0.65, 0.8, 1.0) * rim * (0.12 + 0.15 * (1.0 - localUV.y));
    fragColor = vec4(clamp(color, 0.0, 1.0), material.a * coverage);
}
