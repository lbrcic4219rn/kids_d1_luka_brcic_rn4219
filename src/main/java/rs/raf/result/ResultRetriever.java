package rs.raf.result;

import rs.raf.job.Job;
import rs.raf.job.ScanType;

import java.util.Map;
import java.util.concurrent.Future;

public interface ResultRetriever {
    public Map<String, Map<String, Integer>> singleResult(String query, String command);

    public Map<String, Map<String, Integer>> summaryResult(String query, String command);

    public void addCorpusResult(Job submittedJob, Future<Map<String, Integer>> jobFutureResult);

    public void clearSummary(ScanType summaryType);
}
