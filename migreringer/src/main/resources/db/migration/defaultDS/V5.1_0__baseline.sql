create sequence SEQ_INNGAAENDE_HENDELSE
    minvalue 10000000
    increment by 50
    nocache
/

create sequence SEQ_PROSESS_TASK
    minvalue 1000000
    increment by 50
    nocache
/

create sequence SEQ_PROSESS_TASK_GRUPPE
    minvalue 10000000
    increment by 1000000
    nocache
/

create table INNGAAENDE_HENDELSE
(
    ID                    NUMBER(19)                           not null
        constraint PK_INNGAAENDE_HENDELSE
        primary key,
    KILDE                 VARCHAR2(100 char)                   not null,
    TYPE                  VARCHAR2(100 char)                   not null,
    PAYLOAD               CLOB,
    OPPRETTET_AV          VARCHAR2(20 char)  default 'VL',
    OPPRETTET_TID         TIMESTAMP(3)       default systimestamp,
    ENDRET_AV             VARCHAR2(20 char),
    ENDRET_TID            TIMESTAMP(3),
    HAANDTERES_ETTER      TIMESTAMP(3),
    HAANDTERT_STATUS      VARCHAR2(100 char) default 'MOTTATT' not null,
    SENDT_TID             TIMESTAMP(3),
    HENDELSE_ID           VARCHAR2(100 char)                   not null,
    TIDLIGERE_HENDELSE_ID VARCHAR2(100 char)
)
    /

comment on table INNGAAENDE_HENDELSE is 'Alle hendelser som har blitt mottatt, inkludert payload'
/

comment on column INNGAAENDE_HENDELSE.KILDE is 'Kilden til hendelsen'
/

comment on column INNGAAENDE_HENDELSE.TYPE is 'Hendelsetype'
/

comment on column INNGAAENDE_HENDELSE.PAYLOAD is 'Innhold i hendelsen'
/

comment on column INNGAAENDE_HENDELSE.HAANDTERES_ETTER is 'Angir tidligste tidspunkt for når hendelsen kan håndteres'
/

comment on column INNGAAENDE_HENDELSE.HAANDTERT_STATUS is 'Håndteringsstatusen på en hendelse'
/

comment on column INNGAAENDE_HENDELSE.SENDT_TID is 'Tidspunktet en hendelse ble sendt til FPSAK. Hendelsen har blitt kastet i grovsorteringen dersom SENDT_TID ikke er satt og hendelsen er ferdig håndtert.'
/

comment on column INNGAAENDE_HENDELSE.HENDELSE_ID is 'Unik identifikator for hendelsen innenfor angitt datakilde'
/

comment on column INNGAAENDE_HENDELSE.TIDLIGERE_HENDELSE_ID is 'Hendelsen er knyttet opp mot en tidligere hendelse som har denne verdien som HENDELSE_ID, aktuelt for eksempel ved korrigeringer'
/

create index IDX_INNGAAENDE_HENDELSE_1
    on INNGAAENDE_HENDELSE (HAANDTERT_STATUS)
    /

create index IDX_INNGAAENDE_HENDELSE_2
    on INNGAAENDE_HENDELSE (TYPE)
    /

create index IDX_INPUT_FEED_KODE
    on INNGAAENDE_HENDELSE (KILDE)
    /

create index IDX_INNGAAENDE_HENDELSE_3
    on INNGAAENDE_HENDELSE (HENDELSE_ID, KILDE)
    /

