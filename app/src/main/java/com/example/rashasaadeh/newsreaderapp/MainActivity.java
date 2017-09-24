package com.example.rashasaadeh.newsreaderapp;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static android.R.attr.codes;
import static android.R.attr.content;
import static android.R.attr.process;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();

    ArrayAdapter arrayAdapter;

    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView)findViewById(R.id.listView);

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);

        listView.setAdapter(arrayAdapter);

        //when article is clicked
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("content", content.get(i)); //the article that the user tapped, i is the position on the list

                startActivity(intent);
            }
        });

        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        //contents saved onto the data base
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)");

        updateListView(); //updates list view of articles as soon as we open up the app


        //process to download content don't forget internet permission
        DownloadTask task = new DownloadTask();

        try {
            //gives users the top stories on hacker-news
           // task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    //update the articles when we want to use the app
    public void updateListView() {
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles", null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if(c.moveToFirst()) {
            //checks to see if we have an initial value

            //clear our titles arraylist
            titles.clear();
            content.clear();

            do {

                titles.add(c.getString(titleIndex)); //add the current title\, this adds in new content
                content.add(c.getString(contentIndex));
            } while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged(); //update the arrayAdapter
        }
    }


    //process to download content, don't forget user id!
    public class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(strings[0]); //url that is passed to download task class
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();

                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while(data != -1)
                {
                    char current = (char) data; //char we are at

                    result+= current;

                    data = reader.read();
                }

                Log.i("URLContent", result);

                //take the number id's from url api and extract them into a form we can use
                JSONArray jsonArray = new JSONArray(result);

                int numberOfItems = 20;

                if(jsonArray.length() < 20)
                {
                    numberOfItems = jsonArray.length();
                }

                //clears database table so that we don't add the same articles
                articlesDB.execSQL("DELETE FROM articles");

                for(int i = 0; i < numberOfItems; i++)
                {
                   // Log.i("JSONItem", jsonArray.getString(i)); //separates all the numbers pull from api

                    //load the urls

                    String articleId = jsonArray.getString(i); //gives us the article id of each popular article we loop through

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();

                    String articleInfo = "";

                    while(data != -1)
                    {
                        char current = (char) data;

                        articleInfo += current;
                        data = reader.read();
                    }

                    // Log.i("ArticleInfo", articleInfo); //get the article infor like the url above

                    //More complicated than a list of numbers, so we need a JSONObject,
                    // we are interested in title and url from the api
                    JSONObject jsonObject = new JSONObject(articleInfo);

                    //checks if article either does not have a title or a url
                    //Log.i("Info", jsonObject.toString());

                    //how to handle if title or url is null / does not exist

                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url"))
                    {
                        String articleTitle = jsonObject.getString("title"); //pulled from the api
                        String articleURL = jsonObject.getString("url");

                        //Log.i("Info", articleTitle + articleURL);

                        //final thing is to get the article data itself
                        url = new URL(articleURL);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        in = urlConnection.getInputStream();
                        reader = new InputStreamReader(in);
                        data = reader.read();

                        String articleContent = "";

                        while(data != -1)
                        {
                            char current = (char) data;

                            articleInfo += current;
                            data = reader.read();
                        }

                        //this is too much data for it to be displayed in the logs, so we will save the urls onto a database
                        Log.i("articleContent", articleContent);

                        //adding articles to the table
                        String sql = "INSERT INTO articles (articleID, title, content) VALUES (? , ? , ?)";

                        SQLiteStatement statement = articlesDB.compileStatement(sql);

                        statement.bindString(1, articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3, articleContent);

                        //puts in content into SQL
                        statement.execute();
                    }



                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //method is run when the process in download task is completed (aka new articles are in sql database
            updateListView();

        }
    }
}
