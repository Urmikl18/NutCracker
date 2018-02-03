package ps.models;

/**
 * Model for a change in our opinion.
 * <p>
 * <ol>
 * <li>Replaced text snippet</li>
 * <li>Replacing text snippet</li>
 * <li>Change's position in the original version</li>
 * <li>Change's position in the modified version</li>
 * </ol>
 */
public class Change {
    private String before;
    private String after;
    private int pos1;
    private int pos2;

    /**
     * Constructor.
     * <p>
     * @param before Text snippet that was replaced in the original version of the document.
     * @param after Text snippet that replaced "before" in the modified version of the document.
     * @param pos1 Position of "before" in the original version of the document.
     * @param pos2 Position of "after" in the modified version of the document.
     */
    public Change(String before, String after, int pos1, int pos2) {
        this.before = before;
        this.after = after;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    /**
     * @return Text snippet that was replaced.
     */
    public String getBefore() {
        return this.before;
    }

    /**
     * @return Text snippet in the modified version.
     */
    public String getAfter() {
        return this.after;
    }

    /**
     * @return Position of modified text snippet in the original version.
     */
    public int getPos1() {
        return this.pos1;
    }

    /**
     * @return Position of changed text snippet in the modified version.
     */
    public int getPos2() {
        return this.pos2;
    }

    /**
     * Sets text snippet before the change.
     */
    public void setBefore(String before) {
        this.before = before;
    }

    /**
     * Sets text snippet after the change.
     */
    public void setAfter(String after) {
        this.after = after;
    }

    /**
     * Sets change's position in the original version.
     */
    public void setPos1(int pos1) {
        this.pos1 = pos1;
    }

    /**
     * Sets change's position in the modified version.
     */
    public void setPos2(int pos2) {
        this.pos2 = pos2;
    }

    /**
     * @return Change as a string.
     * <p>
     * Example: Change(BEFORE: before ; AFTER: after | [10 , 12])
     */
    public String toString() {
        return "Change(BEFORE: " + before + " ; AFTER: " + after + " | [" + pos1 + " , " + pos2 + "])";
    }
}