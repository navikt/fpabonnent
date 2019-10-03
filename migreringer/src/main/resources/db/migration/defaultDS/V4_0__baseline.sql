--------------------------------------------------------
--  File created - onsdag-oktober-02-2019
--------------------------------------------------------
--------------------------------------------------------
--  DDL for Sequence SEQ_INNGAAENDE_HENDELSE
--------------------------------------------------------

   CREATE SEQUENCE  "SEQ_INNGAAENDE_HENDELSE"  MINVALUE 10000000 INCREMENT BY 50 START WITH 10000000 NOCACHE NOCYCLE ;
--------------------------------------------------------
--  DDL for Sequence SEQ_KODELISTE
--------------------------------------------------------

   CREATE SEQUENCE  "SEQ_KODELISTE"  MINVALUE 1 INCREMENT BY 50 START WITH 1001050 NOCACHE NOCYCLE ;
--------------------------------------------------------
--  DDL for Sequence SEQ_KODELISTE_NAVN_I18N
--------------------------------------------------------

   CREATE SEQUENCE  "SEQ_KODELISTE_NAVN_I18N"  MINVALUE 1 INCREMENT BY 50 START WITH 1051 NOCACHE NOCYCLE ;
--------------------------------------------------------
--  DDL for Sequence SEQ_KONFIG_VERDI
--------------------------------------------------------

   CREATE SEQUENCE  "SEQ_KONFIG_VERDI"  MINVALUE 1000000 INCREMENT BY 50 START WITH 1001400 NOCACHE NOCYCLE ;
--------------------------------------------------------
--  DDL for Sequence SEQ_PROSESS_TASK
--------------------------------------------------------

   CREATE SEQUENCE  "SEQ_PROSESS_TASK"  MINVALUE 1000000 INCREMENT BY 50 START WITH 1000000 NOCACHE NOCYCLE ;
--------------------------------------------------------
--  DDL for Sequence SEQ_PROSESS_TASK_GRUPPE
--------------------------------------------------------

   CREATE SEQUENCE  "SEQ_PROSESS_TASK_GRUPPE"  MINVALUE 10000000 INCREMENT BY 1000000 START WITH 10000000 NOCACHE NOCYCLE ;
--------------------------------------------------------
--  DDL for Table HENDELSE_LOCK
--------------------------------------------------------

  CREATE TABLE "HENDELSE_LOCK"
   (	"ID" NUMBER(19,0),
	"SIST_LAAST_TID" TIMESTAMP (3)
   ) ;

   COMMENT ON COLUMN "HENDELSE_LOCK"."ID" IS 'Primary key';
   COMMENT ON COLUMN "HENDELSE_LOCK"."SIST_LAAST_TID" IS 'Tidspunkt for sist gang låsen ble tatt';
   COMMENT ON TABLE "HENDELSE_LOCK"  IS 'Dedikert låsetabell for å sikre synkronisering av grovsortering mellom nodene. Workaround for HHH-7525 - skulle helst låst rett på INNGAAENDE_HENDELSE.';
--------------------------------------------------------
--  DDL for Table INNGAAENDE_HENDELSE
--------------------------------------------------------
/*
  CREATE TABLE "INNGAAENDE_HENDELSE"
   (	"ID" NUMBER(19,0),
	"SEKVENSNUMMER" NUMBER(19,0),
	"FEED_KODE" VARCHAR2(100 CHAR),
	"TYPE" VARCHAR2(100 CHAR),
	"PAYLOAD" CLOB,
	"REQUEST_UUID" VARCHAR2(100 CHAR),
	"OPPRETTET_AV" VARCHAR2(20 CHAR) DEFAULT 'VL',
	"OPPRETTET_TID" TIMESTAMP (3) DEFAULT systimestamp,
	"ENDRET_AV" VARCHAR2(20 CHAR),
	"ENDRET_TID" TIMESTAMP (3),
	"KOBLING_ID" NUMBER(19,0),
	"HAANDTERES_ETTER" TIMESTAMP (3),
	"HAANDTERT_STATUS" VARCHAR2(100 CHAR) DEFAULT 'MOTTATT',
	"KL_HAANDTERT_STATUS" VARCHAR2(100 CHAR) GENERATED ALWAYS AS ('HAANDTERT_STATUS') VIRTUAL ,
	"SENDT_TID" TIMESTAMP (3),
	"KL_HENDELSE_TYPE" VARCHAR2(100 CHAR) GENERATED ALWAYS AS ('HENDELSE_TYPE') VIRTUAL ,
	"KL_FEED_KODE" VARCHAR2(100 CHAR) GENERATED ALWAYS AS ('FEED_KODE') VIRTUAL
   )
  PARTITION BY LIST ("HAANDTERT_STATUS")
 (PARTITION "IKKE_HAANDTERT"  VALUES ('MOTTATT', 'SENDT_TIL_SORTERING', 'GROVSORTERT') ,
 PARTITION "HAANDTERT"  VALUES ('HÅNDTERT') ,
 PARTITION "UKJENT"  VALUES (default) )  ENABLE ROW MOVEMENT ;
*/
	DECLARE

		opprett_process_tabell varchar2(999) := 'CREATE TABLE INNGAAENDE_HENDELSE ' ||
											' (	ID NUMBER(19,0), ' ||
											' SEKVENSNUMMER NUMBER(19,0), ' ||
											' FEED_KODE VARCHAR2(100 CHAR), ' ||
											' TYPE VARCHAR2(100 CHAR), ' ||
											' PAYLOAD CLOB, ' ||
											' REQUEST_UUID VARCHAR2(100 CHAR), ' ||
											' OPPRETTET_AV VARCHAR2(20 CHAR) DEFAULT ''VL'', ' ||
											' OPPRETTET_TID TIMESTAMP (3) DEFAULT systimestamp, ' ||
											' ENDRET_AV VARCHAR2(20 CHAR), ' ||
											' ENDRET_TID TIMESTAMP (3), ' ||
											' KOBLING_ID NUMBER(19,0), ' ||
											' HAANDTERES_ETTER TIMESTAMP (3), ' ||
											' HAANDTERT_STATUS VARCHAR2(100 CHAR) DEFAULT ''MOTTATT'', ' ||
											' KL_HAANDTERT_STATUS VARCHAR2(100 CHAR) GENERATED ALWAYS AS (''HAANDTERT_STATUS'') VIRTUAL , ' ||
											' SENDT_TID TIMESTAMP (3), ' ||
											' KL_HENDELSE_TYPE VARCHAR2(100 CHAR) GENERATED ALWAYS AS (''HENDELSE_TYPE'') VIRTUAL , ' ||
											' KL_FEED_KODE VARCHAR2(100 CHAR) GENERATED ALWAYS AS (''FEED_KODE'') VIRTUAL ) ';

		legg_partisjon varchar2(255) := ' PARTITION BY LIST (HAANDTERT_STATUS)(' ||
										  ' PARTITION IKKE_HAANDTERT  VALUES (''MOTTATT'', ''SENDT_TIL_SORTERING'', ''GROVSORTERT''),' ||
										  ' PARTITION HAANDTERT  VALUES (''HÅNDTERT''),' ||
										  ' PARTITION UKJENT  VALUES (default) )  ENABLE ROW MOVEMENT ';
		opprett_index varchar2(255) := 'CREATE INDEX IDX_INNGAAENDE_HENDELSE_1 ON INNGAAENDE_HENDELSE (HAANDTERT_STATUS)';
	BEGIN

		-- Partisjoner opprettes ikke i XE (11)
		IF (DBMS_DB_VERSION.VERSION < 12) THEN
			execute immediate opprett_process_tabell;
			execute immediate opprett_index;
		ELSE
			execute immediate opprett_process_tabell || legg_partisjon;
			execute immediate opprett_index || ' LOCAL';
		END IF;

	END;
	/

   COMMENT ON COLUMN "INNGAAENDE_HENDELSE"."SEKVENSNUMMER" IS 'Sekvensnummer for hendelsen, angitt av feeden';
   COMMENT ON COLUMN "INNGAAENDE_HENDELSE"."FEED_KODE" IS 'Feed koden, definert i KODELISTE';
   COMMENT ON COLUMN "INNGAAENDE_HENDELSE"."TYPE" IS 'Hendelsetype';
   COMMENT ON COLUMN "INNGAAENDE_HENDELSE"."PAYLOAD" IS 'Innhold i hendelsen';
   COMMENT ON COLUMN "INNGAAENDE_HENDELSE"."REQUEST_UUID" IS 'Identifiser en innlesning, som kan bestå av mange hendelser';
   COMMENT ON COLUMN "INNGAAENDE_HENDELSE"."KOBLING_ID" IS 'I tilfeller der enkelt-hendelser ikke er atomiske vil KoblingId vise hvilke hendelser som tilsammen utgjør en atomisk hendelse';
   COMMENT ON COLUMN "INNGAAENDE_HENDELSE"."HAANDTERES_ETTER" IS 'Angir tidligste tidspunkt for når hendelsen kan håndteres';
   COMMENT ON COLUMN "INNGAAENDE_HENDELSE"."HAANDTERT_STATUS" IS 'Håndteringsstatusen på en hendelse';
   COMMENT ON COLUMN "INNGAAENDE_HENDELSE"."KL_HAANDTERT_STATUS" IS 'Referanse til KODEVERK-kolonnen i KODELISTE-tabellen';
   COMMENT ON TABLE "INNGAAENDE_HENDELSE"  IS 'Alle hendelser som har blitt mottatt, inkludert payload';
   COMMENT ON COLUMN "INNGAAENDE_HENDELSE"."SENDT_TID" IS 'Tidspunktet en hendelse ble sendt til FPSAK. Hendelsen har blitt kastet i grovsorteringen dersom SENDT_TID ikke er satt og hendelsen er ferdig håndtert.';
   COMMENT ON COLUMN "INNGAAENDE_HENDELSE"."KL_HENDELSE_TYPE" IS 'Referanse til KODEVERK-kolonnen i KODELISTE-tabellen';
   COMMENT ON COLUMN "INNGAAENDE_HENDELSE"."KL_FEED_KODE" IS 'Referanse til KODEVERK-kolonnen i KODELISTE-tabellen';
