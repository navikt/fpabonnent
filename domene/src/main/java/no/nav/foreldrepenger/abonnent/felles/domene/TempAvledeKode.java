package no.nav.foreldrepenger.abonnent.felles.domene;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;


/**
 * for avledning av kode for enum som ikke er mappet direkte på navn der både ny (@JsonValue) og gammel (@JsonProperty kode + kodeverk) kan
 * bli sendt. Brukes til eksisterende kode er konvertert til @JsonValue på alle grensesnitt.
 *
 * <b>Gammel</b>: {"kode":"BT-004","kodeverk":"BEHANDLING_TYPE"}
 * <p>
 * <b>Ny</b>: "BT-004"
 * <p>
 *
 * @deprecated endre grensesnitt til @JsonValue istdf @JsonProperty + @JsonCreator
 */
@Deprecated(since = "2020-09-17") // OBS: brukes pga gammel payload i inngående hendelse - slett når man sletter eldre htidligereHendelse
public class TempAvledeKode {

    private static final Logger LOG = LoggerFactory.getLogger(TempAvledeKode.class);

    private TempAvledeKode() {
    }

    @SuppressWarnings("rawtypes")
    public static String getVerdi(Class<? extends Enum> enumCls, Object node, String key, String kallCtx) {
        String kode;
        if (node instanceof String string) {
            kode = string;
        } else {
            if (node instanceof JsonNode jsonNode) {
                kode = jsonNode.get(key).asText();
            } else if (node instanceof TextNode textNode) {
                kode = textNode.asText();
            } else if (node instanceof Map mapNode) {
                kode = (String) mapNode.get(key);
            } else {
                throw new IllegalArgumentException("Støtter ikke node av type: " + node.getClass() + " for enum:" + enumCls.getName());
            }
            var kodeverk = "uspesifisert";
            try {
                if (node instanceof JsonNode jsonNode) {
                    kodeverk = jsonNode.get("kodeverk").asText();
                } else if (node instanceof TextNode textNode) {
                    kodeverk = textNode.asText();
                } else if (node instanceof Map mapNode) {
                    kodeverk = (String) mapNode.get("kodeverk");
                }
            } catch (Exception e) {
                LOG.info("KODEVERK-OBJEKT: tempavledekode kalt uten at det finnes kodeverk - kode {}", kode);
            }
            LOG.info("KODEVERK-OBJEKT: mottok kodeverdiobjekt som ikke var String - kode {} fra kodeverk {} kontekst {}", kode, kodeverk, kallCtx);
        }
        return kode;
    }

}
