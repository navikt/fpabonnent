insert into KODEVERK (KODE, NAVN, BESKRIVELSE, KODEVERK_EIER) values ('HENDELSE_TYPE', 'Hendelsestyper', 'Kodeverket angir hendelsene som er støttet', 'VL');

insert into KODELISTE (ID, KODEVERK, KODE, BESKRIVELSE, GYLDIG_FOM, GYLDIG_TOM)
values (SEQ_KODELISTE.NEXTVAL, 'HENDELSE_TYPE', 'FOEDSELSMELDINGOPPRETTET', 'Fødselsmelding opprettet', to_date('01.01.2000', 'dd.mm.yyyy'), to_date('31.12.9999', 'dd.mm.yyyy'));
insert into KODELISTE (ID, KODEVERK, KODE, BESKRIVELSE, GYLDIG_FOM, GYLDIG_TOM)
values (SEQ_KODELISTE.NEXTVAL, 'HENDELSE_TYPE', 'OPPHOERT_v1', 'Ytelse opphørt i Infotrygd', to_date('01.01.2000', 'dd.mm.yyyy'), to_date('31.12.9999', 'dd.mm.yyyy'));
insert into KODELISTE (ID, KODEVERK, KODE, BESKRIVELSE, GYLDIG_FOM, GYLDIG_TOM)
values (SEQ_KODELISTE.NEXTVAL, 'HENDELSE_TYPE', 'INNVILGET_v1', 'Ytelse innvilget i Infotrygd', to_date('01.01.2000', 'dd.mm.yyyy'), to_date('31.12.9999', 'dd.mm.yyyy'));
insert into KODELISTE (ID, KODEVERK, KODE, BESKRIVELSE, GYLDIG_FOM, GYLDIG_TOM)
values (SEQ_KODELISTE.NEXTVAL, 'HENDELSE_TYPE', 'ANNULLERT_v1', 'Ytelse annullert i Infotrygd', to_date('01.01.2000', 'dd.mm.yyyy'), to_date('31.12.9999', 'dd.mm.yyyy'));
insert into KODELISTE (ID, KODEVERK, KODE, BESKRIVELSE, GYLDIG_FOM, GYLDIG_TOM)
values (SEQ_KODELISTE.NEXTVAL, 'HENDELSE_TYPE', 'ENDRET_v1', 'Ytelse endret i Infotrygd', to_date('01.01.2000', 'dd.mm.yyyy'), to_date('31.12.9999', 'dd.mm.yyyy'));

insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'HENDELSE_TYPE', 'FOEDSELSMELDINGOPPRETTET', 'NB', 'Fødselsmelding opprettet');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'HENDELSE_TYPE', 'OPPHOERT_v1', 'NB', 'Ytelse opphørt i Infotrygd');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'HENDELSE_TYPE', 'INNVILGET_v1', 'NB', 'Ytelse innvilget i Infotrygd');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'HENDELSE_TYPE', 'ANNULLERT_v1', 'NB', 'Ytelse annullert i Infotrygd');
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'HENDELSE_TYPE', 'ENDRET_v1', 'NB', 'Ytelse endret i Infotrygd');

alter table INNGAAENDE_HENDELSE add KL_HENDELSE_TYPE varchar2(100 char) generated always as ('HENDELSE_TYPE') virtual;
alter table INNGAAENDE_HENDELSE add constraint FK_INNGAAENDE_HENDELSE_3 foreign key (TYPE, KL_HENDELSE_TYPE) references KODELISTE (KODE, KODEVERK);
create index IDX_INNGAAENDE_HENDELSE_2 on INNGAAENDE_HENDELSE(TYPE);
comment on column INNGAAENDE_HENDELSE.KL_HENDELSE_TYPE is 'Referanse til KODEVERK-kolonnen i KODELISTE-tabellen';
