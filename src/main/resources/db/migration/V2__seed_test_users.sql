INSERT INTO users (login, display_name, role)
VALUES ('ivanov', 'Иванов И.И.', 'EMPLOYEE')
    ON CONFLICT (login) DO NOTHING;