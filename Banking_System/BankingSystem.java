import java.sql.*;
import java.util.Scanner;

public class BankingSystem {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/BankDB";
    private static final String DB_USER = "root"; // Replace with your MySQL username
    private static final String DB_PASSWORD = "sai@724gbd/"; // Replace with your MySQL password

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try (Connection connection = connect()) {
            System.out.println("Connected to the database successfully.");

            while (true) {
                System.out.println("\n=== Enhanced Banking System Menu ===");
                System.out.println("1. Create Account");
                System.out.println("2. Deposit Money");
                System.out.println("3. Withdraw Money");
                System.out.println("4. Check Balance");
                System.out.println("5. Transfer Money");
                System.out.println("6. View Transaction History");
                System.out.println("7. Update Account Details");
                System.out.println("8. Delete Account");
                System.out.println("9. Admin View");
                System.out.println("10. Exit");
                System.out.print("Enter your choice: ");
                
                int choice;
                try {
                    choice = Integer.parseInt(scanner.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a valid number.");
                    continue;
                }

                switch (choice) {
                    case 1 -> createAccount(connection, scanner);
                    case 2 -> depositMoney(connection, scanner);
                    case 3 -> withdrawMoney(connection, scanner);
                    case 4 -> checkBalance(connection, scanner);
                    case 5 -> transferMoney(connection, scanner);
                    case 6 -> viewTransactionHistory(connection, scanner);
                    case 7 -> updateAccountDetails(connection, scanner);
                    case 8 -> deleteAccount(connection, scanner);
                    case 9 -> adminView(connection);
                    case 10 -> {
                        System.out.println("Exiting system. Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
        }
    }

    private static void createAccount(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter account holder name: ");
        String name = scanner.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("Account holder name cannot be empty.");
            return;
        }

        String sql = "INSERT INTO Accounts (account_holder_name, balance) VALUES (?, 0.0)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int accountNumber = rs.getInt(1);
                        System.out.println("Account created successfully. Account Number: " + accountNumber);
                    }
                }
            }
        }
    }

    private static void depositMoney(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter account number: ");
        int accountNumber = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter amount to deposit: ");
        double amount = Double.parseDouble(scanner.nextLine());

        if (amount <= 0) {
            System.out.println("Amount must be greater than zero.");
            return;
        }

        String sql = "UPDATE Accounts SET balance = balance + ? WHERE account_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setInt(2, accountNumber);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logTransaction(connection, accountNumber, "Deposit", amount);
                System.out.println("Deposit successful.");
            } else {
                System.out.println("Account not found.");
            }
        }
    }

    private static void withdrawMoney(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter account number: ");
        int accountNumber = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter amount to withdraw: ");
        double amount = Double.parseDouble(scanner.nextLine());

        if (amount <= 0) {
            System.out.println("Amount must be greater than zero.");
            return;
        }

        String checkSql = "SELECT balance FROM Accounts WHERE account_number = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, accountNumber);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    double balance = rs.getDouble("balance");
                    if (balance >= amount) {
                        String updateSql = "UPDATE Accounts SET balance = balance - ? WHERE account_number = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                            updateStmt.setDouble(1, amount);
                            updateStmt.setInt(2, accountNumber);
                            updateStmt.executeUpdate();
                            logTransaction(connection, accountNumber, "Withdraw", -amount);
                            System.out.println("Withdrawal successful.");
                        }
                    } else {
                        System.out.println("Insufficient balance.");
                    }
                } else {
                    System.out.println("Account not found.");
                }
            }
        }
    }

    private static void checkBalance(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter account number: ");
        int accountNumber = Integer.parseInt(scanner.nextLine());

        String sql = "SELECT balance FROM Accounts WHERE account_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, accountNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double balance = rs.getDouble("balance");
                    System.out.println("Account balance: " + balance);
                } else {
                    System.out.println("Account not found.");
                }
            }
        }
    }

    private static void transferMoney(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter your account number: ");
        int senderAccount = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter recipient account number: ");
        int recipientAccount = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter amount to transfer: ");
        double amount = Double.parseDouble(scanner.nextLine());

        if (amount <= 0) {
            System.out.println("Amount must be greater than zero.");
            return;
        }

        connection.setAutoCommit(false); // Start transaction
        try {
            String checkBalanceSql = "SELECT balance FROM Accounts WHERE account_number = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkBalanceSql)) {
                checkStmt.setInt(1, senderAccount);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getDouble("balance") >= amount) {
                        try (PreparedStatement deductStmt = connection.prepareStatement(
                                "UPDATE Accounts SET balance = balance - ? WHERE account_number = ?")) {
                            deductStmt.setDouble(1, amount);
                            deductStmt.setInt(2, senderAccount);
                            deductStmt.executeUpdate();
                        }

                        try (PreparedStatement addStmt = connection.prepareStatement(
                                "UPDATE Accounts SET balance = balance + ? WHERE account_number = ?")) {
                            addStmt.setDouble(1, amount);
                            addStmt.setInt(2, recipientAccount);
                            addStmt.executeUpdate();
                        }

                        logTransaction(connection, senderAccount, "Transfer to " + recipientAccount, -amount);
                        logTransaction(connection, recipientAccount, "Transfer from " + senderAccount, amount);
                        connection.commit();
                        System.out.println("Transfer successful.");
                    } else {
                        System.out.println("Insufficient balance or sender account not found.");
                        connection.rollback();
                    }
                }
            }
        } catch (SQLException e) {
            connection.rollback();
            System.out.println("Transfer failed: " + e.getMessage());
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static void viewTransactionHistory(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter account number: ");
        int accountNumber = Integer.parseInt(scanner.nextLine());

        String sql = "SELECT * FROM Transactions WHERE account_number = ? ORDER BY transaction_date DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, accountNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println("Transaction History:");
                while (rs.next()) {
                    System.out.printf("Date: %s | Type: %s | Amount: %.2f%n",
                            rs.getString("transaction_date"),
                            rs.getString("transaction_type"),
                            rs.getDouble("amount"));
                }
            }
        }
    }

    private static void updateAccountDetails(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter account number: ");
        int accountNumber = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter new account holder name: ");
        String newName = scanner.nextLine().trim();

        if (newName.isEmpty()) {
            System.out.println("Account holder name cannot be empty.");
            return;
        }

        String sql = "UPDATE Accounts SET account_holder_name = ? WHERE account_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setInt(2, accountNumber);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Account updated successfully.");
            } else {
                System.out.println("Account not found.");
            }
        }
    }

    private static void deleteAccount(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter account number: ");
        int accountNumber = Integer.parseInt(scanner.nextLine());

        String sql = "DELETE FROM Accounts WHERE account_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, accountNumber);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Account deleted successfully.");
            } else {
                System.out.println("Account not found.");
            }
        }
    }

    private static void adminView(Connection connection) throws SQLException {
        String sql = "SELECT * FROM Accounts";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("Admin View - All Accounts:");
            while (rs.next()) {
                System.out.printf("Account Number: %d | Name: %s | Balance: %.2f%n",
                        rs.getInt("account_number"),
                        rs.getString("account_holder_name"),
                        rs.getDouble("balance"));
            }
        }
    }

    private static void logTransaction(Connection connection, int accountNumber, String type, double amount) throws SQLException {
        String sql = "INSERT INTO Transactions (account_number, transaction_type, amount, transaction_date) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, accountNumber);
            stmt.setString(2, type);
            stmt.setDouble(3, amount);
            stmt.executeUpdate();
        }
    }
}
 
