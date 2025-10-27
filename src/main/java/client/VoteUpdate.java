package client;

import java.util.List;

public class VoteUpdate {
    private List<VoteResult> results;

    public VoteUpdate(List<VoteResult> results) {
        this.results = results;
    }

    public List<VoteResult> getResults() { return results; }
    public void setResults(List<VoteResult> results) { this.results = results; }
}