--------------------------------------------------------
--  DDL for Table INPUT_FEED
--------------------------------------------------------

  CREATE TABLE "INPUT_FEED"
   (	"KODE" VARCHAR2(50 CHAR),
	"NAVN" VARCHAR2(200 CHAR),
	"SIST_LEST" TIMESTAMP (3),
	"SIST_FEILET" TIMESTAMP (3),
	"FEILET_ANTALL" NUMBER(19,0) DEFAULT 0,
	"NEXT_URL" VARCHAR2(1000 CHAR),
	"VENTETID_FERDIGLEST" VARCHAR2(20 CHAR),
	"VENTETID_LESBAR" VARCHAR2(20 CHAR),
	"VENTETID_FEILET" VARCHAR2(20 CHAR),
	"OPPRETTET_AV" VARCHAR2(20 CHAR) DEFAULT 'VL',
	"OPPRETTET_TID" TIMESTAMP (3) DEFAULT systimestamp,
	"ENDRET_AV" VARCHAR2(20 CHAR),
	"ENDRET_TID" TIMESTAMP (3)
   ) ;

   COMMENT ON TABLE "INPUT_FEED"  IS 'Definerer JSON_FEEDs som leses fra';
   COMMENT ON COLUMN "INPUT_FEED"."KODE" IS 'Primærnøkkel';
   COMMENT ON COLUMN "INPUT_FEED"."NAVN" IS 'Navn på JSON-feed';
   COMMENT ON COLUMN "INPUT_FEED"."FEILET_ANTALL" IS 'Antall ganger henting har feilet etter siste gang det gikk bra.';
   COMMENT ON COLUMN "INPUT_FEED"."SIST_LEST" IS 'Tidspunkt for sist det ble lest fra kø';
   COMMENT ON COLUMN "INPUT_FEED"."NEXT_URL" IS 'Satt hvis det er mer flere sider å lese fra køen';
   COMMENT ON COLUMN "INPUT_FEED"."VENTETID_FERDIGLEST" IS 'Hvor lenge det ventes før neste forsøk på å lese når kø er lest helt tom';
   COMMENT ON COLUMN "INPUT_FEED"."VENTETID_LESBAR" IS 'Hvor lenge det ventes før neste forsøk på å lese når kø ikke er lest helt tom';
   COMMENT ON COLUMN "INPUT_FEED"."VENTETID_FEILET" IS 'Hvor lenge det ventes før neste forsåøk på å lese når det var feil i forrige lesing';
   COMMENT ON COLUMN "INPUT_FEED"."SIST_FEILET" IS 'Tidspunkt for sist feil ved lesing av feed';
--------------------------------------------------------
--  DDL for Table KODELISTE
--------------------------------------------------------

  CREATE TABLE "KODELISTE"
   (	"ID" NUMBER(19,0),
	"KODEVERK" VARCHAR2(100 CHAR),
	"KODE" VARCHAR2(100 CHAR),
	"OFFISIELL_KODE" VARCHAR2(1000 CHAR),
	"BESKRIVELSE" VARCHAR2(4000 CHAR),
	"GYLDIG_FOM" DATE DEFAULT sysdate,
	"GYLDIG_TOM" DATE DEFAULT to_date('31.12.9999', 'dd.mm.yyyy'),
	"OPPRETTET_AV" VARCHAR2(200 CHAR) DEFAULT 'VL',
	"OPPRETTET_TID" TIMESTAMP (3) DEFAULT systimestamp,
	"ENDRET_AV" VARCHAR2(200 CHAR),
	"ENDRET_TID" TIMESTAMP (3),
	"EKSTRA_DATA" VARCHAR2(4000 CHAR)
   ) ;

   COMMENT ON COLUMN "KODELISTE"."ID" IS 'Primary Key';
   COMMENT ON COLUMN "KODELISTE"."KODEVERK" IS '(PK) og FK - kodeverk';
   COMMENT ON COLUMN "KODELISTE"."KODE" IS '(PK) Unik kode innenfor kodeverk. Denne koden er alltid brukt internt';
   COMMENT ON COLUMN "KODELISTE"."OFFISIELL_KODE" IS '(Optional) Offisiell kode hos kodeverkeier. Denne kan avvike fra kode der systemet har egne koder. Kan brukes til å veksle inn kode i offisiell kode når det trengs for integrasjon med andre systemer';
   COMMENT ON COLUMN "KODELISTE"."BESKRIVELSE" IS 'Beskrivelse av koden';
   COMMENT ON COLUMN "KODELISTE"."GYLDIG_FOM" IS 'Dato Kodeverket er gyldig fra og med';
   COMMENT ON COLUMN "KODELISTE"."GYLDIG_TOM" IS 'Dato Kodeverket er gyldig til og med';
   COMMENT ON COLUMN "KODELISTE"."EKSTRA_DATA" IS '(Optional) Tilleggsdata brukt av kodeverket.  Format er kodeverk spesifikt - eks. kan være tekst, json, key-value, etc.';
   COMMENT ON TABLE "KODELISTE"  IS 'Inneholder lister av koder for alle Kodeverk som benyttes i applikasjonen.  Både offisielle (synkronisert fra sentralt hold i Nav) såvel som interne Kodeverk.  Offisielle koder skiller seg ut ved at nav_offisiell_kode er populert. Følgelig vil gyldig_tom/fom, navn, språk og beskrivelse lastes ned fra Kodeverkklienten eller annen kilde sentralt';
--------------------------------------------------------
--  DDL for Table KODELISTE_NAVN_I18N
--------------------------------------------------------

  CREATE TABLE "KODELISTE_NAVN_I18N"
   (	"ID" NUMBER(19,0),
	"KL_KODEVERK" VARCHAR2(100 CHAR),
	"KL_KODE" VARCHAR2(100 CHAR),
	"SPRAK" VARCHAR2(3 CHAR),
	"NAVN" VARCHAR2(256 CHAR),
	"OPPRETTET_AV" VARCHAR2(20 CHAR) DEFAULT 'VL',
	"OPPRETTET_TID" TIMESTAMP (3) DEFAULT systimestamp,
	"ENDRET_AV" VARCHAR2(20 CHAR),
	"ENDRET_TID" TIMESTAMP (3)
   ) ;

   COMMENT ON TABLE "KODELISTE_NAVN_I18N"  IS 'Ny tabell som vil holde kodeliste navn verdi av all språk vi støtte';
   COMMENT ON COLUMN "KODELISTE_NAVN_I18N"."KL_KODEVERK" IS 'FK - Kodeverk fra kodeliste tabell';
   COMMENT ON COLUMN "KODELISTE_NAVN_I18N"."KL_KODE" IS 'FK - Kode fra kodeliste tabell';
   COMMENT ON COLUMN "KODELISTE_NAVN_I18N"."SPRAK" IS 'Respective språk';
