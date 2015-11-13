/*
 * Copyright (C) 2015 José Luis Castro (@calambrenet)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codefriends.droiddatabase;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;

import com.codefriends.droiddatabase.exceptions.DoesNotExistException;
import com.codefriends.droiddatabase.exceptions.NotNullException;
import com.codefriends.droiddatabase.exceptions.NotValidDatabaseTableException;
import com.codefriends.droiddatabase.interfaces.field;
import com.codefriends.droiddatabase.interfaces.notnull;
import com.codefriends.droiddatabase.interfaces.primary_key;
import com.codefriends.droiddatabase.interfaces.size;
import com.codefriends.droiddatabase.interfaces.table;
import com.codefriends.droiddatabase.interfaces.unique;
import com.codefriends.droiddatabase.base.DatabaseModel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by calambrenet on 17/09/15.
 */
public class DroidDatabase<T extends DatabaseModel> extends SQLiteOpenHelper {
    private final String TAG = "droiddatabase";

    public static final int EQUAL = 1;
    public static final int NOT_EQUAL = 0;

    private Class<T> mModelClass = null;
    private T mModel;

    private static ArrayList<String> databaseStructure = new ArrayList<>();

    private Map<String, String> Query = new HashMap<>();
    private List<Field> filterListFields;

    private static boolean DEBUG = true;
    private static String DATABASE_NAME = null;
    private static Integer DATABASE_VERSION = null;

    private String OrderBy = null;
    private String OrderType = null;

    public DroidDatabase(Context context, T model) throws Exception {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        mModel = model;
    }

    public DroidDatabase(Context context, Class<T> modelClass) throws Exception {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        mModelClass = modelClass;
    }

    @Override
    public void onCreate(android.database.sqlite.SQLiteDatabase db) {
        for(int c = 0; c < databaseStructure.size(); c++) {
            debug("SQL: " + databaseStructure.get(c));

            db.execSQL(databaseStructure.get(c));
        }
    }

    @Override
    public void onUpgrade(android.database.sqlite.SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private void debug(String message) {
        if(DEBUG)
            Log.d(TAG, message);
    }

    public T save() throws Exception, NotNullException {

        if(mModel == null)
            throw new Exception("You must init with model instance.");

        Field[] fields = mModel.getClass().getDeclaredFields();

        if(fields.length == 0)
            throw new Exception("Error: Model has no fields.");

        List<Field> listFields = new ArrayList<>();

        for(Field field : fields) {
            if (field.isAnnotationPresent(field.class)) {
                if (field.isAnnotationPresent(primary_key.class))
                    continue;

                listFields.add(field);
            }
        }

        String table_name = getTableNamefromModel(mModel.getClass());

        Integer id = mModel.getId();
        if (id == null)
            return doSave(table_name, listFields);
        else
            return doUpdate(id, table_name, listFields);

    }

    private T doUpdate(Integer id, String tableName, List<Field> listFields) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String sql = "UPDATE " + tableName + " SET ";

        Class cls = mModel.getClass();

        for(int d = 0; d<listFields.size(); d++) {
            Field current_field = listFields.get(d);

            sql += current_field.getName();
            sql += " = ";

            String type = current_field.getAnnotation(field.class).type();
            String function = "get" + current_field.getName().substring(0, 1).toUpperCase() + current_field.getName().substring(1);
            Method method = cls.getDeclaredMethod(function);

            if(type.equalsIgnoreCase("integer"))
                sql += String.valueOf((Integer) method.invoke(mModel));

            else if(type.equalsIgnoreCase("long"))
                sql += String.valueOf((Long) method.invoke(mModel));

            else if(type.equalsIgnoreCase("varchar"))
                if(method.invoke(mModel)!=null)
                    sql += '"' + (String) method.invoke(mModel) + '"';
                else
                    sql += " null";

            if(d+1 < listFields.size())
                sql += ", ";
        }

        sql += " WHERE id=" + String.valueOf(id) + ";";

        debug("SQL: " + sql);
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(sql);

        db.close();

        return mModel;
    }

    private T doSave(String tableName, List<Field> listFields) throws NotNullException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        //int parameter
        Class[] paramInt = new Class[1];
        paramInt[0] = Integer.TYPE;

        String sql = "INSERT INTO " + tableName + " (";

        for(int d = 0; d<listFields.size(); d++) {
            Field current_field = listFields.get(d);

            sql += current_field.getName();

            if(d+1 < listFields.size())
                sql += ", ";
        }

