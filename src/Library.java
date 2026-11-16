import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Library implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String STATE_FILE = "library_state.dat";

    private final Map<String, LibraryItem> items = new LinkedHashMap<>();
    private final Map<String, Member> members = new LinkedHashMap<>();
    private final Map<String, Loan> activeLoans = new HashMap<>();
    private final Map<String, Queue<Member>> reservationQueues = new HashMap<>();
    private int currentDay = 0;

    public static Library loadOrCreate() {
        File file = new File(STATE_FILE);
        if (!file.exists()) return new Library();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Library) ois.readObject();
        } catch (Exception e) {
            System.err.println("Starting fresh state: " + e.getMessage());
            return new Library();
        }
    }

    private synchronized void saveState() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STATE_FILE))) {
            oos.writeObject(this);
        } catch (IOException e) {
            System.err.println("Failed to persist state: " + e.getMessage());
        }
    }

    public synchronized void addItem(LibraryItem item) throws InvalidInputException {
        if (items.containsKey(item.getId())) throw new InvalidInputException("duplicate item id: " + item.getId());
        items.put(item.getId(), item);
        saveState();
    }

    public synchronized void addMember(Member member) throws InvalidInputException {
        if (members.containsKey(member.getId())) throw new InvalidInputException("duplicate member id: " + member.getId());
        members.put(member.getId(), member);
        saveState();
    }

    public synchronized String borrowItem(String memberId, String itemId)
            throws ItemNotFoundException, MemberNotFoundException, ItemNotAvailableException, BorrowLimitExceededException {
        if (!items.containsKey(itemId)) throw new ItemNotFoundException(itemId);
        if (!members.containsKey(memberId)) throw new MemberNotFoundException(memberId);

        LibraryItem item = items.get(itemId);
        Member member = members.get(memberId);

        if (!(item instanceof Borrowable)) throw new ItemNotAvailableException(itemId + " cannot be borrowed");
        if (activeLoans.containsKey(itemId)) throw new ItemNotAvailableException(itemId + " is on loan");

        long count = activeLoans.values().stream().filter(l -> l.getMember().getId().equals(memberId)).count();
        if (count >= member.getMaxItems()) throw new BorrowLimitExceededException(memberId, member.getMaxItems());

        int dueDay = currentDay + member.getLoanPeriod();
        activeLoans.put(itemId, new Loan(item, member, dueDay));
        saveState();
        return "OK due on day " + dueDay;
    }

    public synchronized String returnItem(String itemId) throws ItemNotFoundException, ItemNotOnLoanException {
        if (!items.containsKey(itemId)) throw new ItemNotFoundException(itemId);
        if (!activeLoans.containsKey(itemId)) throw new ItemNotOnLoanException(itemId);

        Loan loan = activeLoans.remove(itemId);
        int fine = loan.calculateFine(currentDay);

        Queue<Member> queue = reservationQueues.get(itemId);
        if (queue != null && !queue.isEmpty()) {
            Iterator<Member> it = queue.iterator();
            while (it.hasNext()) {
                Member nextMember = it.next();
                long count = activeLoans.values().stream().filter(l -> l.getMember().getId().equals(nextMember.getId())).count();
                if (count < nextMember.getMaxItems()) {
                    it.remove();
                    int newDueDay = currentDay + nextMember.getLoanPeriod();
                    activeLoans.put(itemId, new Loan(items.get(itemId), nextMember, newDueDay));
                    saveState();
                    return "OK fine " + fine + ", issued to " + nextMember.getId() + " due on day " + newDueDay;
                }
            }
        }
        saveState();
        return "OK fine " + fine;
    }

    public synchronized String advanceTime(int days) throws InvalidInputException {
        if (days < 0) throw new InvalidInputException("days cannot be negative");
        currentDay += days;
        saveState();
        return "OK today is day " + currentDay;
    }

    public synchronized int getCurrentDay() {
        return currentDay;
    }

    public synchronized String getOverdueLoans() {
        List<Loan> overdue = activeLoans.values().stream()
                .filter(l -> l.getDaysLate(currentDay) > 0)
                .sorted((l1, l2) -> Integer.compare(l2.getDaysLate(currentDay), l1.getDaysLate(currentDay)))
                .collect(Collectors.toList());
        if (overdue.isEmpty()) return "NONE";
        return overdue.stream()
                .map(l -> l.getItem().getId() + "|" + l.getMember().getId() + "|" + l.getDaysLate(currentDay) + "|" + l.calculateFine(currentDay))
                .collect(Collectors.joining("\n"));
    }

    public synchronized String getInventory() {
        if (items.isEmpty()) return "NONE";
        return items.values().stream()
                .sorted(Comparator.comparing(LibraryItem::getId))
                .map(item -> {
                    String status = !(item instanceof Borrowable) ? "NOT BORROWABLE"
                            : activeLoans.containsKey(item.getId()) ? "ON LOAN " + activeLoans.get(item.getId()).getMember().getId()
                            : "AVAILABLE";
                    return item.getId() + "|" + item.getTypeString() + "|" + status;
                })
                .collect(Collectors.joining("\n"));
    }
}