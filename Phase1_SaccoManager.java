
    // M-Kopo SACCO Management System (Java Capstone Project) - Phase 1
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

// 8. Main program: quick test for Phase 1
public class Phase1_SaccoManager {
    public static void main(String[] args) {
        // Create a member and open a Savings account for them
        Member member = new Member("M-001", "Jane Wanjiku", "12345678");
        SavingsAccount savings = new SavingsAccount("M-001-SAVINGS", 0);
        member.addAccount("Savings", savings);

        // Deposit 5000 and print the balance and interest
        savings.deposit(5000);
        System.out.println("Balance : KSh " + savings.getBalance());
        System.out.println("Interest: KSh " + savings.calculateInterest());   // 5% of 5000 = 250

        // Test the custom exception: withdraw more than the balance
        try {
            savings.withdraw(9000);
        } catch (InsufficientFundsException e) {
            System.out.println("TRANSACTION FAILED: " + e.getMessage());
        }
    }
}

