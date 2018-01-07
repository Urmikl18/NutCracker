package ps.models;

import ps.models.Change;

public class ChangeTag {
    private Change change;
    private Tag tag;

    public static enum Tag {
        UNDEFINED,

        IMPOSSIBLE_TO_CLASSIFY, FORMATTING, CITATION,

        SPELLING, RELATED_WORDS, UNRELATED_WORDS,

        REPHRASING, RELATED_IDEA, NOVEL_IDEA
    }

    public ChangeTag(Change change, Tag tag) {
        this.change = change;
        this.tag = tag;
    }

    public Change getChange() {
        return this.change;
    }

    public Tag getTag() {
        return this.tag;
    }

    public void setChange(Change change) {
        this.change = change;
    }

    public void setChangeClass(Tag tag) {
        this.tag = tag;
    }

    public String toString() {
        return "[" + this.tag.toString() + "] " + this.change.toString();
    }
}