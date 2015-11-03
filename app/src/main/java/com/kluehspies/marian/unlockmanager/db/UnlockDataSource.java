package com.kluehspies.marian.unlockmanager.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.kluehspies.marian.unlockmanager.persistence.PersistenceHandler;
import com.kluehspies.marian.unlockmanager.trigger.Trigger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andy on 03.11.2015.
 */
public abstract class UnlockDataSource<T extends Achievement> extends PersistenceHandler<T>{

    private final Database database;
    private SQLiteDatabase sqLiteDatabase;

    public static final String STATE_UNLOCKED = "UNLOCKED";
    public static final String STATE_LOCKED = "LOCKED";

    public UnlockDataSource(Class<T> clazz,Database database){
        super(clazz);
        this.database = database;
    }

    public void openDatabase(){
        sqLiteDatabase = database.getWritableDatabase();
    }

    public boolean add(T item){
        return add(item, STATE_LOCKED);
    }

    public boolean add(T item,String state){
        ContentValues cv = new ContentValues();
        cv.put(UnlockTable.COLUMN_KEY,item.getKey());
        cv.put(UnlockTable.COLUMN_ACTION,item.getAction());
        cv.put(UnlockTable.COLUMN_UNLOCK_STATE, (state == null) ? STATE_LOCKED : state);
        onBindValues(cv,item);
        return sqLiteDatabase.insert(UnlockTable.TABLE_NAME, null, cv) > 0;
    }

    public boolean update(T item, String state){
        ContentValues cv = new ContentValues();
        cv.put(UnlockTable.COLUMN_UNLOCK_STATE,state);
        return sqLiteDatabase.update(
                UnlockTable.TABLE_NAME,
                cv,
                String.format("%s = ? AND %s = ?", UnlockTable.COLUMN_KEY, UnlockTable.COLUMN_ACTION),
                new String[]{item.getKey(), item.getAction()}
        ) > 0;
    }

    public boolean remove(T item){
        return sqLiteDatabase.delete(
                UnlockTable.TABLE_NAME,
                String.format("%s = ? AND %s = ?", UnlockTable.COLUMN_KEY, UnlockTable.COLUMN_ACTION),
                new String[]{item.getKey(), item.getAction()}
        ) > 0;
    }

    public boolean isLocked(T item){
        return getStatus(item).equals(STATE_LOCKED);
    }

    private String getStatus(T item){
        String status = null;
        Cursor cursor = sqLiteDatabase.query(
                UnlockTable.TABLE_NAME,
                new String[]{UnlockTable.COLUMN_UNLOCK_STATE},
                String.format("%s = ? AND %s = ?", UnlockTable.COLUMN_KEY, UnlockTable.COLUMN_ACTION),
                new String[]{item.getKey(), item.getAction()},
                null, null, null
        );
        if (cursor.moveToFirst()){
            status = cursor.getString(0);
        }
        cursor.close();
        return status;
    }

    public List<T> get(){
        return internalGet(null);
    }

    public List<T> getLocked(){
        return internalGet(STATE_LOCKED);
    }

    public List<T> getUnlocked(){
        return internalGet(STATE_UNLOCKED);
    }

    protected abstract T getNewInstance();

    protected ContentValues onBindValues(ContentValues contentValues,T item){
        return contentValues;
    }
    protected T onBindModel(Cursor cursor, T item){
        return item;
    }

    private List<T> internalGet(String withState){
        List<T> items = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.query(
                UnlockTable.TABLE_NAME,
                null,
                String.format("%s = ?", UnlockTable.COLUMN_UNLOCK_STATE),
                (withState != null) ? new String[]{withState} : null,
                null,null,null
        );
        if (cursor.moveToFirst()){
            do{
                String key = cursor.getString(cursor.getColumnIndex(UnlockTable.COLUMN_KEY));
                String action = cursor.getString(cursor.getColumnIndex(UnlockTable.COLUMN_ACTION));
                String state = cursor.getString(cursor.getColumnIndex(UnlockTable.COLUMN_UNLOCK_STATE));
                T item = getNewInstance();
                item.setKey(key);
                item.setAction(action);
                item.setState(state);
                onBindModel(cursor,item);
                items.add(item);
            }while(cursor.moveToNext());
        }
        cursor.close();
        return items;
    }

    public void closeDatabase(){
        if (sqLiteDatabase != null) {
            sqLiteDatabase.close();
            sqLiteDatabase = null;
        }
    }

    @Override
    public void rewardNotAvailable(T item, Trigger<T> trigger) {

    }

    @Override
    public void rewardAvailable(T item, Trigger<T> trigger) {

    }

    @Override
    public void unlockSucceeded(T achievement, Trigger<T> trigger) {
        update(achievement, STATE_UNLOCKED);
    }

    @Override
    public void unlockFailed(T achievement, Trigger<T> trigger) {
        update(achievement, STATE_LOCKED);
    }

    @Override
    public void triggerUnlockIfAvailable(T item) {
        if (isUnlocked(item))
            unlockSucceeded(item,this);
        else
            unlockFailed(item,this);
    }

    @Override
    public boolean isUnlocked(T item) {
        return getStatus(item).equals(STATE_UNLOCKED);
    }
}
