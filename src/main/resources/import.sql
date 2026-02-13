-- Create test user (password is 'password123' hashed)
INSERT INTO users (id, username, email, password, balance, role, is_blocked) VALUES 
(1, 'demo_user', 'demo@example.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOnu', 1500.00, 'ADMIN', false)
ON CONFLICT (id) DO NOTHING;

-- Create default categories for demo user
INSERT INTO categories (id, name, icon, color, type, budget_limit, user_id) VALUES
(1, 'Salary', 'attach_money', '0xFF4CAF50', 'INCOME', 0.00, 1),
(2, 'Food', 'fastfood', '0xFFE91E63', 'EXPENSE', 500.00, 1),
(3, 'Transport', 'directions_bus', '0xFF2196F3', 'EXPENSE', 100.00, 1),
(4, 'Housing', 'home', '0xFFFF9800', 'EXPENSE', 1000.00, 1)
ON CONFLICT (id) DO NOTHING;

-- Create sample transactions
INSERT INTO transactions (id, amount, category, description, date, type, user_id) VALUES
(1, 5000.00, 'Salary', 'Monthly salary', '2026-02-01 10:00:00', 'INCOME', 1),
(2, 45.50, 'Food', 'Lunch with colleagues', '2026-02-05 13:00:00', 'EXPENSE', 1),
(3, 120.00, 'Housing', 'Electricity bill', '2026-02-10 09:00:00', 'EXPENSE', 1);

-- Create a notification
INSERT INTO notifications (id, title, message, is_read, date, user_id) VALUES
(1, 'Welcome!', 'Welcome to FinTrack. Start tracking your expenses today.', false, '2026-02-01 09:00:00', 1);

-- Sync sequences (H2/Postgres)
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('categories', 'id'), (SELECT MAX(id) FROM categories));
SELECT setval(pg_get_serial_sequence('transactions', 'id'), (SELECT MAX(id) FROM transactions));
SELECT setval(pg_get_serial_sequence('notifications', 'id'), (SELECT MAX(id) FROM notifications));
