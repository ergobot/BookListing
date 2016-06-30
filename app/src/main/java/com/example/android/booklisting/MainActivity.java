package com.example.android.booklisting;

import android.app.ListActivity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ListActivity {

    private ArrayList<Book> books = null;
    private BookAdapter bookAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        books = new ArrayList<Book>();
        this.bookAdapter = new BookAdapter(this, R.layout.row, books);
        setListAdapter(this.bookAdapter);

    }

    public void queryBooks(View view){
        // asynctask
        BookQueryTask bookQueryTask = new BookQueryTask();
        String query = String.valueOf( ((TextView)findViewById(R.id.input)).getText());
        bookQueryTask.execute(query);
    }

    private class BookAdapter extends ArrayAdapter<Book> {

        private ArrayList<Book> items;

        public BookAdapter(Context context, int textViewResourceId, ArrayList<Book> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        public void swapItems(ArrayList<Book> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.row, null);
            }
            Book book = items.get(position);
            if (book != null) {
                TextView title = (TextView) view.findViewById(R.id.title);
                TextView author = (TextView) view.findViewById(R.id.author);
                if (title != null) {
                    title.setText(book.getTitle());                            }
                if(author != null){
                    author.setText(book.getAuthor());
                }
            }
            return view;
        }
    }


    public class BookQueryTask extends AsyncTask<String, Void, ArrayList<Book>> {

        private final String LOG_TAG = BookQueryTask.class.getSimpleName();

        private ArrayList<Book> getBookDataFromJson(String bookJsonStr)
                throws JSONException {

            JSONObject bookJson = new JSONObject(bookJsonStr);
            JSONArray bookArray = bookJson.getJSONArray("items");

            ArrayList<Book> bookResults = new ArrayList<Book>();

            for(int i = 0; i < bookArray.length(); i++) {

                String title;
                String author;


                JSONObject bookResultJson = bookArray.getJSONObject(i);

                if(bookResultJson.getJSONObject("volumeInfo").has("title")){
                    title = bookResultJson.getJSONObject("volumeInfo").getString("title");
                }else{
                    title = "Not available";
                }
                if(bookResultJson.getJSONObject("volumeInfo").has("authors")){
                    JSONArray authors = bookResultJson.getJSONObject("volumeInfo").getJSONArray("authors");
                    StringBuilder sb = new StringBuilder();
                    for(int j = 0; j < authors.length(); j++) {
                        sb.append(authors.getString(j));
                        sb.append(", ");
                    }
                    if (sb.length() > 0) {
                        sb.setLength(sb.length() - 2);
                        author = sb.toString();
                    }else{
                        author = "Not available";
                    }
                }else{
                    author = "Not available";
                }

                Book book = new Book();
                book.setTitle(title);
                book.setAuthor(author);
                bookResults.add(book);
            }

            return bookResults;

        }
        @Override
        protected ArrayList<Book> doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String bookJsonStr = null;

            try {
                final String BOOK_QUERY_BASE_URL =
                        "https://www.googleapis.com/books/v1/volumes?";
                final String QUERY_PARAM = "q";
                final String MAX_PARAM = "maxResults";

                Uri builtUri = Uri.parse(BOOK_QUERY_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(MAX_PARAM, "20")
                        .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                // Create the request to queryapi, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {


                    // Read the input stream into a String
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        return null;
                    }
                    bookJsonStr = buffer.toString();

                    Log.v(LOG_TAG, "Book result string: " + bookJsonStr);
                }else{
                    // Returns null when response code is not good response code
                    return null;
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getBookDataFromJson(bookJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the book results.
            return null;
        }


        @Override
        public void onPostExecute(ArrayList<Book> result){
            if(result == null){
                ((TextView)findViewById(R.id.noresults)).setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), getResources().getText(R.string.message_error), Toast.LENGTH_LONG).show();
            }else if(result.size() == 0){
                ((TextView)findViewById(R.id.noresults)).setVisibility(View.VISIBLE);
            }else{
                ((TextView)findViewById(R.id.noresults)).setVisibility(View.GONE);
                bookAdapter.clear();
                bookAdapter.addAll(result);
                bookAdapter.notifyDataSetChanged();
            }

        }

    }





}
