INSERT INTO users (login, display_name, role)
VALUES ('petrov', 'Петров П.П.', 'APPROVER')
    ON CONFLICT (login) DO NOTHING;

INSERT INTO equipment_requests
(author_id, equipment_name, quantity, justification, status,
 decision_comment, created_at, updated_at, version)
VALUES
    ((SELECT id FROM users WHERE login='ivanov'), 'Монитор 24',  1, 'Старый монитор бухгалтеру',  'DRAFT',        NULL,                     '2026-09-01T10:00:00Z', '2026-09-01T10:00:00Z', 0),
    ((SELECT id FROM users WHERE login='ivanov'), 'Клавиатура',  2, 'Замена изношенных',          'UNDER_REVIEW', NULL,                     '2026-09-15T12:00:00Z', '2026-09-16T09:00:00Z', 1),
    ((SELECT id FROM users WHERE login='ivanov'), 'Ноутбук',     1, 'Командировка',               'APPROVED',     'Согласовано',            '2026-10-01T08:00:00Z', '2026-10-02T10:00:00Z', 2),
    ((SELECT id FROM users WHERE login='ivanov'), 'Док-станция', 1, 'Для ноутбука',               'REJECTED',     'Нет бюджета в квартале', '2026-10-05T14:00:00Z', '2026-10-06T09:00:00Z', 2),
    ((SELECT id FROM users WHERE login='petrov'), 'Монитор 27',  2, 'Для нового сотрудника',      'DRAFT',        NULL,                     '2026-10-10T11:00:00Z', '2026-10-10T11:00:00Z', 0),
    ((SELECT id FROM users WHERE login='petrov'), 'Веб-камера',  1, 'Для созвонов',               'UNDER_REVIEW', NULL,                     '2026-10-12T16:00:00Z', '2026-10-13T09:00:00Z', 1);