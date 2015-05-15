package com.amazonaws.util;

import java.util.concurrent.*;

import rx.*;
import rx.functions.Action0;
import rx.internal.util.*;
import rx.subscriptions.*;
import rx.internal.schedulers.*;

public class EventLoopsScheduler extends Scheduler {
    private static final String THREAD_NAME_PREFIX = "RxAmazonAWSThreadPool-";
    private static final RxThreadFactory THREAD_FACTORY = new RxThreadFactory(THREAD_NAME_PREFIX);
    static final String KEY_MAX_THREADS = "rx.scheduler.max-computation-threads";
    static final int MAX_THREADS;

    static {
        int maxThreads = Integer.getInteger(KEY_MAX_THREADS, 0);
        int ncpu = Runtime.getRuntime().availableProcessors();
        int max;
        if (maxThreads <= 0 || maxThreads > ncpu) {
            max = ncpu;
        } else {
            max = maxThreads;
        }
        MAX_THREADS = max;
    }
    static final class FixedSchedulerPool {
        final int cores;

        final PoolWorker[] eventLoops;
        long n;

        FixedSchedulerPool() {
            this.cores = MAX_THREADS;
            this.eventLoops = new PoolWorker[cores];
            for (int i = 0; i < cores; i++) {
                this.eventLoops[i] = new PoolWorker(THREAD_FACTORY);
            }
        }

        public PoolWorker getEventLoop() {
            return eventLoops[(int)(n++ % cores)];
        }
    }

    final FixedSchedulerPool pool;
    
    public EventLoopsScheduler() {
        pool = new FixedSchedulerPool();
    }
    
    @Override
    public Worker createWorker() {
        return new EventLoopWorker(pool.getEventLoop());
    }
    
    public Subscription scheduleDirect(Action0 action) {
       PoolWorker pw = pool.getEventLoop();
       return pw.scheduleActual(action, -1, TimeUnit.NANOSECONDS);
    }

    private static class EventLoopWorker extends Scheduler.Worker {
        private final SubscriptionList serial = new SubscriptionList();
        private final CompositeSubscription timed = new CompositeSubscription();
        private final SubscriptionList both = new SubscriptionList(serial, timed);
        private final PoolWorker poolWorker;

        EventLoopWorker(PoolWorker poolWorker) {
            this.poolWorker = poolWorker;
            
        }

        @Override
        public void unsubscribe() {
            both.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return both.isUnsubscribed();
        }

        @Override
        public Subscription schedule(Action0 action) {
            if (isUnsubscribed()) {
                return Subscriptions.unsubscribed();
            }
            ScheduledAction s = poolWorker.scheduleActual(action, 0, null, serial);
            
            return s;
        }
        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            if (isUnsubscribed()) {
                return Subscriptions.unsubscribed();
            }
            ScheduledAction s = poolWorker.scheduleActual(action, delayTime, unit, timed);
            
            return s;
        }
    }
    
    private static final class PoolWorker extends NewThreadWorker {
        PoolWorker(ThreadFactory threadFactory) {
            super(threadFactory);
        }
    }
}
