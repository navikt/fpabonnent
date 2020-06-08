alter table INNGAAENDE_HENDELSE set unused (SEKVENSNUMMER);
alter table INNGAAENDE_HENDELSE modify (HENDELSE_ID varchar2(100 char) not null);