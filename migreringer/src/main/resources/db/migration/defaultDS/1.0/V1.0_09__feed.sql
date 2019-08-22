create table INPUT_FEED (
  KODE varchar(50 char) not null,
  NAVN varchar2(200 char) not null,
  SIST_LEST            timestamp(3),
  SIST_FEILET          timestamp(3),
  FEILET_ANTALL        number(19,0) default 0,
  NEXT_URL             varchar2(1000 char),
  VENTETID_FERDIGLEST  varchar2(20 char) not null,
  VENTETID_LESBAR      varchar2(20 char) not null,
  VENTETID_FEILET      varchar2(20 char) not null,
  OPPRETTET_AV         varchar2(20 char) default 'VL',
  OPPRETTET_TID        timestamp(3)      default systimestamp,
  ENDRET_AV            varchar2(20 char),
  ENDRET_TID           timestamp(3),
  CONSTRAINT PK_INPUT_FEED PRIMARY KEY (KODE)
);

comment on table INPUT_FEED is 'Definerer JSON_FEEDs som leses fra';
comment on column INPUT_FEED.KODE is 'Primærnøkkel';
comment on column INPUT_FEED.NAVN is 'Navn på JSON-feed';
comment on column INPUT_FEED.FEILET_ANTALL is 'Antall ganger henting har feilet etter siste gang det gikk bra.';
comment on column INPUT_FEED.SIST_LEST is 'Tidspunkt for sist det ble lest fra kø';
comment on column INPUT_FEED.NEXT_URL is 'Satt hvis det er mer flere sider å lese fra køen';
comment on column INPUT_FEED.VENTETID_FERDIGLEST is 'Hvor lenge det ventes før neste forsøk på å lese når kø er lest helt tom';
comment on column INPUT_FEED.VENTETID_LESBAR is 'Hvor lenge det ventes før neste forsøk på å lese når kø ikke er lest helt tom';
comment on column INPUT_FEED.VENTETID_FEILET is 'Hvor lenge det ventes før neste forsøk på å lese når det var feil i forrige lesing';

create table INNGAAENDE_HENDELSE (
  ID                   number(19,0) not null,
  SEKVENSNUMMER        number(19,0) not null,
  INPUT_FEED_KODE      varchar2(50 char) not null,
  TYPE                 varchar2(100 char) not null,
  PAYLOAD              clob not null,
  REQUEST_UUID         varchar2(100 char) not null,
  OPPRETTET_AV         varchar2(20 char) default 'VL',
  OPPRETTET_TID        timestamp(3)      default systimestamp,
  ENDRET_AV            varchar2(20 char),
  ENDRET_TID           timestamp(3),

  CONSTRAINT PK_INNGAAENDE_HENDELSE PRIMARY KEY (ID),
  CONSTRAINT FK_INNGAAENDE_HENDELSE_1 FOREIGN KEY (INPUT_FEED_KODE) REFERENCES INPUT_FEED (KODE)
);

comment on table INNGAAENDE_HENDELSE is 'Definerer JSON_FEEDs som leses fra';
comment on column INNGAAENDE_HENDELSE.INPUT_FEED_KODE is 'Fremmednøkkel til INPUT_FEED';
comment on column INNGAAENDE_HENDELSE.TYPE is 'Hendelsetype';
comment on column INNGAAENDE_HENDELSE.PAYLOAD is 'Innhold i hendelse';
comment on column INNGAAENDE_HENDELSE.SEKVENSNUMMER is 'Sekvensnummer for hendelsen';
comment on column INNGAAENDE_HENDELSE.REQUEST_UUID is 'id for å identifiser innlesning';


insert into INPUT_FEED (kode, navn, VENTETID_FERDIGLEST, VENTETID_LESBAR, VENTETID_FEILET) values
('JF_TPS', 'JSON feed fra TPS', 'PT1H', 'PT1M', 'PT1M');

CREATE SEQUENCE SEQ_INNGAAENDE_HENDELSE MINVALUE 10000000 START WITH 10000000 INCREMENT BY 1000000 NOCACHE NOCYCLE;