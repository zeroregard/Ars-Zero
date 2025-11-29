package com.github.ars_zero.common.item;

import net.minecraft.world.item.Item;

public class FinialItem extends Item {
    private final String finialType;
    
    public FinialItem(String finialType) {
        super(new Properties());
        this.finialType = finialType;
    }
    
    public String getFinialType() {
        return finialType;
    }
}
