import java.util.logging.Logger;
import java.lang.Thread;

public class DynamicRetryDelayExample1 {

    private Logger log;
    private final String TAG = "FileStoreDB";

    private Thread pollingTask;

    private void startLongPoll(String polledFile, int backOffSeconds) {
        pollingTask = new Thread () {
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
                    newBackoffSeconds = 60;
                }
                else {
                    log.info("Longpoll IO exception, restarting backing off {} seconds" + 30);
                    newBackoffSeconds = 30;
                }
                startLongPoll(polledFile, newBackoffSeconds);
            }
        };
        pollingTask.start();
    }
}



