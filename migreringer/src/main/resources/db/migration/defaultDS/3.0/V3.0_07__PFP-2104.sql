insert into KONFIG_VERDI_KODE (KODE, NAVN, KONFIG_GRUPPE, KONFIG_TYPE, BESKRIVELSE) values ('infotrygdfeed.polling.aktivert', 'Er Infotrygd-feed polling aktivert?', 'INGEN', 'BOOLEAN', 'Angir om Infotrygd-feed vil polles for hendelser (true/false)');
insert into KONFIG_VERDI (ID, KONFIG_KODE, KONFIG_GRUPPE, KONFIG_VERDI, GYLDIG_FOM) values (SEQ_KONFIG_VERDI.NEXTVAL, 'infotrygdfeed.polling.aktivert', 'INGEN', 'false', to_date('01.01.2016', 'dd.mm.yyyy'));

insert into KONFIG_VERDI_KODE (KODE, NAVN, KONFIG_GRUPPE, KONFIG_TYPE, BESKRIVELSE) values ('personfeed.polling.aktivert', 'Er Person-feed polling aktivert?', 'INGEN', 'BOOLEAN', 'Angir om Person-feed vil polles for hendelser (true/false)');
insert into KONFIG_VERDI (ID, KONFIG_KODE, KONFIG_GRUPPE, KONFIG_VERDI, GYLDIG_FOM) values (SEQ_KONFIG_VERDI.NEXTVAL, 'personfeed.polling.aktivert', 'INGEN', 'false', to_date('01.01.2016', 'dd.mm.yyyy'));
