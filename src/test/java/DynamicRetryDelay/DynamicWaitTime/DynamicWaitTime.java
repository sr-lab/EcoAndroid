package DynamicRetryDelay.DynamicWaitTime;
import java.util.logging.Logger;
import java.lang.Thread;

public class DynamicWaitTime {

    private Logger log;

    private Thread pollingTask;

    private void startLongPoll(String polledFile, int backOffSeconds) {
        pollingTask = new Thread () {
            int accessAttempts = 0;

            /*
             * EcoAndroid: DYNAMIC RETRY DELAY ENERGY PATTERN APPLIED
             * Switching the wait time from constant to dynamic
             * Application changed java file "DynamicWaitTime.java"
             */
            public void run() {
                long start_time = System.currentTimeMillis();
                long longpoll_timeout = 480;
                int newBackoffSeconds = 0;

                if(backOffSeconds != 0) {
                    log.info("Backing off for "+ backOffSeconds + " seconds");
                    try {
                        Thread.sleep((long) (backOffSeconds * 1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(System.currentTimeMillis() - start_time < longpoll_timeout * 1000) {
                    log.info("Longpoll timed out to quick, backing off for 60 seconds");
                    accessAttempts++;
                }
                else {
                    log.info("Longpoll IO exception, restarting backing off {} seconds" + 30);
                    accessAttempts++;
                }
                newBackoffSeconds = (int) (60.0 * (Math.pow(2.0, (double) accessAttempts) - 1.0));
                startLongPoll(polledFile, newBackoffSeconds);
            }
        };
        pollingTask.start();
    }
}