create table PROSESS_TASK
(
    ID                        NUMBER(19)                              not null
        constraint PK_PROSESS_TASK
        primary key,
    TASK_TYPE                 VARCHAR2(50 char)                       not null,
    PRIORITET                 NUMBER(3)          default 0            not null,
    STATUS                    VARCHAR2(20 char)  default 'KLAR'       not null
        constraint CHK_PROSESS_TASK_STATUS
        check (status in ('KLAR', 'FEILET', 'VENTER_SVAR', 'SUSPENDERT', 'VETO', 'FERDIG', 'KJOERT')),
    TASK_PARAMETERE           VARCHAR2(4000 char),
    TASK_PAYLOAD              CLOB,
    TASK_GRUPPE               VARCHAR2(250 char),
    TASK_SEKVENS              VARCHAR2(100 char) default '1'          not null,
    NESTE_KJOERING_ETTER      TIMESTAMP(0)       default current_timestamp,
    FEILEDE_FORSOEK           NUMBER(5)          default 0,
    SISTE_KJOERING_TS         TIMESTAMP(6),
    SISTE_KJOERING_FEIL_KODE  VARCHAR2(50 char),
    SISTE_KJOERING_FEIL_TEKST CLOB,
    SISTE_KJOERING_SERVER     VARCHAR2(50 char),
    VERSJON                   NUMBER(19)         default 0            not null,
    OPPRETTET_AV              VARCHAR2(30 char)  default 'VL'         not null,
    OPPRETTET_TID             TIMESTAMP(6)       default systimestamp not null,
    BLOKKERT_AV               NUMBER(19),
    SISTE_KJOERING_PLUKK_TS   TIMESTAMP(6),
    SISTE_KJOERING_SLUTT_TS   TIMESTAMP(6)
)
    /

comment on table PROSESS_TASK is 'Inneholder tasks som skal kjøres i bakgrunnen'
/

comment on column PROSESS_TASK.TASK_TYPE is 'navn på task. Brukes til å matche riktig implementasjon'
/

comment on column PROSESS_TASK.PRIORITET is 'prioritet på task.  Høyere tall har høyere prioritet'
/

comment on column PROSESS_TASK.STATUS is 'status på task: KLAR, NYTT_FORSOEK, FEILET, VENTER_SVAR, FERDIG'
/

comment on column PROSESS_TASK.TASK_PARAMETERE is 'parametere angitt for en task'
/

comment on column PROSESS_TASK.TASK_PAYLOAD is 'inputdata for en task'
/

comment on column PROSESS_TASK.TASK_GRUPPE is 'angir en unik id som grupperer flere '
/

comment on column PROSESS_TASK.TASK_SEKVENS is 'angir rekkefølge på task innenfor en gruppe '
/

comment on column PROSESS_TASK.NESTE_KJOERING_ETTER is 'tasken skal ikke kjøeres før tidspunkt er passert'
/

comment on column PROSESS_TASK.FEILEDE_FORSOEK is 'antall feilede forsøk'
/

comment on column PROSESS_TASK.SISTE_KJOERING_TS is 'siste gang tasken ble forsøkt kjørt (før kjøring)'
/

comment on column PROSESS_TASK.SISTE_KJOERING_FEIL_KODE is 'siste feilkode tasken fikk'
/

comment on column PROSESS_TASK.SISTE_KJOERING_FEIL_TEKST is 'siste feil tasken fikk'
/

comment on column PROSESS_TASK.SISTE_KJOERING_SERVER is 'navn på node som sist kjørte en task (server@pid)'
/

comment on column PROSESS_TASK.VERSJON is 'angir versjon for optimistisk låsing'
/

comment on column PROSESS_TASK.BLOKKERT_AV is 'Id til ProsessTask som blokkerer kjøring av denne (når status=VETO)'
/

comment on column PROSESS_TASK.SISTE_KJOERING_PLUKK_TS is 'siste gang tasken ble forsøkt plukket (fra db til in-memory, før kjøring)'
/

comment on column PROSESS_TASK.SISTE_KJOERING_SLUTT_TS is 'tidsstempel siste gang tasken ble kjørt (etter kjøring)'
/

create index IDX_PROSESS_TASK_2
    on PROSESS_TASK (TASK_TYPE)
    /

create index IDX_PROSESS_TASK_3
    on PROSESS_TASK (BLOKKERT_AV)
    /

