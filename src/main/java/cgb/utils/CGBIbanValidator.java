package cgb.utils;

import org.apache.commons.validator.routines.IBANValidator;
import cgb.transfer.exception.InvalidIbanFormatException;
import cgb.transfer.exception.InvalidUnCheckableIbanException;

public class CGBIbanValidator {

    private static CGBIbanValidator instance;
    private final IBANValidator ibanValidator;

    private CGBIbanValidator() {
        this.ibanValidator = IBANValidator.getInstance();
    }

    public static CGBIbanValidator getInstanceValidator() {
        if (instance == null) {
            instance = new CGBIbanValidator();
        }
        return instance;
    }

    public boolean isIbanStructureValide(String iban) {
        if (iban == null) return false;
        return iban.matches("^[A-Z]{2}\\d{2}[A-Za-z0-9]{11,30}$");
    }

    public boolean isIbanValide(String iban) {
        if (iban == null) return false;
        return ibanValidator.isValid(iban);
    }

    public void validate(String iban) throws InvalidIbanFormatException, InvalidUnCheckableIbanException {
        if (!isIbanStructureValide(iban)) {
            throw new InvalidIbanFormatException("IBAN format invalide: " + iban);
        }
        if (!isIbanValide(iban)) {
            throw new InvalidUnCheckableIbanException("IBAN CRC invalide: " + iban);
        }
    }

    public String getCountryCode(String iban) {
        if (iban == null || iban.length() < 2) return null;
        return iban.substring(0, 2);
    }

    public String getCheckDigits(String iban) {
        if (iban == null || iban.length() < 4) return null;
        return iban.substring(2, 4);
    }

    public String getBBAN(String iban) {
        if (iban == null || iban.length() < 5) return null;
        return iban.substring(4);
    }
}
