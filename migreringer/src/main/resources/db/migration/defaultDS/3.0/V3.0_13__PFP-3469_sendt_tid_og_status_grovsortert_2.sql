DECLARE

  legg_til_grovsortert varchar2(100) := 'alter table INNGAAENDE_HENDELSE modify partition IKKE_HAANDTERT add values (''GROVSORTERT'')';

BEGIN

  IF (DBMS_DB_VERSION.VERSION >= 12) THEN
    execute immediate legg_til_grovsortert;
  END IF;

END;
