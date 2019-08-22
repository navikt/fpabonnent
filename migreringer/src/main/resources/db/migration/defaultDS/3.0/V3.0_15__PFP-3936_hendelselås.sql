create table HENDELSE_LOCK (
  ID number(19,0) not null,
  SIST_LAAST_TID timestamp(3),
  constraint PK_HENDELSE_LOCK primary key ("ID"));
comment on column HENDELSE_LOCK.ID is 'Primary key';
comment on column HENDELSE_LOCK.SIST_LAAST_TID is 'Tidspunkt for sist gang låsen ble tatt';
comment on table HENDELSE_LOCK is 'Dedikert låsetabell for å sikre synkronisering av grovsortering mellom nodene. Workaround for HHH-7525 - skulle helst låst rett på INNGAAENDE_HENDELSE.';
insert into HENDELSE_LOCK (ID, SIST_LAAST_TID) values (1, (select systimestamp from dual));