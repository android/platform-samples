/**
 * This fragment shader performs tone mapping by converting HDR image data into SDR format
 * using a combination of transfer functions, Gainmap adjustments, exposure and gamma
 * correction factors, and epsilon values for numerical stability.
 */
precision mediump float;
varying vec2 vTextureCoord;

uniform highp float srcTF[7];
uniform highp float destTF[7];
uniform sampler2D base;
uniform sampler2D gainmap;
uniform mediump vec3 logRatioMin;
uniform mediump vec3 logRatioMax;
uniform mediump vec3 gainmapGamma;
uniform mediump vec3 epsilonSdr;
uniform mediump vec3 epsilonHdr;
uniform mediump float W;
uniform highp int gainmapIsAlpha;
uniform highp int singleChannel;
uniform highp int noGamma;

highp float fromSrc(highp float x) {
    highp float G = srcTF[0];
    highp float A = srcTF[1];
    highp float B = srcTF[2];
    highp float C = srcTF[3];
    highp float D = srcTF[4];
    highp float E = srcTF[5];
    highp float F = srcTF[6];
    highp float s = sign(x);
    x = abs(x);
    x = x < D ? C * x + F : pow(A * x + B, G) + E;
    return s * x;
}

highp float toDest(highp float x) {
    highp float G = destTF[0];
    highp float A = destTF[1];
    highp float B = destTF[2];
    highp float C = destTF[3];
    highp float D = destTF[4];
    highp float E = destTF[5];
    highp float F = destTF[6];
    highp float s = sign(x);
    x = abs(x);
    x = x < D ? C * x + F : pow(A * x + B, G) + E;
    return s * x;
}

highp vec4 sampleBase(vec2 coord) {
    vec4 color = texture2D(base, vTextureCoord);
    color = vec4(color.xyz / max(color.w, 0.0001), color.w);
    color.x = fromSrc(color.x);
    color.y = fromSrc(color.y);
    color.z = fromSrc(color.z);
    color.xyz *= color.w;
    return color;
}

void main() {
    vec4 S = sampleBase(vTextureCoord);
    vec4 G = texture2D(gainmap, vTextureCoord);
    vec3 H;

    if (gainmapIsAlpha == 1) {
        G = vec4(G.w, G.w, G.w, 1.0);
        mediump float L;

        if (noGamma == 1) {
            L = mix(logRatioMin.x, logRatioMax.x, G.x);
        } else {
            L = mix(logRatioMin.x, logRatioMax.x, pow(G.x, gainmapGamma.x));
        }

        H = (S.xyz + epsilonSdr) * exp(L * W) - epsilonHdr;
    } else {
        mediump vec3 L;
        if (noGamma == 1) {
            L = mix(logRatioMin, logRatioMax, G.xyz);
        } else {
            L = mix(logRatioMin, logRatioMax, pow(G.xyz, gainmapGamma));
        }

        H = (S.xyz + epsilonSdr) * exp(L * W) - epsilonHdr;
    }

    vec4 result = vec4(H.xyz / max(S.w, 0.0001), S.w);
    result.x = toDest(result.x);
    result.y = toDest(result.y);
    result.z = toDest(result.z);
    result.xyz *= result.w;

    gl_FragColor = result;
}