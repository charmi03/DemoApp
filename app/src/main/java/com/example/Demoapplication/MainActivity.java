package com.example.Demoapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.here.sdk.core.Anchor2D;
import com.here.sdk.core.GeoBox;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.LanguageCode;
import com.here.sdk.core.Location;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.mapviewlite.Camera;
import com.here.sdk.mapviewlite.MapImage;
import com.here.sdk.mapviewlite.MapImageFactory;
import com.here.sdk.mapviewlite.MapMarker;
import com.here.sdk.mapviewlite.MapMarkerImageStyle;
import com.here.sdk.mapviewlite.MapPolyline;
import com.here.sdk.mapviewlite.MapPolylineStyle;
import com.here.sdk.mapviewlite.MapScene;
import com.here.sdk.mapviewlite.MapStyle;
import com.here.sdk.mapviewlite.MapViewLite;
import com.example.Demoapplication.PermissionsRequestor.ResultListener;
import com.here.sdk.mapviewlite.PixelFormat;
import com.here.sdk.routing.CalculateRouteCallback;
import com.here.sdk.routing.CarOptions;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.RoutingError;
import com.here.sdk.routing.Waypoint;
import com.here.sdk.search.CategoryQuery;
import com.here.sdk.search.Place;
import com.here.sdk.search.PlaceCategory;
import com.here.sdk.search.SearchCallback;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchError;
import com.here.sdk.search.SearchOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private MapViewLite mapView;
    private PermissionsRequestor permissionsRequestor;
    private PlatformPositioningProvider platformPositioningProvider;
    private Context context;
    private TextView tvSource, tvDestination;
    private Button btnGetDirection;
    private SearchEngine searchEngine;
    private final List<MapMarker> mapMarkerList = new ArrayList<>();
    private RoutingEngine routingEngine;
    private GeoCoordinates startGeoCoordinates, destinationGeoCoordinates;
    private final List<MapPolyline> mapPolylines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        // Get a MapViewLite instance from the layout.
        mapView.onCreate(savedInstanceState);
        context = this;
        platformPositioningProvider = new PlatformPositioningProvider(context);


        // Creating instance of RoutingEngine
        try {
            routingEngine = new RoutingEngine();
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization of RoutingEngine failed: " + e.error.name());
        }

        // Creating instance of SearchEngine
        try {
            searchEngine = new SearchEngine();
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization of SearchEngine failed: " + e.error.name());
        }

        tvSource.setOnClickListener(view -> {
            statLocating();  // to get current location of user
        });

        tvDestination.setOnClickListener(view -> {
            searchRestaurants();    // to search restaurants
        });

        btnGetDirection.setOnClickListener(view -> {
            if (startGeoCoordinates == null || destinationGeoCoordinates == null ) return;

            clearMap();
            calculateRoute();   //to get route between user's current location & ic_restaurant
        });

        handleAndroidPermissions();

    }

    /* this method is used to get list of nearby restaurants */
    private void searchRestaurants() {
        List<PlaceCategory> categoryList = new ArrayList<>();
        categoryList.add(new PlaceCategory(PlaceCategory.EAT_AND_DRINK_RESTAURANT));
        CategoryQuery categoryQuery = new CategoryQuery(categoryList, new GeoCoordinates(startGeoCoordinates.latitude, startGeoCoordinates.longitude));

        int maxItems = 10;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.EN_US, maxItems);

        searchEngine.search(categoryQuery, searchOptions, (searchError, list) -> {
            if (searchError != null) {
                tvDestination.setText("Error: " + searchError.toString());
                return;
            }

            ArrayList<AddressModelClass> arrayList = new ArrayList<>();
            if (list != null && list.size() > 0){
                for (Place searchResult : list) {
                    String addressText = searchResult.getAddress().addressText;
                    AddressModelClass model = new AddressModelClass();
                    model.setRestaurantName(addressText);
                    model.setLocation(searchResult.getGeoCoordinates());
                    arrayList.add(model);

                    Log.d(TAG, addressText);

                }

                showListDialog(arrayList);
            } else{
                Toast.makeText(context, "Please get your current location", Toast.LENGTH_SHORT).show();
            }
        });


    }

    private MapMarker createPoiMapMarker(GeoCoordinates geoCoordinates, int icon) {
        MapImage mapImage = MapImageFactory.fromResource(context.getResources(), icon);
        MapMarker mapMarker = new MapMarker(geoCoordinates);
        MapMarkerImageStyle mapMarkerImageStyle = new MapMarkerImageStyle();
        mapMarkerImageStyle.setAnchorPoint(new Anchor2D(0.5F, 1));
        mapMarker.addImage(mapImage, mapMarkerImageStyle);
        return mapMarker;
    }

    private void initViews() {
        btnGetDirection = findViewById(R.id.btnGetRoute);
        tvSource = findViewById(R.id.tvSource);
        tvDestination = findViewById(R.id.tvDestination);
        mapView = findViewById(R.id.map_view);

    }

    private void statLocating() {

        platformPositioningProvider.stopLocating();   // stope location update before restarting location update

        platformPositioningProvider.startLocating(location -> {
            Log.d(TAG, "onLocationUpdated: " + location);
            tvSource.setText(location.getLatitude() + " , " + location.getLongitude());
            platformPositioningProvider.stopLocating();     // stope location update once current location is received
            Location here_location = convertLocation(location);
            GeoCoordinates coordinates = new GeoCoordinates(here_location.coordinates.latitude, here_location.coordinates.longitude);

            //showing marker on here map
            MapMarker mapMarker = createPoiMapMarker(coordinates, R.drawable.ic_current_position);
            mapView.getMapScene().addMapMarker(mapMarker);
            mapMarkerList.add(mapMarker);
            mapView.getCamera().setTarget(new GeoCoordinates(coordinates.latitude, coordinates.longitude));
            mapView.getCamera().setZoomLevel(14);

            startGeoCoordinates = coordinates;


        });

    }

    private void handleAndroidPermissions() {
        permissionsRequestor = new PermissionsRequestor(this);
        permissionsRequestor.request(new ResultListener() {

            @Override
            public void permissionsGranted() {
                loadMapScene();  // once permission is granted load here map
            }

            @Override
            public void permissionsDenied() {
                Log.e(TAG, "Permissions denied by user.");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsRequestor.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    private void loadMapScene() {
        // Load a scene from the SDK to render the map with a map style.
        mapView.getMapScene().loadScene(MapStyle.NORMAL_DAY, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(@Nullable MapScene.ErrorCode errorCode) {
                if (errorCode == null) {
                    statLocating(); // once map is ready, get user's current location

                } else {
                    Log.d(TAG, "onLoadScene failed: " + errorCode.toString());
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    private void clearMap() {
        if (mapMarkerList != null && mapMarkerList.size() > 0){
            for (MapMarker mapMarker : mapMarkerList) {
                mapView.getMapScene().removeMapMarker(mapMarker);
            }
            mapMarkerList.clear();
        }
        clearRoute();

    }

    private Location convertLocation(android.location.Location nativeLocation) {
        GeoCoordinates geoCoordinates = new GeoCoordinates(
                nativeLocation.getLatitude(),
                nativeLocation.getLongitude(),
                nativeLocation.getAltitude());

        Location location = new Location(geoCoordinates, new Date());

        if (nativeLocation.hasBearing()) {
            location.bearingInDegrees = (double) nativeLocation.getBearing();
        }

        if (nativeLocation.hasSpeed()) {
            location.speedInMetersPerSecond = (double) nativeLocation.getSpeed();
        }

        if (nativeLocation.hasAccuracy()) {
            location.horizontalAccuracyInMeters = (double) nativeLocation.getAccuracy();
        }


        return location;
    }

    public void calculateRoute() {

        Waypoint startWaypoint = new Waypoint(startGeoCoordinates);
        Waypoint destinationWaypoint = new Waypoint(destinationGeoCoordinates);

        List<Waypoint> waypoints =
                new ArrayList<>(Arrays.asList(startWaypoint, destinationWaypoint));

        routingEngine.calculateRoute(
                waypoints,
                new CarOptions(),
                new CalculateRouteCallback() {
                    @Override
                    public void onRouteCalculated(@Nullable RoutingError routingError, @Nullable List<Route> routes) {
                        if (routingError == null) {
                            Route route = routes.get(0);
                            showRouteDetails(route);
                            showRouteOnMap(route, startGeoCoordinates, destinationGeoCoordinates);
                        } else {
                            Toast.makeText(context, "Error while calculating a route: " + routingError.toString(), Toast.LENGTH_SHORT).show();

                        }
                    }
                });

    }

    private void showRouteDetails(Route route) {
        long estimatedTravelTimeInSeconds = route.getDurationInSeconds();
        int lengthInMeters = route.getLengthInMeters();

        String routeDetails =
                "Transport Mode: " + route.getTransportMode() +
                ", Travel Time: " + formatTime(estimatedTravelTimeInSeconds)
                 + ", Distance: " + formatLength(lengthInMeters);

        showDialog("Route Details", routeDetails);
    }

    private String formatTime(long sec) {
        long hours = sec / 3600;
        long minutes = (sec % 3600) / 60;

        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);
    }

    private String formatLength(int meters) {
        int kilometers = meters / 1000;
        int remainingMeters = meters % 1000;

        return String.format(Locale.getDefault(), "%02d.%02d km", kilometers, remainingMeters);
    }

    //Below dialog will show route detail once route is created
    private void showDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    //Below function will display route on map
    public void showRouteOnMap(Route route, GeoCoordinates start, GeoCoordinates end) {

        GeoPolyline routeGeoPolyline;

        try {
            routeGeoPolyline = new GeoPolyline(route.getPolyline());
        } catch (InstantiationErrorException e) {
            // It should never happen that the route polyline contains less than two vertices.
            return;
        }
        MapPolylineStyle mapPolylineStyle = new MapPolylineStyle();
        mapPolylineStyle.setColor(0x00908AA0, PixelFormat.RGBA_8888);
        mapPolylineStyle.setWidthInPixels(10);
        MapPolyline routeMapPolyline = new MapPolyline(routeGeoPolyline, mapPolylineStyle);
        mapView.getMapScene().addMapPolyline(routeMapPolyline);
        mapPolylines.add(routeMapPolyline);

        // Draw a circle to indicate starting point and destination.
        addRouteMapMarker(start, R.drawable.ic_current_location);
        addRouteMapMarker(end, R.drawable.ic_destination);

        Camera camera = mapView.getCamera();
        camera.setTarget(new GeoCoordinates(destinationGeoCoordinates.latitude, destinationGeoCoordinates.longitude));
        camera.setZoomLevel(17);
    }


    //Below function will mark source and destination location on map
    private void addRouteMapMarker(GeoCoordinates geoCoordinates, int resourceId) {
        MapImage mapImage = MapImageFactory.fromResource(context.getResources(), resourceId);
        MapMarker mapMarker = new MapMarker(geoCoordinates);
        mapMarker.addImage(mapImage, new MapMarkerImageStyle());
        mapView.getMapScene().addMapMarker(mapMarker);
        mapMarkerList.add(mapMarker);
    }

    //Below dialog will show list of Restaurants to select
    public void showListDialog(ArrayList<AddressModelClass> addressList) {

        AlertDialog.Builder RestaurantAlertDilaog = new AlertDialog.Builder(MainActivity.this);
        RestaurantAlertDilaog.setIcon(R.drawable.ic_destination);
        RestaurantAlertDilaog.setTitle("Select One Restaurant");


        ArrayAdapter<AddressModelClass> arrayAdapter = new ArrayAdapter<AddressModelClass>(MainActivity.this,
                android.R.layout.select_dialog_singlechoice);

        arrayAdapter.addAll(addressList);


        ArrayAdapter<String> addressTitles = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.select_dialog_singlechoice);
        for (AddressModelClass address : addressList) {
            addressTitles.add(address.getRestaurantName());
        }

        RestaurantAlertDilaog.setNegativeButton("cancel", (dialog, which) -> dialog.dismiss());

        RestaurantAlertDilaog.setAdapter(addressTitles, (dialog, which) -> {
            AddressModelClass restaurantDetail = new AddressModelClass();
            restaurantDetail.setLocation(arrayAdapter.getItem(which).getLocation());
            restaurantDetail.setRestaurantName(arrayAdapter.getItem(which).getRestaurantName());
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(restaurantDetail.getRestaurantName());
            builder.setTitle("Your Selected Restaurant is");

            builder.setPositiveButton("Ok", (dialog1, which1) -> {
                dialog1.dismiss();
                tvDestination.setText(restaurantDetail.getRestaurantName());
                destinationGeoCoordinates = restaurantDetail.getLocation();

            });
            builder.show();
        });
        RestaurantAlertDilaog.show();
    }

    private void clearRoute() {
        for (MapPolyline mapPolyline : mapPolylines) {
            mapView.getMapScene().removeMapPolyline(mapPolyline);
        }
        mapPolylines.clear();
    }

}