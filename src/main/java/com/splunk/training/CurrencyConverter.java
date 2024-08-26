package com.splunk.training;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.lang.Double;

import spark.ModelAndView;
import spark.Spark;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

public class CurrencyConverter {
    private String name;
    private String shortName;
    private HashMap<String, Double> exchangeValues = new HashMap<String, Double>();

    // "Currency" Constructor
    public CurrencyConverter(String nameValue, String shortNameValue) {
        this.name = nameValue;
        this.shortName = shortNameValue;
    }

    // Getter for name
    public String getName() {
        return this.name;
    }

    // Setter for name
    public void setName(String name) {
        this.name = name;
    }

    // Getter for shortName
    public String getShortName() {
        return this.shortName;
    }

    // Setter for shortName
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    // Getter for exchangeValues
    public HashMap<String, Double> getExchangeValues() {
        return this.exchangeValues;
    }

    // Setter for exchangeValues
    public void setExchangeValues(String key, Double value) {
        this.exchangeValues.put(key, value);
    }


    public static void main( String[] args ){

        ArrayList<CurrencyConverter> currencies = CurrencyConverter.init();
        Map<String, Serializable> currenciesMap = new HashMap<String, Serializable>();
        Map<String, String> mappedCurrencies = new HashMap<String, String>();
        ThymeleafTemplateEngine engine = new ThymeleafTemplateEngine();

        Spark.get("/currency-conversion", (request, response) -> {

            String nameCurrency1 = request.queryParams("from");
            String nameCurrency2 = request.queryParams("to");
            String amountString = request.queryParams("amount");
            if (amountString == null || amountString.isEmpty()) {
                amountString = "0";
            }

            String result;
            String formattedPrice;
            String formattedAmount;
            Double price;
            Double amount = Double.parseDouble(amountString);
            DecimalFormat format = new DecimalFormat("#0.00");



            price = convert(nameCurrency1, nameCurrency2, currencies, amount);

            // Format numbers to avoid "E7" problem
            formattedPrice = format.format(price);
            formattedAmount = format.format(amount);

            result = formattedAmount + " " + nameCurrency1 + " = " + formattedPrice + " " + nameCurrency2;

            mappedCurrencies.put("Title", "Currency Converter");
            mappedCurrencies.put("convertedCurrencies", result);
            return new ModelAndView(mappedCurrencies, "currencyConversion");
        }, engine);


        currenciesMap.put("Title", "Currency Converter");


        currenciesMap.put("currencies", currencies);
        Spark.get("/currency-converter", (req, res) -> new ModelAndView(currenciesMap, "template"), engine);

    }

    // Find the short name and the exchange value of the second currency
    public static Double convert(String currency1, String currency2, ArrayList<CurrencyConverter> currencies, Double amount) {
        String shortNameCurrency2 = null;
        Double exchangeValue;
        Double price = 0.0;

        // Find shortname for the second currency
        for (int i = 0; i < currencies.size(); i++) {
            if (currencies.get(i).getName().equals(currency2)) {
                shortNameCurrency2 = currencies.get(i).getShortName();
                break;
            }
        }

        // Find exchange value and call convert() to calcul the new price
        if (shortNameCurrency2 != null) {
            for (int i = 0; i < currencies.size(); i++) {
                if (currencies.get(i).getName().equals(currency1)) {
                    exchangeValue = currencies.get(i).getExchangeValues().get(shortNameCurrency2);
                    price = CurrencyConverter.convert(amount, exchangeValue);
                    break;
                }
            }
        }
        return price;
    }

    // Set default values for a currency
    public void defaultValues() {
        String currency = this.name;

        switch (currency) {
            case "US Dollar":
                this.exchangeValues.put("USD", 1.00);
                this.exchangeValues.put("EUR", 0.93);
                this.exchangeValues.put("GBP", 0.66);
                this.exchangeValues.put("CHF", 1.01);
                this.exchangeValues.put("CNY", 6.36);
                this.exchangeValues.put("JPY", 123.54);
                break;
            case "Euro":
                this.exchangeValues.put("USD", 1.073);
                this.exchangeValues.put("EUR", 1.00);
                this.exchangeValues.put("GBP", 0.71);
                this.exchangeValues.put("CHF", 1.08);
                this.exchangeValues.put("CNY", 6.83);
                this.exchangeValues.put("JPY", 132.57);
                break;
            case "British Pound":
                this.exchangeValues.put("USD", 1.51);
                this.exchangeValues.put("EUR", 1.41);
                this.exchangeValues.put("GBP", 1.00);
                this.exchangeValues.put("CHF", 1.52);
                this.exchangeValues.put("CNY", 9.60);
                this.exchangeValues.put("JPY", 186.41);
                break;
            case "Swiss Franc":
                this.exchangeValues.put("USD", 0.99);
                this.exchangeValues.put("EUR", 0.93);
                this.exchangeValues.put("GBP", 0.66);
                this.exchangeValues.put("CHF", 1.00);
                this.exchangeValues.put("CNY", 6.33);
                this.exchangeValues.put("JPY", 122.84);
                break;
            case "Chinese Yuan Renminbi":
                this.exchangeValues.put("USD", 0.16);
                this.exchangeValues.put("EUR", 0.15);
                this.exchangeValues.put("GBP", 0.11);
                this.exchangeValues.put("CHF", 0.16);
                this.exchangeValues.put("CNY", 1.00);
                this.exchangeValues.put("JPY", 19.41);
                break;
            case "Japanese Yen":
                this.exchangeValues.put("USD", 0.008);
                this.exchangeValues.put("EUR", 0.007);
                this.exchangeValues.put("GBP", 0.005);
                this.exchangeValues.put("CHF", 0.008);
                this.exchangeValues.put("CNY", 0.051);
                this.exchangeValues.put("JPY", 1.000);
                break;
        }
    }

    // Initialize currencies
    public static ArrayList<CurrencyConverter> init() {
        ArrayList<CurrencyConverter> currencies = new ArrayList<CurrencyConverter>();

        currencies.add( new CurrencyConverter("US Dollar", "USD") );
        currencies.add( new CurrencyConverter("Euro", "EUR") );
        currencies.add( new CurrencyConverter("British Pound", "GBP") );
        currencies.add( new CurrencyConverter("Swiss Franc", "CHF") );
        currencies.add( new CurrencyConverter("Chinese Yuan Renminbi", "CNY") );
        currencies.add( new CurrencyConverter("Japanese Yen", "JPY") );

        for (int i =0; i < currencies.size(); i++) {
            currencies.get(i).defaultValues();
        }

        return currencies;
    }

    // Convert a currency to another
    public static Double convert(Double amount, Double exchangeValue) {
        Double price;
        price = amount * exchangeValue;
        price = Math.round(price * 100d) / 100d;

        return price;
    }

}
