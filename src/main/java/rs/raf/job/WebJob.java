package rs.raf.job;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import rs.raf.core.App;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class WebJob implements Callable<Map<String, Integer>>, Job {
    private String path;
    private int hopCount;
    private JobQueue jobQueue;
    private Map<String, Integer> webPageResult;
    private String[] keywords;

    public WebJob(String path, int hopCount, JobQueue jobQueue) {
        this.jobQueue = jobQueue;
        this.hopCount = hopCount;
        this.path = path;
        this.webPageResult = new ConcurrentHashMap<>();
        String keyStr = App.properties.get(App.KEYWORDS);
        this.keywords = keyStr.split(",");
    }
    @Override
    public Map<String, Integer> call() throws Exception {
        try {
            Document document = Jsoup.connect(path).get();
            Elements links = document.select("a[href]");
            if(this.hopCount > 0) {
                for (Element link : links) {
                    String url = link.attr("abs:href").trim();
                    if (
                            url.equals("") ||
                                    url.contains("#") ||
                                    url.contains("%") ||
                                    !url.startsWith("http") || url.endsWith(".jpg") || url.endsWith(".png") || url.endsWith(".pdf") || url.contains(".php") ||
                                    App.isWebPageVisited(url)
                    ) continue;
                    jobQueue.addJob(new WebJob(url, this.hopCount - 1, this.jobQueue));
                }
            }
            String[] words = document.text().split("\\s+");
            for(String word : words) {
                for(String keyword : this.keywords) {
                    if(word.equals(keyword)) {
                        if(this.webPageResult.get(word) == null) {
                            this.webPageResult.put(word, 1);
                        } else {
                            this.webPageResult.put(word, this.webPageResult.get(word) + 1);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("failed: reason: " + e.getMessage());
            return new ConcurrentHashMap<>();
        }

        System.out.println(this.path + " : " + webPageResult);
        return webPageResult;
    }

    @Override
    public ScanType getType() {
        return ScanType.WEB;
    }

    public int getHopCount() {
        return hopCount;
    }

    public String getPath() {
        return path;
    }
}
