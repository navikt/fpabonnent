insert into KODELISTE (ID, KODEVERK, KODE, BESKRIVELSE, GYLDIG_FOM, GYLDIG_TOM)
values (SEQ_KODELISTE.NEXTVAL, 'HAANDTERT_STATUS', 'GROVSORTERT', 'Hendelsen er grovsortert', to_date('01.01.2000', 'dd.mm.yyyy'), to_date('31.12.9999', 'dd.mm.yyyy'));
insert into KODELISTE_NAVN_I18N (ID, KL_KODEVERK, KL_KODE, SPRAK, NAVN)
values (SEQ_KODELISTE_NAVN_I18N.NEXTVAL, 'HAANDTERT_STATUS', 'GROVSORTERT', 'NB', 'Grovsortert');

alter table INNGAAENDE_HENDELSE add SENDT_TID timestamp(3);
comment on column INNGAAENDE_HENDELSE.SENDT_TID is 'Tidspunktet en hendelse ble sendt til FPSAK. Hendelsen har blitt kastet i grovsorteringen dersom SENDT_TID ikke er satt og hendelsen er ferdig håndtert.';

update INNGAAENDE_HENDELSE i set i.SENDT_TID = (
                          select p.SISTE_KJOERING_TS from PROSESS_TASK p
                           where p.TASK_TYPE = 'hendelser.sendHendelse'
                             and p.STATUS = 'FERDIG'
                             and p.TASK_PARAMETERE like '%hendelse.sekvensnummer='||i.SEKVENSNUMMER||'%'
                             and p.TASK_PARAMETERE like '%hendelse.requestUuid='||i.REQUEST_UUID||'%');