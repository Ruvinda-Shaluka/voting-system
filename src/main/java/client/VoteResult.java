package client;

public class VoteResult {
    private String option;
    private int count;

    public VoteResult(String option, int count) {
        this.option = option;
        this.count = count;
    }

    public String getOption() { return option; }
    public void setOption(String option) { this.option = option; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}