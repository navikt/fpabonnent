FP-Abonnent
===============
Applikasjonen tar inn hendelser fra potensielt flere kilder og videresender disse til FP-Sak, dersom aktuelle aktører har en fagsak der. Pr august 2020 er det kun lesing av fødsel, død og dødfødsel fra PDL/LEESAH Kafka topicen aapen-person-pdl-leesah-v1 som er implementert, men kjernen i applikasjonen er laget for at det skal være mulig å lese fra flere kilder i fremtiden, uten å endre noe særlig på den eksisterende koden.

### Virkemåte
* Når en hendelse mottas fra Kafka blir den oversatt til et internt format og serialisert ned som JSON i tabellen INNGAAENDE_HENDELSE i databasen.
* Da FP-Sak fortsatt integrerer med TPS som får batch oppdateringer hver natt, må alle PDL-hendelser holdes igjen minimum til påfølgende natt-batch er kjørt. Dette realiseres ved at det opprettes en ProsessTask VurderSorteringTask pr mottatt hendelse, som får "neste kjøring etter" satt til passende tidspunkt, se TpsForsinkelseTjeneste for detaljer.
* Når VurderSorteringTask kjører, slås det opp mot TPS for å sjekke om hendelsen nå er reflektert der, og dermed kan leveres til FP-Sak. Hvis den kan leveres vil det opprettes en SorterHendelseTask, og hvis ikke blir det opprettet en ny VurderSorteringTask, såfremt hendelsen ikke er over en uke gammel - da vil den forkastes.
* SorterHendelseTask utfører "grovsortering" ved å sjekke med REST-kall om en av aktørene i hendelsen finnes i en eller flere saker i FP-Sak. Ved treff vil det opprettes en SendHendelseTask som leverer hendelsen. Hvis ikke vil hendelsen forkastes.
* SendHendelseTask leverer tilslutt hendelsen til FP-Sak, som vil bruke dette som en trigger om at "noe har skjedd", og undersøke det nærmere ved å gjøre ny registerinnhenting og opprette revurdering / oppdatere åpen behandling etter behov. FP-Sak har også mulighet til å gjøre en "finsortering" i forkant av dette, for eksempel for å sjekke om hendelsens dato treffer innenfor et angitt vindu. 

### Lokal utvikling
* For å teste applikasjonen ende-til-ende lokalt må man benytte VTP + Autotest, sammen med FP-Sak.
* I VTP finnes en REST-tjeneste /api/pdl/leesah som gir mulighet til å opprette hendelser på intern Kafka-topic. FP-Abonnent lytter på denne når den kjøres lokalt.
* For at VTP skal opprette PDL-topicen må environment for VTP settes til dette + komma-separert liste over andre topics du evt. trenger lokalt: CREATE_TOPICS=aapen-person-pdl-leesah-v1-vtp
* For å få hendelsen prosessert uten å vente på forsinkelsen, må du sette NESTE_KJOERING_ETTER på prosesstasken til nåtid i PROSESS_TASK-tabellen.
* Hvis du trenger at hendelsen reflekteres som en endring i TPS når FP-Sak oppdaterer registerdata, må dette hackes til i TPS-mocken i VTP.

### Henvendelser
Spørsmål knyttet til koden eller prosjektet kan rettes til:
* Jan Erik Johnsen (jan.erik.johnsen@nav.no)
* Jens-Otto Larsen (jens-otto.larsen@nav.no)
