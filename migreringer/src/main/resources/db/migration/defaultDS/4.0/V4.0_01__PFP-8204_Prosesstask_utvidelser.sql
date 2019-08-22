ALTER TABLE "PROSESS_TASK_TYPE" ADD ("CRON_EXPRESSION" VARCHAR2(200 CHAR) NULL );

COMMENT ON COLUMN PROSESS_TASK_TYPE.CRON_EXPRESSION IS 'Cron-expression for når oppgaven skal kjøres på nytt';

alter table prosess_task add (SISTE_KJOERING_PLUKK_TS timestamp(6));
alter table prosess_task add (SISTE_KJOERING_SLUTT_TS timestamp(6));

COMMENT ON COLUMN "PROSESS_TASK"."SISTE_KJOERING_TS" IS 'siste gang tasken ble forsøkt kjørt (før kjøring)';
COMMENT ON COLUMN "PROSESS_TASK"."SISTE_KJOERING_SLUTT_TS" IS 'tidsstempel siste gang tasken ble kjørt (etter kjøring)';
COMMENT ON COLUMN "PROSESS_TASK"."SISTE_KJOERING_PLUKK_TS" IS 'siste gang tasken ble forsøkt plukket (fra db til in-memory, før kjøring)';

-----------------------------------------------------------
-- OPPDATERT PER FP-FELLES 1.1.0_20190329073313_e0399cb ---
-----------------------------------------------------------
