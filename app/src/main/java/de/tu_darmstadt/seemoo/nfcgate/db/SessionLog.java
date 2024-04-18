package de.tu_darmstadt.seemoo.nfcgate.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;

@Entity
public class SessionLog {

    public enum SessionType {
        RELAY,
        REPLAY,
        CAPTURE
    }

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    private Date date;

    @ColumnInfo
    private SessionType type;

    public SessionLog(Date date, SessionType type) {
        this.date = date;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public SessionType getType() {
        return type;
    }

    public void setType(SessionType type) {
        this.type = type;
    }

    public static SimpleDateFormat isoDateFormatter() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    }

    @Override
    public String toString() {
        return isoDateFormatter().format(date);
    }
}
