
    // M-Kopo SACCO Management System (Java Capstone Project) - Phase 4 (final)
import java.io.*;                           // file reading and writing
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

// 4. Helper that appends a line to transactions.txt
class FileLogger {
    private static final String LOG_FILE = "transactions.txt";   // log file name

    // static: can be called without creating an object
    public static void append(String line) {
        // try-with-resources closes the file automatically; true = append mode
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.println(line);   // write one line
        } catch (IOException e) {
            System.out.println("Error writing to log file: " + e.getMessage());
        }
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

    // From Auditable: write to transactions.txt
    @Override
    public void logTransaction(String message) {
        FileLogger.append(accountNumber + " | " + message);
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
public class Phase4_SaccoManager {
    // HashMap: find members quickly by ID
    private static HashMap<String, Member> membersDb = new HashMap<>();
    // HashSet: no duplicate National IDs
    private static HashSet<String> registeredNationalIds = new HashSet<>();
    // File where members are saved between runs
    private static final String MEMBERS_FILE = "members.csv";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);   // reads user input
        loadDataFromFile();                         // load saved members first

        int choice = 0;
        // do-while: show the menu at least once, repeat until Exit
        do {
            System.out.println("\n=== M-KOPO SACCO MANAGEMENT SYSTEM ===");
            System.out.println("1. Register New Member");
            System.out.println("2. Open an Account");
            System.out.println("3. Deposit Money");
            System.out.println("4. Withdraw Money");
            System.out.println("5. View Member Details");
            System.out.println("6. Exit");
            choice = readInt(scanner, "Enter choice: ");

            // switch: run the method that matches the choice
            switch (choice) {
                case 1: registerMember(scanner); break;
                case 2: openAccount(scanner); break;
                case 3: depositMoney(scanner); break;
                case 4: withdrawMoney(scanner); break;
                case 5: viewMemberDetails(scanner); break;
                case 6:
                    saveDataToFile();   // save before closing
                    System.out.println("Data saved. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please pick 1 to 6.");
            }
        } while (choice != 6);

        scanner.close();
    }

    // Ask until the user types a whole number (catches InputMismatchException)
    private static int readInt(Scanner scanner, String prompt) {
        while (true) {                              // repeat until valid
            System.out.print(prompt);
            try {
                int value = scanner.nextInt();      // fails if not a whole number
                scanner.nextLine();                 // clear leftover Enter
                return value;
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a whole number.");
                scanner.nextLine();                 // discard bad input
            }
        }
    }

