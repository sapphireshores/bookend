public class ReferenceBook extends LibraryItem {
    private final String author;

    public ReferenceBook(String id, String title, String author) {
        super(id, title);
        this.author = author;
    }

    public String getAuthor() { return author; }

    @Override
    public String getTypeString() { return "REFERENCE"; }

    @Override
    public String getDetailsString() { return getTitle(); }
}