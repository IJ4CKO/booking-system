-- SQLite
CREATE TABLE IF NOT EXISTS booking_request (
    request_id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT NOT NULL,
    service_type TEXT NOT NULL,
    event_date TEXT NOT NULL,
    event_time TEXT,
    guest_count INTEGER,
    budget TEXT,
    details TEXT NOT NULL,
    additional_notes TEXT,
    source TEXT NOT NULL DEFAULT 'website',
    status TEXT NOT NULL DEFAULT 'PENDING',
    submitted_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);