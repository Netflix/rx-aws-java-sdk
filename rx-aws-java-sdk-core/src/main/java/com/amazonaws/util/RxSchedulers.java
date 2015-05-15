/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.util;

import rx.Scheduler;

/**
 * Static factory methods for creating Schedulers.
 */
public final class RxSchedulers {

    private final Scheduler compuationScheduler;

    private static final RxSchedulers INSTANCE = new RxSchedulers();

    private RxSchedulers() {
        compuationScheduler = new EventLoopsScheduler();
    }

    /**
     * Creates and returns a {@link Scheduler} intended for computational work.
     * <p>
     * This can be used for event-loops, processing callbacks and other computational work.
     * <p>
     * Do not perform IO-bound work on this scheduler. Use {@link #io()} instead.
     * <p>
     * Unhandled errors will be delivered to the scheduler Thread's {@link java.lang.Thread.UncaughtExceptionHandler}.
     *
     * @return a {@link Scheduler} meant for computation-bound work
     */
    public static Scheduler computation() {
        return INSTANCE.compuationScheduler;
    }
}
