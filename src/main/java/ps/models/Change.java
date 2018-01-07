package ps.models;

public class Change {
    private String before;
    private String after;
    private int pos1;
    private int pos2;

    public Change(String before, String after, int pos1, int pos2) {
        this.before = before;
        this.after = after;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public String getBefore() {
        return this.before;
    }

    public String getAfter() {
        return this.after;
    }

    public int getPos1() {
        return this.pos1;
    }

    public int getPos2() {
        return this.pos2;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public void setPos1(int pos1) {
        this.pos1 = pos1;
    }

    public void setPos2(int pos2) {
        this.pos2 = pos2;
    }

    public String toString() {
        return "Change(BEFORE: " + before + " ; AFTER: " + after + " | [" + pos1 + " , " + pos2 + "])";
    }
}