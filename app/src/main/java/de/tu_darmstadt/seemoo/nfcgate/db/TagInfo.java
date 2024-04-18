package de.tu_darmstadt.seemoo.nfcgate.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class TagInfo {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    private String name;

    @ColumnInfo
    private byte[] data;

    public TagInfo(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return getName();
    }
}
