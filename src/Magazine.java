public class Magazine extends LibraryItem implements Borrowable {
    private final String issue;

    public Magazine(String id, String title, String issue) {
        super(id, title);
        this.issue = issue;
    }

    public String getIssue() { return issue; }

    @Override
    public int getFinePerDayLate() { return 1; }

    @Override
    public String getTypeString() { return "MAGAZINE"; }

    @Override
    public String getDetailsString() { return getTitle(); }
}