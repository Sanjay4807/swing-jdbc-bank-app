package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model class representing a Bank Account.
 * Encapsulates account properties and joined Customer details.
 */
public class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accountNo;
    private int customerId;
    private String customerName;
    private String customerEmail;
    private String accountType;
    private BigDecimal balance;
    private String status;
    private Timestamp createdAt;

    public Account() {
        this.balance = BigDecimal.ZERO;
        this.status = "ACTIVE";
    }

    public Account(String accountNo, int customerId, String customerName, String customerEmail,
                   String accountType, BigDecimal balance, String status, Timestamp createdAt) {
        this.accountNo = accountNo;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.accountType = accountType;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedBalance() {
        return String.format("$%,.2f", balance != null ? balance.doubleValue() : 0.0);
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountNo='" + accountNo + '\'' +
                ", customerName='" + customerName + '\'' +
                ", accountType='" + accountType + '\'' +
                ", balance=" + getFormattedBalance() +
                '}';
    }
}
