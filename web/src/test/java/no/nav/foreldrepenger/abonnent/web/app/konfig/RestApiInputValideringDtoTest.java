package no.nav.foreldrepenger.abonnent.web.app.konfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.abonnent.felles.domene.Kodeverdi;
import no.nav.foreldrepenger.abonnent.web.app.IndexClasses;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Fail.fail;

class RestApiInputValideringDtoTest extends RestApiTester {

    /**
     * IKKE ignorer eller fjern denne testen, den sørger for at inputvalidering er i orden for REST-grensesnittene
     * <p>
     * Kontakt Team Humle hvis du trenger hjelp til å endre koden din slik at den går igjennom her
     */
    @ParameterizedTest
    @MethodSource("finnAlleDtoTyper")
    void alle_felter_i_objekter_som_brukes_som_inputDTO_skal_enten_ha_valideringsannotering_eller_være_av_godkjent_type(Class<?> dto) throws Exception {
        Set<Class<?>> validerteKlasser = new HashSet<>(); // trengs for å unngå løkker og unngå å validere samme klasse flere multipliser dobbelt
        validerRekursivt(validerteKlasser, dto, null);
    }

    private static final Set<Class<? extends Object>> ALLOWED_ENUM_ANNOTATIONS = Set.of(JsonProperty.class, JsonValue.class, JsonIgnore.class,
        Valid.class, Null.class, NotNull.class);

