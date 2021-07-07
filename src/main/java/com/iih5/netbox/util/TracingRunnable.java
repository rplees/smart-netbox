package com.iih5.netbox.util;

import java.util.Map;

import org.slf4j.MDC;

final public class TracingRunnable implements Runnable {
    private final Runnable runnable;
    private final Map<?, ?> context;

    private TracingRunnable(Runnable runnable){
        this.runnable = runnable;
        context = MDC.getCopyOfContextMap();
    }

    public static TracingRunnable get(Runnable runnable){
        return new TracingRunnable(runnable);
    }

    @Override
    public void run() {
    	if(context != null) {
			MDC.setContextMap(context);
		}
        try {
            runnable.run();
        }
        finally {
            MDC.clear();
        }
    }
}