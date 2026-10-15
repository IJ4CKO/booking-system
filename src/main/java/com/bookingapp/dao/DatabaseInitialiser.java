package com.bookingapp.dao;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Base64;

import com.bookingapp.security.PasswordHasher;

/**
 * Creates and brings up to date the SQLite schema the application runs on.
 */
public class DatabaseInitialiser {

    /**
     * Prevents instantiation; every member of this class is static.
     */
    private DatabaseInitialiser() {
        // Prevent this utility class from being instantiated.
    }

    /**
     * Builds the schema, applies any outstanding migrations, and seeds the
     * development records.
     */
    public static void initialise() {

        /*
         * Staff logins for the admin screens.
         */
        String ownerUserTable = """
            CREATE TABLE IF NOT EXISTS owner_user (
                owner_user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                active INTEGER NOT NULL
            );
            """;

        /*
         * People who place bookings. Contact details are optional, so
         * that a customer can be taken down over the counter with
         * nothing but a name.
         */
        String customerTable = """
            CREATE TABLE IF NOT EXISTS customer (
                customer_id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                phone TEXT,
                email TEXT
            );
            """;

        /*
         * The catalogue of things a customer can order.
         */
        String productsTable = """
            CREATE TABLE IF NOT EXISTS products (
                product_id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                active INTEGER NOT NULL
            );
            """;

        /*
         * Raw materials held in stock, with the level at which they
         * should be reordered.
         */
        String inventoryTable = """
            CREATE TABLE IF NOT EXISTS inventory_items (
                inventory_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                unit TEXT NOT NULL,
                current_quantity REAL NOT NULL,
                reorder_level REAL NOT NULL,
                active INTEGER NOT NULL
            );
            """;

        /*
         * Businesses that stock is bought from.
         */
        String supplierTable = """
            CREATE TABLE IF NOT EXISTS supplier (
                supplier_id INTEGER PRIMARY KEY AUTOINCREMENT,
                supplier_name TEXT NOT NULL,
                contact_name TEXT,
                phone TEXT,
                email TEXT,
                notes TEXT
            );
            """;

        /*
         * Individual deliveries of a stock item. Tracking quantity
         * received alongside quantity remaining is what allows stock to
         * be consumed oldest-first and waste to be costed against the
         * batch it came from.
         */
        String inventoryBatchTable = """
            CREATE TABLE IF NOT EXISTS inventory_batch (
                batch_id INTEGER PRIMARY KEY AUTOINCREMENT,
                inventory_item_id INTEGER NOT NULL,
                supplier_id INTEGER,
                batch_code TEXT,
                purchase_date TEXT NOT NULL,
                expiry_date TEXT,
                quantity_received REAL NOT NULL,
                quantity_remaining REAL NOT NULL,
                unit_cost REAL NOT NULL,
 
                FOREIGN KEY (inventory_item_id)
                    REFERENCES inventory_items(
                        inventory_item_id
                    ),
 
                FOREIGN KEY (supplier_id)
                    REFERENCES supplier(supplier_id)
            );
            """;

        /*
         * Orders placed with a supplier, and the items on them.
         */
        String purchaseOrderTable = """
            CREATE TABLE IF NOT EXISTS purchase_order (
                purchase_order_id INTEGER
                    PRIMARY KEY AUTOINCREMENT,
                supplier_id INTEGER NOT NULL,
                order_date TEXT NOT NULL,
                expected_delivery_date TEXT,
                status TEXT NOT NULL,
                total_cost REAL,
                notes TEXT,
 
                FOREIGN KEY (supplier_id)
                    REFERENCES supplier(supplier_id)
            );
            """;

        String purchaseOrderLineTable = """
            CREATE TABLE IF NOT EXISTS purchase_order_line (
                purchase_order_line_id INTEGER
                    PRIMARY KEY AUTOINCREMENT,
                purchase_order_id INTEGER NOT NULL,
                inventory_item_id INTEGER NOT NULL,
                quantity_ordered REAL NOT NULL,
                unit_cost REAL NOT NULL,
 
                FOREIGN KEY (purchase_order_id)
                    REFERENCES purchase_order(
                        purchase_order_id
                    ),
 
                FOREIGN KEY (inventory_item_id)
                    REFERENCES inventory_items(
                        inventory_item_id
                    )
            );
            """;

        /*
         * Confirmed bookings and the products on them. A booking
         * belongs to a customer and to the member of staff who took it.
         */
        String bookingsTable = """
            CREATE TABLE IF NOT EXISTS bookings (
                booking_id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id INTEGER NOT NULL,
                owner_user_id INTEGER NOT NULL,
                booking_date TEXT NOT NULL,
                booking_time TEXT NOT NULL,
                dropoff_date TEXT,
                dropoff_time TEXT,
                dropoff_address TEXT,
                items_required_text TEXT,
                status TEXT NOT NULL,
 
                FOREIGN KEY (customer_id)
                    REFERENCES customer(customer_id),
 
                FOREIGN KEY (owner_user_id)
                    REFERENCES owner_user(owner_user_id)
            );
            """;

        String bookingLineTable = """
            CREATE TABLE IF NOT EXISTS booking_line (
                booking_line_id INTEGER
                    PRIMARY KEY AUTOINCREMENT,
                booking_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL,
 
                FOREIGN KEY (booking_id)
                    REFERENCES bookings(booking_id),
 
                FOREIGN KEY (product_id)
                    REFERENCES products(product_id)
            );
            """;

        /*
         * How much of each stock item one unit of a product consumes.
         * This is what connects a booking to the stock it uses up.
         */
        String recipeItemsTable = """
            CREATE TABLE IF NOT EXISTS recipe_items (
                recipe_item_id INTEGER
                    PRIMARY KEY AUTOINCREMENT,
                product_id INTEGER NOT NULL,
                inventory_item_id INTEGER NOT NULL,
                quantity_required_per_unit REAL NOT NULL,
 
                FOREIGN KEY (product_id)
                    REFERENCES products(product_id),
 
                FOREIGN KEY (inventory_item_id)
                    REFERENCES inventory_items(
                        inventory_item_id
                    )
            );
            """;

        /*
         * Enquiries submitted by customers, before they become
         * bookings. The product_id and quantity columns are the older
         * single-product fields, kept for requests taken before the
         * request lines table existed. booking_id is filled in once the
         * request is confirmed, and is unique so that one request
         * cannot produce two bookings.
         */
        String bookingRequestTable = """
            CREATE TABLE IF NOT EXISTS booking_request (
                request_id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL,
                phone TEXT NOT NULL,
                service_type TEXT,
                fulfilment_type TEXT,
                dropoff_address TEXT,
                product_id INTEGER,
                quantity INTEGER,
                event_date TEXT NOT NULL,
                event_time TEXT,
                details TEXT NOT NULL,
                source TEXT NOT NULL DEFAULT 'website',
                status TEXT NOT NULL DEFAULT 'PENDING_REVIEW',
                submitted_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                reject_reason TEXT,
                order_total NUMERIC,
                required_deposit NUMERIC,
                deposit_status TEXT NOT NULL
                    DEFAULT 'NOT_REQUESTED',
                deposit_requested_date TEXT,
                deposit_paid_date TEXT,
                payment_method TEXT NOT NULL
                    DEFAULT 'BANK_TRANSFER',
                payment_reference TEXT,
                booking_id INTEGER UNIQUE,
 
                FOREIGN KEY (product_id)
                    REFERENCES products(product_id),
 
                FOREIGN KEY (booking_id)
                    REFERENCES bookings(booking_id)
            );
            """;

        /*
         * The items on a request. Products are recorded by the name the
         * customer typed rather than by identifier, since a request may
         * name something not in the catalogue.
         */
        String bookingRequestLineTable = """
            CREATE TABLE IF NOT EXISTS booking_request_line (
                request_line_id INTEGER
                    PRIMARY KEY AUTOINCREMENT,
                request_id INTEGER NOT NULL,
                service_type TEXT NOT NULL,
                product_name TEXT,
                quantity INTEGER,
 
                FOREIGN KEY (request_id)
                    REFERENCES booking_request(request_id)
            );
            """;

        /*
         * An audit trail of every change in stock levels. A negative
         * quantity_change is stock going out; the booking_id records
         * what it went out for, where applicable.
         */
        String stockMovementTable = """
            CREATE TABLE IF NOT EXISTS stock_movement (
                stock_movement_id INTEGER
                    PRIMARY KEY AUTOINCREMENT,
                inventory_item_id INTEGER NOT NULL,
                movement_time TEXT NOT NULL,
                quantity_change REAL NOT NULL,
                booking_id INTEGER,
                movement_type TEXT NOT NULL,
 
                FOREIGN KEY (inventory_item_id)
                    REFERENCES inventory_items(
                        inventory_item_id
                    ),
 
                FOREIGN KEY (booking_id)
                    REFERENCES bookings(booking_id)
            );
            """;

        /*
         * Which batch each withdrawal of stock came from, with the cost
         * it was bought at, so that a booking can be costed using the
         * price actually paid rather than today's price.
         */
        String stockBatchUsageTable = """
            CREATE TABLE IF NOT EXISTS stock_batch_usage (
                batch_usage_id INTEGER
                    PRIMARY KEY AUTOINCREMENT,
                batch_id INTEGER NOT NULL,
                booking_id INTEGER,
                inventory_item_id INTEGER NOT NULL,
                quantity_used REAL NOT NULL,
                unit_cost REAL NOT NULL,
                usage_date TEXT NOT NULL,
 
                FOREIGN KEY (batch_id)
                    REFERENCES inventory_batch(batch_id),
 
                FOREIGN KEY (booking_id)
                    REFERENCES bookings(booking_id),
 
                FOREIGN KEY (inventory_item_id)
                    REFERENCES inventory_items(
                        inventory_item_id
                    )
            );
            """;

        /*
         * Stock written off rather than used, with the reason and the
         * value lost. The CHECK constraints keep a nonsensical write-off
         * out of the reporting figures.
         */
        String inventoryWasteTable = """
            CREATE TABLE IF NOT EXISTS inventory_waste (
                waste_id INTEGER PRIMARY KEY AUTOINCREMENT,
                batch_id INTEGER NOT NULL,
                inventory_item_id INTEGER NOT NULL,
                waste_date TEXT NOT NULL,
 
                quantity_wasted REAL NOT NULL
                    CHECK (quantity_wasted > 0),
 
                reason TEXT NOT NULL,
 
                unit_cost REAL NOT NULL
                    CHECK (unit_cost >= 0),
 
                waste_value REAL NOT NULL
                    CHECK (waste_value >= 0),
 
                notes TEXT,
 
                FOREIGN KEY (batch_id)
                    REFERENCES inventory_batch(batch_id),
 
                FOREIGN KEY (inventory_item_id)
                    REFERENCES inventory_items(
                        inventory_item_id
                    )
            );
            """;

                    /*
         * Records customer anonymisation actions for accountability.
         *
         * Personal details are deliberately not stored in this table because
         * keeping them would undermine the purpose of anonymisation.
         */
        String dataRetentionLogTable = """
            CREATE TABLE IF NOT EXISTS data_retention_log (
                retention_log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id INTEGER NOT NULL,
                action_taken TEXT NOT NULL,
                action_date TEXT NOT NULL,
                notes TEXT,

                FOREIGN KEY (customer_id)
                    REFERENCES customer(customer_id)
            );
            """;

        /*
         * Schema migrations for databases created by earlier
         * versions of the application.
         *
         * These are only needed by an existing database: a database
         * built from the definitions above already has every column,
         * and each of these statements will simply be ignored.
         */
        String addItemsRequiredTextToBookings = """
            ALTER TABLE bookings
            ADD COLUMN items_required_text TEXT;
            """;

        String addProductIdToBookingRequest = """
            ALTER TABLE booking_request
            ADD COLUMN product_id INTEGER;
            """;

        String addQuantityToBookingRequest = """
            ALTER TABLE booking_request
            ADD COLUMN quantity INTEGER;
            """;

        String addFulfilmentTypeToBookingRequest = """
            ALTER TABLE booking_request
            ADD COLUMN fulfilment_type TEXT;
            """;

        String addDropoffAddressToBookingRequest = """
            ALTER TABLE booking_request
            ADD COLUMN dropoff_address TEXT;
            """;

        String addRejectReasonToBookingRequest = """
            ALTER TABLE booking_request
            ADD COLUMN reject_reason TEXT;
            """;

        /*
         * The deposit workflow columns, added together when deposits
         * were introduced.
         */
        String addOrderTotalToBookingRequest = """
            ALTER TABLE booking_request
            ADD COLUMN order_total NUMERIC;
            """;

        String addRequiredDepositToBookingRequest = """
            ALTER TABLE booking_request
            ADD COLUMN required_deposit NUMERIC;
            """;

        String addDepositStatusToBookingRequest = """
            ALTER TABLE booking_request
            ADD COLUMN deposit_status TEXT NOT NULL
                DEFAULT 'NOT_REQUESTED';
            """;

        String addDepositRequestedDateToBookingRequest = """
            ALTER TABLE booking_request
            ADD COLUMN deposit_requested_date TEXT;
            """;

        String addDepositPaidDateToBookingRequest = """
            ALTER TABLE booking_request
            ADD COLUMN deposit_paid_date TEXT;
            """;

        String addPaymentMethodToBookingRequest = """
            ALTER TABLE booking_request
            ADD COLUMN payment_method TEXT NOT NULL
                DEFAULT 'BANK_TRANSFER';
            """;

        String addPaymentReferenceToBookingRequest = """
            ALTER TABLE booking_request
            ADD COLUMN payment_reference TEXT;
            """;

        String addBookingIdToBookingRequest = """
            ALTER TABLE booking_request
            ADD COLUMN booking_id INTEGER
                REFERENCES bookings(booking_id);
            """;

        /*
         * Delivery details, added when bookings stopped being
         * collection-only.
         */
        String addDropoffDateToBookings = """
            ALTER TABLE bookings
            ADD COLUMN dropoff_date TEXT;
            """;

        String addDropoffTimeToBookings = """
            ALTER TABLE bookings
            ADD COLUMN dropoff_time TEXT;
            """;

        String addDropoffAddressToBookings = """
            ALTER TABLE bookings
            ADD COLUMN dropoff_address TEXT;
            """;

        /*
         * Purchasing and unit-conversion details on stock items. The
         * purchase and recipe units allow stock to be bought in one
         * measure and consumed in another, with conversion_factor
         * bridging the two.
         */
        String addPreferredSupplierToInventory = """
            ALTER TABLE inventory_items
            ADD COLUMN preferred_supplier_id INTEGER;
            """;

        String addDefaultUnitCostToInventory = """
            ALTER TABLE inventory_items
            ADD COLUMN default_unit_cost REAL;
            """;

        String addReorderQuantityToInventory = """
            ALTER TABLE inventory_items
            ADD COLUMN reorder_quantity REAL;
            """;

        String addPurchaseUnitToInventory = """
            ALTER TABLE inventory_items
            ADD COLUMN purchase_unit TEXT;
            """;

        String addRecipeUnitToInventory = """
            ALTER TABLE inventory_items
            ADD COLUMN recipe_unit TEXT;
            """;

        String addConversionFactorToInventory = """
            ALTER TABLE inventory_items
            ADD COLUMN conversion_factor REAL DEFAULT 1.0;
            """;

        String addNotesToStockMovement = """
            ALTER TABLE stock_movement
            ADD COLUMN notes TEXT;
            """;

        /*
         * The request-to-booking relationship must be unique.
         *
         * A request can create no more than one confirmed booking,
         * and a booking can be linked to no more than one request.
         */
        String createBookingRequestBookingIndex = """
            CREATE UNIQUE INDEX IF NOT EXISTS
                idx_booking_request_booking_id
            ON booking_request (booking_id)
            WHERE booking_id IS NOT NULL;
            """;

        /*
         * The remaining unique indexes back up rules the DAO classes
         * also check in Java. Enforcing them here as well means two
         * requests arriving at once cannot slip past a check that
         * passed for both.
         *
         * Cancelled bookings are excluded, so a slot that has been
         * given up can be booked again.
         */
        String createUniqueActiveBookingDateTimeIndex = """
            CREATE UNIQUE INDEX IF NOT EXISTS
                idx_booking_unique_customer_datetime_active
            ON bookings (
                customer_id,
                booking_date,
                booking_time
            )
            WHERE status <> 'CANCELLED';
            """;

        /*
         * One line per product per booking, which is what makes the
         * quantity top-up in BookingLineDAO the only way to add more of
         * something already on a booking.
         */
        String createUniqueBookingLineProductIndex = """
            CREATE UNIQUE INDEX IF NOT EXISTS
                idx_booking_line_unique_booking_product
            ON booking_line (
                booking_id,
                product_id
            );
            """;

        String createUniqueRecipeItemProductInventoryIndex = """
            CREATE UNIQUE INDEX IF NOT EXISTS
                idx_recipe_item_unique_product_inventory
            ON recipe_items (
                product_id,
                inventory_item_id
            );
            """;

        /*
         * Contact details identify a returning customer, so each must
         * belong to one person only. Customers with no email address or
         * no telephone number are excluded rather than colliding with
         * one another.
         */
        String createUniqueCustomerEmailIndex = """
            CREATE UNIQUE INDEX IF NOT EXISTS
                idx_customer_unique_email
            ON customer (LOWER(TRIM(email)))
            WHERE email IS NOT NULL
            AND TRIM(email) <> '';
            """;

        String createUniqueCustomerPhoneIndex = """
            CREATE UNIQUE INDEX IF NOT EXISTS
                idx_customer_unique_phone
            ON customer (TRIM(phone))
            WHERE phone IS NOT NULL
            AND TRIM(phone) <> '';
            """;

        /*
         * Convert booking-request statuses used by earlier
         * versions of the system.
         */
        String migratePendingRequestStatuses = """
            UPDATE booking_request
            SET status = 'PENDING_REVIEW'
            WHERE UPPER(TRIM(status)) = 'PENDING';
            """;

        String migrateDeclinedRequestStatuses = """
            UPDATE booking_request
            SET status = 'REJECTED'
            WHERE UPPER(TRIM(status)) = 'DECLINED';
            """;

        /*
         * Requests that were already awaiting a deposit when the
         * deposit columns were introduced have a status but no deposit
         * details, so the missing details are filled in to match.
         */
        String migrateAwaitingDepositStatuses = """
            UPDATE booking_request
            SET
                deposit_status = 'PENDING',
                payment_method = 'BANK_TRANSFER'
            WHERE status = 'AWAITING_DEPOSIT'
              AND (
                  deposit_status IS NULL
                  OR UPPER(TRIM(deposit_status))
                     = 'NOT_REQUESTED'
              );
            """;

        /*
         * Fill in values that are declared NOT NULL but may be missing
         * on rows written before the column existed. The code reads
         * these without null checks, so they must not be left empty.
         */
        String ensureDepositStatusDefaults = """
            UPDATE booking_request
            SET deposit_status = 'NOT_REQUESTED'
            WHERE deposit_status IS NULL
               OR TRIM(deposit_status) = '';
            """;

        String ensurePaymentMethodDefaults = """
            UPDATE booking_request
            SET payment_method = 'BANK_TRANSFER'
            WHERE payment_method IS NULL
               OR TRIM(payment_method) = '';
            """;

        /*
         * Stock items predating the unit-conversion columns are assumed
         * to be bought and used in the same measure, so both units fall
         * back to the original unit and the factor to one.
         */
        String ensurePurchaseUnitDefaults = """
            UPDATE inventory_items
            SET purchase_unit = unit
            WHERE purchase_unit IS NULL
               OR TRIM(purchase_unit) = '';
            """;

        String ensureRecipeUnitDefaults = """
            UPDATE inventory_items
            SET recipe_unit = unit
            WHERE recipe_unit IS NULL
               OR TRIM(recipe_unit) = '';
            """;

        String ensureConversionFactorDefaults = """
            UPDATE inventory_items
            SET conversion_factor = 1.0
            WHERE conversion_factor IS NULL
               OR conversion_factor <= 0;
            """;

        /*
         * Original development records.
         *
         * INSERT OR IGNORE with a fixed identifier means these are
         * added once and never duplicated or overwritten.
         */
        String insertCustomer = """
            INSERT OR IGNORE INTO customer (
                customer_id,
                full_name,
                phone,
                email
            )
            VALUES (
                1,
                'Test Customer',
                '07123456789',
                'test@example.com'
            );
            """;

        try (Connection conn
                = DBConnection.getConnection(); Statement stmt
                = conn.createStatement()) {

            /*
             * SQLite leaves foreign keys unenforced unless asked, and
             * the setting applies to this connection alone.
             */
            stmt.execute("PRAGMA foreign_keys = ON;");

            /*
             * Create the application tables.
             *
             * Ordered so that a table is created after anything it
             * refers to.
             */
            stmt.execute(ownerUserTable);
            stmt.execute(customerTable);
            stmt.execute(productsTable);
            stmt.execute(inventoryTable);
            stmt.execute(supplierTable);
            stmt.execute(inventoryBatchTable);
            stmt.execute(purchaseOrderTable);
            stmt.execute(purchaseOrderLineTable);
            stmt.execute(bookingsTable);
            stmt.execute(bookingLineTable);
            stmt.execute(recipeItemsTable);
            stmt.execute(bookingRequestTable);
            stmt.execute(bookingRequestLineTable);
            stmt.execute(stockMovementTable);
            stmt.execute(stockBatchUsageTable);
            stmt.execute(inventoryWasteTable);
            stmt.execute(dataRetentionLogTable);

            /*
             * Apply optional column migrations.
             *
             * Each migration is ignored when the column already
             * exists, allowing the application to start normally.
             */
            executeOptionalMigration(
                    stmt,
                    addItemsRequiredTextToBookings
            );

            executeOptionalMigration(
                    stmt,
                    addProductIdToBookingRequest
            );

            executeOptionalMigration(
                    stmt,
                    addQuantityToBookingRequest
            );

            executeOptionalMigration(
                    stmt,
                    addFulfilmentTypeToBookingRequest
            );

            executeOptionalMigration(
                    stmt,
                    addDropoffAddressToBookingRequest
            );

            executeOptionalMigration(
                    stmt,
                    addRejectReasonToBookingRequest
            );

            executeOptionalMigration(
                    stmt,
                    addOrderTotalToBookingRequest
            );

            executeOptionalMigration(
                    stmt,
                    addRequiredDepositToBookingRequest
            );

            executeOptionalMigration(
                    stmt,
                    addDepositStatusToBookingRequest
            );

            executeOptionalMigration(
                    stmt,
                    addDepositRequestedDateToBookingRequest
            );

            executeOptionalMigration(
                    stmt,
                    addDepositPaidDateToBookingRequest
            );

            executeOptionalMigration(
                    stmt,
                    addPaymentMethodToBookingRequest
            );

            executeOptionalMigration(
                    stmt,
                    addPaymentReferenceToBookingRequest
            );

            executeOptionalMigration(
                    stmt,
                    addBookingIdToBookingRequest
            );

            executeOptionalMigration(
                    stmt,
                    addDropoffDateToBookings
            );

            executeOptionalMigration(
                    stmt,
                    addDropoffTimeToBookings
            );

            executeOptionalMigration(
                    stmt,
                    addDropoffAddressToBookings
            );

            executeOptionalMigration(
                    stmt,
                    addPreferredSupplierToInventory
            );

            executeOptionalMigration(
                    stmt,
                    addDefaultUnitCostToInventory
            );

            executeOptionalMigration(
                    stmt,
                    addReorderQuantityToInventory
            );

            executeOptionalMigration(
                    stmt,
                    addPurchaseUnitToInventory
            );

            executeOptionalMigration(
                    stmt,
                    addRecipeUnitToInventory
            );

            executeOptionalMigration(
                    stmt,
                    addConversionFactorToInventory
            );

            executeOptionalMigration(
                    stmt,
                    addNotesToStockMovement
            );

            /*
             * Create the unique request-to-booking index after
             * the booking_id column has been added.
             */
            stmt.execute(
                    createBookingRequestBookingIndex
            );

            /*
             * The remaining indexes are attempted optionally, since
             * creating a unique index over a table that already holds
             * duplicates will fail on an older database.
             */
            executeOptionalMigration(
                    stmt,
                    createUniqueCustomerEmailIndex
            );

            executeOptionalMigration(
                    stmt,
                    createUniqueCustomerPhoneIndex
            );

            executeOptionalMigration(
                    stmt,
                    createUniqueActiveBookingDateTimeIndex
            );

            executeOptionalMigration(
                    stmt,
                    createUniqueBookingLineProductIndex
            );

            executeOptionalMigration(
                    stmt,
                    createUniqueRecipeItemProductInventoryIndex
            );

            /*
             * Update data stored by previous versions.
             *
             * Statuses are converted first, so that the deposit
             * back-fill below sees the current status names.
             */
            stmt.executeUpdate(
                    migratePendingRequestStatuses
            );

            stmt.executeUpdate(
                    migrateDeclinedRequestStatuses
            );

            stmt.executeUpdate(
                    ensureDepositStatusDefaults
            );

            stmt.executeUpdate(
                    ensurePaymentMethodDefaults
            );

            stmt.executeUpdate(
                    ensurePurchaseUnitDefaults
            );

            stmt.executeUpdate(
                    ensureRecipeUnitDefaults
            );

            stmt.executeUpdate(
                    ensureConversionFactorDefaults
            );

            stmt.executeUpdate(
                    migrateAwaitingDepositStatuses
            );

            /*
             * Add development records only when they do not
             * already exist.
             */
            seedAdminUser(conn);

            stmt.executeUpdate(insertCustomer);

        } catch (Exception e) {
            System.err.println(
                    "Unable to initialise the database: "
                    + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    /**
     * Creates the initial administrator account if it is not already there.
     */
    private static void seedAdminUser(
            Connection conn
    ) throws Exception {

        String sql = """
            INSERT OR IGNORE INTO owner_user (
                owner_user_id,
                username,
                password_hash,
                active
            )
            VALUES (1, 'admin', ?, 1);
            """;

        String configuredPassword
                = System.getenv("BOOKINGAPP_ADMIN_PASSWORD");

        boolean generated
                = configuredPassword == null
                || configuredPassword.isBlank();

        String initialPassword
                = generated
                        ? generateInitialPassword()
                        : configuredPassword;

        try (PreparedStatement statement
                = conn.prepareStatement(sql)) {

            statement.setString(
                    1,
                    PasswordHasher.hashPassword(initialPassword)
            );

            int rowsInserted
                    = statement.executeUpdate();

            /*
             * Only announce the password when the account was
             * actually created, and only when we invented it.
             */
            if (rowsInserted == 1 && generated) {
                System.out.println(
                        "Created the 'admin' account with a "
                        + "generated password: "
                        + initialPassword
                );

                System.out.println(
                        "Sign in and change it, or set "
                        + "BOOKINGAPP_ADMIN_PASSWORD "
                        + "before first start."
                );
            }
        }
    }

    /**
     * Produces a random password for a newly created administrator account.
     */
    private static String generateInitialPassword() {

        byte[] randomBytes = new byte[12];

        new SecureRandom().nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    /**
     * Runs a statement that is expected to fail harmlessly on a database where
     * it has already been applied.
     */
    private static void executeOptionalMigration(
            Statement statement,
            String migrationSql
    ) {
        try {
            statement.execute(migrationSql);

        } catch (Exception ignored) {
        }
    }
}
