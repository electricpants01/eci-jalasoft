package com.jumptech.tracklib.repository;

import android.content.Context;
import android.database.Cursor;

import androidx.sqlite.db.SimpleSQLiteQuery;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.jumptech.android.util.Util;
import com.jumptech.tracklib.comms.CommandPrompt;
import com.jumptech.tracklib.data.Prompt;
import com.jumptech.tracklib.room.TrackDB;

public class PromptRepository {

    public static Prompt storeFetch(Context context, JsonElement fetch) {
        //TODO put somewhere else? for(String tbl : TrackDB.TABLES) db.delete(tbl, null, null);
        Gson gson = new Gson();
        PromptRepository.storePrompt(context, gson.fromJson(fetch.getAsJsonObject().get("optimization"), Prompt.class), Prompt.Type.OPTIMIZATION);
        StopRepository.storeStops(context, fetch.getAsJsonObject().get("stops"));

        SiteRepository.guiPostProcess(context);

        return gson.fromJson(fetch.getAsJsonObject().get("prompt"), Prompt.class);
    }

    public static void prompt(Context context, CommandPrompt cmdPrompt) {
        if (cmdPrompt != null) {
            RouteRepository.updateCommand(context, cmdPrompt.command.name());
            if (cmdPrompt.prompt != null)
                PromptRepository.storePrompt(context, cmdPrompt.prompt, Prompt.Type.PROMPT);
        }
    }

    public static void storePrompt(Context context, Prompt prompt, Prompt.Type type) {
        if (prompt != null && prompt._message != null) {
            com.jumptech.tracklib.room.entity.Prompt promptEntity = new com.jumptech.tracklib.room.entity.Prompt();
            promptEntity.setType(type.name());
            promptEntity.setCode(prompt._code);
            promptEntity.setStyle(prompt._style.name());
            promptEntity.setMessage(prompt._message);
            if (Prompt.Type.OPTIMIZATION == type) {
                TrackDB.getInstance(context).getPromptDao().insertValues(type.name(), prompt._code,
                        prompt._style.name(), prompt._message, 0);
            } else {
                TrackDB.getInstance(context).getPromptDao().insert(promptEntity);
            }
        }
    }

    public static Prompt prompt(Context context, Prompt.Type type) {

        Cursor cur = null;
        try {
            String queryString = "SELECT rowid, type, code, style, message FROM prompt WHERE " + (Prompt.Type.OPTIMIZATION.equals(type) ? "rowid = 0" : "type = 'PROMPT' ") + " ORDER BY rowid";

            SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString, null);

            cur = TrackDB.getInstance(context).getGenericDao().genericQuery(query);
            return cur.moveToFirst() ? prompt(cur) : null;
        } finally {
            Util.close(cur);
        }
    }

    public static void deleteFromId(Context context, long rowId) {
        TrackDB.getInstance(context).getPromptDao().deleteFromId(rowId);
    }

    private static Prompt prompt(Cursor cur) {
        int c = -1;
        Prompt prompt = new Prompt();
        prompt._key = cur.getLong(++c);
        prompt._type = cur.isNull(++c) ? null : Prompt.Type.valueOf(cur.getString(c));
        prompt._code = cur.getString(++c);
        prompt._style = cur.isNull(++c) ? null : Prompt.Style.valueOf(cur.getString(c));
        prompt._message = cur.getString(++c);
        return prompt;
    }


}
