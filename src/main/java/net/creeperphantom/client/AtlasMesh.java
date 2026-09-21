package net.creeperphantom.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

final class AtlasMesh {
    static void box(PoseStack pose, VertexConsumer out, float x0, float y0, float z0,
                            float x1, float y1, float z1, float[] uv, float[] front,
                            int light, int overlay, float r, float g, float b, float a) {
        quad(pose, out, new float[]{x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0}, front, 0,0,-1, light,overlay,r,g,b,a);
        quad(pose, out, new float[]{x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1}, uv, 0,0,1, light,overlay,r,g,b,a);
        quad(pose, out, new float[]{x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0}, uv, -1,0,0, light,overlay,r,g,b,a);
        quad(pose, out, new float[]{x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1}, uv, 1,0,0, light,overlay,r,g,b,a);
        quad(pose, out, new float[]{x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1}, uv, 0,-1,0, light,overlay,r,g,b,a);
        quad(pose, out, new float[]{x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0}, uv, 0,1,0, light,overlay,r,g,b,a);
    }

    static void quad(PoseStack pose, VertexConsumer out, float[] xyz, float[] uv,
                             float nx, float ny, float nz, int light, int overlay,
                             float r, float g, float b, float a) {
        var transform = pose.last();
        // Keep UV samples just inside their quadrant to avoid edge bleeding.
        float inset = 0.0005F;
        float[] us = {uv[0]+inset, uv[2]-inset, uv[2]-inset, uv[0]+inset};
        float[] vs = {uv[1]+inset, uv[1]+inset, uv[3]-inset, uv[3]-inset};
        for (int i = 0; i < 4; i++) {
            out.vertex(transform.pose(), xyz[i*3], xyz[i*3+1], xyz[i*3+2])
                    .color(r,g,b,a).uv(us[i],vs[i]).overlayCoords(overlay).uv2(light)
                    .normal(transform.normal(),nx,ny,nz).endVertex();
        }
    }
    private AtlasMesh() {}
}
