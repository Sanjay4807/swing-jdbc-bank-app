# Mini Banking System Desktop Application

A complete, production-ready desktop banking application built with Java Swing, JDBC, and Oracle Database implementing the 3-Tier MVC (Model-View-Controller) and Data Access Object (DAO) architecture with 3NF Normalized Schema.

---

## 📁 Project Structure

```text
swing-jdbc-bank-app/
├── schema.sql                     # 3NF Oracle SQL DDL script with tables, constraints & seed data
└── src/
    ├── Main.java                  # Main application entry point & Look-and-Feel launcher
    ├── config/
    │   └── DatabaseConnection.java# Thread-Safe Singleton Oracle JDBC Connection manager
    ├── model/
    │   ├── Account.java           # Account & Customer domain model
    │   └── Transaction.java       # Transaction domain model
    ├── dao/
    │   └── BankDAO.java           # Core Banking DAO with strict ACID transaction management
    └── ui/
        ├── LoginFrame.java        # Modern Swing Login screen & DB connection config dialog
        ├── DashboardFrame.java    # Account dashboard, balance card & recent activity JTable
        └── TransactionFrame.java  # Deposit, Withdrawal & Inter-Account Transfer dialog
```

---

## 🗄️ Database Setup (Oracle SQL)

1. Open **Oracle SQL Developer**, **SQL*Plus**, or **Oracle Database Command Line**.
2. Connect to your Oracle Database instance (e.g., `SYSTEM` or your database user).
3. Run `schema.sql`:
   ```sql
   @schema.sql
   ```
4. The script will set up:
   - `CUSTOMERS` table (CustomerID, FullName, Email, PIN)
   - `ACCOUNTS` table (AccountNo, CustomerID, AccountType, Balance >= 0)
   - `TRANSACTIONS` table (TransactionID, AccountNo, Type, Amount, TargetAccountNo, Timestamp)
   - Indexes & Sequences (`SEQ_CUSTOMER_ID`, `SEQ_TRANSACTION_ID`)
   - Pre-populated test accounts:
     - **Account No:** `ACC1001` | **PIN:** `1234` | **Balance:** `$2,500.00`
     - **Account No:** `ACC1002` | **PIN:** `5678` | **Balance:** `$1,200.00`
     - **Account No:** `ACC1003` | **PIN:** `9999` | **Balance:** `$5,000.00`

---

## 🚀 How to Compile and Run

### Step 1: Download Oracle JDBC Driver (ojdbc8.jar / ojdbc11.jar)
Place `ojdbc8.jar` or `ojdbc11.jar` into a `lib` directory (or specify its path when compiling and running).

### Step 2: Compile Java Source Code
```bash
javac -cp "lib/ojdbc8.jar" -d bin src/Main.java src/config/*.java src/model/*.java src/dao/*.java src/ui/*.java
```

### Step 3: Run the Application
```bash
java -cp "bin;lib/ojdbc8.jar" Main
```

*(On Linux/macOS, replace `;` with `:` in the classpath parameter).*

---

## ⚙️ Features & Architecture Highlights

- **3NF Normalized Database**: Strictly organized 3rd Normal Form schema with `CHECK` constraints on non-negative balance and transaction types.
- **Strict ACID Transaction Compliance**:
  - Financial transfers execute under `connection.setAutoCommit(false)`.
  - Atomicity guaranteed via explicit `connection.commit()` and `connection.rollback()` within `catch (SQLException e)`.
- **Thread-Safe Singleton Connection**: Thread-safe `DatabaseConnection` with dynamic configuration dialog built directly into the UI.
- **Asynchronous Swing UI**: Operations run via `SwingWorker` threads to keep the desktop interface responsive.
