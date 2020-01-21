alter table INNGAAENDE_HENDELSE set unused (KL_FEED_KODE) cascade constraints ;
alter table INNGAAENDE_HENDELSE set unused (KL_HAANDTERT_STATUS) cascade constraints ;
alter table INNGAAENDE_HENDELSE set unused (KL_HENDELSE_TYPE) cascade constraints ;

drop table KODELISTE_NAVN_I18N cascade constraints purge;
drop table KODELISTE cascade constraints purge;
drop table KODEVERK cascade constraints purge ;

drop sequence SEQ_KODELISTE;
drop sequence SEQ_KODELISTE_NAVN_I18N;