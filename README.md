# Booking and Inventory Management System

## Description

A Java desktop application developed for a small cake and events business to manage customer bookings, inventory, products, suppliers, purchase orders and customer deposits.

The system includes checks to prevent duplicate active bookings, automatically calculates stock requirements from product recipes, adjusts inventory when bookings are confirmed or cancelled, and displays low-stock warnings. It aims to reduce manual record keeping, minimise errors and improve the management of day-to-day business operations.

The application also supports reviewing booking requests submitted through a separate customer-facing website. The website and backend are maintained in separate projects and are not included in this repository.

## Features

- **Bookings and requests:** create and manage bookings, and review requests submitted through the website, with a request history
- **Customers:** store customer records, with a data retention screen for managing old records
- **Products and recipes:** manage products and the ingredients each one uses
- **Inventory:** track stock and batches, with expiry and waste recording and low-stock warnings
- **Suppliers and purchase orders:** manage suppliers and the orders placed with them
- **Security:** login with hashed passwords and a change-password screen

## Built with

- **Java 17**: main language for the desktop application and its business logic
- **JavaFX 21 and FXML**: the graphical interface, including forms, tables, buttons and navigation, with styling applied inline in the FXML files
- **SQLite and JDBC**: relational database storing customers, bookings, products, inventory and suppliers, accessed from Java using JDBC
- **Jakarta Mail**: sends deposit, confirmation and rejection emails to customers
- **JUnit 5**: automated tests for application functionality and business rules
- **Maven**: manages dependencies, compiles the project, runs tests and launches the app

**Tools:** Visual Studio Code, DB Browser for SQLite

## Getting started

### Requirements

- Java 17 or later
- Maven

### Run the application

```
git clone https://github.com/IJ4CKO/booking-system.git
cd booking-system
mvn javafx:run
```

The first run downloads the libraries, so it can take a few minutes. You may see some JavaFX and SQLite warnings in the terminal. These are harmless.

On the first run, the app creates a SQLite database file called `booking_system.db` in the folder it is run from. The database file is not included in this repository.

### First login

The first time the app runs, it creates an administrator account with the username `admin`. By default it generates a random password and prints it in the terminal, in a line starting `Created the 'admin' account with a generated password:`. Use it to sign in, then change it from the change-password screen. The password is only shown on the first run.

To choose your own password instead, set the `BOOKINGAPP_ADMIN_PASSWORD` environment variable before the first start, and nothing will be printed.

### Optional: email

Emails to customers are sent through Gmail. The email password is read from the `BOOKING_APP_EMAIL_PASSWORD` environment variable and is never stored in the code. To send emails, set that variable and change `EMAIL_USERNAME` in `EmailService.java` to your own Gmail address.

### Run the tests

```
mvn test
```

## Project structure

- `dao`: database access (JDBC)
- `model`: data classes
- `service`: business logic, including deposits, bookings and email
- `security`: password hashing and user sessions
- `ui`: JavaFX controllers, with FXML screens in `resources`