--------------------------------------------------------
--  DDL for Table KODEVERK
--------------------------------------------------------

  CREATE TABLE "KODEVERK"
   (	"KODE" VARCHAR2(100 CHAR),
	"KODEVERK_EIER" VARCHAR2(100 CHAR) DEFAULT 'VL',
	"KODEVERK_EIER_REF" VARCHAR2(1000 CHAR),
	"KODEVERK_EIER_VER" VARCHAR2(20 CHAR),
	"KODEVERK_EIER_NAVN" VARCHAR2(100 CHAR),
	"KODEVERK_SYNK_NYE" CHAR(1 BYTE) DEFAULT 'J',
	"KODEVERK_SYNK_EKSISTERENDE" CHAR(1 BYTE) DEFAULT 'J',
	"NAVN" VARCHAR2(256 CHAR),
	"BESKRIVELSE" VARCHAR2(4000 CHAR),
	"OPPRETTET_AV" VARCHAR2(200 CHAR) DEFAULT 'VL',
	"OPPRETTET_TID" TIMESTAMP (3) DEFAULT systimestamp,
	"ENDRET_AV" VARCHAR2(200 CHAR),
	"ENDRET_TID" TIMESTAMP (3),
	"SAMMENSATT" VARCHAR2(1 CHAR) DEFAULT 'N'
   ) ;

   COMMENT ON COLUMN "KODEVERK"."KODE" IS 'PK - definerer kodeverk';
   COMMENT ON COLUMN "KODEVERK"."KODEVERK_EIER" IS 'Offisielt kodeverk eier (kode)';
   COMMENT ON COLUMN "KODEVERK"."KODEVERK_EIER_REF" IS 'Offisielt kodeverk referanse (url)';
   COMMENT ON COLUMN "KODEVERK"."KODEVERK_EIER_VER" IS 'Offisielt kodeverk versjon';
   COMMENT ON COLUMN "KODEVERK"."KODEVERK_EIER_NAVN" IS 'Offisielt kodeverk navn';
   COMMENT ON COLUMN "KODEVERK"."KODEVERK_SYNK_NYE" IS 'Om nye koder fra kodeverkeier skal legges til ved oppdatering.';
   COMMENT ON COLUMN "KODEVERK"."KODEVERK_SYNK_EKSISTERENDE" IS 'Om eksisterende koder fra kodeverkeier skal endres ved oppdatering.';
   COMMENT ON COLUMN "KODEVERK"."NAVN" IS 'Navn på kodeverk';
   COMMENT ON COLUMN "KODEVERK"."BESKRIVELSE" IS 'Beskrivelse av kodeverk';
   COMMENT ON COLUMN "KODEVERK"."SAMMENSATT" IS 'Skiller mellom sammensatt kodeverk og enkel kodeliste';
   COMMENT ON TABLE "KODEVERK"  IS 'Registrerte kodeverk. Representerer grupperinger av koder';
--------------------------------------------------------
--  DDL for Table KONFIG_VERDI
--------------------------------------------------------

  CREATE TABLE "KONFIG_VERDI"
   (	"ID" NUMBER(19,0),
	"KONFIG_KODE" VARCHAR2(50 CHAR),
	"KONFIG_GRUPPE" VARCHAR2(100 CHAR),
	"KONFIG_VERDI" VARCHAR2(255 CHAR),
	"GYLDIG_FOM" DATE DEFAULT sysdate,
	"GYLDIG_TOM" DATE DEFAULT to_date('31.12.9999', 'dd.mm.yyyy'),
	"OPPRETTET_AV" VARCHAR2(20 CHAR) DEFAULT 'VL',
	"OPPRETTET_TID" TIMESTAMP (3) DEFAULT systimestamp,
	"ENDRET_AV" VARCHAR2(20 CHAR),
	"ENDRET_TID" TIMESTAMP (3),
	"KL_KONFIG_VERDI_GRUPPE" VARCHAR2(100 CHAR) GENERATED ALWAYS AS ('KONFIG_VERDI_GRUPPE') VIRTUAL
   ) ;

   COMMENT ON COLUMN "KONFIG_VERDI"."ID" IS 'Primary Key';
   COMMENT ON COLUMN "KONFIG_VERDI"."KONFIG_KODE" IS 'Angir kode som identifiserer en konfigurerbar verdi. ';
   COMMENT ON COLUMN "KONFIG_VERDI"."KONFIG_GRUPPE" IS 'Angir gruppe en konfigurerbar verdi kode tilhører (hvis noen - kan også spesifiseres som INGEN).';
   COMMENT ON COLUMN "KONFIG_VERDI"."KONFIG_VERDI" IS 'Angir verdi';
   COMMENT ON COLUMN "KONFIG_VERDI"."GYLDIG_FOM" IS 'Gydlig fra-og-med dato';
   COMMENT ON COLUMN "KONFIG_VERDI"."GYLDIG_TOM" IS 'Gydlig til-og-med dato';
   COMMENT ON TABLE "KONFIG_VERDI"  IS 'Angir konfigurerbare verdier med kode, eventuelt tilhørende gruppe.';
--------------------------------------------------------
--  DDL for Table KONFIG_VERDI_KODE
--------------------------------------------------------

  CREATE TABLE "KONFIG_VERDI_KODE"
   (	"KODE" VARCHAR2(50 CHAR),
	"KONFIG_GRUPPE" VARCHAR2(100 CHAR) DEFAULT 'INGEN',
	"NAVN" VARCHAR2(50 CHAR),
	"KONFIG_TYPE" VARCHAR2(100 CHAR),
	"BESKRIVELSE" VARCHAR2(255 CHAR),
	"OPPRETTET_AV" VARCHAR2(20 CHAR) DEFAULT 'VL',
	"OPPRETTET_TID" TIMESTAMP (3) DEFAULT systimestamp,
	"ENDRET_AV" VARCHAR2(20 CHAR),
	"ENDRET_TID" TIMESTAMP (3),
	"KL_KONFIG_VERDI_GRUPPE" VARCHAR2(100 CHAR) GENERATED ALWAYS AS ('KONFIG_VERDI_GRUPPE') VIRTUAL ,
	"KL_KONFIG_VERDI_TYPE" VARCHAR2(100 CHAR) GENERATED ALWAYS AS ('KONFIG_VERDI_TYPE') VIRTUAL
   ) ;

   COMMENT ON COLUMN "KONFIG_VERDI_KODE"."KODE" IS 'Primary Key';
   COMMENT ON COLUMN "KONFIG_VERDI_KODE"."KONFIG_GRUPPE" IS 'Angir gruppe en konfigurerbar verdi kode tilhører (hvis noen - kan også spesifiseres som INGEN).';
   COMMENT ON COLUMN "KONFIG_VERDI_KODE"."NAVN" IS 'Angir et visningsnavn';
   COMMENT ON COLUMN "KONFIG_VERDI_KODE"."KONFIG_TYPE" IS 'Type angivelse for koden';
   COMMENT ON COLUMN "KONFIG_VERDI_KODE"."BESKRIVELSE" IS 'Beskrivelse av formålet den konfigurerbare verdien';
   COMMENT ON COLUMN "KONFIG_VERDI_KODE"."KL_KONFIG_VERDI_TYPE" IS 'Referanse til KODEVERK-kolonnen i KODELISTE-tabellen';
   COMMENT ON COLUMN "KONFIG_VERDI_KODE"."KL_KONFIG_VERDI_GRUPPE" IS 'Referanse til KODEVERK-kolonnen i KODELISTE-tabellen';
   COMMENT ON TABLE "KONFIG_VERDI_KODE"  IS 'Angir unik kode for en konfigurerbar verdi for validering og utlisting av tilgjengelige koder.';
