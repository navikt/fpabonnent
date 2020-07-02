alter table INNGAAENDE_HENDELSE set unused (KOBLING_ID);

insert into PROSESS_TASK_TYPE (kode, navn, feil_maks_forsoek, feilhandtering_algoritme, beskrivelse, cron_expression)
values ('retry.feiledeTasks', 'Retry av feilede tasks', 1, 'DEFAULT',
        'Kjører alle feilede tasks på nytt i henhold til cron-expression', '* 20 7 * * *');