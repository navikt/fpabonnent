COMMENT ON COLUMN INNGAAENDE_HENDELSE.SEKVENSNUMMER IS 'Sekvensnummer for hendelsen, angitt av feeden';
COMMENT ON COLUMN INNGAAENDE_HENDELSE.INPUT_FEED_KODE IS 'Fremmednøkkel til INPUT_FEED';
COMMENT ON COLUMN INNGAAENDE_HENDELSE.TYPE IS 'Hendelsetype';
COMMENT ON COLUMN INNGAAENDE_HENDELSE.PAYLOAD IS 'Innhold i hendelsen';
COMMENT ON COLUMN INNGAAENDE_HENDELSE.REQUEST_UUID IS 'Identifiser en innlesning, som kan bestå av mange hendelser';
COMMENT ON COLUMN INNGAAENDE_HENDELSE.KOBLING_ID IS 'I tilfeller der enkelt-hendelser ikke er atomiske vil KoblingId vise hvilke hendelser som tilsammen utgjør en atomisk hendelse';
COMMENT ON COLUMN INNGAAENDE_HENDELSE.HAANDTERES_ETTER IS 'Angir tidligste tidspunkt for når hendelsen kan håndteres';
COMMENT ON COLUMN INNGAAENDE_HENDELSE.HAANDTERT_STATUS IS 'Håndteringsstatusen på en hendelse';
COMMENT ON COLUMN INNGAAENDE_HENDELSE.KL_HAANDTERT_STATUS IS 'Referanse til KODEVERK-kolonnen i KODELISTE-tabellen';
COMMENT ON TABLE INNGAAENDE_HENDELSE IS 'Alle hendelser som har blitt mottatt, inkludert payload';

CREATE INDEX IDX_INPUT_FEED_KODE ON INNGAAENDE_HENDELSE (INPUT_FEED_KODE);