/*
 Copyright 2015-2020 Peter-Josef Meisch (pj.meisch@sothawo.com)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package no.ntnu.ambulanceallocation.simulation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sothawo.mapjfx.Configuration;
import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.CoordinateLine;
import com.sothawo.mapjfx.MapCircle;
import com.sothawo.mapjfx.MapLabel;
import com.sothawo.mapjfx.MapType;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.Marker;
import com.sothawo.mapjfx.Projection;
import com.sothawo.mapjfx.WMSParam;
import com.sothawo.mapjfx.XYZParam;
import com.sothawo.mapjfx.offline.OfflineCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.optimization.Allocation;
import no.ntnu.ambulanceallocation.optimization.initializer.AllCityCenter;
import no.ntnu.ambulanceallocation.optimization.initializer.PopulationProportionate;
import no.ntnu.ambulanceallocation.optimization.initializer.Random;
import no.ntnu.ambulanceallocation.optimization.initializer.Uniform;
import no.ntnu.ambulanceallocation.optimization.initializer.UniformRandom;
import no.ntnu.ambulanceallocation.simulation.event.NewCall;
import no.ntnu.ambulanceallocation.simulation.grid.DistanceIO;
import no.ntnu.ambulanceallocation.simulation.incident.UrgencyLevel;

/**
 * Controller for the FXML defined code.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public class SimulationController {

    /** logger for the class. */
    private static final Logger logger = LoggerFactory.getLogger(SimulationController.class);

    /** default zoom value. */
    private static final int ZOOM_DEFAULT = 11;

    private final Coordinate center = new Coordinate(59.929671, 10.738381);
    private final Marker markerClick;
    private final MapLabel labelClick;

    @FXML
    private Button startSimulationbutton;

    @FXML
    private Button stopSimulationbutton;

    /** the MapView containing the map */
    @FXML
    private MapView mapView;

    /**
     * the box containing the top controls, must be enabled when mapView is
     * initialized
     */
    @FXML
    private HBox topControls;

    /** Slider to change the zoom value */
    @FXML
    private Slider sliderZoom;

    /** Accordion for all the different options */
    @FXML
    private Accordion leftControls;

    /** section containing the location button */
    @FXML
    private TitledPane optionsLocations;

    /** for editing the animation duration */
    @FXML
    private TextField animationDuration;

    /** the BIng Maps API Key. */
    @FXML
    private TextField bingMapsApiKey;

    @FXML
    private Label currentTime;

    @FXML
    private Label activeAmbulances;

    @FXML
    private TextField numDayAmbulances;

    @FXML
    private TextField numNightAmbulances;

    @FXML
    private TextField dayShift;

    @FXML
    private TextField nightShift;

    /** Label to display the current center */
    @FXML
    private Label labelCenter;

    /** Label to display the current extent */
    @FXML
    private Label labelExtent;

    /** Label to display the current zoom */
    @FXML
    private Label labelZoom;

    /** label to display the last event. */
    @FXML
    private Label labelEvent;

    /** RadioButton for MapStyle OSM */
    @FXML
    private RadioButton radioMsOSM;

    /** RadioButton for MapStyle Stamen Watercolor */
    @FXML
    private RadioButton radioMsSTW;

    /** RadioButton for MapStyle Bing Roads */
    @FXML
    private RadioButton radioMsBR;

    /** RadioButton for MapStyle Bing Roads - dark */
    @FXML
    private RadioButton radioMsCd;

    /** RadioButton for MapStyle Bing Roads - grayscale */
    @FXML
    private RadioButton radioMsCg;

    /** RadioButton for MapStyle Bing Roads - light */
    @FXML
    private RadioButton radioMsCl;

    /** RadioButton for MapStyle Bing Aerial */
    @FXML
    private RadioButton radioMsBA;

    /** RadioButton for MapStyle Bing Aerial with Label */
    @FXML
    private RadioButton radioMsBAwL;

    /** RadioButton for MapStyle WMS. */
    @FXML
    private RadioButton radioMsWMS;

    /** RadioButton for MapStyle XYZ */
    @FXML
    private RadioButton radioMsXYZ;

    /** ToggleGroup for the MapStyle radios */
    @FXML
    private ToggleGroup mapTypeGroup;

    /** Check button for click marker */
    @FXML
    private CheckBox checkClickMarker;

    @FXML
    private CheckBox checkShowGridCentroids;

    @FXML
    private CheckBox checkShowPathLines;

    @FXML
    private CheckBox checkShowHospitals;

    @FXML
    private CheckBox checkShowBaseStations;

    @FXML
    private CheckBox checkShowIncidents;

    @FXML
    private CheckBox checkShowAmbulances;

    @FXML
    private Slider simulationUpdateIntervalSlider;

    @FXML
    private DatePicker simulationStartTime;

    @FXML
    private DatePicker simulationEndTime;

    private URL hospitalIcon = getClass().getResource("hospital_boring_icon.png");
    private URL standbyPointIcon = getClass().getResource("base_station_boring.png");
    private URL ambulanceIcon = getClass().getResource("ambulance_marker.png");

    private Map<no.ntnu.ambulanceallocation.simulation.grid.Coordinate, Coordinate> utmToLatLongMap = new HashMap<>();
    private List<Coordinate> baseStationCoordinateList = new ArrayList<>();
    private List<Marker> baseStationMarkerList = Collections.synchronizedList(new ArrayList<>());
    private List<MapLabel> baseStationLabelList = Collections.synchronizedList(new ArrayList<>());
    private List<Coordinate> hospitalCoordinateList = Collections.synchronizedList(new ArrayList<>());
    private List<Marker> hospitalMarkerList = Collections.synchronizedList(new ArrayList<>());
    private List<MapLabel> hospitalLabelList = Collections.synchronizedList(new ArrayList<>());
    private List<MapCircle> gridCentroidCirclesList = Collections.synchronizedList(new ArrayList<>());
    private List<MapCircle> incidentCircleList = Collections.synchronizedList(new ArrayList<>());
    private Map<Ambulance, Marker> ambulanceMarkers = Collections.synchronizedMap(new HashMap<>());
    private Map<Ambulance, MapCircle> destinationCircles = Collections.synchronizedMap(new HashMap<>());
    private Map<Ambulance, CoordinateLine> destinationLines = Collections.synchronizedMap(new HashMap<>());
    private LocalDateTime currentTimeInternal = LocalDateTime.MIN;

    private Thread simulationThread;
    private long lastUiUpdate = 0;

    /** Check Button for polygon drawing mode. */
    @FXML
    private CheckBox checkDrawPolygon;

    /** params for the WMS server. */
    private WMSParam wmsParam = new WMSParam()
            .setUrl("http://ows.terrestris.de/osm/service?")
            .addParam("layers", "OSM-WMS");

    private XYZParam xyzParams = new XYZParam()
            .withUrl("https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x})")
            .withAttributions(
                    "'Tiles &copy; <a href=\"https://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer\">ArcGIS</a>'");

    private void readCSVThenParse(String fileName, Consumer<String[]> consumer) {
        try (Stream<String> lines = new BufferedReader(
                new InputStreamReader(getClass().getResource(fileName).openStream(), StandardCharsets.UTF_8))
                .lines()) {
            lines.map(line -> line.split(",")).forEach(consumer);
        } catch (IOException | NumberFormatException e) {
            logger.error("load {}", e);
        }
    }

    private static int bearingInDegrees(Coordinate src, Coordinate dst) {
        double srcLat = Math.toRadians(src.getLatitude());
        double dstLat = Math.toRadians(dst.getLatitude());
        double dLng = Math.toRadians(dst.getLongitude() - src.getLongitude());

        return (int) Math.round(Math.toDegrees((Math.atan2(Math.sin(dLng) * Math.cos(dstLat),
                Math.cos(srcLat) * Math.sin(dstLat) -
                        Math.sin(srcLat) * Math.cos(dstLat) * Math.cos(dLng))
                + Math.PI)));

    }

    private void updateIncidents(Collection<NewCall> callQueue) {
        Platform.runLater(() -> {
            incidentCircleList.forEach(mapView::removeMapCircle);
            incidentCircleList = callQueue.stream()
                    .map(call -> new MapCircle(utmToLatLongMap.get(call.incident.getLocation()), 1000)
                            .setColor(call.incident.urgencyLevel() == UrgencyLevel.ACUTE ? Color.web("#ff0000", 0.7)
                                    : Color.web("#ffff00"))
                            .setVisible(checkShowIncidents.isSelected()))
                    .toList();
            incidentCircleList.forEach(mapView::addMapCircle);
        });
    }

    private void updateAmbulances(Collection<Ambulance> ambulanceList) {
        Platform.runLater(() -> {
            if (ambulanceMarkers.size() > 0) {
                synchronized (ambulanceMarkers) {
                    ambulanceList.forEach(ambulance -> {
                        Coordinate coordinate = utmToLatLongMap
                                .get(ambulance.getCurrentLocationVisualized(currentTimeInternal));
                        Marker marker = ambulanceMarkers.get(ambulance);
                        // MapLabel markerLabel = marker.getMapLabel().get();

                        if (ambulance.getCurrentLocation() == ambulance.getBaseStationLocation()
                                && ambulance.isOffDuty()) {
                            marker.setVisible(false);
                        } else {
                            marker.setVisible(checkShowAmbulances.isSelected());
                        }

                        if (ambulance.isAvailable()) {
                            // markerLabel.setVisible(false);
                        } else if (!ambulance.isOffDuty()) {
                            // if (!ambulance.isAvailable() && !ambulance.isTransport()) {
                            // UrgencyLevel urgencyLevel = ambulance.getIncident().urgencyLevel();
                            // markerLabel.setCssClass(
                            // urgencyLevel == UrgencyLevel.ACUTE ? "red-label" : "orange-label")
                            // .setVisible(checkShowAmbulances.isSelected());
                            // } else if (!ambulance.isAvailable() && ambulance.isTransport()
                            // && ambulance.getDestination().equals(ambulance.getHospitalLocation())) {
                            // markerLabel.setCssClass(
                            // "green-label")
                            // .setVisible(checkShowAmbulances.isSelected());
                            // }
                        }

                        if (!marker.getPosition().equals(coordinate)) {
                            marker.setRotation(bearingInDegrees(marker.getPosition(), coordinate) + 90);
                            if (destinationLines.containsKey(ambulance)) {
                                mapView.removeCoordinateLine(destinationLines.get(ambulance));
                            }
                            Coordinate ambulanceDestination = utmToLatLongMap
                                    .get(ambulance.getDestination());
                            Color color;
                            if (!ambulance.isAvailable() && ambulance.isTransport()
                                    && ambulance.getDestination().equals(ambulance.getHospitalLocation())) {
                                color = Color.web("#ff0000", 0.9);
                            } else if (ambulance.isAvailable()
                                    && ambulance.getDestination() == ambulance.getBaseStationLocation()) {
                                color = Color.web("#0000ff", 0.9);
                            } else {
                                color = Color.web("#00ff00", 0.9);
                            }
                            destinationLines.put(ambulance,
                                    new CoordinateLine(ambulanceDestination,
                                            utmToLatLongMap.get(ambulance
                                                    .getCurrentLocationVisualized(currentTimeInternal)))
                                            .setColor(color)
                                            .setVisible(checkShowPathLines.isSelected()));
                            mapView.addCoordinateLine(destinationLines.get(ambulance));
                        }

                        if (destinationCircles.containsKey(ambulance)) {
                            if (ambulance.getDestination() == null || !destinationCircles.get(ambulance).getCenter()
                                    .equals(utmToLatLongMap.get(ambulance.getDestination()))) {
                                mapView.removeMapCircle(destinationCircles.get(ambulance));
                                destinationCircles.remove(ambulance);
                            }
                        }

                        if (ambulance.getDestination() != null && !destinationCircles.containsKey(ambulance)) {
                            Coordinate destinationCoordinate = utmToLatLongMap.get(ambulance.getDestination());
                            if (!baseStationCoordinateList.contains(destinationCoordinate)
                                    && !hospitalCoordinateList.contains(destinationCoordinate)) {
                                destinationCircles.put(ambulance,
                                        new MapCircle(destinationCoordinate,
                                                1000)
                                                .setColor(Color.web("#00ff00", 0.7))
                                                .setVisible(checkShowIncidents.isSelected()));
                                mapView.addMapCircle(destinationCircles.get(ambulance));
                            }
                        }
                        animateMarker(marker, marker.getPosition(), coordinate);

                    });
                }
            } else {
                for (Ambulance ambulance : ambulanceList) {
                    Coordinate coordinates = utmToLatLongMap
                            .get(ambulance.getCurrentLocationVisualized(currentTimeInternal));
                    Marker marker = new Marker(ambulanceIcon, -15, -15).setPosition(coordinates)
                            .setVisible(checkShowAmbulances.isSelected());
                    ambulanceMarkers.put(ambulance, marker);
                    // MapLabel label = new MapLabel("Responding", -25, 20)
                    // .setVisible(false)
                    // .setCssClass("orange-label");
                    // marker.attachLabel(label).setVisible(true);
                    mapView.addMarker(marker);
                }
            }
            currentTime.setText("Current time:\n" + currentTimeInternal.toString());
            activeAmbulances.setText(
                    "Active ambulances: "
                            + ambulanceList.stream()
                                    .filter(ambulance -> !ambulance.isOffDuty())
                                    .count()
                            + "");
        });
    }

    private int getDayAllocation() {
        if (numDayAmbulances.getText() == null || numDayAmbulances.getText().isEmpty()) {
            return Parameters.NUMBER_OF_AMBULANCES_DAY;
        } else {
            return Integer.parseInt(numDayAmbulances.getText());
        }
    }

    private int getNightAllocation() {
        if (numNightAmbulances.getText() == null || numNightAmbulances.getText().isEmpty()) {
            return Parameters.NUMBER_OF_AMBULANCES_NIGHT;
        } else {
            return Integer.parseInt(numNightAmbulances.getText());
        }
    }

    @FXML
    private void setUniformAllocation() {
        dayShift.setText(new Uniform().initialize(getDayAllocation()).stream()
                .map(i -> i.toString()).collect(Collectors.joining(", ")));
        nightShift.setText(new Uniform().initialize(getNightAllocation()).stream()
                .map(i -> i.toString()).collect(Collectors.joining(", ")));
    }

    @FXML
    private void setUniformRandomAllocation() {
        dayShift.setText(new UniformRandom().initialize(getDayAllocation()).stream()
                .map(i -> i.toString()).collect(Collectors.joining(", ")));
        nightShift.setText(new UniformRandom().initialize(getNightAllocation()).stream()
                .map(i -> i.toString()).collect(Collectors.joining(", ")));
    }

    @FXML
    private void setPopulationProportionateAllocation() {
        dayShift.setText(new PopulationProportionate().initialize(getDayAllocation()).stream()
                .map(i -> i.toString()).collect(Collectors.joining(", ")));
        nightShift.setText(new PopulationProportionate().initialize(getNightAllocation()).stream()
                .map(i -> i.toString()).collect(Collectors.joining(", ")));
    }

    @FXML
    private void setAllCityCenterAllocation() {
        dayShift.setText(new AllCityCenter().initialize(getDayAllocation()).stream()
                .map(i -> i.toString()).collect(Collectors.joining(", ")));
        nightShift.setText(new AllCityCenter().initialize(getNightAllocation()).stream()
                .map(i -> i.toString()).collect(Collectors.joining(", ")));
    }

    @FXML
    private void setRandomAllocation() {
        dayShift.setText(new Random().initialize(getDayAllocation()).stream()
                .map(i -> i.toString()).collect(Collectors.joining(", ")));
        nightShift.setText(new Random().initialize(getNightAllocation()).stream()
                .map(i -> i.toString()).collect(Collectors.joining(", ")));
    }

    @FXML
    private void setVisibilityBaseStations() {
        logger.info("Setting base station layer visibility to " + checkShowBaseStations.isSelected());
        baseStationMarkerList.forEach(marker -> marker.setVisible(checkShowBaseStations.isSelected()));
    }

    @FXML
    private void setVisibilityHospitals() {
        logger.info("Setting hospital layer visibility to " + checkShowHospitals.isSelected());
        hospitalMarkerList.forEach(marker -> marker.setVisible(checkShowHospitals.isSelected()));
    }

    @FXML
    private void setVisibilityAmbulances() {
        logger.info("Setting ambulance layer visibility to " + checkShowAmbulances.isSelected());
        ambulanceMarkers.values().forEach(mapView::removeMarker);
        ambulanceMarkers.values().forEach(marker -> marker.setVisible(checkShowAmbulances.isSelected()));
        ambulanceMarkers.values().forEach(mapView::addMarker);
    }

    @FXML
    private void setVisibilityGridCentroids() {
        logger.info("Setting grid centroids layer visibility to " + checkShowGridCentroids.isSelected());
        gridCentroidCirclesList.forEach(mapView::removeMapCircle);
        gridCentroidCirclesList.forEach(circle -> circle.setVisible(checkShowGridCentroids.isSelected()));
        gridCentroidCirclesList.forEach(mapView::addMapCircle);
    }

    @FXML
    private void setVisibilityPathLines() {
        logger.info("Setting path line layer visibility to " + checkShowPathLines.isSelected());
        destinationLines.values().forEach(mapView::removeCoordinateLine);
        destinationLines.values().forEach(line -> line.setVisible(checkShowPathLines.isSelected()));
        destinationLines.values().forEach(mapView::addCoordinateLine);
    }

    @FXML
    private void setVisibilityIncidents() {
        logger.info("Setting incident layer visibility to " + checkShowIncidents.isSelected());
        destinationCircles.values().forEach(mapView::removeMapCircle);
        incidentCircleList.forEach(mapView::removeMapCircle);
        destinationCircles.values().forEach(circle -> circle.setVisible(checkShowIncidents.isSelected()));
        incidentCircleList.forEach(circle -> circle.setVisible(checkShowIncidents.isSelected()));
        destinationCircles.values().forEach(mapView::addMapCircle);
        incidentCircleList.forEach(mapView::addMapCircle);
    }

    public SimulationController() {
        // logger.debug("Prefetching incidents and distances for simulation");
        // DistanceIO distances = new DistanceIO();
        // IncidentIO incidents = new IncidentIO();

        logger.debug("Reading coordinate CSV files");
        readCSVThenParse("base_stations.csv", values -> {
            Coordinate coordinate = new Coordinate(Double.valueOf(values[1]), Double.valueOf(values[2]));
            Marker mapMarker = new Marker(standbyPointIcon, -15, -15).setPosition(coordinate)
                    .setVisible(true);
            MapLabel label = new MapLabel(values[0], -57, 20).setVisible(false).setCssClass("label");
            mapMarker.attachLabel(label);
            baseStationCoordinateList.add(coordinate);
            baseStationMarkerList.add(mapMarker);
            baseStationLabelList.add(label);
        });

        readCSVThenParse("hospitals.csv", values -> {
            Coordinate coordinate = new Coordinate(Double.valueOf(values[1]), Double.valueOf(values[2]));
            Marker mapMarker = new Marker(hospitalIcon, -15, -15).setPosition(coordinate)
                    .setVisible(true);
            MapLabel label = new MapLabel(values[0], -57, 20)
                    .setVisible(false)
                    .setCssClass("label");
            mapMarker.attachLabel(label);
            hospitalCoordinateList.add(coordinate);
            hospitalMarkerList.add(mapMarker);
            hospitalLabelList.add(label);
        });

        logger.debug("Reading UTM to LatLong conversion map");
        readCSVThenParse("grid_coordinates.csv", values -> {
            Coordinate coordinate = new Coordinate(Double.valueOf(values[2]), Double.valueOf(values[3]));
            utmToLatLongMap.put(new no.ntnu.ambulanceallocation.simulation.grid.Coordinate(Integer.valueOf(values[0]),
                    Integer.valueOf(values[1])), coordinate);
        });

        DistanceIO.uniqueGridCoordinates.forEach(utmCoordinate -> {
            Coordinate coordinate = utmToLatLongMap.get(utmCoordinate);
            gridCentroidCirclesList.add(new MapCircle(coordinate, 100)
                    .setFillColor(Color.web("#000000", 0.4))
                    .setColor(Color.TRANSPARENT)
                    .setVisible(false));
        });

        markerClick = Marker.createProvided(Marker.Provided.ORANGE).setVisible(false);

        labelClick = new MapLabel("click!", 10, -10).setVisible(false).setCssClass("orange-label");

        markerClick.attachLabel(labelClick);

    }

    @FXML
    private void centerMap() {
        mapView.setCenter(center);
        mapView.setZoom(ZOOM_DEFAULT);
    }

    @FXML
    private void startSimulation() {
        if (simulationThread != null && simulationThread.isAlive()) {
            synchronized (simulationThread) {
                simulationThread.notify();
            }
        } else {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    List<Integer> dayShiftAllocation = List.of(dayShift.getText().replaceAll("\\s", "").split(","))
                            .stream().mapToInt(Integer::parseInt).boxed().toList();
                    List<Integer> nightShiftAllocation = List.of(nightShift.getText().replaceAll("\\s", "").split(","))
                            .stream().mapToInt(Integer::parseInt).boxed().toList();

                    Simulation.visualizedSimulation(new Allocation(List.of(dayShiftAllocation, nightShiftAllocation)),
                            (currentTime, ambulanceList, callQueue) -> {
                                if (System.currentTimeMillis() - lastUiUpdate > Parameters.GUI_UPDATE_INTERVAL
                                        && (int) ChronoUnit.SECONDS.between(currentTimeInternal, currentTime) > 120) {
                                    currentTimeInternal = currentTime;
                                    updateAmbulances(ambulanceList);
                                    updateIncidents(callQueue);
                                    lastUiUpdate = System.currentTimeMillis();
                                }
                            }, simulationUpdateIntervalSlider.valueProperty());
                    return null;
                }
            };
            simulationThread = new Thread(task);
            simulationThread.setDaemon(true);
            simulationThread.start();
        }
        startSimulationbutton.setVisible(false);
    }

    @FXML
    private void stopSimulation() {
        try {
            synchronized (simulationThread) {
                simulationThread.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startSimulationbutton.setVisible(true);
    }

    /**
     * called after the fxml is loaded and all objects are created. This is not
     * called initialize any more,
     * because we need to pass in the projection before initializing.
     *
     * @param projection
     *                   the projection to use in the map.
     */
    public void initMapAndControls(Projection projection) {
        setRandomAllocation();
        checkShowPathLines.setSelected(true);
        checkShowHospitals.setSelected(true);
        checkShowBaseStations.setSelected(true);
        checkShowIncidents.setSelected(true);
        checkShowAmbulances.setSelected(true);

        logger.trace("begin initialize");

        // init MapView-Cache
        final OfflineCache offlineCache = mapView.getOfflineCache();
        final String cacheDir = System.getProperty("java.io.tmpdir") + "/mapjfx-cache";
        logger.info("using dir for cache: " + cacheDir);
        try {
            Files.createDirectories(Paths.get(cacheDir));
            offlineCache.setCacheDirectory(cacheDir);
            offlineCache.setActive(true);
        } catch (IOException e) {
            logger.warn("could not activate offline cache", e);
        }

        // set the custom css file for the MapView
        mapView.setCustomMapviewCssURL(getClass().getResource("/custom_mapview.css"));

        leftControls.setExpandedPane(optionsLocations);

        // set the controls to disabled, this will be changed when the MapView is
        // intialized
        setControlsDisable(true);

        sliderZoom.valueProperty().bindBidirectional(mapView.zoomProperty());

        // add a listener to the animationDuration field and make sure we only accept
        // int values
        animationDuration.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                mapView.setAnimationDuration(0);
            } else {
                try {
                    mapView.setAnimationDuration(Integer.parseInt(newValue));
                } catch (NumberFormatException e) {
                    animationDuration.setText(oldValue);
                }
            }
        });
        animationDuration.setText("500");

        // bind the map's center and zoom properties to the corresponding labels and
        // format them
        labelCenter.textProperty().bind(Bindings.format("center: %s", mapView.centerProperty()));
        labelZoom.textProperty().bind(Bindings.format("zoom: %.0f", mapView.zoomProperty()));
        logger.trace("options and labels done");

        // watch the MapView's initialized property to finish initialization
        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                afterMapIsInitialized();
            }
        });

        // observe the map type radiobuttons
        mapTypeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            logger.debug("map type toggled to {}", newValue.toString());
            MapType mapType = MapType.OSM;
            if (newValue == radioMsOSM) {
                mapType = MapType.OSM;
            } else if (newValue == radioMsBR) {
                mapType = MapType.BINGMAPS_ROAD;
            } else if (newValue == radioMsCd) {
                mapType = MapType.BINGMAPS_CANVAS_DARK;
            } else if (newValue == radioMsCg) {
                mapType = MapType.BINGMAPS_CANVAS_GRAY;
            } else if (newValue == radioMsCl) {
                mapType = MapType.BINGMAPS_CANVAS_LIGHT;
            } else if (newValue == radioMsBA) {
                mapType = MapType.BINGMAPS_AERIAL;
            } else if (newValue == radioMsBAwL) {
                mapType = MapType.BINGMAPS_AERIAL_WITH_LABELS;
            } else if (newValue == radioMsWMS) {
                mapView.setWMSParam(wmsParam);
                mapType = MapType.WMS;
            } else if (newValue == radioMsXYZ) {
                mapView.setXYZParam(xyzParams);
                mapType = MapType.XYZ;
            }
            mapView.setBingMapsApiKey(bingMapsApiKey.getText());
            mapView.setMapType(mapType);
        });
        mapTypeGroup.selectToggle(radioMsOSM);

        // finally initialize the map view
        logger.trace("start map initialization");
        mapView.initialize(Configuration.builder()
                .projection(projection)
                .showZoomControls(false)
                .build());
        logger.debug("initialization finished");

    }

    private void animateMarker(Marker marker, Coordinate oldPosition, Coordinate newPosition) {
        // animate the marker to the new position
        final Transition transition = new Transition() {
            private final Double oldPositionLongitude = oldPosition.getLongitude();
            private final Double oldPositionLatitude = oldPosition.getLatitude();
            private final double deltaLatitude = newPosition.getLatitude() - oldPositionLatitude;
            private final double deltaLongitude = newPosition.getLongitude() - oldPositionLongitude;

            {
                setCycleDuration(Duration.seconds(0.5));
                setOnFinished(evt -> marker.setPosition(newPosition));
            }

            @Override
            protected void interpolate(double v) {
                final double latitude = oldPosition.getLatitude() + v * deltaLatitude;
                final double longitude = oldPosition.getLongitude() + v * deltaLongitude;
                marker.setPosition(new Coordinate(latitude, longitude));
            }
        };
        transition.play();
    }

    /**
     * enables / disables the different controls
     *
     * @param flag
     *             if true the controls are disabled
     */
    private void setControlsDisable(boolean flag) {
        topControls.setDisable(flag);
        leftControls.setDisable(flag);
    }

    /**
     * finishes setup after the mpa is initialzed
     */
    private void afterMapIsInitialized() {
        logger.trace("map intialized");
        logger.debug("setting center and enabling controls...");
        // start at the harbour with default zoom
        mapView.setZoom(ZOOM_DEFAULT);
        mapView.setCenter(center);
        baseStationMarkerList.forEach(mapView::addMarker);
        hospitalMarkerList.forEach(mapView::addMarker);
        gridCentroidCirclesList.forEach(mapView::addMapCircle);
        // now enable the controls
        setControlsDisable(false);
    }
}
