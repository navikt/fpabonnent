COMMENT ON TABLE PROSESS_TASK is 'Inneholder tasks som skal kjøres i bakgrunnen';

COMMENT ON COLUMN INPUT_FEED.SIST_FEILET is 'Tidspunkt for sist feil ved lesing av feed';
COMMENT ON COLUMN PROSESS_TASK.task_type is 'navn på task. Brukes til å matche riktig implementasjon';
COMMENT ON COLUMN PROSESS_TASK.prioritet is 'prioritet på task.  Høyere tall har høyere prioritet';
COMMENT ON COLUMN PROSESS_TASK.status is 'status på task: KLAR, NYTT_FORSOEK, FEILET, VENTER_SVAR, FERDIG';
COMMENT ON COLUMN PROSESS_TASK.neste_kjoering_etter is 'tasken skal ikke kjøeres før tidspunkt er passert';
COMMENT ON COLUMN PROSESS_TASK.feilede_forsoek is 'antall feilede forsøk';
COMMENT ON COLUMN PROSESS_TASK.siste_kjoering_ts is 'siste gang tasken ble forsøkt kjørt';
COMMENT ON COLUMN PROSESS_TASK.siste_kjoering_feil_kode is 'siste feilkode tasken fikk';
COMMENT ON COLUMN PROSESS_TASK.siste_kjoering_feil_tekst is 'siste feil tasken fikk';
COMMENT ON COLUMN PROSESS_TASK.siste_kjoering_server is 'navn på node som sist kjørte en task (server@pid)';
COMMENT ON COLUMN PROSESS_TASK.task_parametere is 'parametere angitt for en task';
comment on column PROSESS_TASK.task_payload is 'inputdata for en task';
COMMENT ON COLUMN PROSESS_TASK.task_sekvens is 'angir rekkefølge på task innenfor en gruppe ';
COMMENT ON COLUMN PROSESS_TASK.task_gruppe is 'angir en unik id som grupperer flere ';
COMMENT ON COLUMN PROSESS_TASK.versjon is 'angir versjon for optimistisk låsing';
COMMENT ON COLUMN PROSESS_TASK_FEILHAND.INPUT_VARIABEL1 is 'input variabel 1 for feilhåndtering';
COMMENT ON COLUMN PROSESS_TASK_FEILHAND.INPUT_VARIABEL2 is 'input variabel 2 for feilhåndtering';