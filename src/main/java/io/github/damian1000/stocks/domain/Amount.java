package io.github.damian1000.stocks.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

/**
 * A price with its currency, normalised to an ISO code on construction.
 *
 * <p>Lombok stays here for the same reason it stays on the entities: {@code @Embeddable} is
 * persisted by Hibernate, which builds an instance through a no-arg constructor and populates it
 * by field. That rules out a record, so this keeps generated accessors rather than thirty lines of
 * hand-written ones.
 */
@Data
@ToString
@EqualsAndHashCode
@Embeddable
@NoArgsConstructor
public class Amount {

    private String currency;
    private BigDecimal price;
    private static Map<String, String> inputOutputCurrency;

    static {
        inputOutputCurrency = new HashMap<>();
        // euro
        inputOutputCurrency.put("€", "EUR");
        inputOutputCurrency.put("Euro", "EUR");
        // pounds
        inputOutputCurrency.put("£", "GBP");
        inputOutputCurrency.put("British Pounds", "GBP");
        // us
        inputOutputCurrency.put("$", "USD");
        inputOutputCurrency.put("U.S. Dollars", "USD");
        // russia
        inputOutputCurrency.put("new", "RUB");
        inputOutputCurrency.put("Ruble", "RUB");
        inputOutputCurrency.put("руб", "RUB");
        // turkey
        inputOutputCurrency.put("Turkish Lira", "TRL");
        inputOutputCurrency.put("TL", "TRL");
        // iceland
        inputOutputCurrency.put("Icelandic Krona", "ISK");
        // georgia
        inputOutputCurrency.put("Georgian Lari", "GEL");
        // other
        inputOutputCurrency.put("Kč", "CZK");
        inputOutputCurrency.put("Swiss Francs", "CHF");
        inputOutputCurrency.put("Swedish Krona", "SEK");
        inputOutputCurrency.put("Danish Krone", "DKK");
        inputOutputCurrency.put("Czech Koruna", "CZK");
        inputOutputCurrency.put("Norwegian Krone", "NOK");
        inputOutputCurrency.put("Argentine Peso", "ARS");
        inputOutputCurrency.put("Australian Dollars", "AUD");
        inputOutputCurrency.put("Canadian Dollars", "CAD");
        inputOutputCurrency.put("Chilean Peso", "CLP");
        inputOutputCurrency.put("Mexican Pesos", "MXN");
        inputOutputCurrency.put("New Sol", "PEN");;
        inputOutputCurrency.put("Philippine Pesos", "PHP");
        inputOutputCurrency.put("Rand", "ZAR");
        inputOutputCurrency.put("Real", "BRL");
        inputOutputCurrency.put("Renminbi", "CNY");
        inputOutputCurrency.put("Rupee", "INR");
        inputOutputCurrency.put("Shekel", "ILS");
        inputOutputCurrency.put("Taiwanese Dollars", "TWD");
        inputOutputCurrency.put("Yen", "JPY");
        inputOutputCurrency.put("Won", "KRW");
        inputOutputCurrency.put("N.Z. Dollars", "NZD");
        inputOutputCurrency.put("Hong Kong Dollars", "HKD");
        inputOutputCurrency.put("Singapore Dollars", "SGD");
        inputOutputCurrency.put("Peso", "MXN");
        inputOutputCurrency.put("Sol", "PEN");
    }

    public Amount(String currency, BigDecimal price) {
        if (currency.equalsIgnoreCase("kr.")) {
            currency = "kr";
        }

        this.price = price;
        if (currency.equals("p") || currency.equals("GBp")) {
            this.price = price.divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
            this.currency = "GBP";
        } else {
            String newCurrency = inputOutputCurrency.get(currency);
            if (newCurrency != null) {
                this.currency = newCurrency;
            } else {
                this.currency = currency;
            }
        }
    }

    public static Amount createAmount(String currency, double price) {
        return new Amount(currency, BigDecimal.valueOf(price));
    }

    public static Amount createAmount(String currency, BigDecimal price) {
        return new Amount(currency, price);
    }

    public boolean isPriceZero() {
        return BigDecimal.ZERO.compareTo(price) == 0;
    }

    public boolean isValid() {
        return StringUtils.isNotEmpty(currency) && currency.length() == 3 && currency.toUpperCase().equals(currency);
    }

    public BigDecimal divide(Amount amount) {
        if (price != null && amount.getPrice() != null) {
            if (!amount.isPriceZero()) {
                return price.divide(amount.getPrice(), MathContext.DECIMAL128);
            }
        }
        return null;
    }

}
