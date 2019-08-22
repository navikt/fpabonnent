alter table INNGAAENDE_HENDELSE modify (PAYLOAD null);
update INNGAAENDE_HENDELSE a set a.PAYLOAD = null where a.SENDT_TID is null and a.SEKVENSNUMMER not in (
  select b.KOBLING_ID from INNGAAENDE_HENDELSE b
   where b.KOBLING_ID is not null
     and b.ID != a.ID
     and b.INPUT_FEED_KODE = a.INPUT_FEED_KODE
     and b.SENDT_TID is not null);