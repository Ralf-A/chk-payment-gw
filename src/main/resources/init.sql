CREATE TABLE payments (
                        id UUID PRIMARY KEY,
                        status VARCHAR(50) NOT NULL,
                        card_number_last_four VARCHAR(4),
                        expiry_month INTEGER NOT NULL,
                        expiry_year INTEGER NOT NULL,
                        currency VARCHAR(3) NOT NULL,
                        amount INTEGER NOT NULL
);
