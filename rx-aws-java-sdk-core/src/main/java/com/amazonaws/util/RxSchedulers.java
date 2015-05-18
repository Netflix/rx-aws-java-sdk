package com.amazonaws.util;

import rx.Scheduler;

public final class RxSchedulers {

    private final Scheduler computationScheduler;

    private static final RxSchedulers INSTANCE = new RxSchedulers();

    private RxSchedulers() {
        computationScheduler = new EventLoopsScheduler();
    }

    public static Scheduler computation() {
        return INSTANCE.computationScheduler;
    }
}
