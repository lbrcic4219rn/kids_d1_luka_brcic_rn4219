package rs.raf.pools;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebThreadPool {

    private ExecutorService pool;

    public WebThreadPool(int size) {
        this.pool = Executors.newFixedThreadPool(size);
    }

    public ExecutorService getPool() {
        return pool;
    }
}
