package com.vivanksharma.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLData;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    int totalNumberOfHeadLines;
    String u;
    ListView listView;
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articleDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
        articleDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS newArticles (id INTEGER PRIMARY KEY,articleId INTEGER,title VARCHAR,link VARCHAR)");
        updateListView();

        DownloadTask task = new DownloadTask();
        DownloadTask1 task1 = new DownloadTask1();
        try
        {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
            task1.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getApplicationContext(),webView.class);
                intent.putExtra("url",content.get(position));
                startActivity(intent);
            }
        });
    }

    public void updateListView()
    {
        Cursor c = articleDB.rawQuery("SELECT * FROM newArticles",null);
        int contentIndex = c.getColumnIndex("link");
        int titleIndex = c.getColumnIndex("title");

        if(c.moveToNext()){
            titles.clear();
            content.clear();
            do{
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
                arrayAdapter.notifyDataSetChanged();
            }while (c.moveToNext());


        }
    }

    public class DownloadTask extends AsyncTask<String,Integer,String>{


        @Override
        protected String doInBackground(String... strings) {

            u = strings[0];
            downloadData(40,u);
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
            Toast.makeText(MainActivity.this,"Please give 5 minutes to load more data",Toast.LENGTH_LONG).show();
        }
    }

    public class DownloadTask1 extends AsyncTask<String,Integer,String>{


        @Override
        protected String doInBackground(String... strings) {

            u = strings[0];
            downloadData(totalNumberOfHeadLines,u);
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }

    public String downloadData(int numberOfResultNeeded,String u)
    {
        String result = "";
        URL url;

        HttpURLConnection httpURLConnection = null;
        try {
            url = new URL(u);
            httpURLConnection = (HttpURLConnection)url.openConnection();
            InputStream in  = httpURLConnection.getInputStream();
            BufferedReader reader =new BufferedReader(new InputStreamReader(in));
            String data = reader.readLine();

            while (data!=null)
            {
                result+=data;
                data=reader.readLine();
            }

            //Log.i("Result",""+result);
            JSONArray jsonArray = new JSONArray(result);
            totalNumberOfHeadLines = jsonArray.length();

            articleDB.execSQL("DELETE FROM newArticles");
            for(int i=0;i<numberOfResultNeeded;i++)
            {
                //Log.i("IDs",""+jsonArray.getString(i));
                String articleID = jsonArray.getString(i);
                url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleID+".json?print=pretty");
                httpURLConnection = (HttpURLConnection) url.openConnection();
                in = httpURLConnection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(in));
                data = reader.readLine();
                String articleInfo = "";
                while (data!=null)
                {
                    articleInfo+=data;
                    data=reader.readLine();
                }

                //Log.i("Article content",""+articleInfo);
                JSONObject jsonObject = new JSONObject(articleInfo);
                if (!jsonObject.isNull("title") && !jsonObject.isNull("url"))
                {
                    String articleTitle = jsonObject.getString("title");
                    String articleUrl = jsonObject.getString("url");
                    //Log.i("info",articleTitle+articleUrl);
                        /*url = new URL(articleUrl);
                        httpURLConnection = (HttpURLConnection) url.openConnection();
                        in = httpURLConnection.getInputStream();
                        reader = new BufferedReader(new InputStreamReader(in));
                        data = reader.readLine();
                        String articleContent = "";
                        while (data!=null)
                        {
                            articleInfo+=data;
                            data=reader.readLine();
                        }*/
                    String sql = "INSERT INTO newArticles (articleId , title , link) VALUES ( ? , ? , ?)";
                    SQLiteStatement statement = articleDB.compileStatement(sql);
                    statement.bindString(1,articleID);
                    statement.bindString(2,articleTitle);
                    Log.i("URLs",""+articleUrl);
                    statement.bindString(3,articleUrl);
                    statement.execute();
                }
            }
            return result;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
