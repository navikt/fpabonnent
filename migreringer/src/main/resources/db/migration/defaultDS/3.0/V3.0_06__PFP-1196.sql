insert into KODEVERK (KODE, NAVN, BESKRIVELSE, KODEVERK_EIER) values ('HAANDTERT_STATUS', 'Håndteringsstatus på en hendelse', 'Kodeverket angir håndteringsstatusen til en hendelse', 'VL');

insert into KODELISTE (ID, KODEVERK, KODE, BESKRIVELSE, GYLDIG_FOM, GYLDIG_TOM)
values (SEQ_KODELISTE.NEXTVAL, 'HAANDTERT_STATUS', 'MOTTATT', 'Hendelsen er mottatt, men ikke håndtert', to_date('01.01.2000', 'dd.mm.yyyy'), to_date('31.12.9999', 'dd.mm.yyyy'));
insert into KODELISTE (ID, KODEVERK, KODE, BESKRIVELSE, GYLDIG_FOM, GYLDIG_TOM)
values (SEQ_KODELISTE.NEXTVAL, 'HAANDTERT_STATUS', 'SENDT_TIL_SORTERING', 'Hendelsen er sendt til grovsortering', to_date('01.01.2000', 'dd.mm.yyyy'), to_date('31.12.9999', 'dd.mm.yyyy'));
insert into KODELISTE (ID, KODEVERK, KODE, BESKRIVELSE, GYLDIG_FOM, GYLDIG_TOM)
values (SEQ_KODELISTE.NEXTVAL, 'HAANDTERT_STATUS', 'HÅNDTERT', 'Hendelsen er ferdig håndtert, dvs sendt til FPSAK eller forkastet', to_date('01.01.2000', 'dd.mm.yyyy'), to_date('31.12.9999', 'dd.mm.yyyy'));

insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'HAANDTERT_STATUS', 'MOTTATT', 'NB', 'Mottatt');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'HAANDTERT_STATUS', 'SENDT_TIL_SORTERING', 'NB', 'Sendt til sortering');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'HAANDTERT_STATUS', 'HÅNDTERT', 'NB', 'Håndtert');

alter table INNGAAENDE_HENDELSE add KOBLING_ID number(19,0);
alter table INNGAAENDE_HENDELSE add HAANDTERES_ETTER timestamp(3);
alter table INNGAAENDE_HENDELSE add HAANDTERT_STATUS varchar2(100 char) default 'MOTTATT';
alter table INNGAAENDE_HENDELSE add KL_HAANDTERT_STATUS varchar2(100 char) generated always as ('HAANDTERT_STATUS') virtual;
alter table INNGAAENDE_HENDELSE add constraint FK_INNGAAENDE_HENDELSE_2 foreign key (HAANDTERT_STATUS, KL_HAANDTERT_STATUS) references KODELISTE (KODE, KODEVERK);
create index IDX_INNGAAENDE_HENDELSE_1 on INNGAAENDE_HENDELSE(HAANDTERT_STATUS);

update INNGAAENDE_HENDELSE set HAANDTERT_STATUS = 'HÅNDTERT';
alter table INNGAAENDE_HENDELSE modify HAANDTERT_STATUS varchar2(100 char) not null;

comment on column INNGAAENDE_HENDELSE.KOBLING_ID is 'I tilfeller der enkelt-hendelser ikke er atomiske vil KoblingId vise hvilke hendelser som tilsammen utgjør en atomisk hendelse';
comment on column INNGAAENDE_HENDELSE.HAANDTERES_ETTER is 'Angir tidligste tidspunkt for når hendelsen kan håndteres';
comment on column INNGAAENDE_HENDELSE.HAANDTERT_STATUS is 'Håndteringsstatusen på en hendelse';
comment on column INNGAAENDE_HENDELSE.KL_HAANDTERT_STATUS is 'Referanse til KODEVERK-kolonnen i KODELISTE-tabellen';

insert into KODEVERK (KODE, NAVN, BESKRIVELSE) values ('KONFIG_VERDI_TYPE', 'KonfigVerdiType', 'Angir type den konfigurerbare verdien er av slik at dette kan brukes til validering og fremstilling.');
insert into KODEVERK (KODE, NAVN, BESKRIVELSE) values ('KONFIG_VERDI_GRUPPE', 'KonfigVerdiGruppe', 'Angir en gruppe konfigurerbare verdier tilhører. Det åpner for å kunne ha lister og Maps av konfigurerbare verdier');

