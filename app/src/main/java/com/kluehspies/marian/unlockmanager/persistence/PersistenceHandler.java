package com.kluehspies.marian.unlockmanager.persistence;

import com.kluehspies.marian.unlockmanager.listener.RewardListener;
import com.kluehspies.marian.unlockmanager.trigger.Trigger;

/**
 * Created by Marian on 14.10.2015.
 */
public abstract class PersistenceHandler<M> extends Trigger<M> implements RewardListener<M> {

    public PersistenceHandler(Class clazz) {
        super(clazz);
    }

    public PersistenceHandler(Class clazz, M... items) {
        super(clazz, items);
    }
}
