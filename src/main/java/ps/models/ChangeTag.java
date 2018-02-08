package ps.models;

import ps.models.Change;

/**
 * Model for a classified change.
 * <p>
 * <ol>
 * <li>Change of type {@link Change}
 * <li>Change's class of type {@link Tag} 
 * </ol>
 */
public class ChangeTag {
    private Change change;
    private Tag tag;

    /**
     * Enumeration of possible types of changes. Classes are derived from a user study among authors
     * of academic papers.
     * <p>
     * <ul>
     * <li>FORMATTING: simple cases of formatting (e.g. extra spacing, line breaks)</li>
     * <li>CITATION: changes in citations.</li>
     * <li>SPELLING: spelling correction.</li>
     * <li>GRAMMAR: grammar correction.</li>
     * <li>SYNONYM: individual words were replaced, and they are interchangeable.</li>
     * <li>RELATED_TERM: replaced word has a similar meaning.</li>
     * <li>UNRELATED_TERM: individual words were replaced, but they are not related.</li>
     * <li>REPHRASING: rephrasing sentences.</li>
     * <li>RELATED_IDEA: large text snippets were modified, but the idea remains similar.</li>
     * <li>NOVEL_IDEA: large text snippets were modified, and newly added text expresses a new idea.</li>
     * <li>UNDEFINED: default case. Some changes can not be classified correctly.</li>
     * </ul>
     */
    public static enum Tag {
        FORMATTING, CITATION, SPELLING, GRAMMAR,

        RELATED_TERM, UNRELATED_TERM, SYNONYM,

        REPHRASING, RELATED_IDEA, NOVEL_IDEA,

        UNDEFINED
    }

    /**
     * Constructor.
     * <p>
     * @param change Change in a plain-text document.
     * @param tag Change's type.
     */
    public ChangeTag(Change change, Tag tag) {
        this.change = change;
        this.tag = tag;
    }

    /**
     * @return Change that happened in a document.
     */
    public Change getChange() {
        return this.change;
    }

    /**
     * @return Type of the change that occured.
     */
    public Tag getTag() {
        return this.tag;
    }

    /**
     * Sets change that happened in a document.
     */
    public void setChange(Change change) {
        this.change = change;
    }

    /**
     * Sets change's class.
     */
    public void setChangeClass(Tag tag) {
        this.tag = tag;
    }

    /**
     * @return Change and its classification as a string.
     * <p>
     * Example: [RELATED_TERM] Change(BEFORE: team ; AFTER: squad | [10 , 12])
     */
    public String toString() {
        return "[" + this.tag.toString() + "] " + this.change.toString();
    }
}