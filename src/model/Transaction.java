package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Model class representing a financial Transaction.
 */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private long transactionId;
    private String accountNo;
    private String transactionType; // 'DEPOSIT', 'WITHDRAWAL', 'TRANSFER'
    private BigDecimal amount;
    private String targetAccountNo;
    private String description;
    private Timestamp timestamp;

    public Transaction() {
    }

    public Transaction(long transactionId, String accountNo, String transactionType,
                       BigDecimal amount, String targetAccountNo, String description, Timestamp timestamp) {
        this.transactionId = transactionId;
        this.accountNo = accountNo;
        this.transactionType = transactionType;
        this.amount = amount;
        this.targetAccountNo = targetAccountNo;
        this.description = description;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTargetAccountNo() {
        return targetAccountNo;
    }

    public void setTargetAccountNo(String targetAccountNo) {
        this.targetAccountNo = targetAccountNo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedAmount() {
        return String.format("$%,.2f", amount != null ? amount.doubleValue() : 0.0);
    }

    public String getFormattedTimestamp() {
        if (timestamp == null) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(timestamp);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + transactionId +
                ", accountNo='" + accountNo + '\'' +
                ", type='" + transactionType + '\'' +
                ", amount=" + getFormattedAmount() +
                ", timestamp=" + getFormattedTimestamp() +
                '}';
    }
}