    @SuppressWarnings("rawtypes")
    private static final Map<Class, List<List<Class<? extends Annotation>>>> UNNTATT_FRA_VALIDERING = new HashMap<>() {
        {

            put(boolean.class, List.of(List.of()));
            put(Boolean.class, List.of(List.of()));

            // LocalDate og LocalDateTime har egne deserializers
            put(LocalDate.class, List.of(List.of()));
            put(LocalDateTime.class, List.of(List.of()));

            // Enforces av UUID selv
            put(UUID.class, List.of(List.of()));
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Map<Class, List<List<Class<? extends Annotation>>>> VALIDERINGSALTERNATIVER = new HashMap<>() {
        {
            put(String.class, List.of(List.of(Pattern.class, Size.class), List.of(Pattern.class), List.of(Digits.class)));
            put(Long.class, List.of(List.of(Min.class, Max.class), List.of(Digits.class)));
            put(long.class, List.of(List.of(Min.class, Max.class), List.of(Digits.class)));
            put(Integer.class, List.of(List.of(Min.class, Max.class)));
            put(int.class, List.of(List.of(Min.class, Max.class)));
            put(BigDecimal.class, List.of(List.of(Min.class, Max.class, Digits.class), List.of(DecimalMin.class, DecimalMax.class, Digits.class)));

            putAll(UNNTATT_FRA_VALIDERING);
        }
    };

    private static List<List<Class<? extends Annotation>>> getVurderingsalternativer(Field field) {
        var type = field.getType();
        if (field.getType().isEnum()) {
            return List.of(List.of(Valid.class));
        } else if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
            if (brukerGenerics(field)) {
                var args = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
                if (Arrays.stream(args).allMatch(UNNTATT_FRA_VALIDERING::containsKey)) {
                    return List.of(List.of(Size.class));
                } else if (args.length == 1 && erKodeverk(args)) {
                    return List.of(List.of(Valid.class, Size.class));
                }

            }
            return List.of(List.of(Valid.class, Size.class));

        }
        return VALIDERINGSALTERNATIVER.get(type);
    }

    private static boolean erKodeverk(Type... args) {
        return Kodeverdi.class.isAssignableFrom((Class<?>) args[0]);
    }

    private static Set<Class<?>> finnAlleDtoTyper() {
        Set<Class<?>> parametre = new TreeSet<>(Comparator.comparing(Class::getName));
        for (Method method : finnAlleRestMetoder()) {
            parametre.addAll(List.of(method.getParameterTypes()));
            for (Type type : method.getGenericParameterTypes()) {
                if (type instanceof ParameterizedType genericTypes) {
                    for (Type gen : genericTypes.getActualTypeArguments()) {
                        parametre.add((Class<?>) gen);
                    }
                }
            }
        }
        Set<Class<?>> filtreteParametre = new TreeSet<>(Comparator.comparing(Class::getName));
        for (Class<?> klasse : parametre) {
            if (klasse.getName().startsWith("java")) {
                // ikke sjekk nedover i innebygde klasser, det skal brukes annoteringer på tidligere tidspunkt
                continue;
            }
            filtreteParametre.add(klasse);
        }
        return filtreteParametre;
    }

    private static void validerRekursivt(Set<Class<?>> besøkteKlasser, Class<?> klasse, Class<?> forrigeKlasse) throws URISyntaxException {
        if (erKodeverk(klasse)) {
            return;
        }
        if (besøkteKlasser.contains(klasse)) {
            return;
        }

        var protectionDomain = klasse.getProtectionDomain();
        var codeSource = protectionDomain.getCodeSource();
        if (codeSource == null) {
            // system klasse
            return;
        }

        besøkteKlasser.add(klasse);
        if (klasse.getAnnotation(Entity.class) != null || klasse.getAnnotation(MappedSuperclass.class) != null) {
            throw new AssertionError("Klassen " + klasse + " er en entitet, kan ikke brukes som DTO. Brukes i " + forrigeKlasse);
        }

        var klasseLocation = codeSource.getLocation();
        for (var subklasse : IndexClasses.getIndexFor(klasseLocation.toURI()).getSubClassesWithAnnotation(klasse, JsonTypeName.class)) {
            validerRekursivt(besøkteKlasser, subklasse, forrigeKlasse);
        }
        for (var field : getRelevantFields(klasse)) {
            if (field.getAnnotation(JsonIgnore.class) != null) {
                continue; // feltet blir hverken serialisert elle deserialisert, unntas fra sjekk
            }
            if (field.getType().isEnum()) {
                validerEnum(field);
                continue; // enum er OK
            }
            if (getVurderingsalternativer(field) != null) {
                validerRiktigAnnotert(field); // har konfigurert opp spesifikk validering
            } else if (field.getType().getName().startsWith("java")) {
                throw new AssertionError(
                    "Feltet " + field + " har ikke påkrevde annoteringer. Trenger evt. utvidelse av denne testen for å akseptere denne typen.");
            } else {
                validerHarValidAnnotering(field);
                validerRekursivt(besøkteKlasser, field.getType(), forrigeKlasse);
            }
            if (brukerGenerics(field)) {
                validerRekursivt(besøkteKlasser, field.getType(), forrigeKlasse);
                for (var klazz : genericTypes(field)) {
                    validerRekursivt(besøkteKlasser, klazz, forrigeKlasse);
                }
            }
        }
    }

    private static void validerEnum(Field field) {
        if (!erKodeverk(field.getType())) {
            validerRiktigAnnotert(field);
        }
        var illegal = Stream.of(field.getAnnotations())
            .filter(a -> !ALLOWED_ENUM_ANNOTATIONS.contains(a.annotationType()))
            .toList();
        if (!illegal.isEmpty()) {
            fail("Ugyldige annotasjoner funnet på [" + field + "]: " + illegal);
        }

    }

    private static boolean erKodeverk(Class<?> klasse) {
        return Kodeverdi.class.isAssignableFrom(klasse);
    }

    private static void validerHarValidAnnotering(Field field) {
        if (field.getAnnotation(Valid.class) == null) {
            fail("Feltet " + field + " må ha @Valid-annotering.");
        }
    }

    private static Set<Class<?>> genericTypes(Field field) {
        Set<Class<?>> klasser = new HashSet<>();
        var type = (ParameterizedType) field.getGenericType();
        for (var t : type.getActualTypeArguments()) {
            klasser.add((Class<?>) t);
        }
        return klasser;
    }

    private static boolean brukerGenerics(Field field) {
        return field.getGenericType() instanceof ParameterizedType;
    }

    private static Set<Field> getRelevantFields(Class<?> klasse) {
        Set<Field> fields = new LinkedHashSet<>();
        while (!klasse.isPrimitive() && !klasse.getName().startsWith("java")) {
            fields.addAll(fjernStaticFields(List.of(klasse.getDeclaredFields())));
            klasse = klasse.getSuperclass();
        }
        return fields;
    }

    private static Collection<Field> fjernStaticFields(List<Field> fields) {
        return fields.stream().filter(f -> !Modifier.isStatic(f.getModifiers())).collect(Collectors.toList());
    }

    private static void validerRiktigAnnotert(Field field) {
        var alternativer = getVurderingsalternativer(field);
        for (var alternativ : alternativer) {
            var harAlleAnnoteringerForAlternativet = true;
            for (var annotering : alternativ) {
                if (field.getAnnotation(annotering) == null) {
                    harAlleAnnoteringerForAlternativet = false;
                }
            }
            if (harAlleAnnoteringerForAlternativet) {
                return;
            }
        }
        fail("Feltet " + field + " har ikke påkrevde annoteringer: " + alternativer);
    }


}
