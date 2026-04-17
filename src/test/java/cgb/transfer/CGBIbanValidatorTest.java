package cgb.transfer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cgb.transfer.exception.InvalidIbanFormatException;
import cgb.transfer.exception.InvalidUnCheckableIbanException;
import cgb.utils.CGBIbanValidator;
import cgb.utils.IbanGenerator;

public class CGBIbanValidatorTest {

    private CGBIbanValidator validator;

    @BeforeEach
    void setUp() {
        validator = CGBIbanValidator.getInstanceValidator();
    }

    @Test
    void testSingletonReturnsSameInstance() {
        CGBIbanValidator v1 = CGBIbanValidator.getInstanceValidator();
        CGBIbanValidator v2 = CGBIbanValidator.getInstanceValidator();
        assertSame(v1, v2);
    }

    @Test
    void testValidIbanFromGenerator() {
        String iban = IbanGenerator.generateValidIban();
        assertTrue(validator.isIbanStructureValide(iban));
        assertTrue(validator.isIbanValide(iban));
    }

    @Test
    void testValidatePassesForValidIban() {
        String iban = IbanGenerator.generateValidIban();
        assertDoesNotThrow(() -> validator.validate(iban));
    }

    @Test
    void testInvalidFormat_TooShort() {
        assertFalse(validator.isIbanStructureValide("FR12"));
    }

    @Test
    void testInvalidFormat_NoCountryCode() {
        assertFalse(validator.isIbanStructureValide("1234567890123456"));
    }

    @Test
    void testInvalidFormat_LowercaseCountry() {
        assertFalse(validator.isIbanStructureValide("fr1234567890123456789012345"));
    }

    @Test
    void testValidateThrowsFormatException() {
        assertThrows(InvalidIbanFormatException.class, () -> validator.validate("INVALID"));
    }

    @Test
    void testValidStructureButInvalidCRC() {
        String badCrc = "FR0000000000000000000000000";
        assertTrue(validator.isIbanStructureValide(badCrc));
        assertFalse(validator.isIbanValide(badCrc));
    }

    @Test
    void testValidateThrowsCRCException() {
        String badCrc = "FR0000000000000000000000000";
        assertThrows(InvalidUnCheckableIbanException.class, () -> validator.validate(badCrc));
    }

    @Test
    void testGetCountryCode() {
        String iban = IbanGenerator.generateValidIban();
        assertEquals("FR", validator.getCountryCode(iban));
    }

    @Test
    void testGetCheckDigits() {
        String iban = IbanGenerator.generateValidIban();
        String digits = validator.getCheckDigits(iban);
        assertNotNull(digits);
        assertEquals(2, digits.length());
        assertTrue(digits.matches("\\d{2}"));
    }

    @Test
    void testGetBBAN() {
        String iban = IbanGenerator.generateValidIban();
        String bban = validator.getBBAN(iban);
        assertNotNull(bban);
        assertEquals(23, bban.length());
    }

    @Test
    void testNullIban() {
        assertFalse(validator.isIbanStructureValide(null));
        assertFalse(validator.isIbanValide(null));
        assertNull(validator.getCountryCode(null));
        assertNull(validator.getCheckDigits(null));
        assertNull(validator.getBBAN(null));
    }
}
