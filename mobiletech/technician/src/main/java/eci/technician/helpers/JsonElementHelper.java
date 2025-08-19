package eci.technician.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class JsonElementHelper {
    public static String getFormattedDate(JsonElement element, String propertyName) {
        DateFormat format = new SimpleDateFormat("HH:mm, MMM dd, yyyy", Locale.getDefault());
        DateFormat converter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z", Locale.getDefault());
        try {
            return DateTimeHelper.INSTANCE.formatTimeDateYear(converter.parse(getElementValue(element, propertyName)));
        } catch (ParseException e) {
            return "";
        }
    }

    public static String getElementValue(JsonElement element, String propertyName) {
        JsonElement jsonElement = element.getAsJsonObject().get(propertyName);
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return "";
        }
        return jsonElement.getAsString();
    }

    public static String getMeters(JsonElement element) {
        StringBuilder result = new StringBuilder();
        JsonArray meters = element.getAsJsonObject().get("Meters").getAsJsonArray();
        if (meters.size() == 0) {
            result.append("No meter data");
        } else {
            for (int i = 0; i < meters.size(); i++) {
                if (i > 0) {
                    result.append("\n");
                }
                JsonElement jsonElement = meters.get(i);
                result.append(getElementValue(jsonElement, "MeterType"));
                result.append("\n");
                result.append("Display: ");
                result.append(jsonWithoutDecimal(getElementValue(jsonElement, "Display")));
                result.append("\n");
                result.append("Actual: ");
                result.append(jsonWithoutDecimal(getElementValue(jsonElement, "Actual")));
            }
        }
        return result.toString();
    }

    private static String jsonWithoutDecimal(String value) {
        if (value.contains(".00")) {
            return value.substring(0, value.length() - 3);
        } else {
            return value;
        }
    }

    public static int getPartsCount(JsonElement element) {
        return element.getAsJsonObject().get("Parts").getAsJsonArray().size();
    }

    public static String getParts(JsonElement element) {
        StringBuilder result = new StringBuilder();
        JsonArray items = element.getAsJsonObject().get("Parts").getAsJsonArray();
        if (items.size() == 0) {
            result.append("No used parts data");
        } else {
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    result.append("\n");
                }
                JsonElement jsonElement = items.get(i);
                result.append(getElementValue(jsonElement, "Item"));
                result.append(" - ");
                result.append(getElementValue(jsonElement, "Description"));
                result.append("\n");
                result.append("Quantity: ");
                result.append(jsonWithoutDecimal(getElementValue(jsonElement, "Quantity")));
                result.append("\n");
                result.append("Cost: ");
                String cost = getElementValue(jsonElement, "Cost");
                result.append(DecimalsHelper.INSTANCE.getAmountWithCurrency(cost));
                result.append("\n");
                result.append("VoidCost: ");
                String voidCost = getElementValue(jsonElement, "VoidCost");
                result.append(DecimalsHelper.INSTANCE.getAmountWithCurrency(voidCost));
                result.append("\n");
            }
        }
        return result.toString();
    }

    public static String getProblemCodes(JsonElement element) {
        StringBuilder result = new StringBuilder();
        JsonArray items = element.getAsJsonObject().get("ProblemCodes").getAsJsonArray();
        if (items.size() == 0) {
            result.append("No problem codes data");
        } else {
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    result.append("\n");
                }
                JsonElement jsonElement = items.get(i);
                result.append(getElementValue(jsonElement, "ProblemCodeName"));
                result.append(" - ");
                result.append(getElementValue(jsonElement, "Description"));
                result.append("\n");
            }
        }
        String resultWithoutNewLine = result.toString().trim();
        return resultWithoutNewLine;
    }

    public static String getRepairCodes(JsonElement element) {
        StringBuilder result = new StringBuilder();
        JsonArray items = element.getAsJsonObject().get("RepairCodes").getAsJsonArray();
        if (items.size() == 0) {
            result.append("No repair codes data");
        } else {
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    result.append("\n");
                }
                JsonElement jsonElement = items.get(i);
                result.append(getElementValue(jsonElement, "RepairCodeName"));
                result.append(" - ");
                result.append(getElementValue(jsonElement, "Description"));
                result.append("\n");
            }
        }
        String resultWithoutNewLine = result.toString().trim();
        return resultWithoutNewLine;
    }
}
