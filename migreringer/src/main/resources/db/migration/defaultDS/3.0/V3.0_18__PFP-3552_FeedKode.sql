insert into KODEVERK (KODE, NAVN, BESKRIVELSE, KODEVERK_EIER) values ('FEED_KODE', 'Feedkoder', 'Kodeverket angir hvilke feed koder som er definert', 'VL');

insert into KODELISTE (ID, KODEVERK, KODE, BESKRIVELSE, GYLDIG_FOM, GYLDIG_TOM)
values (SEQ_KODELISTE.NEXTVAL, 'FEED_KODE', 'JF_TPS', 'TPS person-feed', to_date('01.01.2000', 'dd.mm.yyyy'), to_date('31.12.9999', 'dd.mm.yyyy'));
insert into KODELISTE (ID, KODEVERK, KODE, BESKRIVELSE, GYLDIG_FOM, GYLDIG_TOM)
values (SEQ_KODELISTE.NEXTVAL, 'FEED_KODE', 'JF_INFOTRYGD', 'Infotrygd ytelse feed', to_date('01.01.2000', 'dd.mm.yyyy'), to_date('31.12.9999', 'dd.mm.yyyy'));

insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'FEED_KODE', 'JF_TPS', 'NB', 'TPS person-feed');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'FEED_KODE', 'JF_INFOTRYGD', 'NB', 'Infotrygd ytelse feed');

alter table INNGAAENDE_HENDELSE rename column INPUT_FEED_KODE to FEED_KODE;
alter table INNGAAENDE_HENDELSE modify FEED_KODE varchar2(100 char);
alter table INNGAAENDE_HENDELSE add KL_FEED_KODE varchar2(100 char) generated always as ('FEED_KODE') virtual;
alter table INNGAAENDE_HENDELSE drop constraint FK_INNGAAENDE_HENDELSE_1;
alter table INNGAAENDE_HENDELSE add constraint FK_INNGAAENDE_HENDELSE_1 foreign key (FEED_KODE, KL_FEED_KODE) references KODELISTE (KODE, KODEVERK);
comment on column INNGAAENDE_HENDELSE.KL_FEED_KODE is 'Referanse til KODEVERK-kolonnen i KODELISTE-tabellen';
comment on column INNGAAENDE_HENDELSE.FEED_KODE is 'Feed koden, definert i KODELISTE';
