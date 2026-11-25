// M-Kopo SACCO Management System (Java Capstone Project) - Phase 2
import java.time.LocalDateTime;             // current date and time
import java.time.format.DateTimeFormatter;  // format the date nicely
import java.util.*;                         // Scanner, HashMap, HashSet, ArrayList, Map

// 1. Custom checked exception for withdrawals above the balance
class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);   // pass the message to Exception
    }
}

// 2. Interface: any implementing class must write logTransaction()
interface Auditable {
    void logTransaction(String message);   // no body, only the contract
}

// 3. One deposit or withdrawal record
class Transaction {
    private String type;            // DEPOSIT or WITHDRAWAL
    private double amount;          // money moved
    private double balanceAfter;    // balance after the transaction
    private LocalDateTime time;     // when it happened

    public Transaction(String type, double amount, double balanceAfter) {
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.time = LocalDateTime.now();   // time of creation
    }

    // Getters
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }

    // How the transaction looks when printed
    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // %.2f = 2 decimal places
        return time.format(fmt) + " | " + type + " | KSh " + String.format("%.2f", amount)
                + " | Balance: KSh " + String.format("%.2f", balanceAfter);
    }
}

// 5. Abstract parent class for all accounts (cannot be created directly)
abstract class Account implements Auditable {
    private String accountNumber;                        // private = encapsulation
    private double balance;
    private ArrayList<Transaction> transactionHistory;   // history in order

    public Account(String accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        setBalance(initialBalance);                      // validated by the setter
        this.transactionHistory = new ArrayList<>();     // start empty
    }

    // Getters
    public String getAccountNumber() { return accountNumber; }
    public double getBalance() { return balance; }
    public ArrayList<Transaction> getTransactionHistory() { return transactionHistory; }

    // Setter with validation: balance cannot be negative
    private void setBalance(double balance) {
        if (balance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative.");
        }
        this.balance = balance;
    }

    // Add money to the account
    public void deposit(double amount) {
        // Reject zero or negative amounts
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");
        }
        setBalance(balance + amount);                    // add the money
        Transaction t = new Transaction("DEPOSIT", amount, balance);
        transactionHistory.add(t);                       // keep in memory
        logTransaction(t.toString());                    // write to file
    }

    // Take money out; "throws" warns callers to handle the exception
    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }
        // Not enough money: fire our custom exception
        if (amount > balance) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Your balance is KSh " + String.format("%.2f", balance));
        }
        setBalance(balance - amount);                    // subtract the money
        Transaction t = new Transaction("WITHDRAWAL", amount, balance);
        transactionHistory.add(t);
        logTransaction(t.toString());
    }

    // Each child class writes its own version
    public abstract double calculateInterest();

    // From Auditable: print the log (file writing is added in Phase 4)
    @Override
    public void logTransaction(String message) {
        System.out.println("LOG: " + accountNumber + " | " + message);
    }
}

// 6. Child classes: inherit from Account, only the interest rate differs
class SavingsAccount extends Account {
    public SavingsAccount(String accountNumber, double initialBalance) {
        super(accountNumber, initialBalance);   // call the parent constructor
    }

    @Override
    public double calculateInterest() {
        return getBalance() * 0.05;   // 5% interest
    }
}

class LoanAccount extends Account {
    public LoanAccount(String accountNumber, double initialBalance) {
        super(accountNumber, initialBalance);
    }

    @Override
    public double calculateInterest() {
        return getBalance() * 0.12;   // 12% interest
    }
}

// 7. A SACCO member
class Member {
    private String memberId;                 // e.g. M-001
    private String name;
    private String nationalId;
    private Map<String, Account> accounts;   // key: "Savings" or "Loan"

    public Member(String memberId, String name, String nationalId) {
        this.memberId = memberId;
        setName(name);                       // setters validate the data
        setNationalId(nationalId);
        this.accounts = new HashMap<>();     // no accounts yet
    }

    // Getters
    public String getMemberId() { return memberId; }
    public String getName() { return name; }
    public String getNationalId() { return nationalId; }
    public Map<String, Account> getAccounts() { return accounts; }

    // Name cannot be empty
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        this.name = name.trim().replace(",", " ");   // commas would break the CSV
    }

    // National ID must be 6 to 10 digits
    public void setNationalId(String nationalId) {
        if (nationalId == null || !nationalId.trim().matches("\\d{6,10}")) {
            throw new IllegalArgumentException("National ID must be 6 to 10 digits.");
        }
        this.nationalId = nationalId.trim();
    }

    // Give the member an account
    public void addAccount(String type, Account account) {
        accounts.put(type, account);
    }
}

