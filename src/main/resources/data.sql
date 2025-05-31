-- Delete all
DELETE
FROM loan_installments;
DELETE
FROM loans;
DELETE
FROM customers;
DELETE
FROM users;

-- Create users and a loan
INSERT INTO users (id, username, password, role)
VALUES (1, 'admin', '$2a$12$/M3/5lhxbd1ukfBUapwmxO0zX1Q4xgpQ4NDCUSvl6ACOoNFiqvutK', 'ADMIN'),
       (2, 'baris.manco', '$2a$12$/M3/5lhxbd1ukfBUapwmxO0zX1Q4xgpQ4NDCUSvl6ACOoNFiqvutK', 'CUSTOMER'),
       (3, 'kemal.sunal', '$2a$12$/M3/5lhxbd1ukfBUapwmxO0zX1Q4xgpQ4NDCUSvl6ACOoNFiqvutK', 'CUSTOMER'),
       (4, 'mesut.cevik', '$2a$12$/M3/5lhxbd1ukfBUapwmxO0zX1Q4xgpQ4NDCUSvl6ACOoNFiqvutK', 'CUSTOMER'),
       (5, 'levent.pekcan', '$2a$12$/M3/5lhxbd1ukfBUapwmxO0zX1Q4xgpQ4NDCUSvl6ACOoNFiqvutK', 'CUSTOMER');

INSERT INTO customers (id, name, surname, credit_limit, used_credit_limit, user_id)
VALUES (2, 'Baris', 'Manco', 50000.00, 0.00, 2),
       (3, 'Kemal', 'Sunal', 75000.00, 10000.00, 3),
       (4, 'Mesut', 'Cevik', 25000.00, 10000.00, 4),
       (5, 'Levent', 'Pekcan', 55000.00, 10000.00, 5);

INSERT INTO loans (id, customer_id, loan_amount, total_loan_amount_with_interest,
                   number_of_installments, interest_rate, create_date, is_paid)
VALUES (1, 2, 5000.00, 5500.00,
        6, 0.10, '2025-05-01', false);

INSERT INTO loan_installments (loan_id, amount, paid_amount, due_date, is_paid)
VALUES (1, 916.67, 0.00, '2025-06-01', false),
       (1, 916.67, 0.00, '2025-07-01', false),
       (1, 916.67, 0.00, '2025-08-01', false),
       (1, 916.67, 0.00, '2025-09-01', false),
       (1, 916.67, 0.00, '2025-10-01', false),
       (1, 916.67, 0.00, '2025-11-01', false);
