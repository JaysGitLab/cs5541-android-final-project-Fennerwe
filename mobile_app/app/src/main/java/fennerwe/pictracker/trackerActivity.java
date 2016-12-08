package fennerwe.pictracker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.VisibleRegion;

import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.Manifest;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.Request;
import com.android.volley.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

public class trackerActivity extends FragmentActivity implements OnMapReadyCallback, ConnectionCallbacks, OnConnectionFailedListener, GoogleMap.OnMarkerClickListener{

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LatLng mCoords;
    private final int FINE_LOCATION_PERMISSION_REQUEST = 0;
    private final float DEFAULT_ZOOM = 11.0f;
    private String mCurrentPhotoPath;

    private ArrayList<ImageData> images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        images = new ArrayList<ImageData>();

        ImageButton imgButton = (ImageButton) findViewById(R.id.add_new_image_button);
        imgButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(takePictureIntent.resolveActivity(getPackageManager()) != null){
                    File storageDir = getFilesDir();
                    File image = null;
                    try {
                        image = File.createTempFile("temp", ".jpg", storageDir);
                    }catch(IOException e){
                        e.printStackTrace();
                    }

                    if(image != null) {
                        mCurrentPhotoPath = image.getAbsolutePath();
                        Uri photoUri = FileProvider.getUriForFile(trackerActivity.this, "com.fennerwe.pictracker.fileprovider", image);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mLastLocation != null) {
                mCoords = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            } else {
                mCoords = new LatLng(36.2167256, -81.681588);
            }

            mMap.moveCamera(CameraUpdateFactory.newLatLng(mCoords));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));

            getNearbyImages();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onConnectionSuspended(int cause){
        System.out.println(cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult cr){
        Log.i("Pic_Tracker","onConnectionFailed:"+cr.getErrorCode()+","+cr.getErrorMessage());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch(requestCode){
            case FINE_LOCATION_PERMISSION_REQUEST:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Location mLastLocation = null;
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    }
                    if (mLastLocation != null) {
                        mCoords = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    } else {
                        mCoords = new LatLng(36.2167256, -81.681588);
                    }

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(mCoords));
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));

                    getNearbyImages();
                }
        }
    }


    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private void getNearbyImages(){
        VisibleRegion vr = mMap.getProjection().getVisibleRegion();
        String url = "http://54.80.53.58:3030/api/pics/";
        String[] coords = new String[] {""+vr.farLeft.latitude, ""+vr.farLeft.longitude, ""+vr.nearRight.latitude, ""+vr.nearRight.longitude};

        url = url + TextUtils.join("/", coords);

        JsonObjectRequest jsonRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // the response is already constructed as a JSONObject!
                        try {
                            boolean success = response.getBoolean("success");
                            if(success){
                                JSONArray jsonImages = response.getJSONArray("data");
                                for(int i = 0; i < jsonImages.length(); i++){
                                    JSONObject imgData = jsonImages.getJSONObject(i);
                                    images.add(new ImageData(imgData.getString("url"),
                                            imgData.getDouble("geo_lat"),
                                            imgData.getDouble("geo_long")));
                                }

                                placeMarkers();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });

        Volley.newRequestQueue(this).add(jsonRequest);
    }

    private void placeMarkers(){
        String base_url = "http://54.80.53.58:3030/images/";
        for(int i = 0; i < images.size(); i++){
            ImageRequest imgRequest = new ImageRequest(base_url + images.get(i).getName() + "_thumb.jpg", getResponseListener(i), 0, 0, null, Bitmap.Config.ARGB_8888,
                    new Response.ErrorListener(){
                        @Override
                        public void onErrorResponse(VolleyError error){
                            error.printStackTrace();
                        }
                    });

            Volley.newRequestQueue(this).add(imgRequest);
        }
    }

    private Response.Listener<Bitmap> getResponseListener(int i) {
        final int index = i;
        return new Response.Listener<Bitmap>(){
            public void onResponse(Bitmap response){
                ImageData img = images.get(index);
                img.setMarker(mMap.addMarker(new MarkerOptions()
                                    .position(img.getCoords())
                                    .icon(BitmapDescriptorFactory.fromBitmap(response))
                                    .title(img.getName())));
            }
        };
    }

    public boolean onMarkerClick(final Marker marker){
        String name = marker.getTitle();

        Intent myIntent = new Intent(trackerActivity.this, ImageViewer.class);
        myIntent.putExtra("image_name", name);

        trackerActivity.this.startActivity(myIntent);

        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

            final String encodedImage = getStringImage(bitmap);

            String upload_url = "http://54.80.53.58:3030/upload/";
            JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.POST, upload_url, null,
                    new Response.Listener<JSONObject>(){
                        @Override
                        public void onResponse(JSONObject j){
                            try {
                                boolean success = j.getBoolean("success");
                                if(success){
                                    Toast.makeText(trackerActivity.this, "Image uploaded", Toast.LENGTH_SHORT).show();
                                }
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener(){
                        @Override
                        public void onErrorResponse(VolleyError error){
                            error.printStackTrace();
                        }
            }) {
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new Hashtable<String, String>();

                    if (ContextCompat.checkSelfPermission(trackerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                                mGoogleApiClient);
                        if (mLastLocation != null) {
                            mCoords = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                        } else {
                            mCoords = new LatLng(36.2167256, -81.681588);
                        }
                    }

                    params.put("image", encodedImage);
                    params.put("lat", ""+mCoords.latitude);
                    params.put("long", ""+mCoords.longitude);
                    return params;
                }
            };
        }
    }

    private String getStringImage(Bitmap bitmap){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

}