--------------------------------------------------------
--  DDL for Table PROSESS_TASK
--------------------------------------------------------
/*
  CREATE TABLE "PROSESS_TASK"
   (	"ID" NUMBER(19,0),
	"TASK_TYPE" VARCHAR2(50 CHAR),
	"PRIORITET" NUMBER(3,0) DEFAULT 0,
	"STATUS" VARCHAR2(20 CHAR) DEFAULT 'KLAR',
	"TASK_PARAMETERE" VARCHAR2(4000 CHAR),
	"TASK_PAYLOAD" CLOB,
	"TASK_GRUPPE" VARCHAR2(250 CHAR),
	"TASK_SEKVENS" VARCHAR2(100 CHAR) DEFAULT '1',
	"NESTE_KJOERING_ETTER" TIMESTAMP (0) DEFAULT current_timestamp,
	"FEILEDE_FORSOEK" NUMBER(5,0) DEFAULT 0,
	"SISTE_KJOERING_TS" TIMESTAMP (6),
	"SISTE_KJOERING_FEIL_KODE" VARCHAR2(50 CHAR),
	"SISTE_KJOERING_FEIL_TEKST" CLOB,
	"SISTE_KJOERING_SERVER" VARCHAR2(50 CHAR),
	"VERSJON" NUMBER(19,0) DEFAULT 0,
	"OPPRETTET_AV" VARCHAR2(30 CHAR) DEFAULT 'VL',
	"OPPRETTET_TID" TIMESTAMP (6) DEFAULT systimestamp,
	"BLOKKERT_AV" NUMBER(19,0),
	"SISTE_KJOERING_PLUKK_TS" TIMESTAMP (6),
	"SISTE_KJOERING_SLUTT_TS" TIMESTAMP (6)
   )
  PARTITION BY LIST ("STATUS")
 (PARTITION "STATUS_FERDIG"  VALUES ('FERDIG') ,
 PARTITION "STATUS_FEILET"  VALUES ('FEILET') ,
 PARTITION "STATUS_KLAR"  VALUES ('KLAR', 'VENTER_SVAR', 'SUSPENDERT', 'VETO') )  ENABLE ROW MOVEMENT ;
 */
	DECLARE

		opprett_process_tabell varchar2(999) := 'CREATE TABLE PROSESS_TASK ' ||
											' (ID NUMBER(19,0), ' ||
											' TASK_TYPE VARCHAR2(50 CHAR), ' ||
											' PRIORITET NUMBER(3,0) DEFAULT 0, ' ||
											' STATUS VARCHAR2(20 CHAR) DEFAULT ''KLAR'', ' ||
											' TASK_PARAMETERE VARCHAR2(4000 CHAR), ' ||
											' TASK_PAYLOAD CLOB, ' ||
											' TASK_GRUPPE VARCHAR2(250 CHAR), ' ||
											' TASK_SEKVENS VARCHAR2(100 CHAR) DEFAULT ''1'', ' ||
											' NESTE_KJOERING_ETTER TIMESTAMP (0) DEFAULT current_timestamp, ' ||
											' FEILEDE_FORSOEK NUMBER(5,0) DEFAULT 0, ' ||
											' SISTE_KJOERING_TS TIMESTAMP (6), ' ||
											' SISTE_KJOERING_FEIL_KODE VARCHAR2(50 CHAR), ' ||
											' SISTE_KJOERING_FEIL_TEKST CLOB, ' ||
											' SISTE_KJOERING_SERVER VARCHAR2(50 CHAR), ' ||
											' VERSJON NUMBER(19,0) DEFAULT 0, ' ||
											' OPPRETTET_AV VARCHAR2(30 CHAR) DEFAULT ''VL'', ' ||
											' OPPRETTET_TID TIMESTAMP (6) DEFAULT systimestamp, ' ||
											' BLOKKERT_AV NUMBER(19,0), ' ||
											' SISTE_KJOERING_PLUKK_TS TIMESTAMP (6), ' ||
											' SISTE_KJOERING_SLUTT_TS TIMESTAMP (6)) ';

		legg_partisjon varchar2(255) :=   ' PARTITION BY LIST (STATUS)(' ||
											' PARTITION STATUS_FERDIG  VALUES (''FERDIG'') , ' ||
											' PARTITION STATUS_FEILET  VALUES (''FEILET'') , ' ||
											' PARTITION STATUS_KLAR  VALUES (''KLAR'', ''VENTER_SVAR'', ''SUSPENDERT'', ''VETO'') )  ENABLE ROW MOVEMENT ';
	BEGIN

	    -- Partisjoner opprettes ikke i XE (11)
		IF (DBMS_DB_VERSION.VERSION < 12) THEN
			execute immediate opprett_process_tabell;
		ELSE
			execute immediate opprett_process_tabell || legg_partisjon;
		END IF;

	END;
	/
   COMMENT ON TABLE "PROSESS_TASK"  IS 'Inneholder tasks som skal kjøres i bakgrunnen';
   COMMENT ON COLUMN "PROSESS_TASK"."TASK_TYPE" IS 'navn på task. Brukes til å matche riktig implementasjon';
   COMMENT ON COLUMN "PROSESS_TASK"."PRIORITET" IS 'prioritet på task.  Høyere tall har høyere prioritet';
   COMMENT ON COLUMN "PROSESS_TASK"."STATUS" IS 'status på task: KLAR, NYTT_FORSOEK, FEILET, VENTER_SVAR, FERDIG';
   COMMENT ON COLUMN "PROSESS_TASK"."NESTE_KJOERING_ETTER" IS 'tasken skal ikke kjøeres før tidspunkt er passert';
   COMMENT ON COLUMN "PROSESS_TASK"."FEILEDE_FORSOEK" IS 'antall feilede forsøk';
   COMMENT ON COLUMN "PROSESS_TASK"."SISTE_KJOERING_TS" IS 'siste gang tasken ble forsøkt kjørt (før kjøring)';
   COMMENT ON COLUMN "PROSESS_TASK"."SISTE_KJOERING_FEIL_KODE" IS 'siste feilkode tasken fikk';
   COMMENT ON COLUMN "PROSESS_TASK"."SISTE_KJOERING_FEIL_TEKST" IS 'siste feil tasken fikk';
   COMMENT ON COLUMN "PROSESS_TASK"."SISTE_KJOERING_SERVER" IS 'navn på node som sist kjørte en task (server@pid)';
   COMMENT ON COLUMN "PROSESS_TASK"."TASK_PARAMETERE" IS 'parametere angitt for en task';
   COMMENT ON COLUMN "PROSESS_TASK"."TASK_PAYLOAD" IS 'inputdata for en task';
   COMMENT ON COLUMN "PROSESS_TASK"."TASK_SEKVENS" IS 'angir rekkefølge på task innenfor en gruppe ';
   COMMENT ON COLUMN "PROSESS_TASK"."TASK_GRUPPE" IS 'angir en unik id som grupperer flere ';
   COMMENT ON COLUMN "PROSESS_TASK"."VERSJON" IS 'angir versjon for optimistisk låsing';
   COMMENT ON COLUMN "PROSESS_TASK"."BLOKKERT_AV" IS 'Id til ProsessTask som blokkerer kjøring av denne (når status=VETO)';
   COMMENT ON COLUMN "PROSESS_TASK"."SISTE_KJOERING_SLUTT_TS" IS 'tidsstempel siste gang tasken ble kjørt (etter kjøring)';
   COMMENT ON COLUMN "PROSESS_TASK"."SISTE_KJOERING_PLUKK_TS" IS 'siste gang tasken ble forsøkt plukket (fra db til in-memory, før kjøring)';
--------------------------------------------------------
--  DDL for Table PROSESS_TASK_FEILHAND
--------------------------------------------------------

  CREATE TABLE "PROSESS_TASK_FEILHAND"
   (	"KODE" VARCHAR2(20 CHAR),
	"NAVN" VARCHAR2(50 CHAR),
	"BESKRIVELSE" VARCHAR2(2000 CHAR),
	"OPPRETTET_AV" VARCHAR2(20 CHAR) DEFAULT 'VL',
	"OPPRETTET_TID" TIMESTAMP (3) DEFAULT systimestamp,
	"ENDRET_AV" VARCHAR2(20 CHAR),
	"ENDRET_TID" TIMESTAMP (3),
	"INPUT_VARIABEL1" NUMBER,
	"INPUT_VARIABEL2" NUMBER
   ) ;

   COMMENT ON COLUMN "PROSESS_TASK_FEILHAND"."KODE" IS 'Kodeverk Primary Key';
   COMMENT ON COLUMN "PROSESS_TASK_FEILHAND"."NAVN" IS 'Lesbart navn på type feilhåndtering brukt i prosesstask';
   COMMENT ON COLUMN "PROSESS_TASK_FEILHAND"."BESKRIVELSE" IS 'Utdypende beskrivelse av koden';
   COMMENT ON TABLE "PROSESS_TASK_FEILHAND"  IS 'Kodetabell for feilhåndterings-metoder. For eksempel antall ganger å prøve på nytt og til hvilke tidspunkt';
   COMMENT ON COLUMN "PROSESS_TASK_FEILHAND"."INPUT_VARIABEL1" IS 'input variabel 1 for feilhåndtering';
   COMMENT ON COLUMN "PROSESS_TASK_FEILHAND"."INPUT_VARIABEL2" IS 'input variabel 2 for feilhåndtering';
