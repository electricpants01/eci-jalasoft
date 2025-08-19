package eci.technician.models;

import com.google.gson.annotations.SerializedName;

public class RequestPart {
    @SerializedName("ItemId")
    private int itemId;
    private String name;
    @SerializedName("Quantity")
    private double quantity;

    public RequestPart(int itemId, String name, double quantity) {
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
}
