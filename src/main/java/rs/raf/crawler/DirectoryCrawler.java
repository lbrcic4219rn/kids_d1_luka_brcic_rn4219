package rs.raf.crawler;

import rs.raf.core.App;
import rs.raf.job.FileJob;
import rs.raf.job.Job;
import rs.raf.job.JobQueue;
import rs.raf.job.ScanType;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DirectoryCrawler {
    private final JobQueue jobQueue;
    private final ExecutorService pool;
    private final long DIR_CRAWLER_SLEEP_TIME;
    private final CopyOnWriteArrayList<String> roots;
    private final ConcurrentHashMap<String, Long> timeModifiedCache;
    private boolean working = true;

    public DirectoryCrawler(JobQueue jobQueue) {
        this.jobQueue = jobQueue;
        this.pool = Executors.newSingleThreadExecutor();
        this.DIR_CRAWLER_SLEEP_TIME = Long.parseLong(App.properties.get(App.DIR_CRAWLER_SLEEP_TIME));
        this.roots = new CopyOnWriteArrayList();
        this.timeModifiedCache = new ConcurrentHashMap<>();
    }

    public void start() {
        pool.execute(new Runnable() {
            @Override
            public void run() {
                crawl();
            }
        });
    }

    public void addPath(String path) {
        this.roots.add(path);
    }

    private void crawl() {
        while(working) {
//        for(int i = 0; i < 2; i++) {
            Iterator<String> it = this.roots.iterator();

            try {

                while(it.hasNext()) {
                    searchFolder(it.next());
                }

                Thread.sleep(this.DIR_CRAWLER_SLEEP_TIME);

            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("dir crawler stopped");

    }

    private void searchFolder(String root) throws InterruptedException {
        File rootFolder = new File(root);
        if(rootFolder.exists()) {
            for(File f : rootFolder.listFiles()) {
                if(f.isDirectory()) {
                    if(f.getName().startsWith(App.properties.get(App.FILE_CORPUS_PREFIX))){
                        Long cachedLastModified = this.timeModifiedCache.get(f.getAbsolutePath());
                        if(cachedLastModified == null) {
                            this.timeModifiedCache.put(f.getAbsolutePath(), f.lastModified());
                            this.jobQueue.addJob(new FileJob(f.getAbsolutePath(), 0, f.listFiles().length - 1));
                            continue;
                        }

                        if(f.lastModified() != cachedLastModified) {
                            this.timeModifiedCache.put(f.getAbsolutePath(), f.lastModified());
                            jobQueue.addJob(new FileJob(f.getAbsolutePath(), 0, f.listFiles().length - 1));
                        }
                    } else {
                        this.searchFolder(f.getAbsolutePath());
                    }
                }
            }
        }
    }

    private void addJob(Job job) {
        try {
            this.jobQueue.addJob(job);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        this.working = false;
        System.out.println("dir crawler stopping...");
    }
}
