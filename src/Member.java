import java.io.Serializable;

public abstract class Member implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String name;

    public Member(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public abstract int getMaxItems();
    public abstract int getLoanPeriod();
}