package rs.raf.result;

import rs.raf.job.ScanType;

import java.util.Map;
import java.util.concurrent.Callable;

public class ResultJob implements Callable<Map<String, Map<String, Integer>>> {

    private ResultRetriever resultRetriever;
    private String query;
    private String command;

    public ResultJob(String query, ResultRetriever resultRetriever, String command) {
        this.resultRetriever = resultRetriever;
        this.query = query;
        this.command = command;
    }

    @Override
    public Map<String, Map<String, Integer>> call() throws Exception {
        if(command.equals("cfs")) {
            resultRetriever.clearSummary(ScanType.FILE);
            return null;
        }
        if(command.equals("cws")) {
            resultRetriever.clearSummary(ScanType.WEB);
            return null;
        }

        if(command.equals("get") || command.equals("query")) {
            if(isSummary())
                return resultRetriever.summaryResult(query, command);
            return resultRetriever.singleResult(query, command);
        }
        return null;
    }

    private boolean isSummary() {
        return this.query.split("\\|")[1].contains("summary") &&
                (this.query.split("\\|")[0].equals("web") || this.query.split("\\|")[0].equals("file"));
    }
}
