package sg.edu.sutd.bank.webapp.util;

import java.util.Random;

/**
 * A Account number generator
 */
public class AccountNumberGenerator {

    private static int ACCOUNT_NUMBER_LENGTH = 9;

    private static Random random = new Random(System.currentTimeMillis());

    /**
     * Generates a random valid account number
     */
    public static synchronized String generate() {

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ACCOUNT_NUMBER_LENGTH; i++) {
            int digit = random.nextInt(10);
            builder.append(digit);
        }

        // Do the Luhn algorithm to generate the check digit.
        int checkDigit = getCheckDigit(builder.toString());
        builder.append(checkDigit);

        return builder.toString();
    }

    /**
     * Generates the check digit required to make the given account number
     * valid (i.e. pass the Luhn check)
     *
     * @param number
     *            The account number for which to generate the check digit.
     * @return The check digit required to make the given account number
     *         valid.
     */
    private static int getCheckDigit(String number) {

        // Get the sum of all the digits, however we need to replace the value
        // of the first digit, and every other digit, with the same digit
        // multiplied by 2. If this multiplication yields a number greater
        // than 9, then add the two digits together to get a single digit
        // number.
        //
        // The digits we need to replace will be those in an even position for
        // card numbers whose length is an even number, or those is an odd
        // position for card numbers whose length is an odd number. This is
        // because the Luhn algorithm reverses the card number, and doubles
        // every other number starting from the second number from the last
        // position.
        int sum = 0;
        for (int i = 0; i < number.length(); i++) {

            // Get the digit at the current position.
            int digit = Integer.parseInt(number.substring(i, (i + 1)));

            if ((i % 2) == 0) {
                digit = digit * 2;
                if (digit > 9) {
                    digit = (digit / 10) + (digit % 10);
                }
            }
            sum += digit;
        }

        // The check digit is the number required to make the sum a multiple of
        // 10.
        int mod = sum % 10;
        return ((mod == 0) ? 0 : 10 - mod);
    }

    public static boolean validateAccountNumber(String accountNumber) {
        try {
            String preAccountNumber = accountNumber.substring(0, ACCOUNT_NUMBER_LENGTH);
            String checkSumDigit = accountNumber.substring(ACCOUNT_NUMBER_LENGTH);

            return getCheckDigit(preAccountNumber) == Integer.valueOf(checkSumDigit);

        } catch (Exception e) {
            return false;
        }
    }
}