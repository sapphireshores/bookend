import java.io.Serializable;

public class Loan implements Serializable {
    private static final long serialVersionUID = 1L;
    private final LibraryItem item;
    private final Member member;
    private final int dueDay;

    public Loan(LibraryItem item, Member member, int dueDay) {
        this.item = item;
        this.member = member;
        this.dueDay = dueDay;
    }

    public LibraryItem getItem() { return item; }
    public Member getMember() { return member; }
    public int getDueDay() { return dueDay; }

    public int getDaysLate(int currentDay) {
        return Math.max(0, currentDay - dueDay);
    }

    public int calculateFine(int currentDay) {
        int daysLate = getDaysLate(currentDay);
        if (daysLate == 0 || !(item instanceof Borrowable)) return 0;
        return daysLate * ((Borrowable) item).getFinePerDayLate();
    }
}