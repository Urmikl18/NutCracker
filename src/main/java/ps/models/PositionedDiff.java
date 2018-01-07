package ps.models;

import name.fraser.neil.plaintext.diff_match_patch.Diff;

public class PositionedDiff {
    private Diff diff;
    private int pos1;
    private int pos2;

    public PositionedDiff(Diff diff, int pos1, int pos2) {
        this.diff = diff;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public PositionedDiff(PositionedDiff mydiff) {
        this.diff = new Diff(mydiff.diff.operation, mydiff.diff.text);
        this.pos1 = mydiff.pos1;
        this.pos2 = mydiff.pos2;
    }

    public Diff getDiff() {
        return this.diff;
    }

    public int getPos1() {
        return this.pos1;
    }

    public int getPos2() {
        return this.pos2;
    }

    public void setDiff(Diff diff) {
        this.diff = diff;
    }

    public void setPos1(int pos1) {
        this.pos1 = pos1;
    }

    public void setPos2(int pos2) {
        this.pos2 = pos2;
    }

    public String toString() {
        return "[" + this.diff.toString() + " | (" + this.pos1 + " , " + this.pos2 + ")]";
    }
}