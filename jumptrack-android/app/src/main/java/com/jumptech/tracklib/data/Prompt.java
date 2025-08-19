package com.jumptech.tracklib.data;

import android.graphics.Color;

import com.google.gson.annotations.SerializedName;
import com.jumptech.jumppod.R;

public class Prompt {
    public enum Type {
        OPTIMIZATION,
        PROMPT,
        ;
    }

    public enum Style {
        GOOD(R.color.prompt_good_text_color),
        NORMAL(R.color.login_control),
        WARNING(R.color.prompt_warning_text_color),
        ERROR(R.color.stop_windowDisplay_error_text_color),
        ;

        private int _color;

        Style(int color) {
            _color = color;
        }

        public int getColor() {
            return _color;
        }
    }

    public long _key;
    public Type _type;
    @SerializedName("code")
    public String _code;
    @SerializedName("style")
    public Style _style;
    @SerializedName("message")
    public String _message;

    public Prompt() {
    }

    public Prompt(String message) {
        _style = Style.NORMAL;
        _message = message;
    }

    public Style getStyle() {
        return _style;
    }

    public String getMessage() {
        return _message;
    }

}
