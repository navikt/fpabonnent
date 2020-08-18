drop table HENDELSE_LOCK;
drop table INPUT_FEED;
--expand-contract er overkill her, da evt. PDL-melding under deploy vil gå igjennom når den forsøkes på nytt på ny node:
alter table INNGAAENDE_HENDELSE rename column FEED_KODE to KILDE;
alter table INNGAAENDE_HENDELSE modify (REQUEST_UUID varchar2(100 char) null);