    // Same, but for decimal amounts
    private static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double value = scanner.nextDouble();
                scanner.nextLine();
                return value;
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number (e.g. 1500 or 250.50).");
                scanner.nextLine();
            }
        }
    }

    // Menu 1: register a member
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
            FileLogger.append("NEW MEMBER | " + memberId + " | " + member.getName()
                    + " | National ID: " + member.getNationalId());
            System.out.println("Success: Member registered with ID " + memberId);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());   // validation message
        }
    }

    // Menu 2: open a Savings or Loan account
    private static void openAccount(Scanner scanner) {
        Member member = findMember(scanner);
        if (member == null) return;   // member not found

        System.out.println("1. Savings Account");
        System.out.println("2. Loan Account");
        int type = readInt(scanner, "Choose account type: ");

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
        FileLogger.append("NEW ACCOUNT | " + accountNumber + " | " + typeName);
        System.out.println("Success: " + typeName + " account " + accountNumber + " opened.");
    }

    // Menu 3: deposit
    private static void depositMoney(Scanner scanner) {
        Account account = findAccount(scanner);
        if (account == null) return;   // member or account not found

        double amount = readDouble(scanner, "Enter amount to deposit: ");
        try {
            account.deposit(amount);
            System.out.println("Deposit successful. New balance: KSh "
                    + String.format("%.2f", account.getBalance()));
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());   // amount was zero or negative
        }
    }

    // Menu 4: withdraw
    private static void withdrawMoney(Scanner scanner) {
        Account account = findAccount(scanner);
        if (account == null) return;

        double amount = readDouble(scanner, "Enter amount to withdraw: ");
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

    // Menu 5: show member, balances, interest and history
    private static void viewMemberDetails(Scanner scanner) {
        Member member = findMember(scanner);
        if (member == null) return;

        System.out.println("\n--- MEMBER DETAILS ---");
        System.out.println("Member ID  : " + member.getMemberId());
        System.out.println("Name       : " + member.getName());
        System.out.println("National ID: " + member.getNationalId());

        if (member.getAccounts().isEmpty()) {
            System.out.println("No accounts opened yet.");
            return;
        }

        // Loop through each account (key = type, value = Account)
        for (Map.Entry<String, Account> entry : member.getAccounts().entrySet()) {
            Account acc = entry.getValue();
            System.out.println("\n" + entry.getKey() + " Account (" + acc.getAccountNumber() + ")");
            System.out.println("  Balance : KSh " + String.format("%.2f", acc.getBalance()));
            // Polymorphism: the Savings or Loan version runs automatically
            System.out.println("  Interest: KSh " + String.format("%.2f", acc.calculateInterest()));
            // Print this session's transactions
            for (Transaction t : acc.getTransactionHistory()) {
                System.out.println("    " + t);
            }
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
        int type = readInt(scanner, "Choose account type: ");

        // 1 = Savings, 2 = Loan, anything else = empty
        String typeName = (type == 1) ? "Savings" : (type == 2) ? "Loan" : "";
        Account account = member.getAccounts().get(typeName);   // null if missing
        if (account == null) {
            System.out.println("Error: That account does not exist for this member.");
        }
        return account;
    }

    // members.csv: one line per member
    // MemberId,Name,NationalId,SavingsAccNo,SavingsBalance,LoanAccNo,LoanBalance
    // "-" means the member has no such account

    // Load members from members.csv at startup
    private static void loadDataFromFile() {
        File file = new File(MEMBERS_FILE);
        // First run: no file yet
        if (!file.exists()) {
            System.out.println("No previous data found. Starting fresh.");
            return;
        }

        // try-with-resources closes the reader automatically
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            // Read one line at a time until the end of the file
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);   // cut at commas, -1 keeps empty values
                if (p.length != 7) continue;        // skip bad lines

                try {
                    Member member = new Member(p[0], p[1], p[2]);   // id, name, national ID
                    // Rebuild the Savings account if there is one
                    if (!p[3].equals("-")) {
                        member.addAccount("Savings", new SavingsAccount(p[3], Double.parseDouble(p[4])));
                    }
                    // Rebuild the Loan account if there is one
                    if (!p[5].equals("-")) {
                        member.addAccount("Loan", new LoanAccount(p[5], Double.parseDouble(p[6])));
                    }
                    membersDb.put(member.getMemberId(), member);        // back into the HashMap
                    registeredNationalIds.add(member.getNationalId());  // and the HashSet
                } catch (IllegalArgumentException e) {   // also covers NumberFormatException
                    System.out.println("Skipped a bad record: " + line);
                }
            }
            System.out.println("Loaded " + membersDb.size() + " member(s) from " + MEMBERS_FILE);
        } catch (IOException e) {
            System.out.println("Error loading data: " + e.getMessage());
        }
    }

    // Save all members to members.csv on exit (overwrites the old file)
    private static void saveDataToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(MEMBERS_FILE))) {
            for (Member m : membersDb.values()) {
                // null means the member has no such account
                Account sav = m.getAccounts().get("Savings");
                Account loan = m.getAccounts().get("Loan");

                // (condition ? a : b) is a short if/else; no account -> "-" and 0
                pw.println(m.getMemberId() + "," + m.getName() + "," + m.getNationalId() + ","
                        + (sav == null ? "-" : sav.getAccountNumber()) + ","
                        + (sav == null ? "0" : sav.getBalance()) + ","
                        + (loan == null ? "-" : loan.getAccountNumber()) + ","
                        + (loan == null ? "0" : loan.getBalance()));
            }
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }
}

