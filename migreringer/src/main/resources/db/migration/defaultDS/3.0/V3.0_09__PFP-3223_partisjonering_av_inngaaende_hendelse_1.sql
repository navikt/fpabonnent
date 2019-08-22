DECLARE

  antall number;
  opprett_tmp_tabell varchar2(75) := 'create table INNGAAENDE_HENDELSE_TMP as (select * from INNGAAENDE_HENDELSE)';

  drop_orig_tabell varchar2(75) := 'drop table INNGAAENDE_HENDELSE cascade constraints';

  copydata varchar2(2000) := 'insert into INNGAAENDE_HENDELSE (ID, SEKVENSNUMMER, INPUT_FEED_KODE, TYPE, PAYLOAD, REQUEST_UUID, ' ||
                             '  OPPRETTET_AV, OPPRETTET_TID, ENDRET_AV, ENDRET_TID, KOBLING_ID, HAANDTERES_ETTER, HAANDTERT_STATUS) ' ||
                             '(select ID, SEKVENSNUMMER, INPUT_FEED_KODE, TYPE, PAYLOAD, REQUEST_UUID, ' ||
                             '  OPPRETTET_AV, OPPRETTET_TID, ENDRET_AV, ENDRET_TID, KOBLING_ID, HAANDTERES_ETTER, HAANDTERT_STATUS ' ||
                             'from INNGAAENDE_HENDELSE_TMP)';

  drop_tmp_tabell varchar2(75) := 'drop table INNGAAENDE_HENDELSE_TMP cascade constraints';

  opprett_hendelsestabell varchar2(2000) := 'CREATE TABLE INNGAAENDE_HENDELSE ( ' ||
                                            ' ID NUMBER(19,0) NOT NULL, ' ||
                                            ' SEKVENSNUMMER NUMBER(19,0) NOT NULL, ' ||
                                            ' INPUT_FEED_KODE VARCHAR2(50 CHAR) NOT NULL, ' ||
                                            ' TYPE VARCHAR2(100 CHAR) NOT NULL, ' ||
                                            ' PAYLOAD CLOB NOT NULL, ' ||
                                            ' REQUEST_UUID VARCHAR2(100 CHAR) NOT NULL, ' ||
                                            ' OPPRETTET_AV VARCHAR2(20 CHAR) DEFAULT ''VL'', ' ||
                                            ' OPPRETTET_TID TIMESTAMP(3) DEFAULT systimestamp, ' ||
                                            ' ENDRET_AV VARCHAR2(20 CHAR), ' ||
                                            ' ENDRET_TID TIMESTAMP(3), ' ||
                                            ' KOBLING_ID NUMBER(19,0), ' ||
                                            ' HAANDTERES_ETTER TIMESTAMP(3), ' ||
                                            ' HAANDTERT_STATUS VARCHAR2(100 CHAR) DEFAULT ''MOTTATT'' NOT NULL, ' ||
                                            ' KL_HAANDTERT_STATUS VARCHAR2(100 CHAR) GENERATED ALWAYS AS (''HAANDTERT_STATUS'') VIRTUAL, ' ||
                                            ' CONSTRAINT PK_INNGAAENDE_HENDELSE PRIMARY KEY (ID), ' ||
                                            ' CONSTRAINT FK_INNGAAENDE_HENDELSE_1 FOREIGN KEY (INPUT_FEED_KODE) REFERENCES INPUT_FEED (KODE), ' ||
                                            ' CONSTRAINT FK_INNGAAENDE_HENDELSE_2 FOREIGN KEY (HAANDTERT_STATUS, KL_HAANDTERT_STATUS) REFERENCES KODELISTE (KODE, KODEVERK))';

  legg_partisjon varchar2(255) := ' partition by list (HAANDTERT_STATUS) (' ||
                                  ' partition IKKE_HAANDTERT values (''MOTTATT'', ''SENDT_TIL_SORTERING''),' ||
                                  ' partition HAANDTERT values (''HÅNDTERT''),' ||
                                  ' partition UKJENT values (default))';

  opprett_index varchar2(255) := 'CREATE INDEX IDX_INNGAAENDE_HENDELSE_1 ON INNGAAENDE_HENDELSE (HAANDTERT_STATUS)';

BEGIN

  select count(*) into antall from USER_TABLES where TABLE_NAME = 'INNGAAENDE_HENDELSE';
  IF (antall = 1) THEN
    execute immediate opprett_tmp_tabell;
    execute immediate drop_orig_tabell;
  END IF;

  IF (DBMS_DB_VERSION.VERSION < 12) THEN
    execute immediate opprett_hendelsestabell;
    execute immediate opprett_index;
  ELSE
    execute immediate opprett_hendelsestabell || legg_partisjon;
    execute immediate opprett_index || ' LOCAL';
  END IF;

  IF (antall = 1) THEN
    execute immediate copydata;
    execute immediate drop_tmp_tabell;
  END IF;
END;