--------------------------------------------------------
--  DDL for Table PROSESS_TASK_TYPE
--------------------------------------------------------

  CREATE TABLE "PROSESS_TASK_TYPE"
   (	"KODE" VARCHAR2(50 CHAR),
	"NAVN" VARCHAR2(50 CHAR),
	"FEIL_MAKS_FORSOEK" NUMBER(10,0) DEFAULT 1,
	"FEIL_SEK_MELLOM_FORSOEK" NUMBER(10,0) DEFAULT 30,
	"FEILHANDTERING_ALGORITME" VARCHAR2(20 CHAR) DEFAULT 'DEFAULT',
	"BESKRIVELSE" VARCHAR2(2000 CHAR),
	"OPPRETTET_AV" VARCHAR2(20 CHAR) DEFAULT 'VL',
	"OPPRETTET_TID" TIMESTAMP (3) DEFAULT systimestamp,
	"ENDRET_AV" VARCHAR2(20 CHAR),
	"ENDRET_TID" TIMESTAMP (3),
	"CRON_EXPRESSION" VARCHAR2(200 CHAR)
   ) ;

   COMMENT ON COLUMN "PROSESS_TASK_TYPE"."KODE" IS 'Kodeverk Primary Key';
   COMMENT ON COLUMN "PROSESS_TASK_TYPE"."NAVN" IS 'Lesbart navn på prosesstasktype';
   COMMENT ON COLUMN "PROSESS_TASK_TYPE"."FEIL_MAKS_FORSOEK" IS 'MISSING COLUMN COMMENT';
   COMMENT ON COLUMN "PROSESS_TASK_TYPE"."FEIL_SEK_MELLOM_FORSOEK" IS 'MISSING COLUMN COMMENT';
   COMMENT ON COLUMN "PROSESS_TASK_TYPE"."FEILHANDTERING_ALGORITME" IS 'FK: PROSESS_TASK_FEILHAND';
   COMMENT ON COLUMN "PROSESS_TASK_TYPE"."BESKRIVELSE" IS 'Utdypende beskrivelse av koden';
   COMMENT ON TABLE "PROSESS_TASK_TYPE"  IS 'Kodetabell for typer prosesser med beskrivelse og informasjon om hvilken feilhåndteringen som skal benyttes';
   COMMENT ON COLUMN "PROSESS_TASK_TYPE"."CRON_EXPRESSION" IS 'Cron-expression for når oppgaven skal kjøres på nytt';

SET DEFINE OFF;
Insert into HENDELSE_LOCK (ID,SIST_LAAST_TID) values ('1',to_timestamp('02.10.2019 16.32.18,024000000','DD.MM.RRRR HH24.MI.SSXFF'));

SET DEFINE OFF;
Insert into INPUT_FEED (KODE,NAVN,SIST_LEST,SIST_FEILET,FEILET_ANTALL,NEXT_URL,VENTETID_FERDIGLEST,VENTETID_LESBAR,VENTETID_FEILET) values ('JF_TPS','JSON feed fra TPS',null,null,'0',null,'PT1H','PT1M','PT1M');
Insert into INPUT_FEED (KODE,NAVN,SIST_LEST,SIST_FEILET,FEILET_ANTALL,NEXT_URL,VENTETID_FERDIGLEST,VENTETID_LESBAR,VENTETID_FEILET) values ('JF_INFOTRYGD','JSON feed fra Infotrygd',null,null,'0',null,'PT1H','PT1M','PT1M');

SET DEFINE OFF;
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'HAANDTERT_STATUS','MOTTATT',null,'Hendelsen er mottatt, men ikke håndtert',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'HAANDTERT_STATUS','SENDT_TIL_SORTERING',null,'Hendelsen er sendt til grovsortering',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'HAANDTERT_STATUS','HÅNDTERT',null,'Hendelsen er ferdig håndtert, dvs sendt til FPSAK eller forkastet',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'KONFIG_VERDI_GRUPPE','INGEN',null,'Ingen gruppe definert (default).  Brukes istdf. NULL siden dette inngår i en Primary Key. Koder som ikke er del av en gruppe må alltid være unike.',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'KONFIG_VERDI_TYPE','PERIOD',null,'ISO 8601 Periode verdier.  Eks. P10M (10 måneder), P1D (1 dag) ',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'KONFIG_VERDI_TYPE','DATE',null,null,to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'KONFIG_VERDI_TYPE','INTEGER',null,'Heltallsverdier (positiv/negativ)',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'KONFIG_VERDI_TYPE','STRING',null,null,to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'KONFIG_VERDI_TYPE','URI',null,'URI for å angi id til en ressurs',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'KONFIG_VERDI_TYPE','BOOLEAN',null,'Støtter J(a) / N(ei) flagg',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'KONFIG_VERDI_TYPE','DURATION',null,'ISO 8601 Duration (tid) verdier.  Eks. PT1H (1 time), PT1M (1 minutt) ',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'HAANDTERT_STATUS','GROVSORTERT',null,'Hendelsen er grovsortert',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'HENDELSE_TYPE','FOEDSELSMELDINGOPPRETTET',null,'Fødselsmelding opprettet',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'HENDELSE_TYPE','OPPHOERT_v1',null,'Ytelse opphørt i Infotrygd',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'HENDELSE_TYPE','INNVILGET_v1',null,'Ytelse innvilget i Infotrygd',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'HENDELSE_TYPE','ANNULLERT_v1',null,'Ytelse annullert i Infotrygd',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'HENDELSE_TYPE','ENDRET_v1',null,'Ytelse endret i Infotrygd',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'HENDELSE_TYPE','DOEDSMELDINGOPPRETTET',null,'Dødsmelding opprettet',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'FEED_KODE','JF_TPS',null,'TPS person-feed',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'FEED_KODE','JF_INFOTRYGD',null,'Infotrygd ytelse feed',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);
Insert into KODELISTE (ID,KODEVERK,KODE,OFFISIELL_KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM,EKSTRA_DATA) values (seq_kodeliste.NEXTVAL,'HENDELSE_TYPE','DOEDFOEDSELOPPRETTET',null,'Dødfødsel opprettet',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'),null);

SET DEFINE OFF;
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'HAANDTERT_STATUS','MOTTATT','NB','Mottatt');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'HAANDTERT_STATUS','SENDT_TIL_SORTERING','NB','Sendt til sortering');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'HAANDTERT_STATUS','HÅNDTERT','NB','Håndtert');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'KONFIG_VERDI_GRUPPE','INGEN','NB','-');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'KONFIG_VERDI_TYPE','PERIOD','NB','Periode verdier');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'KONFIG_VERDI_TYPE','DURATION','NB','Periode verdier');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'KONFIG_VERDI_TYPE','DATE','NB','Dato');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'KONFIG_VERDI_TYPE','STRING','NB','Streng verdier');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'KONFIG_VERDI_TYPE','URI','NB','Uniform Resource Identifier');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'KONFIG_VERDI_TYPE','BOOLEAN','NB','Boolske verdier');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'KONFIG_VERDI_TYPE','INTEGER','NB','Heltall');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'HAANDTERT_STATUS','GROVSORTERT','NB','Grovsortert');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'HENDELSE_TYPE','FOEDSELSMELDINGOPPRETTET','NB','Fødselsmelding opprettet');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'HENDELSE_TYPE','OPPHOERT_v1','NB','Ytelse opphørt i Infotrygd');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'HENDELSE_TYPE','INNVILGET_v1','NB','Ytelse innvilget i Infotrygd');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'HENDELSE_TYPE','ANNULLERT_v1','NB','Ytelse annullert i Infotrygd');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'HENDELSE_TYPE','ENDRET_v1','NB','Ytelse endret i Infotrygd');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'HENDELSE_TYPE','DOEDSMELDINGOPPRETTET','NB','Dødsmelding opprettet');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'FEED_KODE','JF_TPS','NB','TPS person-feed');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'FEED_KODE','JF_INFOTRYGD','NB','Infotrygd ytelse feed');
Insert into KODELISTE_NAVN_I18N (ID,KL_KODEVERK,KL_KODE,SPRAK,NAVN) values (seq_kodeliste_navn_i18n.NEXTVAL,'HENDELSE_TYPE','DOEDFOEDSELOPPRETTET','NB','Dødfødsel opprettet');

