alter table INNGAAENDE_HENDELSE drop column REQUEST_UUID;
comment on column INNGAAENDE_HENDELSE.KILDE is 'Kilden til hendelsen';