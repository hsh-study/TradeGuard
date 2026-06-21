CREATE TABLE kis_api_configurations (
    environment VARCHAR(10) NOT NULL,
    encrypted_app_key VARCHAR(1024) NOT NULL,
    encrypted_app_secret VARCHAR(2048) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (environment),
    CONSTRAINT chk_kis_api_config_environment CHECK (environment IN ('REAL','DEMO'))
);

CREATE TABLE dart_api_configuration (
    id BIGINT NOT NULL,
    encrypted_api_key VARCHAR(1024) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);