// 8. Main program: menu and file handling
public class Phase2_SaccoManager {
    // HashMap: find members quickly by ID
    private static HashMap<String, Member> membersDb = new HashMap<>();
    // HashSet: no duplicate National IDs
    private static HashSet<String> registeredNationalIds = new HashSet<>();

    // Phase 2 test: calls the methods one after another (the menu is added in Phase 3)
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        registerMember(scanner);   // type: a name, then a National ID
        openAccount(scanner);      // type: member ID, then 1 for Savings
        depositMoney(scanner);     // type: member ID, 1, then an amount
        withdrawMoney(scanner);    // type: member ID, 1, then an amount
        scanner.close();
    }

    // Register a member
    private static void registerMember(Scanner scanner) {
        System.out.print("Enter Full Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter National ID: ");
        String nationalId = scanner.nextLine().trim();

        // HashSet duplicate check
        if (registeredNationalIds.contains(nationalId)) {
            System.out.println("Error: A member with this National ID already exists.");
            return;
        }

        // Auto-generate the ID: M-001, M-002...
        String memberId = String.format("M-%03d", membersDb.size() + 1);

        try {
            Member member = new Member(memberId, name, nationalId);   // setters validate
            membersDb.put(memberId, member);                          // store the member
            registeredNationalIds.add(member.getNationalId());        // remember the ID
            System.out.println("Success: Member registered with ID " + memberId);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());   // validation message
        }
    }

    // Open a Savings or Loan account
    private static void openAccount(Scanner scanner) {
        Member member = findMember(scanner);
        if (member == null) return;   // member not found

        System.out.println("1. Savings Account");
        System.out.println("2. Loan Account");
        System.out.print("Choose account type: ");
        int type = scanner.nextInt();
        scanner.nextLine();   // clear leftover Enter

        // Turn the number into a type name
        String typeName;
        if (type == 1) {
            typeName = "Savings";
        } else if (type == 2) {
            typeName = "Loan";
        } else {
            System.out.println("Invalid account type.");
            return;
        }

        // One account per type
        if (member.getAccounts().containsKey(typeName)) {
            System.out.println("Error: Member already has a " + typeName + " account.");
            return;
        }

        // Account number example: M-001-SAVINGS
        String accountNumber = member.getMemberId() + "-" + typeName.toUpperCase();
        Account account;   // parent type can hold either child
        if (type == 1) {
            account = new SavingsAccount(accountNumber, 0);   // starts at KSh 0
        } else {
            account = new LoanAccount(accountNumber, 0);
        }
        member.addAccount(typeName, account);
        System.out.println("Success: " + typeName + " account " + accountNumber + " opened.");
    }

    // Deposit
    private static void depositMoney(Scanner scanner) {
        Account account = findAccount(scanner);
        if (account == null) return;   // member or account not found

        System.out.print("Enter amount to deposit: ");
        double amount = scanner.nextDouble();
        scanner.nextLine();   // clear leftover Enter
        try {
            account.deposit(amount);
            System.out.println("Deposit successful. New balance: KSh "
                    + String.format("%.2f", account.getBalance()));
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());   // amount was zero or negative
        }
    }

    // Withdraw
    private static void withdrawMoney(Scanner scanner) {
        Account account = findAccount(scanner);
        if (account == null) return;

        System.out.print("Enter amount to withdraw: ");
        double amount = scanner.nextDouble();
        scanner.nextLine();   // clear leftover Enter
        try {
            account.withdraw(amount);   // checked exception, so try-catch is required
            System.out.println("Withdrawal successful. New balance: KSh "
                    + String.format("%.2f", account.getBalance()));
        } catch (InsufficientFundsException e) {
            System.out.println("TRANSACTION FAILED: " + e.getMessage());   // not enough money
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());                // bad amount
        }
    }

    // Ask for a member ID; returns null if not found
    private static Member findMember(Scanner scanner) {
        System.out.print("Enter Member ID (e.g. M-001): ");
        String id = scanner.nextLine().trim().toUpperCase();   // "m-001" also works
        Member member = membersDb.get(id);                     // null if missing
        if (member == null) {
            System.out.println("Error: Member not found.");
        }
        return member;
    }

    // Ask for a member and account type; returns null if not found
    private static Account findAccount(Scanner scanner) {
        Member member = findMember(scanner);
        if (member == null) return null;

        System.out.println("1. Savings Account");
        System.out.println("2. Loan Account");
        System.out.print("Choose account type: ");
        int type = scanner.nextInt();
        scanner.nextLine();   // clear leftover Enter

        // 1 = Savings, 2 = Loan, anything else = empty
        String typeName = (type == 1) ? "Savings" : (type == 2) ? "Loan" : "";
        Account account = member.getAccounts().get(typeName);   // null if missing
        if (account == null) {
            System.out.println("Error: That account does not exist for this member.");
        }
        return account;
    }
}