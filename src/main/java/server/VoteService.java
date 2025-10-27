package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class VoteService {
    private final Map<String, AtomicInteger> votes;
    private final Map<String, Boolean> votedClients;

    public VoteService() {
        votes = new ConcurrentHashMap<>();
        votedClients = new ConcurrentHashMap<>();
        initializeOptions();
    }

    private void initializeOptions() {
        votes.put("Option A", new AtomicInteger(0));
        votes.put("Option B", new AtomicInteger(0));
        votes.put("Option C", new AtomicInteger(0));
    }

    public synchronized boolean castVote(String clientId, String option) {
        if (votedClients.containsKey(clientId)) {
            return false; // Client already voted
        }
        
        if (votes.containsKey(option)) {
            votes.get(option).incrementAndGet();
            votedClients.put(clientId, true);
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
        return votedClients.size();
    }
}