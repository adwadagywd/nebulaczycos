package pl.nebula.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Small immediate-mode helper. All coordinates passed in are RELATIVE to the camera.
 * Lines are drawn as camera-facing ribbons, so thickness works on every GPU/driver
 * (GL line width is not reliable in core profile) and stays constant in screen pixels.
 */
public final class Gfx {
    public final Camera camera;
    public final Vec3d cam;
    public final Matrix4f pos;
    public final float tickDelta;
    /** world units per pixel per unit of distance */
    public final float pxFactor;
    /** point exactly under the crosshair (screen centre), camera-relative */
    public final Vec3d crosshair;

    private BufferBuilder buf;
    private int count;

    private Gfx(Camera camera, Matrix4f pos, float tickDelta, float pxFactor, Vec3d crosshair) {
        this.camera = camera;
        this.cam = camera.getPos();
        this.pos = pos;
        this.tickDelta = tickDelta;
        this.pxFactor = pxFactor;
        this.crosshair = crosshair;
    }

    public static Gfx create(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = ctx.camera();

        MatrixStack ms = ctx.matrixStack();
        Matrix4f pos = ms != null
                ? new Matrix4f(ms.peek().getPositionMatrix())
                : new Matrix4f().rotation(camera.getRotation().conjugate(new Quaternionf()));

        Matrix4f proj = new Matrix4f(RenderSystem.getProjectionMatrix());
        float h = Math.max(1, mc.getWindow().getFramebufferHeight());
        float pxFactor = 2f / (h * Math.max(0.0001f, Math.abs(proj.m11())));

        // Unproject the screen centre (NDC 0,0) -> view space -> camera-relative world space.
        // Goes through the real projection + view matrices, so view-bobbing/hurt-tilt are handled.
        Vector4f v = new Vector4f(0, 0, 0, 1);
        proj.invert(new Matrix4f()).transform(v);
        if (v.w != 0f) v.div(v.w);
        Vector3f view = new Vector3f(v.x, v.y, v.z);
        if (view.lengthSquared() < 1e-8f) view.set(0, 0, -1);
        view.normalize();
        if (view.z > 0) view.negate();
        view.mul(0.5f);
        pos.invert(new Matrix4f()).transformPosition(view);

        float td = mc.getRenderTickCounter().getTickDelta(true);
        return new Gfx(camera, pos, td, pxFactor, new Vec3d(view.x, view.y, view.z));
    }

    // ------------------------------------------------------------ GL state
    public void state(boolean on) {
        if (on) {
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
        } else {
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    // ------------------------------------------------------------ batching
    public void begin() {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        count = 0;
    }

    public void end() {
        if (buf != null && count > 0) BufferRenderer.drawWithGlobalProgram(buf.end());
        buf = null;
        count = 0;
    }

    private void vert(float x, float y, float z, int argb) {
        buf.vertex(pos, x, y, z).color(argb);
        count++;
    }

    private void vert(Vec3d v, int argb) {
        vert((float) v.x, (float) v.y, (float) v.z, argb);
    }

    // ------------------------------------------------------------ primitives
    /** Thick line; px = thickness in framebuffer pixels. */
    public void ribbon(Vec3d a, Vec3d b, float px, int argb) {
        Vector3f A = new Vector3f((float) a.x, (float) a.y, (float) a.z);
        Vector3f B = new Vector3f((float) b.x, (float) b.y, (float) b.z);
        Vector3f dir = B.sub(A, new Vector3f());
        Vector3f mid = A.add(B, new Vector3f()).mul(0.5f);
        Vector3f n = dir.cross(mid, new Vector3f());
        float len = n.length();
        if (len < 1e-6f) return;
        n.div(len);

        float wa = px * 0.5f * pxFactor * Math.max(A.length(), 0.05f);
        float wb = px * 0.5f * pxFactor * Math.max(B.length(), 0.05f);

        vert(A.x + n.x * wa, A.y + n.y * wa, A.z + n.z * wa, argb);
        vert(A.x - n.x * wa, A.y - n.y * wa, A.z - n.z * wa, argb);
        vert(B.x - n.x * wb, B.y - n.y * wb, B.z - n.z * wb, argb);
        vert(B.x + n.x * wb, B.y + n.y * wb, B.z + n.z * wb, argb);
    }

    private static Vec3d[] corners(Box b) {
        return new Vec3d[]{
                new Vec3d(b.minX, b.minY, b.minZ), new Vec3d(b.maxX, b.minY, b.minZ),
                new Vec3d(b.maxX, b.minY, b.maxZ), new Vec3d(b.minX, b.minY, b.maxZ),
                new Vec3d(b.minX, b.maxY, b.minZ), new Vec3d(b.maxX, b.maxY, b.minZ),
                new Vec3d(b.maxX, b.maxY, b.maxZ), new Vec3d(b.minX, b.maxY, b.maxZ)
        };
    }

    private static final int[][] EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private static final int[][] FACES = {
            {0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {1, 2, 6, 5}, {2, 3, 7, 6}, {3, 0, 4, 7}
    };

    public void box(Box b, float px, int argb) {
        Vec3d[] c = corners(b);
        for (int[] e : EDGES) ribbon(c[e[0]], c[e[1]], px, argb);
    }

    public void filledBox(Box b, int argb) {
        Vec3d[] c = corners(b);
        for (int[] f : FACES) {
            vert(c[f[0]], argb);
            vert(c[f[1]], argb);
            vert(c[f[2]], argb);
            vert(c[f[3]], argb);
        }
    }

    /** Rectangle that always faces the camera (2D-style frame around an entity). */
    public void frame(Vec3d centerRel, double w, double h, float px, int argb) {
        Vec3d right = new Vec3d(0, 1, 0).crossProduct(centerRel);
        if (right.lengthSquared() < 1e-6) return;
        right = right.normalize().multiply(w / 2.0);
        Vec3d up = new Vec3d(0, h / 2.0, 0);

        Vec3d p1 = centerRel.add(right).add(up);
        Vec3d p2 = centerRel.subtract(right).add(up);
        Vec3d p3 = centerRel.subtract(right).subtract(up);
        Vec3d p4 = centerRel.add(right).subtract(up);
        ribbon(p1, p2, px, argb);
        ribbon(p2, p3, px, argb);
        ribbon(p3, p4, px, argb);
        ribbon(p4, p1, px, argb);
    }
}