        sql += ") values(";

        Class cls = mModel.getClass();

        for(int d = 0; d<listFields.size(); d++) {
            Field current_field = listFields.get(d);

            String function = "get" + current_field.getName().substring(0, 1).toUpperCase() + current_field.getName().substring(1);
            Method method = cls.getDeclaredMethod(function);

            String type = current_field.getAnnotation(field.class).type();

            if(current_field.isAnnotationPresent(notnull.class))
                if(current_field.getAnnotation(notnull.class).value())
                    if(method.invoke(mModel) == null)
                        throw new NotNullException("Field '" + current_field.getName() + "' does not support nulls values.");

            if(type.equalsIgnoreCase("integer"))
                sql += String.valueOf((Integer) method.invoke(mModel));

            else if(type.equalsIgnoreCase("long"))
                sql += String.valueOf((Long) method.invoke(mModel));

            else if(type.equalsIgnoreCase("varchar"))
                if(method.invoke(mModel)!=null)
                    sql += '"' + (String) method.invoke(mModel) + '"';
                else
                    sql += " null";

            if(d+1 < listFields.size())
                sql += ", ";
        }

        sql +=");";

        debug("SQL: " + sql);
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(sql);

        //get last id
        Cursor c = db.rawQuery("SELECT last_insert_rowid()", null);
        c.moveToFirst();

        mModel.setId(c.getInt(0));

        db.close();
        c.close();

