package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

public class VoteService {
    private final Map<String, AtomicInteger> votes;
    private final Map<String, String> clientVotes; // clientId -> votedOption
    private final List<String> voteHistory; // Store vote history

    public VoteService() {
        votes = new ConcurrentHashMap<>();
        clientVotes = new ConcurrentHashMap<>();
        voteHistory = new ArrayList<>();
        initializeOptions();
    }

    private void initializeOptions() {
        votes.put("Option A", new AtomicInteger(0));
        votes.put("Option B", new AtomicInteger(0));
        votes.put("Option C", new AtomicInteger(0));
    }

    public synchronized boolean castVote(String clientId, String clientName, String option) {

        if (votes.containsKey(option)) {

            if (clientVotes.containsKey(clientId)) {
                String previousVote = clientVotes.get(clientId);
                votes.get(previousVote).decrementAndGet();
                voteHistory.add(clientName + " voted");
            } else {
                voteHistory.add(clientName + " voted");
            }

            votes.get(option).incrementAndGet();
            clientVotes.put(clientId, option);
            return true;
        }
        return false;
    }

    public Map<String, Integer> getVoteResults() {
        Map<String, Integer> results = new ConcurrentHashMap<>();
        votes.forEach((option, count) -> results.put(option, count.get()));
        return results;
    }

    public int getTotalVotes() {
        return clientVotes.size();
    }

    public List<String> getVoteHistory() {
        return new ArrayList<>(voteHistory);
    }

    public String getClientVote(String clientId) {
        return clientVotes.get(clientId);
    }

    public void resetVotes() {
        votes.forEach((option, count) -> count.set(0));
        clientVotes.clear();
        voteHistory.clear();
    }
}