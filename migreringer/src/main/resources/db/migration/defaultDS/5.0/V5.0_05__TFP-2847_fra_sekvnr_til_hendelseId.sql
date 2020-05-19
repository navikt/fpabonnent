alter table INNGAAENDE_HENDELSE modify SEKVENSNUMMER null;
alter table INNGAAENDE_HENDELSE add (HENDELSE_ID varchar2(100 char));
update INNGAAENDE_HENDELSE set HENDELSE_ID = SEKVENSNUMMER;
comment on column INNGAAENDE_HENDELSE.HENDELSE_ID is 'Unik identifikator for hendelsen innenfor angitt datakilde';