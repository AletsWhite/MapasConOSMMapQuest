package net.ivanvega.mapasconosmdroida;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    MapView map = null;
    private MyLocationNewOverlay mLocationOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;

    private RequestQueue queue;
    private JsonObjectRequest requestMapRequest;

    private ArrayList<GeoPoint> puntosRuta = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    IMapController mapController;
    private double wayLatitude = 0.0, wayLongitude = 0.0;
    ArrayList<Polyline> listaPuntos;
    FloatingActionButton fab;
    private MinimapOverlay mMinimapOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        //inflate and create the map
        setContentView(R.layout.activity_main);

        listaPuntos = new ArrayList<>();
        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setMultiTouchControls(true);

        mapController = map.getController();
        mapController.setZoom(15.0);
        GeoPoint startPoint = new GeoPoint(20.139476, -101.150737);


        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx
        ),map);
        this.mLocationOverlay.enableMyLocation();
        map.getOverlays().add(this.mLocationOverlay);

        mRotationGestureOverlay = new RotationGestureOverlay(ctx, map);
        mRotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(this.mRotationGestureOverlay);

        //your items
        ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
        items.add(new OverlayItem("Title", "Description", new GeoPoint(20.139476d,
                -101.150737d))); // Lat/Lon decimal degrees

        //the overlay
        ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        //do something
                        return true;
                    }
                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        return false;
                    }
                }, ctx);
        mOverlay.setFocusItemsOnTap(true);

        minimap();

        ultimoPunto();


        Intent intent=getIntent();
        if(intent.getStringExtra("lat1")!=null) {
            String message = intent.getStringExtra("lat1") + ", " +
                    intent.getStringExtra("long1") + ", " +
                    intent.getStringExtra("lat2") + ", " +
                    intent.getStringExtra("long2");
            System.out.println(message);

            try {
                obtenerRouteFromMapRequest(Double.parseDouble(intent.getStringExtra("lat1")),
                        Double.parseDouble(intent.getStringExtra("long1")),
                        Double.parseDouble(intent.getStringExtra("lat2")),
                        Double.parseDouble(intent.getStringExtra("long2"))
                );
            } catch (Exception e) {
                Toast.makeText(ctx, "Error formato de direccion", Toast.LENGTH_SHORT).show();
            }


        }

    }

    private void ultimoPunto() {

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(
                this,
                location -> {
                    if(location!=null){
                        String ubica = "Lat: " + location.getLatitude()
                                + ", lon: " + location.getLongitude();
                        GeoPoint point = new GeoPoint(location.getLatitude(),location.getLongitude());
                        mapController.setCenter(point);
                        Marker startMarker = new Marker(map);
                        startMarker.setPosition(point);
                        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        map.getOverlays().add(startMarker);

                        wayLatitude=location.getLatitude();
                        wayLongitude=location.getLongitude();

                        System.out.println("ubicacion: "+ubica);
                    }else {
                        Log.d ("UBIX", "Location null");
                    }
                }
        );

    }

    private void obtenerRouteFromMapRequest(Double lat, Double longitude, Double lat2, Double long2){
        puntosRuta=new ArrayList<>();
        System.out.println("http://www.mapquestapi.com/directions/v2/route?key=uX9JBe8sViFaaajad6BzCvAZ8jtCinQp&from="+
                lat+","+longitude+"&to="+lat2+","+long2);
        queue =
                Volley.newRequestQueue(this);

        requestMapRequest =
                new JsonObjectRequest(
                        "http://www.mapquestapi.com/directions/v2/route?key=uX9JBe8sViFaaajad6BzCvAZ8jtCinQp&from="+
                                lat+","+longitude+"&to=20.079139869364447, -101.13146972361896",
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.d("GIVO", "se ejecuto");
                                try {
                                    JSONArray indicaiones =  response.getJSONObject("route")
                                            .getJSONArray("legs")
                                            .getJSONObject(0).
                                                    getJSONArray("maneuvers");


                                    //se recorre la lista de puntos y se crea una polilinea
                                    for( int i =0 ;  i <indicaiones.length(); i++){
                                        JSONObject indi = indicaiones.getJSONObject(i);
                                        GeoPoint punto = new GeoPoint(
                                                Double.parseDouble(indi.getJSONObject("startPoint").get("lat").toString()),
                                                Double.parseDouble(indi.getJSONObject("startPoint").get("lng").toString()));
                                        puntosRuta.add(punto);
                                        System.out.print(punto.getLatitude());
                                        System.out.println(" "+punto.getLongitude());
                                        String strlatlog = indi.getJSONObject("startPoint").get("lat").toString()
                                                + "," +
                                                indi.getJSONObject("startPoint").get("lng").toString();

                                        Log.d("GIVO", "se ejecuto: " +  strlatlog );

                                    }

                                    Polyline line = new Polyline();   //see note below!
                                    line.setPoints(puntosRuta);
                                    line.setColor(Color.BLUE);
                                    line.setOnClickListener(new Polyline.OnClickListener() {
                                        @Override
                                        public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {
                                            Toast.makeText(mapView.getContext(), "polyline with " +
                                                    polyline.getPoints().size() + "pts was tapped", Toast.LENGTH_LONG).show();
                                            return false;
                                        }
                                    });
                                    map.getOverlayManager().add(line);
                                    listaPuntos.add(line);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("GIVO", "se ejecuto CON ERROR");

                            }
                        }
                );

        queue.add(requestMapRequest);
    }

    private void minimap() {
        final DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        mMinimapOverlay = new MinimapOverlay(getApplicationContext(), map.getTileRequestCompleteHandler());
        mMinimapOverlay.setWidth(dm.widthPixels / 5);
        mMinimapOverlay.setHeight(dm.heightPixels / 5);
        map.getOverlays().add(this.mMinimapOverlay);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ruta:
                Intent intent = new Intent(this, Activity_RutaConfigurable.class);
                startActivity(intent);
                return true;
            case R.id.limpiarRuta:
                Toast.makeText(this, "limpiando rutas", Toast.LENGTH_SHORT).show();
                for (Polyline line:listaPuntos){
                    map.getOverlayManager().remove(line);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up

    }





    @Override
    protected void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }
}