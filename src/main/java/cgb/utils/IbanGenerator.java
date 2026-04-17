package cgb.utils;

import java.math.BigInteger;
import java.util.Random;

public class IbanGenerator {

    private static final String COUNTRY_CODE = "FR";
    private static final int BBAN_LENGTH = 23; // Longueur du BBAN pour un IBAN français (27 - 4)
    private static final Random RANDOM = new Random();

    /**
     * Génère un IBAN fictif valide pour la France.
     *
     * @return Un IBAN fictif valide.
     */
    public static String generateValidIban() {
        // Génère un BBAN fictif (sans les chiffres de contrôle)
        StringBuilder bban = new StringBuilder(BBAN_LENGTH);
        for (int i = 0; i < BBAN_LENGTH; i++) {
            bban.append(RANDOM.nextInt(10));
        }

        // Calcule le CRC (chiffres de contrôle)
        String checkDigits = calculateCheckDigits(COUNTRY_CODE, bban.toString());

        // Assembler l'IBAN complet
        return COUNTRY_CODE + checkDigits + bban.toString();
    }

    /**
     * Calcule les chiffres de contrôle pour un IBAN donné.
     * Algorithme standard : déplacer les 4 premiers caractères à la fin,
     * remplacer les lettres par leurs valeurs numériques (A=10, B=11, ..., Z=35),
     * ajouter "00" à la place des chiffres de contrôle, calculer 98 - (mod 97).
     *
     * @param countryCode Le code du pays.
     * @param bban Le BBAN (sans les chiffres de contrôle).
     * @return Les chiffres de contrôle calculés.
     */
    private static String calculateCheckDigits(String countryCode, String bban) {
        // Rearrangement : BBAN + pays + "00"
        String rearranged = bban + countryCode + "00";

        // Convertir les lettres en chiffres (A=10, B=11, ..., Z=35)
        StringBuilder numericString = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numericString.append(c - 'A' + 10);
            } else {
                numericString.append(c);
            }
        }

        // Calcule du modulo 97 avec BigInteger pour éviter les débordements
        BigInteger number = new BigInteger(numericString.toString());
        int remainder = number.mod(BigInteger.valueOf(97)).intValue();
        int checkDigits = 98 - remainder;

        // Formatage du chiffre de contrôle en deux chiffres
        return String.format("%02d", checkDigits);
    }

    public static void main(String[] args) {
        String validIban = generateValidIban();
        System.out.println("Generated Valid IBAN: " + validIban);
        System.out.println("Length: " + validIban.length());
    }
}
