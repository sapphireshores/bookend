import java.io.Serializable;

public abstract class LibraryItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String title;

    public LibraryItem(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public abstract String getTypeString();
    public abstract String getDetailsString();
}