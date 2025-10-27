package com.github.ars_zero.common.util;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class MathHelper {
    
    public static List<Vec3> getCirclePositions(Vec3 center, Vec3 lookDirection, double radius, int count) {
        if (count <= 0) {
            return List.of();
        }
        
        if (count == 1) {
            return List.of(center);
        }
        
        Vec3 lookDir = lookDirection.normalize();
        
        Vec3 right;
        if (Math.abs(lookDir.y) < 0.99) {
            Vec3 worldUp = new Vec3(0, 1, 0);
            right = lookDir.cross(worldUp).normalize();
        } else {
            Vec3 worldForward = new Vec3(0, 0, 1);
            right = lookDir.cross(worldForward).normalize();
        }
        Vec3 up = right.cross(lookDir).normalize();
        
        List<Vec3> positions = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            double angle = (2.0 * Math.PI * i) / count;
            double offsetRight = Math.cos(angle) * radius;
            double offsetUp = Math.sin(angle) * radius;
            
            Vec3 offset = right.scale(offsetRight).add(up.scale(offsetUp));
            positions.add(center.add(offset));
        }
        
        return positions;
    }
}

