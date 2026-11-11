CREATE TABLE api (
    id          uuid         PRIMARY KEY,
    name        varchar(100) NOT NULL,
    version     varchar(50)  NOT NULL,
    base_path   varchar(200) NOT NULL,
    owner_team  varchar(100) NOT NULL,
    description varchar(1000),
    status      varchar(20)  NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'DEPRECATED', 'RETIRED')),
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    row_version bigint       NOT NULL,
    CONSTRAINT uq_api_name_version UNIQUE (name, version)
);

CREATE TABLE consumer (
    id            uuid         PRIMARY KEY,
    name          varchar(100) NOT NULL,
    contact_email varchar(200) NOT NULL,
    status        varchar(20)  NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL,
    row_version   bigint       NOT NULL,
    CONSTRAINT uq_consumer_name UNIQUE (name)
);

CREATE TABLE subscription (
    id                    uuid        PRIMARY KEY,
    api_id                uuid        NOT NULL REFERENCES api (id),
    consumer_id           uuid        NOT NULL REFERENCES consumer (id),
    plan                  varchar(20) NOT NULL CHECK (plan IN ('FREE', 'STANDARD', 'PREMIUM')),
    rate_limit_per_minute integer     NOT NULL,
    status                varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED')),
    created_at            timestamptz NOT NULL,
    updated_at            timestamptz NOT NULL,
    row_version           bigint      NOT NULL,
    CONSTRAINT uq_subscription_api_consumer UNIQUE (api_id, consumer_id)
);

CREATE TABLE api_key (
    id              uuid        PRIMARY KEY,
    subscription_id uuid        NOT NULL REFERENCES subscription (id),
    key_prefix      varchar(8)  NOT NULL,
    key_hash        varchar(64) NOT NULL,
    status          varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'REVOKED')),
    created_at      timestamptz NOT NULL,
    expires_at      timestamptz,
    revoked_at      timestamptz,
    last_used_at    timestamptz,
    CONSTRAINT uq_api_key_hash UNIQUE (key_hash)
);

CREATE INDEX idx_api_key_prefix ON api_key (key_prefix);
CREATE INDEX idx_api_key_subscription ON api_key (subscription_id);
