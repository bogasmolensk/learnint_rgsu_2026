INSERT INTO users (login, display_name, role, created_at, password)
VALUES
    ('emp_one',  'Сотрудник Один',  'EMPLOYEE', now(), '$2a$10$Yer4.KZQX6zzJ1LmKPE16ucCQJibDeWBiOtGs7Sv5RKhrF5Vwg3U.'),
    ('emp_two',  'Сотрудник Два',   'EMPLOYEE', now(), '$2a$10$VPPm/rbfcBSmIK5cZNRDEO6XnVlCrc5467wpwy9q/ZOx3pzHZOgEe'),
    ('appr_one', 'Ответственный Один','APPROVER', now(), '$2a$10$IJ7ifidz5ibtT4lXxQCtK.Y2VLnqTcBr.6GBl3JgNperCvYsufoMO')
    ON CONFLICT (login) DO NOTHING;