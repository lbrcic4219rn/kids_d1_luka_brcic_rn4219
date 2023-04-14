package rs.raf.job;

import rs.raf.core.App;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.*;

public class FileJob extends RecursiveTask<Map<String, Integer>> implements Job {
    private String path;
    private Map<String, Integer> jobResult;
    private int left;
    private int right;
    private String[] keywords;


    public FileJob(String path, int left, int right) {
        this.path = path;
        this.jobResult = new ConcurrentHashMap<>();
        this.left = left;
        this.right = right;
        String keyStr = App.properties.get(App.KEYWORDS);
        this.keywords = keyStr.split(",");
    }


    public String getPath() {
        return path;
    }

    @Override
    protected Map<String, Integer>  compute() {
        File file = new File(this.path);
        File[] fileList = file.listFiles();

        long corpSize = 0;
        for(int i = left; i <= right; i++) {
            corpSize += fileList[i].length();
        }

        if(corpSize <= Long.parseLong(App.properties.get(App.FILE_SCANNING_SIZE_LIMIT)) || left == right) {
            this.countKeywords();
        }

        else {
            int mid = (right - left) / 2 + left;

            FileJob leftTask = new FileJob(this.path, left, mid);
            leftTask.fork();

            left = mid + 1;

            Map<String, Integer> rightResult = this.compute();
            Map<String, Integer> leftResult = leftTask.join();
//            System.out.println("left: " + leftResult);
//            System.out.println("Right: " + rightResult);
            leftResult.forEach((k, v) -> rightResult.merge(k, v, Integer::sum));
            this.jobResult.putAll(rightResult);
        }
        return this.jobResult;
    }

    private void countKeywords() {
        Scanner sc = null;
        try {
            for (int i = left; i <= right; i++){
                File file = new File(this.path);
                File[] files = file.listFiles();
                sc = new Scanner(files[i]);

                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    String[] words = line.split("\\s+");
                    for(String word : words) {
                        for(String keyword : this.keywords) {
                           if(word.equals(keyword)) {
                               if(this.jobResult.get(word) == null) {
                                   this.jobResult.put(word, 1);
                               } else {
                                   this.jobResult.put(word, this.jobResult.get(word) + 1);
                               }
                           }
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            sc.close();
        }

    }

    @Override
    public ScanType getType() {
        return ScanType.FILE;
    }
}