SET DEFINE OFF;
Insert into KODEVERK (KODE,KODEVERK_EIER,KODEVERK_EIER_REF,KODEVERK_EIER_VER,KODEVERK_EIER_NAVN,KODEVERK_SYNK_NYE,KODEVERK_SYNK_EKSISTERENDE,NAVN,BESKRIVELSE,SAMMENSATT) values ('HAANDTERT_STATUS','VL',null,null,null,'J','J','Håndteringsstatus på en hendelse','Kodeverket angir håndteringsstatusen til en hendelse','N');
Insert into KODEVERK (KODE,KODEVERK_EIER,KODEVERK_EIER_REF,KODEVERK_EIER_VER,KODEVERK_EIER_NAVN,KODEVERK_SYNK_NYE,KODEVERK_SYNK_EKSISTERENDE,NAVN,BESKRIVELSE,SAMMENSATT) values ('KONFIG_VERDI_TYPE','VL',null,null,null,'J','J','KonfigVerdiType','Angir type den konfigurerbare verdien er av slik at dette kan brukes til validering og fremstilling.','N');
Insert into KODEVERK (KODE,KODEVERK_EIER,KODEVERK_EIER_REF,KODEVERK_EIER_VER,KODEVERK_EIER_NAVN,KODEVERK_SYNK_NYE,KODEVERK_SYNK_EKSISTERENDE,NAVN,BESKRIVELSE,SAMMENSATT) values ('KONFIG_VERDI_GRUPPE','VL',null,null,null,'J','J','KonfigVerdiGruppe','Angir en gruppe konfigurerbare verdier tilhører. Det åpner for å kunne ha lister og Maps av konfigurerbare verdier','N');
Insert into KODEVERK (KODE,KODEVERK_EIER,KODEVERK_EIER_REF,KODEVERK_EIER_VER,KODEVERK_EIER_NAVN,KODEVERK_SYNK_NYE,KODEVERK_SYNK_EKSISTERENDE,NAVN,BESKRIVELSE,SAMMENSATT) values ('HENDELSE_TYPE','VL',null,null,null,'J','J','Hendelsestyper','Kodeverket angir hendelsene som er støttet','N');
Insert into KODEVERK (KODE,KODEVERK_EIER,KODEVERK_EIER_REF,KODEVERK_EIER_VER,KODEVERK_EIER_NAVN,KODEVERK_SYNK_NYE,KODEVERK_SYNK_EKSISTERENDE,NAVN,BESKRIVELSE,SAMMENSATT) values ('FEED_KODE','VL',null,null,null,'J','J','Feedkoder','Kodeverket angir hvilke feed koder som er definert','N');

SET DEFINE OFF;
Insert into KONFIG_VERDI (ID,KONFIG_KODE,KONFIG_GRUPPE,KONFIG_VERDI,GYLDIG_FOM,GYLDIG_TOM) values (seq_konfig_verdi.NEXTVAL,'infotrygd.hendelser.forsinkelse.minutter','INGEN','60',to_date('01.01.2016','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'));
Insert into KONFIG_VERDI (ID,KONFIG_KODE,KONFIG_GRUPPE,KONFIG_VERDI,GYLDIG_FOM,GYLDIG_TOM) values (seq_konfig_verdi.NEXTVAL,'sortering.intervall.sekunder','INGEN','10',to_date('01.01.2016','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'));
Insert into KONFIG_VERDI (ID,KONFIG_KODE,KONFIG_GRUPPE,KONFIG_VERDI,GYLDIG_FOM,GYLDIG_TOM) values (seq_konfig_verdi.NEXTVAL,'infotrygdfeed.polling.aktivert','INGEN','false',to_date('01.01.2016','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'));
Insert into KONFIG_VERDI (ID,KONFIG_KODE,KONFIG_GRUPPE,KONFIG_VERDI,GYLDIG_FOM,GYLDIG_TOM) values (seq_konfig_verdi.NEXTVAL,'personfeed.polling.aktivert','INGEN','false',to_date('01.01.2016','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'));

SET DEFINE OFF;
Insert into KONFIG_VERDI_KODE (KODE,KONFIG_GRUPPE,NAVN,KONFIG_TYPE,BESKRIVELSE) values ('infotrygd.hendelser.forsinkelse.minutter','INGEN','Infotrygd hendelse forsinkelse i minutter','INTEGER','Antall minutter som hendelser fra Infotrygd blir forsinket før de sjekkes for atomitet og evt. håndteres videre');
Insert into KONFIG_VERDI_KODE (KODE,KONFIG_GRUPPE,NAVN,KONFIG_TYPE,BESKRIVELSE) values ('sortering.intervall.sekunder','INGEN','Hendelsesortering intervall i sekunder','INTEGER','Antall sekunder det går mellom hver gang det blir sjekket for nye hendelser som har passert sitt tidspunkt for håndtering');
Insert into KONFIG_VERDI_KODE (KODE,KONFIG_GRUPPE,NAVN,KONFIG_TYPE,BESKRIVELSE) values ('infotrygdfeed.polling.aktivert','INGEN','Er Infotrygd-feed polling aktivert?','BOOLEAN','Angir om Infotrygd-feed vil polles for hendelser (true/false)');
Insert into KONFIG_VERDI_KODE (KODE,KONFIG_GRUPPE,NAVN,KONFIG_TYPE,BESKRIVELSE) values ('personfeed.polling.aktivert','INGEN','Er Person-feed polling aktivert?','BOOLEAN','Angir om Person-feed vil polles for hendelser (true/false)');

SET DEFINE OFF;
Insert into PROSESS_TASK_FEILHAND (KODE,NAVN,BESKRIVELSE,INPUT_VARIABEL1,INPUT_VARIABEL2) values ('DEFAULT','Eksponentiell back-off med tak',null,null,null);

SET DEFINE OFF;
Insert into PROSESS_TASK_TYPE (KODE,NAVN,FEIL_MAKS_FORSOEK,FEIL_SEK_MELLOM_FORSOEK,FEILHANDTERING_ALGORITME,BESKRIVELSE,CRON_EXPRESSION) values ('hendelser.grovsorter','Utfører grovsortering av hendelser','3','30','DEFAULT',null,null);
Insert into PROSESS_TASK_TYPE (KODE,NAVN,FEIL_MAKS_FORSOEK,FEIL_SEK_MELLOM_FORSOEK,FEILHANDTERING_ALGORITME,BESKRIVELSE,CRON_EXPRESSION) values ('hendelser.sendHendelse','Sender hendelse til vedtaksløsning','3','30','DEFAULT',null,null);

--------------------------------------------------------
--  DDL for Index IDX_INNGAAENDE_HENDELSE_2
--------------------------------------------------------

  CREATE INDEX "IDX_INNGAAENDE_HENDELSE_2" ON "INNGAAENDE_HENDELSE" ("TYPE")
  ;
--------------------------------------------------------
--  DDL for Index IDX_INPUT_FEED_KODE
--------------------------------------------------------

  CREATE INDEX "IDX_INPUT_FEED_KODE" ON "INNGAAENDE_HENDELSE" ("FEED_KODE")
  ;
--------------------------------------------------------
--  DDL for Index IDX_KODELISTE_1
--------------------------------------------------------

  CREATE INDEX "IDX_KODELISTE_1" ON "KODELISTE" ("KODE")
  ;
--------------------------------------------------------
--  DDL for Index IDX_KODELISTE_2
--------------------------------------------------------

  CREATE INDEX "IDX_KODELISTE_2" ON "KODELISTE" ("OFFISIELL_KODE")
  ;
--------------------------------------------------------
--  DDL for Index IDX_KODELISTE_3
--------------------------------------------------------

  CREATE INDEX "IDX_KODELISTE_3" ON "KODELISTE" ("GYLDIG_FOM")
  ;
--------------------------------------------------------
--  DDL for Index IDX_KODELISTE_6
--------------------------------------------------------

  CREATE INDEX "IDX_KODELISTE_6" ON "KODELISTE" ("KODEVERK")
  ;
--------------------------------------------------------
--  DDL for Index IDX_KONFIG_VERDI_KODE_6
--------------------------------------------------------

  CREATE INDEX "IDX_KONFIG_VERDI_KODE_6" ON "KONFIG_VERDI_KODE" ("KONFIG_TYPE")
  ;
--------------------------------------------------------
--  DDL for Index IDX_KONFIG_VERDI_KODE_7
--------------------------------------------------------

  CREATE INDEX "IDX_KONFIG_VERDI_KODE_7" ON "KONFIG_VERDI_KODE" ("KONFIG_GRUPPE")
  ;
--------------------------------------------------------
--  DDL for Index IDX_KONFIG_VERDI_1
--------------------------------------------------------

  CREATE INDEX "IDX_KONFIG_VERDI_1" ON "KONFIG_VERDI" ("GYLDIG_FOM", "GYLDIG_TOM")
  ;
--------------------------------------------------------
--  DDL for Index IDX_KONFIG_VERDI_2
--------------------------------------------------------

  CREATE INDEX "IDX_KONFIG_VERDI_2" ON "KONFIG_VERDI" ("KONFIG_GRUPPE")
  ;
--------------------------------------------------------
--  DDL for Index IDX_KONFIG_VERDI_3
--------------------------------------------------------

  CREATE INDEX "IDX_KONFIG_VERDI_3" ON "KONFIG_VERDI" ("KONFIG_KODE")
  ;
--------------------------------------------------------
--  DDL for Index IDX_PROSESS_TASK_TYPE_1
--------------------------------------------------------

  CREATE INDEX "IDX_PROSESS_TASK_TYPE_1" ON "PROSESS_TASK_TYPE" ("FEILHANDTERING_ALGORITME")
  ;
