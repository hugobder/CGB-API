package cgb.utils;

import cgb.transfer.exception.InvalidIbanFormatException;
import cgb.transfer.exception.InvalidUnCheckableIbanException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IbanTest {

    private IbanValidator validator;

    @BeforeEach
    void setUp() {
        validator = IbanValidator.getInstanceValidator();
    }

    // --- IbanGenerator tests ---

    @Test
    void generatedIban_hasCorrectLength() {
        String iban = IbanGenerator.generateValidIban();
        assertEquals(27, iban.length());
    }

    @Test
    void generatedIban_startsWithFR() {
        String iban = IbanGenerator.generateValidIban();
        assertTrue(iban.startsWith("FR"));
    }

    @Test
    void generatedIban_hasValidStructure() {
        String iban = IbanGenerator.generateValidIban();
        assertTrue(validator.isIbanStructureValid(iban));
    }

    @Test
    void generatedIban_passesChecksumValidation() {
        String iban = IbanGenerator.generateValidIban();
        assertTrue(validator.isIbanValid(iban));
    }

    @Test
    void multipleGeneratedIbans_areValid() {
        for (int i = 0; i < 10; i++) {
            String iban = IbanGenerator.generateValidIban();
            assertTrue(validator.isIbanValid(iban), "Generated IBAN failed validation: " + iban);
        }
    }

    // --- IbanValidator.isIbanStructureValid tests ---

    @Test
    void isIbanStructureValid_withNull_returnsFalse() {
        assertFalse(validator.isIbanStructureValid(null));
    }

    @Test
    void isIbanStructureValid_withTooShortInput_returnsFalse() {
        assertFalse(validator.isIbanStructureValid("FR7"));
    }

    @Test
    void isIbanStructureValid_withLowercaseCountryCode_throwsInvalidIbanFormatException() {
        assertThrows(InvalidIbanFormatException.class,
                () -> validator.isIbanStructureValid("fr7630006000011234567890189"));
    }

    @Test
    void isIbanStructureValid_withNonDigitControlNumber_throwsInvalidIbanFormatException() {
        assertThrows(InvalidIbanFormatException.class,
                () -> validator.isIbanStructureValid("FRXX30006000011234567890189"));
    }

    @Test
    void isIbanStructureValid_withValidFrenchIban_returnsTrue() {
        assertTrue(validator.isIbanStructureValid("FR7630006000011234567890189"));
    }

    // --- IbanValidator.isIbanValid tests ---

    @Test
    void isIbanValid_withNull_returnsFalse() {
        assertFalse(validator.isIbanValid(null));
    }

    @Test
    void isIbanValid_withEmptyString_returnsFalse() {
        assertFalse(validator.isIbanValid(""));
    }

    @Test
    void isIbanValid_withWrongChecksum_throwsInvalidUnCheckableIbanException() {
        // Valid structure but wrong check digits (changed 76 -> 00)
        assertThrows(InvalidUnCheckableIbanException.class,
                () -> validator.isIbanValid("FR0030006000011234567890189"));
    }

    @Test
    void isIbanValid_withValidFrenchIban_returnsTrue() {
        assertTrue(validator.isIbanValid("FR7630006000011234567890189"));
    }

    @Test
    void isIbanValid_withKnownValidGermanIban_returnsTrue() {
        assertTrue(validator.isIbanValid("DE89370400440532013000"));
    }
}
