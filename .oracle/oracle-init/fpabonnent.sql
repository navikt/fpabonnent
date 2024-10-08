ALTER SESSION SET CONTAINER=FREEPDB1;

-- CREATE USER FPABONNENT
CREATE USER FPABONNENT
    IDENTIFIED BY fpabonnent
    PROFILE DEFAULT
    ACCOUNT UNLOCK;

GRANT
    CREATE SESSION,
    ALTER SESSION,
    CONNECT,
    RESOURCE,
    CREATE MATERIALIZED VIEW,
    CREATE JOB,
    CREATE TABLE,
    CREATE SYNONYM,
    CREATE VIEW,
    CREATE SEQUENCE,
    UNLIMITED TABLESPACE,
    SELECT ANY TABLE
TO FPABONNENT;

ALTER USER FPABONNENT QUOTA UNLIMITED ON SYSTEM;

-- CREATE USER FPABONNENT_UNIT
CREATE USER FPABONNENT_UNIT
    IDENTIFIED BY fpabonnent_unit
    PROFILE DEFAULT
    ACCOUNT UNLOCK;

GRANT
    CREATE SESSION,
    ALTER SESSION,
    CONNECT,
    RESOURCE,
    CREATE MATERIALIZED VIEW,
    CREATE JOB,
    CREATE TABLE,
    CREATE SYNONYM,
    CREATE VIEW,
    CREATE SEQUENCE,
    UNLIMITED TABLESPACE,
    SELECT ANY TABLE
TO FPABONNENT_UNIT;

ALTER USER FPABONNENT_UNIT QUOTA UNLIMITED ON SYSTEM;
