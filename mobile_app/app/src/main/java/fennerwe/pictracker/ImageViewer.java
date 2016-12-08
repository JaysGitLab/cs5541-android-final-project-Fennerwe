package fennerwe.pictracker;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

public class ImageViewer extends AppCompatActivity {

    private String imageName;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_viewer);

        Bundle bundle = getIntent().getExtras();
        imageName = bundle.getString("image_name");

        getImage();

    }

    private void getImage(){
        String base_url = "http://54.80.53.58:3030/images/";
        final ImageView mImageView = (ImageView) findViewById(R.id.image);

        ImageRequest imgRequest = new ImageRequest(base_url + imageName + ".jpg", new Response.Listener<Bitmap>(){
            @Override
            public void onResponse(Bitmap response){
                mImageView.setImageBitmap(response);
            }
        }, 0, 0, null, Bitmap.Config.ARGB_8888, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                mImageView.setBackgroundColor(Color.parseColor("#ff0000"));
            }
        });

        Volley.newRequestQueue(this).add(imgRequest);
    }
}
