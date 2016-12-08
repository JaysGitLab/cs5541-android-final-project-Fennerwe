package fennerwe.pictracker;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Trey on 12/8/2016.
 */

public class ImageData {
    private String name;
    private LatLng coords;
    private Marker marker;

    public ImageData(String name, double geo_lat, double geo_long){
        this.name = name;
        coords = new LatLng(geo_lat, geo_long);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCoords(double geo_lat, double geo_long){
        coords = new LatLng(geo_lat, geo_long);
    }

    public LatLng getCoords(){
        return coords;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }
}
