/**
 * This vertex shader program transforms vertices from object space to clip space and
 * prepares texture coordinates for fragment shader processing in a fragment shader.
 *
 * refer to fs_uhdr_tonemapper.frag
 */
uniform mat4 uMVPMatrix;
attribute vec4 aPosition;
attribute vec2 aTextureCoord;
varying vec2 vTextureCoord;

void main() {
    gl_Position = uMVPMatrix * aPosition;
    vTextureCoord = aTextureCoord;
}