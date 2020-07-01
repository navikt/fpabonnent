alter table INNGAAENDE_HENDELSE add (TIDLIGERE_HENDELSE_ID varchar2(100 char));
update INNGAAENDE_HENDELSE set TIDLIGERE_HENDELSE_ID = KOBLING_ID;
comment on column INNGAAENDE_HENDELSE.TIDLIGERE_HENDELSE_ID is 'Hendelsen er knyttet opp mot en tidligere hendelse som har denne verdien som HENDELSE_ID, aktuelt for eksempel ved korrigeringer';