package com.coursecoder.training;

import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import spark.ModelAndView;
import spark.Spark;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

public class CurrencyConverter {
    private String name;
    private String shortName;
    private HashMap<String, Double> exchangeValues = new HashMap<>();

    // Cache live rates at startup
    private static HashMap<String, Double> liveRates = new HashMap<>();
    private static String lastUpdated = "never";

    // Supported currencies: display name → ISO code
    private static final HashMap<String, String> SUPPORTED = new LinkedHashMap<>() {{
        put("US Dollar",               "USD");
        put("Euro",                    "EUR");
        put("British Pound",           "GBP");
        put("Swiss Franc",             "CHF");
        put("Chinese Yuan Renminbi",   "CNY");
        put("Japanese Yen",            "JPY");
        put("Canadian Dollar",         "CAD");
        put("Australian Dollar",       "AUD");
        put("Indian Rupee",            "INR");
        put("Mexican Peso",            "MXN");
    }};

    public CurrencyConverter(String nameValue, String shortNameValue) {
        this.name = nameValue;
        this.shortName = shortNameValue;
    }

    public String getName()                          { return this.name; }
    public void   setName(String name)               { this.name = name; }
    public String getShortName()                     { return this.shortName; }
    public void   setShortName(String shortName)     { this.shortName = shortName; }
    public HashMap<String, Double> getExchangeValues() { return this.exchangeValues; }
    public void setExchangeValues(String key, Double value) { this.exchangeValues.put(key, value); }

    // ─────────────────────────────────────────────
    // Fetch live rates from ExchangeRate-API (free,
    // no API key required for basic USD base rates)
    // ─────────────────────────────────────────────
    public static boolean fetchLiveRates() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.exchangerate-api.com/v4/latest/USD"))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            // Simple manual JSON parse — no extra dependencies needed
            // Response looks like: {"rates":{"USD":1.0,"EUR":0.92,...}}
            int ratesStart = body.indexOf("\"rates\":{") + 9;
            int ratesEnd   = body.indexOf("}", ratesStart);
            String ratesPart = body.substring(ratesStart, ratesEnd);