--------------------------------------------------------
--  DDL for Index IDX_PROSESS_TASK_2
--------------------------------------------------------

  CREATE INDEX "IDX_PROSESS_TASK_2" ON "PROSESS_TASK" ("TASK_TYPE")
  ;
--------------------------------------------------------
--  DDL for Index IDX_PROSESS_TASK_3
--------------------------------------------------------

  CREATE INDEX "IDX_PROSESS_TASK_3" ON "PROSESS_TASK" ("BLOKKERT_AV")
  ;
--------------------------------------------------------
--  DDL for Index PK_HENDELSE_LOCK
--------------------------------------------------------

  CREATE UNIQUE INDEX "PK_HENDELSE_LOCK" ON "HENDELSE_LOCK" ("ID")
  ;
--------------------------------------------------------
--  DDL for Index PK_INNGAAENDE_HENDELSE
--------------------------------------------------------

  CREATE UNIQUE INDEX "PK_INNGAAENDE_HENDELSE" ON "INNGAAENDE_HENDELSE" ("ID")
  ;
--------------------------------------------------------
--  DDL for Index PK_INPUT_FEED
--------------------------------------------------------

  CREATE UNIQUE INDEX "PK_INPUT_FEED" ON "INPUT_FEED" ("KODE")
  ;
--------------------------------------------------------
--  DDL for Index PK_KODELISTE
--------------------------------------------------------

  CREATE UNIQUE INDEX "PK_KODELISTE" ON "KODELISTE" ("ID")
  ;
--------------------------------------------------------
--  DDL for Index PK_KODELISTE_NAVN_I18N
--------------------------------------------------------

  CREATE UNIQUE INDEX "PK_KODELISTE_NAVN_I18N" ON "KODELISTE_NAVN_I18N" ("ID")
  ;
--------------------------------------------------------
--  DDL for Index PK_KODEVERK
--------------------------------------------------------

  CREATE UNIQUE INDEX "PK_KODEVERK" ON "KODEVERK" ("KODE")
  ;
--------------------------------------------------------
--  DDL for Index PK_KONFIG_VERDI
--------------------------------------------------------

  CREATE UNIQUE INDEX "PK_KONFIG_VERDI" ON "KONFIG_VERDI" ("ID")
  ;
--------------------------------------------------------
--  DDL for Index PK_KONFIG_VERDI_KODE
--------------------------------------------------------

  CREATE UNIQUE INDEX "PK_KONFIG_VERDI_KODE" ON "KONFIG_VERDI_KODE" ("KODE", "KONFIG_GRUPPE")
  ;
--------------------------------------------------------
--  DDL for Index PK_PROSESS_TASK
--------------------------------------------------------

  CREATE UNIQUE INDEX "PK_PROSESS_TASK" ON "PROSESS_TASK" ("ID")
  ;
--------------------------------------------------------
--  DDL for Index PK_PROSESS_TASK_FEILHAND
--------------------------------------------------------

  CREATE UNIQUE INDEX "PK_PROSESS_TASK_FEILHAND" ON "PROSESS_TASK_FEILHAND" ("KODE")
  ;
--------------------------------------------------------
--  DDL for Index PK_PROSESS_TASK_TYPE
--------------------------------------------------------

  CREATE UNIQUE INDEX "PK_PROSESS_TASK_TYPE" ON "PROSESS_TASK_TYPE" ("KODE")
  ;
--------------------------------------------------------
--  DDL for Index UIDX_KONFIG_VERDI_1
--------------------------------------------------------

  CREATE UNIQUE INDEX "UIDX_KONFIG_VERDI_1" ON "KONFIG_VERDI" ("KONFIG_GRUPPE", "GYLDIG_TOM", "KONFIG_KODE")
  ;
--------------------------------------------------------
--  Constraints for Table HENDELSE_LOCK
--------------------------------------------------------

  ALTER TABLE "HENDELSE_LOCK" MODIFY ("ID" NOT NULL ENABLE);
  ALTER TABLE "HENDELSE_LOCK" ADD CONSTRAINT "PK_HENDELSE_LOCK" PRIMARY KEY ("ID")
  USING INDEX  ENABLE;
--------------------------------------------------------
--  Constraints for Table INNGAAENDE_HENDELSE
--------------------------------------------------------

  ALTER TABLE "INNGAAENDE_HENDELSE" MODIFY ("ID" NOT NULL ENABLE);
  ALTER TABLE "INNGAAENDE_HENDELSE" MODIFY ("SEKVENSNUMMER" NOT NULL ENABLE);
  ALTER TABLE "INNGAAENDE_HENDELSE" MODIFY ("FEED_KODE" NOT NULL ENABLE);
  ALTER TABLE "INNGAAENDE_HENDELSE" MODIFY ("TYPE" NOT NULL ENABLE);
  ALTER TABLE "INNGAAENDE_HENDELSE" MODIFY ("REQUEST_UUID" NOT NULL ENABLE);
  ALTER TABLE "INNGAAENDE_HENDELSE" MODIFY ("HAANDTERT_STATUS" NOT NULL ENABLE);
  ALTER TABLE "INNGAAENDE_HENDELSE" ADD CONSTRAINT "PK_INNGAAENDE_HENDELSE" PRIMARY KEY ("ID")
  USING INDEX  ENABLE;
--------------------------------------------------------
--  Constraints for Table INPUT_FEED
--------------------------------------------------------

  ALTER TABLE "INPUT_FEED" MODIFY ("KODE" NOT NULL ENABLE);
  ALTER TABLE "INPUT_FEED" MODIFY ("NAVN" NOT NULL ENABLE);
  ALTER TABLE "INPUT_FEED" MODIFY ("VENTETID_FERDIGLEST" NOT NULL ENABLE);
  ALTER TABLE "INPUT_FEED" MODIFY ("VENTETID_LESBAR" NOT NULL ENABLE);
  ALTER TABLE "INPUT_FEED" MODIFY ("VENTETID_FEILET" NOT NULL ENABLE);
  ALTER TABLE "INPUT_FEED" ADD CONSTRAINT "PK_INPUT_FEED" PRIMARY KEY ("KODE")
  USING INDEX  ENABLE;