        return mModel;
    }

    public static void Register(Context context, Class[] modelList) throws Exception, NotValidDatabaseTableException {
        ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        Bundle bundle = ai.metaData;

        DATABASE_NAME = bundle.getString("DATABASE_NAME");
        if(DATABASE_NAME == null)
            DATABASE_NAME = "database_db";

        DATABASE_VERSION = bundle.getInt("DATABASE_VERSION", 1);
        DEBUG = bundle.getBoolean("QUERY_LOG", false);


        for (int c = 0; c < modelList.length; c++) {
            if(!modelList[c].isAnnotationPresent(table.class))
                throw new NotValidDatabaseTableException("Class " + table.class.getName() + " is not a valid database table.");

            String table_name = getTableNamefromModel(modelList[c]);

            String sql = "CREATE TABLE IF NOT EXISTS " + table_name + " (";

            Field[] fields = modelList[c].getDeclaredFields();

            for(int d = 0; d<fields.length; d++) {
                if(fields[d].isAnnotationPresent(field.class)) {
                    String type = fields[d].getAnnotation(field.class).type();

                    if(type.equalsIgnoreCase("integer") || type.equalsIgnoreCase("long")){
                        sql += fields[d].getName() + " INTEGER";
                        if (fields[d].isAnnotationPresent(primary_key.class)) {
                            sql += " PRIMARY KEY";
                            if(fields[d].getAnnotation(primary_key.class).type()!=null)
                                sql += " AUTOINCREMENT";
                        }
                    }
                    else if(type.equalsIgnoreCase("varchar")){
                        sql += fields[d].getName() + " VARCHAR";
                        if(fields[d].isAnnotationPresent(size.class)) {
                            sql += "(" + String.valueOf(fields[d].getAnnotation(size.class).value()) + ")";

                            if (fields[d].isAnnotationPresent(notnull.class)) {
                                if(fields[d].getAnnotation(notnull.class).value())
                                    sql += " NOT NULL";
                            }
                        }

                        else
                            throw new Exception("You must declare varchar size.");
                    }

                    if(fields[d].isAnnotationPresent(unique.class))
                        if(fields[d].getAnnotation(unique.class).value())
                            sql += " UNIQUE";


                    if(d+1<fields.length)
                        sql += ", ";
                }
            }

            sql += "); ";

            databaseStructure.add(sql);
        }
    }

    private static String getTableNamefromModel(Class model) {
        Annotation[] annotations = model.getAnnotations();

        String table_name = "";
        for(Annotation annotation : annotations){
            if(annotation instanceof table){
                table table_annotation = (table) annotation;
                table_name = table_annotation.name();
            }
        }
        return table_name;
    }

    public DroidDatabase orderBy(String column, String type) throws Exception {
        if((!type.equals("ASC")) && (!type.equals("DESC")))
                throw new Exception("Error: Unknown order by type.");

        this.OrderBy = column;
        this.OrderType = type;

        return this;
    }

    public DroidDatabase<T> filterby(String column, String value, int operator) throws Exception {
        if(column == null)
            return filterby(null, operator);
        else {
            Map<String, String> filter = new HashMap<>();
            filter.put(column, value);

            return filterby(filter, operator);
        }
    }

    public DroidDatabase<T> filterby(Map<String, String> filter, int operator) throws Exception {
        if(mModelClass == null)
            throw new Exception("You must init with a model class.");

        filterListFields = new ArrayList<>();

        Field[] fields = mModelClass.getDeclaredFields();
        for (int d = 0; d < fields.length; d++) {
            if (fields[d].isAnnotationPresent(field.class))
                filterListFields.add(fields[d]);
        }

        if (filter == null)
            return this;

        for(Map.Entry<String, String> entry : filter.entrySet()) {
            String column = entry.getKey();
            String value = entry.getValue();

            //debug("Filter (column,value): " + column + ", " + value);

            boolean exist_field = false;
            for(Field field : filterListFields)
                if(field.getName().equals(column))
                    exist_field = true;

            if (!exist_field)
                throw new Exception("The column '" + column + "' does not exist in the model " + mModelClass.getName() + ".");

            if (operator == EQUAL)
                Query.put(column, " = '" + value + "'");
            else if (operator == NOT_EQUAL)
                Query.put(column, " != '" + value + "'");
            else
                throw new Exception("Error, Operator not valid.");
        }

        return this;
    }

    public List<T> find() throws Exception {
        return find(null);
    }

    public T findOne() throws Exception, DoesNotExistException {
        List<T> list = find(1);
        if(list.size()==0)
            throw new DoesNotExistException("No objects matching found.");
        else
            return list.get(0);
    }

    public List<T> find(Integer limit) throws Exception {
        if(filterListFields == null)
            throw new Exception("You must use filterby() before findOne()/find().");

        //String parameter
        Class[] paramString = new Class[1];
        paramString[0] = String.class;

        //int parameter
        Class[] paramInt = new Class[1];
        paramInt[0] = Integer.TYPE;

        String table_name = getTableNamefromModel(mModelClass);

        String sql = "SELECT ";

        for(int c = 0; c < filterListFields.size(); c++) {
            sql += filterListFields.get(c).getName();

            if(c+1<filterListFields.size())
                sql += ", ";
        }

        sql += " FROM " + table_name;

        if(Query.size() > 0)
            sql += " WHERE";

        boolean first = true;
        for(Map.Entry<String, String> e : Query.entrySet()) {
            if(!first){
                sql += " AND";
            }

            sql += " " + e.getKey() + e.getValue();
            first = false;
        }

        sql += " ORDER BY";
        if(OrderBy != null)
            sql += " " + OrderBy + " " + OrderType;
        else
            sql += " id";

        if(limit != null)
            sql += " LIMIT " + String.valueOf(limit) + ";";
        else
            sql += ";";

        debug("SQL: " + sql);

        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery(sql, null);

        T obj;

        List<T> list = new ArrayList<>();
        while(c.moveToNext()){
            obj = mModelClass.newInstance();

            for(int d = 0; d < filterListFields.size(); d++) {
                Field current_field = filterListFields.get(d);

                String function = "set" + current_field.getName().substring(0, 1).toUpperCase() + current_field.getName().substring(1);

                String type = current_field.getAnnotation(field.class).type();

                if(type.equalsIgnoreCase("integer")) {
                    Method method = mModelClass.getDeclaredMethod(function, paramInt);
                    method.invoke(obj, c.getInt(d));
                }
                else if(type.equalsIgnoreCase("long")) {
                    Method method = mModelClass.getDeclaredMethod(function, paramInt);
                    method.invoke(obj, c.getLong(d));
                }
                else if(type.equalsIgnoreCase("varchar")) {
                    Method method = mModelClass.getDeclaredMethod(function, paramString);
                    method.invoke(obj, c.getString(d));
                }
            }

            list.add(obj);
        }

        c.close();
        db.close();

        return list;
    }

    public void delete() throws Exception {
        String table_name = getTableNamefromModel(mModelClass);

        String sql = "DELETE FROM " + table_name;

        if(Query.size() > 0) {
            sql += " WHERE";

            boolean first = true;
            for (Map.Entry<String, String> e : Query.entrySet()) {
                if (!first) {
                    sql += " AND";
                }

                sql += " " + e.getKey() + e.getValue();
                first = false;
            }
        }

        sql += ";";

        debug("SQL: " + sql);

        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(sql);
        //TODO: como sqlite de Android no retorna nada no podemos saber nada del comando

        db.close();
    }
}
