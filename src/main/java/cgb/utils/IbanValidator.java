package cgb.utils;

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
        boolean isValidCountryCode = getCountryCode(iban).matches("[A-Z]{2}");
        boolean isValidControlNumbers = true;
        boolean isValidAccountNumbers = getAccountNumber(iban).;

        return true; //TODO
    }

    public boolean isIbanValid(String iban) {
        return true; //TODO
    }
}
