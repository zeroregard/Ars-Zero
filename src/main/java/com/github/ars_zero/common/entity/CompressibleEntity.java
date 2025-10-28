package com.github.ars_zero.common.entity;

public interface CompressibleEntity {
    
    void setCompressionLevel(float compressionLevel);
    float getCompressionLevel();
    
    void setEmissiveIntensity(float intensity);
    float getEmissiveIntensity();
    
    void setDamageEnabled(boolean enabled);
    boolean isDamageEnabled();
    
    int getCompressedColor();
}


