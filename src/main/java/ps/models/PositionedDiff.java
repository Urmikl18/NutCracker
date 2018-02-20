package ps.models;

import name.fraser.neil.plaintext.diff_match_patch.Diff;

/**
 * Model for a positioned diff.
 * <p>
 * <ol>
 * <li>Diff from edit script</li>
 * <li>Diff's position in the original version of the document</li>
 * <li>Diff's position in the modified version of the document</li>
 * </ol>
 */
public class PositionedDiff {
    private Diff diff;
    private int pos1;
    private int pos2;

    /**
     * Constructor
     * @param diff Diff, obtained from edit script.
     * @param pos1 Diff's position in the original version.
     * @param pos2 Diff's position in the modified version.
     */
    public PositionedDiff(Diff diff, int pos1, int pos2) {
        this.diff = diff;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    /**
     * Copy constructor.
     */
    public PositionedDiff(PositionedDiff mydiff) {
        this.diff = new Diff(mydiff.diff.operation, mydiff.diff.text);
        this.pos1 = mydiff.pos1;
        this.pos2 = mydiff.pos2;
    }

    /**
     * @return Difference from edit script.
     */
    public Diff getDiff() {
        return this.diff;
    }

    /**
     * @return Diff's position in the original version of the document.
     */
    public int getPos1() {
        return this.pos1;
    }

    /**
     * @return Diff's position in the modified version of the document.
     */
    public int getPos2() {
        return this.pos2;
    }

    /**
     * Sets diff from edit script.
     */
    public void setDiff(Diff diff) {
        this.diff = diff;
    }

    /**
     * Sets diff's position in the original version of the document.
     */
    public void setPos1(int pos1) {
        this.pos1 = pos1;
    }

    /**
     * Sets diff's position in the modified version of the document.
     */
    public void setPos2(int pos2) {
        this.pos2 = pos2;
    }

    /**
     * @return PositionedDiff as a string.
     * <p>
     * Example.: [(EQUAL, common text) | (10, 12)]
     */
    public String toString() {
        return "[" + this.diff.toString() + " | (" + this.pos1 + " , " + this.pos2 + ")]";
    }
}