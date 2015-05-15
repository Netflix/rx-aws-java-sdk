package com.amazonaws.util;

import rx.Scheduler;

public final class RxSchedulers {

    private final Scheduler compuationScheduler;

    private static final RxSchedulers INSTANCE = new RxSchedulers();

    private RxSchedulers() {
        compuationScheduler = new EventLoopsScheduler();
    }

    public static Scheduler computation() {
        return INSTANCE.compuationScheduler;
    }
}
