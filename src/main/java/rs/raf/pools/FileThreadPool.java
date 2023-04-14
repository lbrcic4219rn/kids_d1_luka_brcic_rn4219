package rs.raf.pools;

import java.util.concurrent.ForkJoinPool;

public class FileThreadPool{

    private ForkJoinPool forkJoinPool;

    public FileThreadPool() {
        this.forkJoinPool = new ForkJoinPool();
    }

    public ForkJoinPool getPool() {
        return forkJoinPool;
    }
}
