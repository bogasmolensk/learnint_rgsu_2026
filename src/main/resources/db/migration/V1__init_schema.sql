-- Таблица пользователей
CREATE TABLE users (
                       id            BIGSERIAL     PRIMARY KEY,
                       login         VARCHAR(50)   NOT NULL UNIQUE,
                       display_name  VARCHAR(100)  NOT NULL,
                       role          VARCHAR(20)   NOT NULL,
                       created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
                       CONSTRAINT chk_users_role CHECK (role IN ('EMPLOYEE', 'APPROVER'))
);

-- Таблица заявок
CREATE TABLE equipment_requests (
                                    id                BIGSERIAL     PRIMARY KEY,
                                    author_id         BIGINT        NOT NULL,
                                    equipment_name    VARCHAR(200)  NOT NULL,
                                    quantity          INTEGER       NOT NULL,
                                    justification     TEXT          NOT NULL,
                                    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
                                    decision_comment  TEXT,
                                    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                    version           INTEGER       NOT NULL DEFAULT 0,
                                    CONSTRAINT fk_requests_author
                                        FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT,
                                    CONSTRAINT chk_requests_quantity   CHECK (quantity > 0),
                                    CONSTRAINT chk_requests_status     CHECK (
                                        status IN ('DRAFT', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED')
                                        )
);

-- Таблица истории статусов
CREATE TABLE request_status_history (
                                        id               BIGSERIAL     PRIMARY KEY,
                                        request_id       BIGINT        NOT NULL,
                                        changed_by       BIGINT        NOT NULL,
                                        previous_status  VARCHAR(20),
                                        new_status       VARCHAR(20)   NOT NULL,
                                        comment          TEXT,
                                        created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                        CONSTRAINT fk_history_request
                                            FOREIGN KEY (request_id)  REFERENCES equipment_requests (id) ON DELETE CASCADE,
                                        CONSTRAINT fk_history_changed_by
                                            FOREIGN KEY (changed_by)  REFERENCES users (id) ON DELETE RESTRICT,
                                        CONSTRAINT chk_history_new_status CHECK (
                                            new_status IN ('DRAFT', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED')
                                            ),
                                        CONSTRAINT chk_history_prev_status CHECK (
                                            previous_status IS NULL OR previous_status IN
                                                                       ('DRAFT', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED')
                                            )
);

-- Индексы для быстрых выборок (по автору заявки и по заявке в истории)
CREATE INDEX idx_requests_author ON equipment_requests (author_id);
CREATE INDEX idx_history_request ON request_status_history (request_id);