CREATE DATABASE BankDB;
USE BankDB;

CREATE TABLE Accounts (
    account_number INT AUTO_INCREMENT PRIMARY KEY,
    account_holder_name VARCHAR(100) NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00
);

CREATE TABLE Transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    account_number INT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_number) REFERENCES Accounts(account_number) ON DELETE CASCADE
);