--------------------------------------------------------
--  Constraints for Table KODELISTE
--------------------------------------------------------

  ALTER TABLE "KODELISTE" ADD CONSTRAINT "CHK_UNIQUE_KODELISTE" UNIQUE ("KODE", "KODEVERK")
  USING INDEX (CREATE UNIQUE INDEX "UIDX_KODELISTE_1" ON "KODELISTE" ("KODE", "KODEVERK")
  )  ENABLE;
  ALTER TABLE "KODELISTE" ADD CONSTRAINT "PK_KODELISTE" PRIMARY KEY ("ID")
  USING INDEX  ENABLE;
  ALTER TABLE "KODELISTE" MODIFY ("OPPRETTET_TID" NOT NULL ENABLE);
  ALTER TABLE "KODELISTE" MODIFY ("OPPRETTET_AV" NOT NULL ENABLE);
  ALTER TABLE "KODELISTE" MODIFY ("GYLDIG_TOM" NOT NULL ENABLE);
  ALTER TABLE "KODELISTE" MODIFY ("GYLDIG_FOM" NOT NULL ENABLE);
  ALTER TABLE "KODELISTE" MODIFY ("KODE" NOT NULL ENABLE);
  ALTER TABLE "KODELISTE" MODIFY ("KODEVERK" NOT NULL ENABLE);
  ALTER TABLE "KODELISTE" MODIFY ("ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table KODELISTE_NAVN_I18N
--------------------------------------------------------

  ALTER TABLE "KODELISTE_NAVN_I18N" MODIFY ("ID" NOT NULL ENABLE);
  ALTER TABLE "KODELISTE_NAVN_I18N" MODIFY ("KL_KODEVERK" NOT NULL ENABLE);
  ALTER TABLE "KODELISTE_NAVN_I18N" MODIFY ("KL_KODE" NOT NULL ENABLE);
  ALTER TABLE "KODELISTE_NAVN_I18N" MODIFY ("SPRAK" NOT NULL ENABLE);
  ALTER TABLE "KODELISTE_NAVN_I18N" MODIFY ("OPPRETTET_AV" NOT NULL ENABLE);
  ALTER TABLE "KODELISTE_NAVN_I18N" MODIFY ("OPPRETTET_TID" NOT NULL ENABLE);
  ALTER TABLE "KODELISTE_NAVN_I18N" ADD CONSTRAINT "PK_KODELISTE_NAVN_I18N" PRIMARY KEY ("ID")
  USING INDEX  ENABLE;
--------------------------------------------------------
--  Constraints for Table KODEVERK
--------------------------------------------------------

  ALTER TABLE "KODEVERK" MODIFY ("NAVN" NOT NULL ENABLE);
  ALTER TABLE "KODEVERK" MODIFY ("KODEVERK_SYNK_EKSISTERENDE" NOT NULL ENABLE);
  ALTER TABLE "KODEVERK" MODIFY ("KODEVERK_SYNK_NYE" NOT NULL ENABLE);
  ALTER TABLE "KODEVERK" MODIFY ("KODEVERK_EIER" NOT NULL ENABLE);
  ALTER TABLE "KODEVERK" MODIFY ("KODE" NOT NULL ENABLE);
  ALTER TABLE "KODEVERK" ADD CONSTRAINT "PK_KODEVERK" PRIMARY KEY ("KODE")
  USING INDEX  ENABLE;
  ALTER TABLE "KODEVERK" ADD CHECK (kodeverk_synk_eksisterende IN ('J', 'N')) ENABLE;
  ALTER TABLE "KODEVERK" ADD CHECK (kodeverk_synk_nye IN ('J', 'N')) ENABLE;
  ALTER TABLE "KODEVERK" MODIFY ("OPPRETTET_TID" NOT NULL ENABLE);
  ALTER TABLE "KODEVERK" MODIFY ("OPPRETTET_AV" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table KONFIG_VERDI
--------------------------------------------------------

  ALTER TABLE "KONFIG_VERDI" MODIFY ("KL_KONFIG_VERDI_GRUPPE" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI" ADD CONSTRAINT "PK_KONFIG_VERDI" PRIMARY KEY ("ID")
  USING INDEX  ENABLE;
  ALTER TABLE "KONFIG_VERDI" MODIFY ("OPPRETTET_TID" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI" MODIFY ("OPPRETTET_AV" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI" MODIFY ("GYLDIG_TOM" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI" MODIFY ("GYLDIG_FOM" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI" MODIFY ("KONFIG_GRUPPE" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI" MODIFY ("KONFIG_KODE" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI" MODIFY ("ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table KONFIG_VERDI_KODE
--------------------------------------------------------

  ALTER TABLE "KONFIG_VERDI_KODE" MODIFY ("KL_KONFIG_VERDI_GRUPPE" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI_KODE" MODIFY ("KL_KONFIG_VERDI_TYPE" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI_KODE" ADD CONSTRAINT "PK_KONFIG_VERDI_KODE" PRIMARY KEY ("KODE", "KONFIG_GRUPPE")
  USING INDEX  ENABLE;
  ALTER TABLE "KONFIG_VERDI_KODE" MODIFY ("OPPRETTET_TID" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI_KODE" MODIFY ("OPPRETTET_AV" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI_KODE" MODIFY ("KONFIG_TYPE" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI_KODE" MODIFY ("NAVN" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI_KODE" MODIFY ("KONFIG_GRUPPE" NOT NULL ENABLE);
  ALTER TABLE "KONFIG_VERDI_KODE" MODIFY ("KODE" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table PROSESS_TASK
--------------------------------------------------------

  ALTER TABLE "PROSESS_TASK" MODIFY ("ID" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK" MODIFY ("TASK_TYPE" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK" MODIFY ("PRIORITET" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK" MODIFY ("STATUS" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK" MODIFY ("TASK_SEKVENS" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK" MODIFY ("VERSJON" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK" ADD CONSTRAINT "CHK_PROSESS_TASK_STATUS" CHECK (status in ('KLAR', 'FEILET', 'VENTER_SVAR', 'SUSPENDERT', 'VETO', 'FERDIG')) ENABLE;
  ALTER TABLE "PROSESS_TASK" ADD CONSTRAINT "PK_PROSESS_TASK" PRIMARY KEY ("ID")
  USING INDEX  ENABLE;
  ALTER TABLE "PROSESS_TASK" MODIFY ("OPPRETTET_AV" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK" MODIFY ("OPPRETTET_TID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table PROSESS_TASK_FEILHAND
--------------------------------------------------------

  ALTER TABLE "PROSESS_TASK_FEILHAND" ADD CONSTRAINT "PK_PROSESS_TASK_FEILHAND" PRIMARY KEY ("KODE")
  USING INDEX  ENABLE;
  ALTER TABLE "PROSESS_TASK_FEILHAND" MODIFY ("OPPRETTET_TID" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK_FEILHAND" MODIFY ("OPPRETTET_AV" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK_FEILHAND" MODIFY ("NAVN" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK_FEILHAND" MODIFY ("KODE" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table PROSESS_TASK_TYPE
--------------------------------------------------------

  ALTER TABLE "PROSESS_TASK_TYPE" ADD CONSTRAINT "PK_PROSESS_TASK_TYPE" PRIMARY KEY ("KODE")
  USING INDEX  ENABLE;
  ALTER TABLE "PROSESS_TASK_TYPE" MODIFY ("OPPRETTET_TID" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK_TYPE" MODIFY ("OPPRETTET_AV" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK_TYPE" MODIFY ("FEIL_SEK_MELLOM_FORSOEK" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK_TYPE" MODIFY ("FEIL_MAKS_FORSOEK" NOT NULL ENABLE);
  ALTER TABLE "PROSESS_TASK_TYPE" MODIFY ("KODE" NOT NULL ENABLE);
--------------------------------------------------------
--  Ref Constraints for Table INNGAAENDE_HENDELSE
--------------------------------------------------------

  ALTER TABLE "INNGAAENDE_HENDELSE" ADD CONSTRAINT "FK_INNGAAENDE_HENDELSE_2" FOREIGN KEY ("HAANDTERT_STATUS", "KL_HAANDTERT_STATUS")
	  REFERENCES "KODELISTE" ("KODE", "KODEVERK") ENABLE;
  ALTER TABLE "INNGAAENDE_HENDELSE" ADD CONSTRAINT "FK_INNGAAENDE_HENDELSE_3" FOREIGN KEY ("TYPE", "KL_HENDELSE_TYPE")
	  REFERENCES "KODELISTE" ("KODE", "KODEVERK") ENABLE;
  ALTER TABLE "INNGAAENDE_HENDELSE" ADD CONSTRAINT "FK_INNGAAENDE_HENDELSE_1" FOREIGN KEY ("FEED_KODE", "KL_FEED_KODE")
	  REFERENCES "KODELISTE" ("KODE", "KODEVERK") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table KODELISTE
--------------------------------------------------------

  ALTER TABLE "KODELISTE" ADD CONSTRAINT "FK_KODELISTE_01" FOREIGN KEY ("KODEVERK")
	  REFERENCES "KODEVERK" ("KODE") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table KODELISTE_NAVN_I18N
--------------------------------------------------------

  ALTER TABLE "KODELISTE_NAVN_I18N" ADD CONSTRAINT "FK_KODELISTE_02" FOREIGN KEY ("KL_KODE", "KL_KODEVERK")
	  REFERENCES "KODELISTE" ("KODE", "KODEVERK") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table KONFIG_VERDI
--------------------------------------------------------

  ALTER TABLE "KONFIG_VERDI" ADD CONSTRAINT "FK_KONFIG_VERDI_1" FOREIGN KEY ("KONFIG_GRUPPE", "KL_KONFIG_VERDI_GRUPPE")
	  REFERENCES "KODELISTE" ("KODE", "KODEVERK") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table KONFIG_VERDI_KODE
--------------------------------------------------------

  ALTER TABLE "KONFIG_VERDI_KODE" ADD CONSTRAINT "FK_KONFIG_VERDI_KODE_82" FOREIGN KEY ("KONFIG_GRUPPE", "KL_KONFIG_VERDI_GRUPPE")
	  REFERENCES "KODELISTE" ("KODE", "KODEVERK") ENABLE;
  ALTER TABLE "KONFIG_VERDI_KODE" ADD CONSTRAINT "FK_KONFIG_VERDI_KODE_83" FOREIGN KEY ("KONFIG_TYPE", "KL_KONFIG_VERDI_TYPE")
	  REFERENCES "KODELISTE" ("KODE", "KODEVERK") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table PROSESS_TASK
--------------------------------------------------------

  ALTER TABLE "PROSESS_TASK" ADD CONSTRAINT "FK_PROSESS_TASK_1" FOREIGN KEY ("TASK_TYPE")
	  REFERENCES "PROSESS_TASK_TYPE" ("KODE") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table PROSESS_TASK_TYPE
--------------------------------------------------------

  ALTER TABLE "PROSESS_TASK_TYPE" ADD CONSTRAINT "FK_PROSESS_TASK_TYPE_1" FOREIGN KEY ("FEILHANDTERING_ALGORITME")
	  REFERENCES "PROSESS_TASK_FEILHAND" ("KODE") ENABLE;