insert into KODELISTE (ID,KODEVERK,KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM) values
  (SEQ_KODELISTE.NEXTVAL,'KONFIG_VERDI_GRUPPE','INGEN','Ingen gruppe definert (default).  Brukes istdf. NULL siden dette inngår i en Primary Key. Koder som ikke er del av en gruppe må alltid være unike.',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'));
insert into KODELISTE (ID,KODEVERK,KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM) values
  (SEQ_KODELISTE.NEXTVAL,'KONFIG_VERDI_TYPE','PERIOD','ISO 8601 Periode verdier.  Eks. P10M (10 måneder), P1D (1 dag) ',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'));
insert into KODELISTE (ID,KODEVERK,KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM) values
  (SEQ_KODELISTE.NEXTVAL,'KONFIG_VERDI_TYPE','DATE',null,to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'));
insert into KODELISTE (ID,KODEVERK,KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM) values
  (SEQ_KODELISTE.NEXTVAL,'KONFIG_VERDI_TYPE','INTEGER','Heltallsverdier (positiv/negativ)',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'));
insert into KODELISTE (ID,KODEVERK,KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM) values
  (SEQ_KODELISTE.NEXTVAL,'KONFIG_VERDI_TYPE','STRING',null,to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'));
insert into KODELISTE (ID,KODEVERK,KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM) values
  (SEQ_KODELISTE.NEXTVAL,'KONFIG_VERDI_TYPE','URI','URI for å angi id til en ressurs',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'));
insert into KODELISTE (ID,KODEVERK,KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM) values
  (SEQ_KODELISTE.NEXTVAL,'KONFIG_VERDI_TYPE','BOOLEAN','Støtter J(a) / N(ei) flagg',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'));
insert into KODELISTE (ID,KODEVERK,KODE,BESKRIVELSE,GYLDIG_FOM,GYLDIG_TOM) values
  (SEQ_KODELISTE.NEXTVAL,'KONFIG_VERDI_TYPE','DURATION','ISO 8601 Duration (tid) verdier.  Eks. PT1H (1 time), PT1M (1 minutt) ',to_date('01.01.2000','DD.MM.RRRR'),to_date('31.12.9999','DD.MM.RRRR'));

insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'KONFIG_VERDI_GRUPPE', 'INGEN', 'NB', '-');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'KONFIG_VERDI_TYPE', 'PERIOD', 'NB', 'Periode verdier');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'KONFIG_VERDI_TYPE', 'DURATION', 'NB', 'Periode verdier');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'KONFIG_VERDI_TYPE', 'DATE', 'NB', 'Dato');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'KONFIG_VERDI_TYPE', 'STRING', 'NB', 'Streng verdier');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'KONFIG_VERDI_TYPE', 'URI', 'NB', 'Uniform Resource Identifier');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'KONFIG_VERDI_TYPE', 'BOOLEAN', 'NB', 'Boolske verdier');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'KONFIG_VERDI_TYPE', 'INTEGER', 'NB', 'Heltall');

insert into KONFIG_VERDI_KODE (KODE, NAVN, KONFIG_GRUPPE, KONFIG_TYPE, BESKRIVELSE) values ('infotrygd.hendelser.forsinkelse.minutter', 'Infotrygd hendelse forsinkelse i minutter', 'INGEN', 'INTEGER', 'Antall minutter som hendelser fra Infotrygd blir forsinket før de sjekkes for atomitet og evt. håndteres videre');
insert into KONFIG_VERDI (ID, KONFIG_KODE, KONFIG_GRUPPE, KONFIG_VERDI, GYLDIG_FOM) values (SEQ_KONFIG_VERDI.NEXTVAL, 'infotrygd.hendelser.forsinkelse.minutter', 'INGEN', '60', to_date('01.01.2016', 'dd.mm.yyyy'));

insert into KONFIG_VERDI_KODE (KODE, NAVN, KONFIG_GRUPPE, KONFIG_TYPE, BESKRIVELSE) values ('sortering.intervall.sekunder', 'Hendelsesortering intervall i sekunder', 'INGEN', 'INTEGER', 'Antall sekunder det går mellom hver gang det blir sjekket for nye hendelser som har passert sitt tidspunkt for håndtering');
insert into KONFIG_VERDI (ID, KONFIG_KODE, KONFIG_GRUPPE, KONFIG_VERDI, GYLDIG_FOM) values (SEQ_KONFIG_VERDI.NEXTVAL, 'sortering.intervall.sekunder', 'INGEN', '10', to_date('01.01.2016', 'dd.mm.yyyy'));
