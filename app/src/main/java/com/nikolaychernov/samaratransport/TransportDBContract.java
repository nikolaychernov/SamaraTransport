package com.nikolaychernov.samaratransport;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.readystatesoftware.sqliteasset_markikokik.SQLiteAssetHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TransportDBContract implements Serializable {
    // private static final String DB_PATH =
    // "/data/data/com.markikokik.transarrival63/databases/";

    public class MainReaderDbHelper extends SQLiteAssetHelper implements Serializable {

        public static final int DB_VERSION = 7; //updated 8.11.14
        public static final String DB_NAME = "main.db";

        public Stop[] getStops(int[] KS_IDs) {
            Stop tmp;
            int k = 0;
            Stop[] result = new Stop[KS_IDs.length];
            for (int i = 0; i < KS_IDs.length; i++) {
                tmp = getStop(KS_IDs[i]);
                if (tmp != null) {
                    result[k++] = tmp;
                }
            }

            return copy(result, k);
        }

        private Stop[] copy(Stop[] src, int count) {
            Stop[] result = new Stop[count];
            for (int i = 0; i < count; i++) {
                result[i] = src[i];
            }
            return result;
        }

        public Route getRoute(int KR_ID) {
            Route route = new Route();
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.query(TransportDBContract.RouteEntry.TABLE_NAME, null, TransportDBContract.RouteEntry.COLUMN_NAME_KR_ID + " = " + KR_ID, null, null, null, null);

            if (c == null) {
                return null;
            } else if (!c.moveToFirst()) {
                c.close();
                // db.close();
                return null;
            }

            cursorToRoute(c, route);
            c.close();
            return route;
        }

        public String getStopGeoID(int KS_ID) {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.query(StopEntry.TABLE_NAME, null, StopEntry.COLUMN_NAME_KS_ID + " = " + KS_ID, null, null, null, null);
            c.moveToFirst();
            String result = c.getString(c.getColumnIndexOrThrow(StopEntry.COLUMN_NAME_GEO_ID));
            c.close();
            //db.close();
            return result;
        }

        public Stop getStop(int KS_ID) {
            // Log.appendLog("DataController getStop " + KS_ID);
            Stop st = new Stop();
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.query(StopEntry.TABLE_NAME, null, StopEntry.COLUMN_NAME_KS_ID + " = " + KS_ID, null, null, null, null);

            if (c == null) {
                return null;
            } else if (!c.moveToFirst()) {
                c.close();
                // db.close();
                return null;
            }

            cursorToStop(c, st, true);
            c.close();
            //db.close();

            return st;
        }

        public Stop[] searchNearMe(double lat, double lng, int searchRadius, Set<Integer> area) {
            SQLiteDatabase db = getReadableDatabase();
            String where =
                    "(" + StopEntry.COLUMN_NAME_LATITUDE + " - " + lat + ") * (" + StopEntry.COLUMN_NAME_LATITUDE +
                            " - " + lat + ") * " + Math.pow(CoordUtils.metersInLatitudeDegree(), 2) + " + (" +
                            StopEntry.COLUMN_NAME_LONGITUDE + " - " + lng + ") * (" + StopEntry.COLUMN_NAME_LONGITUDE + " - " +
                            lng + ") * " + Math.pow(CoordUtils.metersInLongitudeDegree(lng), 2) + " <= " +
                            Math.pow(searchRadius, 2);

            Cursor cur = db.query(StopEntry.TABLE_NAME, null, where, null, null, null,
                    StopEntry.COLUMN_NAME_ADJACENT_STREET + ", " + StopEntry.COLUMN_NAME_TITLE);

            Stop st;
            ArrayList<Stop> result = new ArrayList<Stop>();
            if (cur.moveToFirst()) {
                do {
                    if (area.contains(cur.getInt(cur.getColumnIndex(StopEntry.COLUMN_NAME_KS_ID)))) {
                        st = new Stop();
                        cursorToStop(cur, st, true);
                        result.add(st);
                    }
                } while (cur.moveToNext());
            }
            cur.close();
            //db.close();

            return result.toArray(new Stop[0]);
        }

        public Stop[] searchNearMe(double lat, double lng, int searchRadius) {
            SQLiteDatabase db = getReadableDatabase();
            String where = "(" + StopEntry.COLUMN_NAME_LATITUDE + " - " + lat + ") * (" + StopEntry.COLUMN_NAME_LATITUDE + " - " + lat + ") * " + Math.pow(CoordUtils.metersInLatitudeDegree(), 2) + " + (" + StopEntry.COLUMN_NAME_LONGITUDE + " - " + lng + ") * (" + StopEntry.COLUMN_NAME_LONGITUDE + " - " + lng + ") * " + Math.pow(CoordUtils.metersInLongitudeDegree(lat), 2) + " <= " + Math.pow(searchRadius, 2);

            Cursor cur = db.query(StopEntry.TABLE_NAME, null, where, null, null, null,
                    StopEntry.COLUMN_NAME_ADJACENT_STREET + ", " + StopEntry.COLUMN_NAME_TITLE);

            Stop[] result = new Stop[cur.getCount()];
            if (cur.moveToFirst()) {
                for (int i = 0; i < result.length; i++, cur.moveToNext()) {
                    result[i] = new Stop();
                    cursorToStop(cur, result[i], true);
                }
            }
            cur.close();
            //db.close();

            return result;
        }

        public Stop[] searchByName(String name, Set<Integer> area) {
            String where = TransportDBContract.StopEntry.COLUMN_NAME_TITLE_LOWCASE + " LIKE '%" + name.toLowerCase() + "%'";
            SQLiteDatabase db = getReadableDatabase();
            Cursor cur = db.query(TransportDBContract.StopEntry.TABLE_NAME, null, where, null, null, null, StopEntry.COLUMN_NAME_ADJACENT_STREET + ", " + TransportDBContract.StopEntry.COLUMN_NAME_TITLE);

            Stop st;
            ArrayList<Stop> result = new ArrayList<Stop>();
            if (cur.moveToFirst()) {
                do {
                    if (area.contains(cur.getInt(cur.getColumnIndex(StopEntry.COLUMN_NAME_KS_ID)))) {
                        st = new Stop();
                        cursorToStop(cur, st, true);
                        result.add(st);
                    }
                } while (cur.moveToNext());
            }
            cur.close();
            //db.close();

            return result.toArray(new Stop[0]);
        }

        public Stop[] searchByName(String name) {
            String where = TransportDBContract.StopEntry.COLUMN_NAME_TITLE_LOWCASE + " LIKE '%" + name.toLowerCase() + "%'";
            SQLiteDatabase db = getReadableDatabase();
            Cursor cur = db.query(TransportDBContract.StopEntry.TABLE_NAME, null, where, null, null, null, StopEntry.COLUMN_NAME_ADJACENT_STREET + ", " + TransportDBContract.StopEntry.COLUMN_NAME_TITLE);

            Stop[] result = new Stop[cur.getCount()];
            if (cur.moveToFirst()) {
                for (int i = 0; i < result.length; i++) {
                    result[i] = new Stop();
                    cursorToStop(cur, result[i], true);
                    cur.moveToNext();
                }
            }
            cur.close();
            //db.close();
            return result;
        }

        public void updateStops(ArrayList<Stop> stops){
            Log.d("DATABASE", "updating stops");
            SQLiteDatabase db = getWritableDatabase();
            for (int i=0; i<stops.size(); i++){
                updateStop(stops.get(i), db);
            }
            //db.close();
        }

        public void updateStop(Stop stop, SQLiteDatabase db){
            if (!(stop.busesCommercial.equals("") && stop.busesMunicipal.equals("") && stop.busesPrigorod.equals("") && stop.busesSeason.equals("") &&
                    stop.busesSeason.equals("") && stop.trams.equals("") && stop.trolleybuses.equals(""))) {
                ContentValues values = new ContentValues();
                values.put(StopEntry.COLUMN_NAME_KS_ID, stop.KS_ID);
                values.put(StopEntry.COLUMN_NAME_TITLE, stop.title);
                values.put(StopEntry.COLUMN_NAME_TITLE_LOWCASE, stop.titleLowcase);
                values.put(StopEntry.COLUMN_NAME_ADJACENT_STREET, stop.adjacentStreet);
                values.put(StopEntry.COLUMN_NAME_DIRECTION, stop.direction);
                values.put(StopEntry.COLUMN_NAME_BUSES_MUNICIPAL, stop.busesMunicipal);
                values.put(StopEntry.COLUMN_NAME_BUSES_COMMERCIAL, stop.busesCommercial);
                values.put(StopEntry.COLUMN_NAME_BUSES_PRIGOROD, stop.busesPrigorod);
                values.put(StopEntry.COLUMN_NAME_BUSES_SEASON, stop.busesSeason);
                values.put(StopEntry.COLUMN_NAME_BUSES_SPECIAL, stop.busesSpecial);
                values.put(StopEntry.COLUMN_NAME_TRAMS, stop.trams);
                values.put(StopEntry.COLUMN_NAME_TROLLEYBUSES, stop.trolleybuses);
                values.put(StopEntry.COLUMN_NAME_LATITUDE, stop.latitude);
                values.put(StopEntry.COLUMN_NAME_LONGITUDE, stop.longitude);
                db.beginTransaction();

                db.insertWithOnConflict(StopEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

                db.setTransactionSuccessful();
                db.endTransaction();
            }
        }

        public MainReaderDbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }
    }

    public class FavorReaderDbHelper extends SQLiteOpenHelper implements Serializable {

        public static final int DB_VERSION = 1;
        public final static String DB_NAME = "favor.db";
        public FavorReaderDbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        public Set<Integer> getFavorSet() {
            SQLiteDatabase db = getReadableDatabase();
            String where = TransportDBContract.FavorEntry.COLUMN_NAME_FAVOR + " = '1'";
            Cursor cur = db.query(TransportDBContract.FavorEntry.TABLE_NAME, null, where, null, null, null, null);
            Set<Integer> result = new HashSet<Integer>(cur.getCount());

            if (cur.moveToFirst()) {
                do {
                    result.add(cur.getInt(cur.getColumnIndex(FavorEntry.COLUMN_NAME_KS_ID)));
                } while (cur.moveToNext());
            }
            cur.close();
            //db.close();

            return result;
        }

        public int[] getFavor() {
            SQLiteDatabase db = getReadableDatabase();
            String where = TransportDBContract.FavorEntry.COLUMN_NAME_FAVOR + " = '1'";
            Cursor cur = db.query(TransportDBContract.FavorEntry.TABLE_NAME, null, where, null, null, null, null);
            int[] result = new int[cur.getCount()];

            if (cur.moveToFirst()) {
                for (int i = 0; i < result.length; i++, cur.moveToNext()) {
                    result[i] = cur.getInt(cur.getColumnIndex(FavorEntry.COLUMN_NAME_KS_ID));
                }

            }
            cur.close();
            //db.close();

            return result;
        }

        public void setFavor(int KS_ID, boolean isFavor) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(FavorEntry.COLUMN_NAME_KS_ID, KS_ID);
            values.put(FavorEntry.COLUMN_NAME_FAVOR, isFavor ? "1" : "0");
            db.beginTransaction();

            if (db.update(FavorEntry.TABLE_NAME, values, "KS_ID = " + KS_ID, null) == 0) {
                db.insertOrThrow(FavorEntry.TABLE_NAME, null, values);
            }

            db.setTransactionSuccessful();
            db.endTransaction();
            //db.close();
        }

        public void setFavor(int[] KS_IDs, boolean isFavor) {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            ContentValues values;
            for (int i = 0; i < KS_IDs.length; i++) {
                values = new ContentValues();
                values.put(FavorEntry.COLUMN_NAME_KS_ID, KS_IDs[i]);
                values.put(FavorEntry.COLUMN_NAME_FAVOR, isFavor ? "1" : "0");
                if (db.update(FavorEntry.TABLE_NAME, values, "KS_ID = " + KS_IDs[i], null) == 0) {
                    db.insertOrThrow(FavorEntry.TABLE_NAME, null, values);
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            //db.close();
        }

        public boolean isInFavor(int KS_ID) {
            SQLiteDatabase db = getReadableDatabase();
            String where = TransportDBContract.FavorEntry.COLUMN_NAME_FAVOR + " = '1' AND KS_ID = " + KS_ID;
            Cursor cur = db.query(TransportDBContract.FavorEntry.TABLE_NAME, null, where, null, null, null, null);

            boolean result = cur.getCount() > 0;
            cur.close();
            //db.close();
            return result;
        }

        public boolean isInFavor(int[] KS_IDs) {
            SQLiteDatabase db = getReadableDatabase();
            boolean res = true;
            for (int i = 0; i < KS_IDs.length; i++) {
                String where = TransportDBContract.FavorEntry.COLUMN_NAME_FAVOR + " = '1' AND KS_ID = " + KS_IDs[i];
                Cursor cur = db.query(TransportDBContract.FavorEntry.TABLE_NAME, null, where, null, null, null, null);
                res &= (cur.getCount() > 0);
                cur.close();
            }
            //
            // db.close();
            return res;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_Favor);
        }

        @Override
        public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        }
    }

    public class RouteReaderDbHelper extends SQLiteOpenHelper implements Serializable {

        public static final int DB_VERSION = 1;
        public final static String DB_NAME = "main.db";
        public RouteReaderDbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        public ArrayList<Stop> getStopsForRoute(int KR_ID){
            ArrayList<Stop> stops = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM stops WHERE KS_ID IN ( SELECT KS_ID FROM routestopbind WHERE KR_ID =" + KR_ID +  ");", null);
            while(c.moveToNext()){
                Stop st = new Stop();
                st.KS_ID = Integer.parseInt(c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_KS_ID)));
                st.direction = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_DIRECTION));
                st.adjacentStreet = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_ADJACENT_STREET));
                st.title = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_TITLE));
                st.titleLowcase = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_TITLE_LOWCASE));
                st.busesCommercial = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_BUSES_COMMERCIAL));
                st.busesMunicipal = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_BUSES_MUNICIPAL));
                st.busesPrigorod = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_BUSES_PRIGOROD));
                st.busesSeason = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_BUSES_SEASON));
                st.busesSpecial = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_BUSES_SPECIAL));
                st.latitude = c.getDouble(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_LATITUDE));
                st.longitude = c.getDouble(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_LONGITUDE));
                st.trams = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_TRAMS));
                st.trolleybuses = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_TROLLEYBUSES));
                st.geoportalID = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_GEO_ID));

                stops.add(st);
            }
            c.close();
            //db.close();

            return stops;
        }


        @Override
        public void onCreate(SQLiteDatabase db) {
            // TODO Auto-generated method stub
            db.execSQL(SQL_CREATE_Favor);
        }

        @Override
        public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
            // TODO Auto-generated method stub

        }
    }


    private static void cursorToRoute(Cursor c, Route route) {
        // //Log.appendLog("DataController cursorToRoute");
        if (route == null) {
            route = new Route();
        }
        route.affiliationID = c.getInt(c.getColumnIndexOrThrow(TransportDBContract.RouteEntry.COLUMN_NAME_AFFILIATION_ID));
        route.direction = c.getString(c.getColumnIndexOrThrow(TransportDBContract.RouteEntry.COLUMN_NAME_DIRECTION));
        route.KR_ID = c.getInt(c.getColumnIndexOrThrow(TransportDBContract.RouteEntry.COLUMN_NAME_KR_ID));
        route.number = c.getString(c.getColumnIndexOrThrow(TransportDBContract.RouteEntry.COLUMN_NAME_NUMBER));
        route.transportTypeID = c.getInt(c.getColumnIndexOrThrow(TransportDBContract.RouteEntry.COLUMN_NAME_TRANSPORT_TYPE_ID));

    }

    public static void cursorToStop(Cursor c, Stop st, boolean copyDirection) {
        if (st == null) {
            st = new Stop();
        }
        if (copyDirection) {
            st.KS_ID = Integer.parseInt(c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_KS_ID)));
            st.direction = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_DIRECTION));
        }
        st.adjacentStreet = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_ADJACENT_STREET));
        st.title = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_TITLE));
        st.titleLowcase = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_TITLE_LOWCASE));
        st.busesCommercial = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_BUSES_COMMERCIAL));
        st.busesMunicipal = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_BUSES_MUNICIPAL));
        st.busesPrigorod = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_BUSES_PRIGOROD));
        st.busesSeason = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_BUSES_SEASON));
        st.busesSpecial = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_BUSES_SPECIAL));

        st.latitude = Double.parseDouble(c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_LATITUDE)));
        st.longitude = Double.parseDouble(c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_LONGITUDE)));
        st.trams = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_TRAMS));
        st.trolleybuses = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_TROLLEYBUSES));
        st.geoportalID = c.getString(c.getColumnIndexOrThrow(TransportDBContract.StopEntry.COLUMN_NAME_GEO_ID));
    }

    public static abstract class StopEntry implements BaseColumns {

        public static final String TABLE_NAME = "stops";
        public static final String COLUMN_NAME_KS_ID = "KS_ID";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_TITLE_LOWCASE = "titleLowcase";
        public static final String COLUMN_NAME_ADJACENT_STREET = "adjacentStreet";
        public static final String COLUMN_NAME_DIRECTION = "direction";
        public static final String COLUMN_NAME_BUSES_MUNICIPAL = "busesMunicipal";
        public static final String COLUMN_NAME_BUSES_COMMERCIAL = "busesCommercial";
        public static final String COLUMN_NAME_BUSES_PRIGOROD = "busesPrigorod";
        public static final String COLUMN_NAME_BUSES_SEASON = "busesSeason";
        public static final String COLUMN_NAME_BUSES_SPECIAL = "busesSpecial";
        public static final String COLUMN_NAME_TRAMS = "trams";
        public static final String COLUMN_NAME_TROLLEYBUSES = "trolleybuses";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_GEO_ID = "Geoportal_ID";
        public static final String COLUMN_NAME_FAVOR = "favor";

        private StopEntry() {
        }
    }

    public static abstract class RouteEntry implements BaseColumns {

        public static final String TABLE_NAME = "routes";
        public static final String COLUMN_NAME_KR_ID = "KR_ID";
        public static final String COLUMN_NAME_NUMBER = "number";
        public static final String COLUMN_NAME_AFFILIATION_ID = "affiliationID";
        public static final String COLUMN_NAME_TRANSPORT_TYPE_ID = "transportTypeID";
        public static final String COLUMN_NAME_DIRECTION = "direction";

        private RouteEntry() {
        }
    }


    public static abstract class FavorEntry implements BaseColumns {

        public static final String TABLE_NAME = "Favor";
        public static final String COLUMN_NAME_KS_ID = "KS_ID";
        public static final String COLUMN_NAME_FAVOR = "Favor";

        private FavorEntry() {
        }
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String REAL_TYPE = " REAL";
    private static final String INT_TYPE = " INT";
    private static final String BOOL_TYPE = " CHAR(1)";
    private static final String COMMA_SEP = ", ";


    private static final String SQL_CREATE_Favor = "CREATE TABLE " + FavorEntry.TABLE_NAME + " (" + FavorEntry.COLUMN_NAME_KS_ID + " INTEGER PRIMARY KEY" + COMMA_SEP + FavorEntry.COLUMN_NAME_FAVOR + BOOL_TYPE + " )";


    private static final String SQL_CREATE_STOPS_2 = "CREATE TABLE " + StopEntry.TABLE_NAME + " (" + StopEntry._ID + " INTEGER PRIMARY KEY," + StopEntry.COLUMN_NAME_KS_ID + INT_TYPE + COMMA_SEP + StopEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP + StopEntry.COLUMN_NAME_TITLE_LOWCASE + TEXT_TYPE + COMMA_SEP + StopEntry.COLUMN_NAME_ADJACENT_STREET + TEXT_TYPE + COMMA_SEP + StopEntry.COLUMN_NAME_DIRECTION + TEXT_TYPE + COMMA_SEP
                                                             + StopEntry.COLUMN_NAME_BUSES_MUNICIPAL + TEXT_TYPE + COMMA_SEP + StopEntry.COLUMN_NAME_BUSES_COMMERCIAL + TEXT_TYPE + COMMA_SEP + StopEntry.COLUMN_NAME_BUSES_PRIGOROD + TEXT_TYPE + COMMA_SEP + StopEntry.COLUMN_NAME_BUSES_SEASON + TEXT_TYPE + COMMA_SEP + StopEntry.COLUMN_NAME_BUSES_SPECIAL + TEXT_TYPE + COMMA_SEP + StopEntry.COLUMN_NAME_TRAMS + TEXT_TYPE + COMMA_SEP + StopEntry.COLUMN_NAME_TROLLEYBUSES + TEXT_TYPE + COMMA_SEP
                                                             + StopEntry.COLUMN_NAME_LATITUDE + REAL_TYPE + COMMA_SEP + StopEntry.COLUMN_NAME_LONGITUDE + REAL_TYPE + COMMA_SEP + StopEntry.COLUMN_NAME_GEO_ID + TEXT_TYPE + " )";

    private static final String SQL_CREATE_ROUTES = "CREATE TABLE " + RouteEntry.TABLE_NAME + " (" + RouteEntry._ID + " INTEGER PRIMARY KEY," + RouteEntry.COLUMN_NAME_KR_ID + INT_TYPE + COMMA_SEP + RouteEntry.COLUMN_NAME_NUMBER + TEXT_TYPE + COMMA_SEP + RouteEntry.COLUMN_NAME_TRANSPORT_TYPE_ID + INT_TYPE + COMMA_SEP + RouteEntry.COLUMN_NAME_AFFILIATION_ID + INT_TYPE + COMMA_SEP
                                                            + RouteEntry.COLUMN_NAME_DIRECTION + TEXT_TYPE
                                                            + " )";



    private static final String SQL_DELETE_STOPS = "DROP TABLE IF EXISTS " + StopEntry.TABLE_NAME;
    private static final String SQL_DELETE_ROUTES = "DROP TABLE IF EXISTS " + RouteEntry.TABLE_NAME;

    private static final String SQL_DELETE_FAVOR = "DROP TABLE IF EXISTS " + FavorEntry.TABLE_NAME;


    public class StopsReaderDbHelper extends SQLiteOpenHelper implements Serializable {

        public static final int DB_VERSION = 1;
        public final String DB_NAME;

        public StopsReaderDbHelper(Context context, String dbName) {
            super(context, dbName, null, DB_VERSION);
            // Log.appendLog("StopsReaderDbHelper constructor");
            DB_NAME = dbName;
            // createStopsDB(context);
        }

        public void onCreate(SQLiteDatabase db) {
            // Log.appendLog("StopsReaderDbHelper onCreate");
            db.execSQL(SQL_DELETE_STOPS);
            db.execSQL(SQL_CREATE_STOPS_2);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade
            // policy is
            // to simply to discard the data and start over
            // Log.appendLog("StopsReaderDbHelper onUpgrade");
            db.execSQL(SQL_DELETE_STOPS);
            onCreate(db);
        }

        @SuppressLint("Override")
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }



}
