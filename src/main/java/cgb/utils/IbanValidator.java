package cgb.utils;

import cgb.transfer.exception.InvalidIbanFormatException;
import cgb.transfer.exception.InvalidUnCheckableIbanException;
import org.apache.commons.validator.routines.IBANValidator;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

public class IbanValidator {
    private static IbanValidator instance = null;

    private IbanValidator() {}

    public static IbanValidator getInstanceValidator() {
        if (instance == null) {
            instance = new IbanValidator();
        }
        return instance;
    }

    private String getCountryCode(String iban) {
        return iban.substring(0 ,2);
    }

    private String getControlNumber(String iban) {
        return iban.substring(2 ,4);
    }

    private String getAccountNumber(String iban) {
        return iban.substring(4, iban.length() - 1);
    }

    public boolean isIbanStructureValid(String iban) {
        if (iban == null || iban.length() < 5) {
            return false;
        }
        boolean isValidCountryCode = getCountryCode(iban).matches("[A-Z]{2}");
        boolean isValidControlNumbers = getControlNumber(iban).matches("\\d{2}");
        boolean isValidAccountNumbers = getAccountNumber(iban).matches("\\w+");

        if ( !(isValidCountryCode && isValidControlNumbers && isValidAccountNumbers) ) {
            throw new InvalidIbanFormatException("Invalid IBAN format.");
        }
        return true;
    }

    public boolean isIbanValid(String iban) {
        if (iban == null || iban.isEmpty()) {
            return false;
        }
        IBANValidator ibv = IBANValidator.getInstance();

        if ( !ibv.isValid(iban) ) {
            throw new InvalidUnCheckableIbanException("Invalid IBAN.");
        }

        return ibv.isValid(iban);
    }
}
