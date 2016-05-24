package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {



    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
//        try {
////            FileOutputStream fos = getContext().openFileOutput(values.get("key").toString(), Context.MODE_PRIVATE);
////            fos.write(values.get("value").toString().getBytes());
////            fos.close();
//        }catch(Exception e){
//            Log.e("insert", values.toString());
//        }
//        //uri = Uri.withAppendedPath(Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider"), String.valueOf(db.insertWithOnConflict("totalfifo", null, values, SQLiteDatabase.CONFLICT_REPLACE )));
//        Log.v("insert", values.toString());
//        return uri;
            String keyToBeWritten = values.getAsString("key");
            String valueToBeWritten = values.getAsString("value");
        Log.v("Path for insertion URI:", uri.getPath());
        Log.v("Path for insertion co:", getContext().getFilesDir().getAbsolutePath());

        try {
            FileWriter fw = new FileWriter(new File(getContext().getFilesDir().getAbsolutePath(), keyToBeWritten));
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(valueToBeWritten);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */

        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(getContext().getFilesDir().getAbsolutePath()) + "/" + selection));
            String valueRead = br.readLine();
            MatrixCursor mc = new MatrixCursor(new String[]{"key", "value"});
            mc.addRow(new String[]{selection, valueRead});
            mc.close();
            return mc;


        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v("query", selection);
        return null;
    }
}
//        Cursor cursor = db.rawQuery("SELECT * FROM totalfifo WHERE key = '"+selection+"'", null);
//        Log.v("query", selection);

//        String msgValue="" ;
//        try {
//            FileInputStream fis = getContext().openFileInput(selection);
//            BufferedInputStream bis= new BufferedInputStream(fis);
//            int temp;
//            while((temp= bis.read())!=-1){
//                msgValue+= (char)temp;
//            }
//            bis.close();
//        }catch(IOException ioe){
//            Log.e("Query",ioe.getMessage());
//        }catch(NullPointerException npe){
//            Log.e("Query",npe.getMessage());
//        }catch(Exception e){
//            Log.e("Query",e.getMessage());
//        }
//        String[] columnNames= {"key", "value"};
//        MatrixCursor msgCursor= new MatrixCursor(columnNames);
//        msgCursor.addRow(new String[]{selection, msgValue});
////        Log.v("Query", selection);
//        return cursor;


