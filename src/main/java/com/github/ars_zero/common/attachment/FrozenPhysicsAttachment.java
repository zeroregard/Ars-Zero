package com.github.ars_zero.common.attachment;

public class FrozenPhysicsAttachment {
    private long freezeUntilTick;
    
    public FrozenPhysicsAttachment() {
        this.freezeUntilTick = 0;
    }
    
    public FrozenPhysicsAttachment(long freezeUntilTick) {
        this.freezeUntilTick = freezeUntilTick;
    }
    
    public boolean isFrozen(long currentTick) {
        return currentTick <= freezeUntilTick;
    }
    
    public void freeze(long currentTick) {
        this.freezeUntilTick = currentTick + 1;
    }
    
    public long getFreezeUntilTick() {
        return freezeUntilTick;
    }
    
    public void setFreezeUntilTick(long tick) {
        this.freezeUntilTick = tick;
    }
}