            liveRates.clear();
            for (String entry : ratesPart.split(",")) {
                String[] kv = entry.replace("\"", "").split(":");
                if (kv.length == 2) {
                    liveRates.put(kv[0].trim(), Double.parseDouble(kv[1].trim()));
                }
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            lastUpdated = LocalDateTime.now().format(fmt);
            System.out.println("[CurrencyConverter] Live rates fetched at " + lastUpdated);
            return true;

        } catch (Exception e) {
            System.err.println("[CurrencyConverter] API fetch failed: " + e.getMessage());
            System.err.println("[CurrencyConverter] Falling back to hardcoded rates.");
            return false;
        }
    }

    // ─────────────────────────────────────────────
    // Convert using live rates (all relative to USD)
    // e.g. USD→EUR: amount * liveRates["EUR"]
    //      EUR→GBP: (amount / liveRates["EUR"]) * liveRates["GBP"]
    // ─────────────────────────────────────────────
    public static Double convertLive(String fromCode, String toCode, Double amount) {
        if (!liveRates.containsKey(fromCode) || !liveRates.containsKey(toCode)) {
            return 0.0;
        }
        double inUSD = amount / liveRates.get(fromCode);
        double result = inUSD * liveRates.get(toCode);
        return Math.round(result * 100d) / 100d;
    }

    public static void main(String[] args) {

        // Fetch live rates on startup; fall back to hardcoded if API is unreachable
        boolean usingLiveRates = fetchLiveRates();
        ArrayList<CurrencyConverter> currencies = CurrencyConverter.init(usingLiveRates);

        ThymeleafTemplateEngine engine = new ThymeleafTemplateEngine();

        // ── GET /currency-converter — main form ──
        Spark.get("/currency-converter", (req, res) -> {
            Map<String, Serializable> model = new HashMap<>();
            model.put("Title", "Currency Converter");
            model.put("currencies", currencies);
            model.put("lastUpdated", lastUpdated);
            model.put("usingLiveRates", usingLiveRates);
            return new ModelAndView(model, "template");
        }, engine);

        // ── GET /currency-conversion — result page ──
        Spark.get("/currency-conversion", (request, response) -> {
            String nameCurrency1 = request.queryParams("from");
            String nameCurrency2 = request.queryParams("to");
            String amountString  = request.queryParams("amount");

            if (amountString == null || amountString.isEmpty()) amountString = "0";

            Double amount = Double.parseDouble(amountString);
            DecimalFormat format = new DecimalFormat("#0.00");
            Double price;

            if (usingLiveRates) {
                // Look up ISO codes for the selected display names
                String fromCode = SUPPORTED.get(nameCurrency1);
                String toCode   = SUPPORTED.get(nameCurrency2);
                price = convertLive(fromCode, toCode, amount);
            } else {
                price = convert(nameCurrency1, nameCurrency2, currencies, amount);
            }

            String result = format.format(amount) + " " + nameCurrency1
                          + " = " + format.format(price) + " " + nameCurrency2;

            Map<String, String> model = new HashMap<>();
            model.put("Title", "Currency Converter");
            model.put("convertedCurrencies", result);
            model.put("lastUpdated", lastUpdated);
            model.put("usingLiveRates", String.valueOf(usingLiveRates));
            return new ModelAndView(model, "currencyConversion");
        }, engine);
    }

    // ─────────────────────────────────────────────
    // Init — builds currency list from SUPPORTED map
    // ─────────────────────────────────────────────
    public static ArrayList<CurrencyConverter> init(boolean usingLiveRates) {
        ArrayList<CurrencyConverter> currencies = new ArrayList<>();
        for (Map.Entry<String, String> entry : SUPPORTED.entrySet()) {
            CurrencyConverter c = new CurrencyConverter(entry.getKey(), entry.getValue());
            if (!usingLiveRates) c.defaultValues();
            currencies.add(c);
        }
        return currencies;
    }

    // ─────────────────────────────────────────────
    // Fallback: hardcoded rates (used if API fails)
    // ─────────────────────────────────────────────
    public void defaultValues() {
        switch (this.name) {
            case "US Dollar":
                exchangeValues.put("USD", 1.00); exchangeValues.put("EUR", 0.93);
                exchangeValues.put("GBP", 0.66); exchangeValues.put("CHF", 1.01);
                exchangeValues.put("CNY", 6.36); exchangeValues.put("JPY", 123.54);
                exchangeValues.put("CAD", 1.36); exchangeValues.put("AUD", 1.53);
                exchangeValues.put("INR", 83.10); exchangeValues.put("MXN", 17.15);
                break;
            case "Euro":
                exchangeValues.put("USD", 1.07); exchangeValues.put("EUR", 1.00);
                exchangeValues.put("GBP", 0.71); exchangeValues.put("CHF", 1.08);
                exchangeValues.put("CNY", 6.83); exchangeValues.put("JPY", 132.57);
                exchangeValues.put("CAD", 1.46); exchangeValues.put("AUD", 1.65);
                exchangeValues.put("INR", 89.50); exchangeValues.put("MXN", 18.45);
                break;
            case "British Pound":
                exchangeValues.put("USD", 1.51); exchangeValues.put("EUR", 1.41);
                exchangeValues.put("GBP", 1.00); exchangeValues.put("CHF", 1.52);
                exchangeValues.put("CNY", 9.60); exchangeValues.put("JPY", 186.41);
                exchangeValues.put("CAD", 2.06); exchangeValues.put("AUD", 2.32);
                exchangeValues.put("INR", 126.00); exchangeValues.put("MXN", 25.95);
                break;
            case "Swiss Franc":
                exchangeValues.put("USD", 0.99); exchangeValues.put("EUR", 0.93);
                exchangeValues.put("GBP", 0.66); exchangeValues.put("CHF", 1.00);
                exchangeValues.put("CNY", 6.33); exchangeValues.put("JPY", 122.84);
                exchangeValues.put("CAD", 1.35); exchangeValues.put("AUD", 1.52);
                exchangeValues.put("INR", 82.30); exchangeValues.put("MXN", 16.95);
                break;
            case "Chinese Yuan Renminbi":
                exchangeValues.put("USD", 0.16); exchangeValues.put("EUR", 0.15);
                exchangeValues.put("GBP", 0.11); exchangeValues.put("CHF", 0.16);
                exchangeValues.put("CNY", 1.00); exchangeValues.put("JPY", 19.41);
                exchangeValues.put("CAD", 0.21); exchangeValues.put("AUD", 0.24);
                exchangeValues.put("INR", 13.05); exchangeValues.put("MXN", 2.69);
                break;
            case "Japanese Yen":
                exchangeValues.put("USD", 0.008); exchangeValues.put("EUR", 0.007);
                exchangeValues.put("GBP", 0.005); exchangeValues.put("CHF", 0.008);
                exchangeValues.put("CNY", 0.051); exchangeValues.put("JPY", 1.000);
                exchangeValues.put("CAD", 0.011); exchangeValues.put("AUD", 0.012);
                exchangeValues.put("INR", 0.67);  exchangeValues.put("MXN", 0.14);
                break;
            case "Canadian Dollar":
                exchangeValues.put("USD", 0.74); exchangeValues.put("EUR", 0.69);
                exchangeValues.put("GBP", 0.49); exchangeValues.put("CHF", 0.74);
                exchangeValues.put("CNY", 4.68); exchangeValues.put("JPY", 90.84);
                exchangeValues.put("CAD", 1.00); exchangeValues.put("AUD", 1.13);
                exchangeValues.put("INR", 61.10); exchangeValues.put("MXN", 12.60);
                break;
            case "Australian Dollar":
                exchangeValues.put("USD", 0.65); exchangeValues.put("EUR", 0.61);
                exchangeValues.put("GBP", 0.43); exchangeValues.put("CHF", 0.66);
                exchangeValues.put("CNY", 4.15); exchangeValues.put("JPY", 80.55);
                exchangeValues.put("CAD", 0.89); exchangeValues.put("AUD", 1.00);
                exchangeValues.put("INR", 54.20); exchangeValues.put("MXN", 11.18);
                break;
            case "Indian Rupee":
                exchangeValues.put("USD", 0.012); exchangeValues.put("EUR", 0.011);
                exchangeValues.put("GBP", 0.008); exchangeValues.put("CHF", 0.012);
                exchangeValues.put("CNY", 0.077); exchangeValues.put("JPY", 1.49);
                exchangeValues.put("CAD", 0.016); exchangeValues.put("AUD", 0.018);
                exchangeValues.put("INR", 1.00);  exchangeValues.put("MXN", 0.21);
                break;
            case "Mexican Peso":
                exchangeValues.put("USD", 0.058); exchangeValues.put("EUR", 0.054);
                exchangeValues.put("GBP", 0.039); exchangeValues.put("CHF", 0.059);
                exchangeValues.put("CNY", 0.37);  exchangeValues.put("JPY", 7.20);
                exchangeValues.put("CAD", 0.079); exchangeValues.put("AUD", 0.089);
                exchangeValues.put("INR", 4.84);  exchangeValues.put("MXN", 1.00);
                break;
        }
    }

    // Legacy static convert (used in fallback mode)
    public static Double convert(String currency1, String currency2,
                                  ArrayList<CurrencyConverter> currencies, Double amount) {
        String shortNameCurrency2 = null;
        for (CurrencyConverter c : currencies) {
            if (c.getName().equals(currency2)) { shortNameCurrency2 = c.getShortName(); break; }
        }
        if (shortNameCurrency2 != null) {
            for (CurrencyConverter c : currencies) {
                if (c.getName().equals(currency1)) {
                    Double exchangeValue = c.getExchangeValues().get(shortNameCurrency2);
                    return convert(amount, exchangeValue);
                }
            }
        }
        return 0.0;
    }

    public static Double convert(Double amount, Double exchangeValue) {
        Double price = amount * exchangeValue;
        return Math.round(price * 100d) / 100d;
    }
}