//  The above code is a simple banking system that allows users to create accounts, deposit money, withdraw money, check balance, transfer money, view transaction history, update account details, delete accounts, and view all accounts (admin view). 
//  The program uses a MySQL database to store account information and transaction history. The database schema consists of two tables:  Accounts  and  Transactions . 
//  The  Accounts  table has the following columns: 
//  account_number  (int, primary key) account_holder_name  (varchar) balance  (double) 
//  The  Transactions  table has the following columns: 
//  transaction_id  (int, primary key) account_number  (int, foreign key to  Accounts ) transaction_type  (varchar) amount  (double) transaction_date  (datetime) 
//  The program uses JDBC to connect to the MySQL database and perform various operations. The  connect()  method establishes a connection to the database using the JDBC URL, username, and password. 
//  The main method displays a menu with various options for the user to choose from. Depending on the user's choice, the program executes the corresponding operation. 
//  The program includes methods for creating accounts, depositing money, withdrawing money, transferring money, viewing transaction history, updating account details, deleting accounts, and viewing all accounts (admin view). 
//  The  logTransaction()  method is used to log transactions in the  Transactions  table whenever a deposit, withdrawal, or transfer operation is performed. 
//  The program uses prepared statements to execute SQL queries and handle user input safely. It also includes exception handling to catch and display any errors that occur during database operations. 
//  To run the program, you need to have a MySQL database set up with the appropriate schema and tables. You can create the required database and tables using the following SQL commands: 
//  CREATE DATABASE BankDB;