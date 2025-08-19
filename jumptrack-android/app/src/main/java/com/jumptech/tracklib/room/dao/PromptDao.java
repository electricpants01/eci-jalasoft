package com.jumptech.tracklib.room.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.jumptech.tracklib.room.entity.Prompt;

@Dao
public interface PromptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Prompt item);

    @Update
    void update(Prompt item);

    @Delete
    void delete(Prompt item);

    @Query("INSERT OR REPLACE INTO prompt (type ,code , style ,message , rowid) VALUES (:type,:code,:style,:message,:rowId)")
    void insertValues(String type, String code, String style, String message, long rowId);

    @Query("DELETE FROM prompt WHERE rowid = :rowId")
    void deleteFromId(long rowId);

    @Query("DELETE FROM prompt")
    void nukeTable();
}
