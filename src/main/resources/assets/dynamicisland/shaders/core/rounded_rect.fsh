#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec2 localUV;
layout(location = 1) in vec4 shapeColor;
layout(location = 2) flat in float radiusStyle;
layout(location = 0) out vec4 fragColor;
void main() {
    vec2 size = 1.0 / max(vec2(length(dFdx(localUV)), length(dFdy(localUV))), vec2(0.00001));
    float radius = min(size.x, size.y) * abs(radiusStyle);
    vec2 q = abs((localUV - 0.5) * size) - size * 0.5 + radius;
    float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float aa = max(fwidth(d), 1.0);
    float coverage = 1.0 - smoothstep(-aa, aa, d);
    if (radiusStyle < 0.0) coverage *= 1.0 - smoothstep(0.4, 1.5, -d);
    if (coverage <= 0.0) discard;
    fragColor = vec4(shapeColor.rgb, shapeColor.a * coverage);
}
