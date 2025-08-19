package eci.technician.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Settings {
    public static Gson createGson() {
        return new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z").create();
    }
}