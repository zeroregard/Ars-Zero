package com.github.ars_zero.client.jade;

import com.github.ars_zero.common.entity.AbstractBlightedSkeleton;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ArsZeroJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(
                EntityManaProvider.INSTANCE, AbstractBlightedSkeleton.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(
                EntityManaProvider.Client.INSTANCE, AbstractBlightedSkeleton.class);
    }
}
