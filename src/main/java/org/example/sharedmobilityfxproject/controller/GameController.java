package org.example.sharedmobilityfxproject.controller;
import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.sharedmobilityfxproject.model.*;
import org.example.sharedmobilityfxproject.model.Map;
import org.example.sharedmobilityfxproject.model.tranportMode.Bicycle;
import org.example.sharedmobilityfxproject.model.tranportMode.Bus;
import org.example.sharedmobilityfxproject.model.tranportMode.Taxi;
import org.example.sharedmobilityfxproject.view.GameView;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class GameController {
    public Boolean flagLoadGame = false;
    private boolean playerTimeout = true;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private SceneController sceneController;
    //class call
    public MainController mainController;
    private GameView gameView;
    public Gem gem;
    public Cell currentCell; // Made public for access in start method
    public Cell playerUnosCell; // Made public for access in start method
    public int currentRow;
    public int currentColumn;


    //GameStart flag
    private boolean isGameStarted = false;
    // Newly Added
    public boolean playerMovementEnabled = true;
    public boolean onBus = false;
    public boolean inTaxi = false;
    public boolean onBicycle = false;
    public Player playerUno;

    //Stamina related
    private int lastX, lastY;  // Last coordinates of the player
    private int stationaryTime = 0;  // Time to stay



    public List<Obstacle> obstacles = new ArrayList<>();
    public ArrayList<int[]> obstacleCoordinates;
    public String stageName;
    public Cell finishCell;
    public Taxi taximan;
    public Bus busman;
    public Bus busman2;
    public Bus busman3;
    public Bus busman4;
    public Bicycle cycleman;
    public Bicycle cycleman2;
    public Bicycle cycleman3;
    public Bicycle cycleman4;
    public ArrayList<busStop> busStops = new ArrayList<>();
    public ArrayList<busStop> busStops2 = new ArrayList<>();
    public ArrayList<busStop> busStops3 = new ArrayList<>();
    public ArrayList<busStop> busStops4 = new ArrayList<>();
    public ArrayList<int[]> busStopCoordinates = new ArrayList<>();
    private Timer timer = new Timer();  // Create a Timer object
    private ScrollPane scrollPane;
    private double cellWidth;
    private double cellHeight;
    private GameOverListener gameOverListener;

    private int numberOfInitialGems = 5; // Replace 5 with the number of gems you want to generate

    private void enableMovementAfterDelay(double timespeed) {
        playerTimeout = false;  // Disable further moves immediately when this method is called
        int delayInMilliseconds = (int) ((timespeed * 1000)+25);  // Convert seconds to milliseconds
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // This will run in a background thread, make sure to run UI updates on the JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    playerTimeout = true;  // Re-enable moves after the specified time
                });
            }
        }, delayInMilliseconds);
    }

    //Player Movement Check for Stamina
    private int moveCounter = 0;

    @FunctionalInterface
    public interface GemCollector {
        void collectGem();
    }

    // Constructor to initialise grid
    public GameController(SceneController sceneController, GameView gameView, Player playerUno) {
        this.sceneController = sceneController;
        this.gameView = gameView;
        this.playerUno = playerUno;
        this.cellWidth = gameView.grid.width/gameView.grid.columns;
        this.cellHeight = gameView.grid.height/gameView.grid.rows;

        Timeline checkStationary = new Timeline(new KeyFrame(Duration.seconds(1), e -> checkAndIncreaseStamina()));
        checkStationary.setCycleCount(Timeline.INDEFINITE);
        setupGameEndListener();
        checkStationary.play();


    }
    private void setupGameEndListener() {
System.out.println("GameEndListener in GameController");
        this.gameView.gameEndFlagProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                this.gameView.getEndStage().setOnAction(event -> {
                    this.gameView.mediaPlayer.stop();
                    this.gameView.gameOverDialog.close();
                    if (this.gameView.isTimeOut && this.gameView.isGemCollectedEnough && this.gameView.isCO2Safe) {
//                       this.gameView.gemCountReset();
                        this.gameView.setNextStageCleared(stageName);
                        SceneController.isGoingToNext();
                    }else{
                        // Mission Fail
                        SceneController.isGoingToNext();
                    }

                });


            }
        });
    }

    public void startPlayingGame(String stageName) {
        this.sceneController.initGameScene(stageName);

        this.stageName = stageName;
        this.isGameStarted = true;
        // Before showing the primary stage, set the close request handler to save the game state
        gameView.getPrimaryStage().setOnCloseRequest(event -> {
            saveGameState();
            System.out.println("Game state saved on close.");
        });


        System.out.println("GameController startPlayingGame");

        // Fill the grid with cells
        for (int row = 0; row < gameView.getRows(); row++) {
            for (int column = 0; column < gameView.getColumns(); column++) {
                Cell cell = new Cell(column, row);
                gameView.grid.add(cell, column, row);
            }
        }

        // Create label for carbon footprint
//            carbonFootprintLabel = new Label("Carbon Footprint: " + String.format("%.1f", carbonFootprint));
//            carbonFootprintLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: red;");
//            carbonFootprintLabel.setAlignment(Pos.TOP_LEFT);
//            carbonFootprintLabel.setPadding(new Insets(10));

        // Create a VBox to hold the gem count label
//            VBox vbox = new VBox(gemCountLabel, carbonFootprintLabel);
//            vbox.setAlignment(Pos.TOP_LEFT);

//        // Sample map array
//        int[][] mapArray = {
//                {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
//                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
//                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
//                {0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
//                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
//                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
//                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
//                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
//                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
//                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1}
//        };

        // Start filling the grid with obstacles and other stuff first checking the map size
        /**
         * Checks if the stage name contains "big". If it does, it sets the number of columns and rows
         * in the game view to larger values (850 and 330 respectively). If it doesn't, it sets the number
         * of columns and rows to smaller values (120 and 80 respectively). This is used to adjust the size
         * of the game view based on the stage.
         */
        if (stageName.contains("Big")) {
            gameView.setCOLUMNS(850);
            gameView.setROWS(330);
        } else {
            gameView.setCOLUMNS(120);
            gameView.setROWS(80);
        }
        // Pass the sizes to Map so it can generate the correct map array
        Map map = new Map(gameView.getRows(), gameView.getColumns());
        // Fill the grid with the map array, we pass the stage name to the method so it can load the correct map from the resources
        fillGridWithMapArray(map, stageName);





        obstacleCoordinates = new ArrayList<>();

        for (Obstacle obstacle : obstacles) {
            obstacleCoordinates.add(new int[]{obstacle.getColumn(), obstacle.getRow()});
        }
        System.out.println("Obstacle Coordinates: ");

        generateGems(gameView.grid, numberOfInitialGems); // Replace 5 with the number of gems you want to generate

        // Place the finish cell after the grid is filled and the player's position is initialised
        int finishColumn;
        int finishRow;
        finishColumn = 102;
        finishRow = 58;

        finishCell = new Cell(finishColumn, finishRow);
        finishCell.getStyleClass().add("finish");
        gameView.grid.add(finishCell, finishColumn, finishRow);

        metroStop metro1 = new metroStop(2,30);
        gameView.grid.add(metro1,2,30);



        busman = new Bus(busStops,busStops.getFirst().getX()-1, busStops.getFirst().getY());
        if(busStops2.size() > 0){
            busman2 = new Bus(busStops2,busStops2.getFirst().getX()-1, busStops2.getFirst().getY());
        }
        if(busStops3.size() > 0){
            busman3 = new Bus(busStops3,busStops3.getFirst().getX(), busStops3.getFirst().getY());
        }
        if(busStops4.size() > 0) {
            busman4 = new Bus(busStops4, busStops4.getFirst().getX() - 1, busStops4.getFirst().getY());
        }

        for (int i = 0; i < busman.list().size(); i++){
            busStop stop = busman.list().get(i);
            gameView.grid.add(stop, stop.getX(), stop.getY());
        }

        gameView.grid.add(busman, busman.getX(), busman.getY());// Example starting position

        if(busStops2.size() > 0){
            for (int i = 0; i < busman2.list().size(); i++){
                busStop stop = busman2.list().get(i);
                gameView.grid.add(stop, stop.getX(), stop.getY());
            }
            gameView.grid.add(busman2, busman2.getX(), busman2.getY());
        }
        if(busStops3.size() > 0){
            for (int i = 0; i < busman3.list().size(); i++){
                busStop stop = busman3.list().get(i);
                gameView.grid.add(stop, stop.getX(), stop.getY());
            }
            gameView.grid.add(busman3, busman3.getX(), busman3.getY());
        }
        if(busStops4.size() > 0){
            for (int i = 0; i < busman4.list().size(); i++){
                busStop stop = busman4.list().get(i);
                gameView.grid.add(stop, stop.getX(), stop.getY());
            }
            gameView.grid.add(busman4, busman4.getX(), busman4.getY());
        }

        taximan = new Taxi (58,28);

        gameView.grid.add(taximan, taximan.getX(), taximan.getY());
        gameView.grid.add(cycleman, cycleman.getX(), cycleman.getY());
        gameView.grid.add(cycleman2, cycleman2.getX(), cycleman2.getY());
        if (cycleman3 != null) {
            gameView.grid.add(cycleman3, cycleman3.getX(), cycleman3.getY());
        }
        if (cycleman4 != null) {
            gameView.grid.add(cycleman4, cycleman4.getX(), cycleman4.getY());
        }

        // Schedule the bus to move every second
        List<Bus> busmen = Arrays.asList(busman);

        if(busStops2.size() > 0){
            busmen = Arrays.asList(busman, busman2);
        }
        if(busStops3.size() > 0){
            busmen = Arrays.asList(busman, busman2, busman3);
        }
        if(busStops4.size() > 0) {
            busmen = Arrays.asList(busman, busman2, busman3, busman4);
        }

        // Schedule the bus to move every second
        List<Bus> finalBusmen = busmen;
        Timeline busMovementTimeline = new Timeline(new KeyFrame(Duration.seconds(.1), event -> {
            for (Bus busman : finalBusmen) {
                busStop targetBusStop = busman.nextStop(); // Assuming this method correctly returns the next bus stop
                if (taximan.hailed && !taximan.arrived) {
                    moveTaxiTowardsPlayer(gameView.grid, taximan);
                }
                if (onBicycle) {
                    if (cycleman.bikeTime == 0 || cycleman2.bikeTime == 0|| cycleman3.bikeTime == 0|| cycleman4.bikeTime == 0) {
                        onBicycle = false;
                        Image bikeman = new Image(new File("src/main/resources/images/playerSprite.png").toURI().toString());
                        playerUno.playerVisual = new ImageView(bikeman);
                        ((ImageView) playerUno.playerVisual).setFitHeight(10); // Set the size as needed
                        ((ImageView) playerUno.playerVisual).setFitWidth(30);
                        ((ImageView) playerUno.playerVisual).setPreserveRatio(true);
                    }
                    if (cycleman.bikeTime >= 1 || cycleman2.bikeTime >= 1|| cycleman3.bikeTime >= 1|| cycleman4.bikeTime >= 1) {
                        cycleman.bikeTime -= 1;
                        cycleman2.bikeTime -= 1;
                        if (cycleman3 != null) {
                            cycleman3.bikeTime -= 1;
                        }
                        if (cycleman4 != null) {
                            cycleman4.bikeTime -= 1;
                        }
                    }
                }
                if (!busman.isWaiting) {
                    moveBusTowardsBusStop(busman, targetBusStop);

                    // Here's the updated part
                    if (onBus) {
                        // Update player's coordinates to match the bus when the player is on the bus
                        playerUno.setCellByCoords(gameView.grid, busman.getX(), busman.getY());
                        System.out.println("Player coordinates (on bus): " + playerUno.getCoordX() + ", " + playerUno.getCoordY());
                        //Increase carbon footprint amount as long as player is on the bus
                        double carbonFootprint = 0.2; //subject to change
                    }
                } else {
                    if (busman.waitTime == 0) {
                        busman.waitASec();
                    } else {
                        busman.waitTime--;
                    }
                }
            }
        }));

        busMovementTimeline.setCycleCount(Animation.INDEFINITE);
        busMovementTimeline.play();


        // give playerUno a cell goddamit
        playerUno.initCell(gameView.grid);

        // Set the players position in a place that is not an obstacle
        Cell middlePlayerCell = findRoadNearMiddle(gameView.grid);
        playerUno.setCellByCoords(gameView.grid, middlePlayerCell.getColumn(), middlePlayerCell.getRow());

        gameView.getScene().setOnKeyPressed(e -> setupKeyboardActions(e.getCode()));

        // Load the gameState
        if(flagLoadGame) {
            loadGameState();
        }
        //TODO: later on we need to enable game load

        double pivotX = this.gameView.scale.getPivotX();
        double pivotY = this.gameView.scale.getPivotY();

// Calculate the translation needed to recenter the scale
        double translateX = playerUno.getCoordX() * cellWidth * (1.7 - this.gameView.scale.getX()) - pivotX;
        double translateY = playerUno.getCoordY() * cellHeight * (2.3 - this.gameView.scale.getY()) - pivotY;

// Apply translation to the grid to recenter
        this.gameView.grid.setTranslateX(this.gameView.grid.getTranslateX() - translateX);
        this.gameView.grid.setTranslateY(this.gameView.grid.getTranslateY() - translateY);
        System.out.println(busStopCoordinates.toString());
        labelChangr();
    }
    public int manhattanDistance(int x,int y ,int px, int py) {
        return Math.abs(x - px) + Math.abs(y - py);
    }
    public Gem findClosestGem(List<Gem> gems, int playerX, int playerY) {
        Gem closest = null;
        int minDistance = Integer.MAX_VALUE;  // Start with the largest possible distance

        for (Gem gem : gems) {
            int distance = manhattanDistance(playerUno.getCoordX(), playerUno.getCoordY(),gem.getColumn(),gem.getRow());
            if (distance < minDistance) {
                minDistance = distance;
                closest = gem;
            }
        }

        return closest;
    }
    private void labelChangr() {

        Gem closestGem = findClosestGem(gameView.gemlist, playerUno.getCoordX(), playerUno.getCoordY());
        if (closestGem != null) {
            System.out.println("New closest gem at: " + closestGem.getColumn() + ", " + closestGem.getRow());
            gameView.gemX = closestGem.getColumn();
            gameView.gemY = closestGem.getRow();
            gameView.updateGemLoc();
            findDirec(closestGem);
            gameView.updateGemDirec();
        } else {
            System.out.println("No more gems left to collect.");
            // Optionally, disable directions or handle the case when no gems are left.
        }
    }
    private void findDirec(Gem closestGem){
        if (closestGem.getColumn() < playerUno.getCoordX()){
            gameView.toGem = "LEFT";
        }
        if (closestGem.getColumn() > playerUno.getCoordX()){
            gameView.toGem = "RIGHT";
        }
        if (closestGem.getRow() > playerUno.getCoordY()){
            gameView.toGem = "DOWN";
        }
        if (closestGem.getRow() < playerUno.getCoordY()){
            gameView.toGem = "UP";
        }

    }
    /**
     * This method fills the grid based on the contents of a map array from a Map object.
     * It initializes a 2D array with dimensions 80x120 and attempts to retrieve the map array.
     * If an exception occurs, it prints the stack trace.
     * Each cell in the array is processed based on its value:
     * - 1: Creates and adds an Obstacle to the list.
     * - 2: Colors the cell green.
     * - 3: Colors the cell blue.
     * - 4: Marks the cell as a bus stop.
     * - 9: Also creates and adds an Obstacle, similar to 1.
     * The method handles each cell by its specified action, enhancing the game's visual and functional complexity.
     */
    public void fillGridWithMapArray(Map map, String stageName) {
        System.out.printf("Filling grid with map array %s - %s...%n", gameView.getColumns(), gameView.getRows());
        System.out.printf("Filling grid with map array %s - %s...%n", gameView.grid.getColumns(), gameView.grid.getRows());
        int[][] mapArray = new int[gameView.getRows()][gameView.getColumns()];  // Default map size initialization
        try {
            mapArray = map.getMapArray(stageName);  // Attempt to retrieve the map array TODO: needs to be converted to stageName
        } catch (Exception e) {
            e.printStackTrace();  // Print any errors encountered
        }

        for (int row = 0; row < mapArray.length; row++) {
            for (int column = 0; column < mapArray[row].length; column++) {
                switch (mapArray[row][column]) {
                    case 0:  // Cell is empty ie road
                        Cell roadCell = gameView.grid.getCell(column, row);
                        roadCell.getStyleClass().add("road");
                        gameView.grid.add(roadCell, column, row);
                        break;
                    case 1:  // Cell indicates an obstacle
                    case 9:  // Cell also indicates an obstacle (same behavior as 1)
                        Obstacle obstacle = new Obstacle(gameView.grid, column, row);
                        obstacles.add(obstacle);
                        break;
                    case 2:  // Color the cell green
                        gameView.grid.grass(column,row);
                        break;
                    case 3:  // Initialise water as an obstacle and then colour the cell blue
                        Obstacle obstacleWater = new Obstacle(gameView.grid, column, row);
                        obstacles.add(obstacleWater);
                        gameView.grid.water(column,row);
                        break;
                    case 4:  // Mark as bus stop
                        busStop busS = new busStop(column,row);
                        busStopCoordinates.add(new int[]{busS.getX(), busS.getY()});
                        busStops.add(busS);
                        break;
                    case 42:  // Mark as bus stop
                        busStop busS2 = new busStop(column,row);
                        busStopCoordinates.add(new int[]{busS2.getX(), busS2.getY()}); // Coordinates can stay the same only once used for player to interact with bus stop
                        busStops2.add(busS2);
                        break;
                    case 43:  // Mark as bus stop
                        busStop busS3 = new busStop(column,row);
                        busStopCoordinates.add(new int[]{busS3.getX(), busS3.getY()});
                        busStops3.add(busS3);
                        break;
                    case 44:  // Mark as bus stop
                        busStop busS4 = new busStop(column,row);
                        busStopCoordinates.add(new int[]{busS4.getX(), busS4.getY()});
                        busStops4.add(busS4);
                        break;
                    case 6:  // Mark as cycleman
                        if(cycleman == null) {
                            cycleman = new Bicycle(column, row);
                        } else if(cycleman2 == null) {
                            cycleman2 = new Bicycle(column, row);
                        } else if(cycleman3 == null) {
                            cycleman2 = new Bicycle(column, row);
                        } else if(cycleman4 == null) {
                            cycleman2 = new Bicycle(column, row);
                        }
                        break;
                    default:
                        // Optionally handle default case if needed
                        break;
                }
            }
        }
        busStops = sortAndReverseBusStops(busStops);
        if(busStops2.size() > 0){
            busStops2 = sortAndReverseBusStops(busStops2);
        }
        if(busStops3.size() > 0){
            busStops3 = sortAndReverseBusStops(busStops3);
        }
        if(busStops4.size() > 0){
            busStops4 = sortAndReverseBusStops(busStops4);
        }

        // Optionally, print bus stop coordinates
        busStops4.forEach(stop -> System.out.println("Bus Stop Coordinates: X = " + stop.getX() + ", Y = " + stop.getY()));
    }

    public ArrayList<busStop> sortAndReverseBusStops(ArrayList<busStop> busStopInstance) {
        // Sort bus stops
        busStopInstance.sort((busStop1, busStop2) -> {
            if (busStop1.getY() > busStop2.getY() && busStop1.getX() <= busStop2.getX() && busStop1.getY() >= busStop1.getX()){
                return -1;
            } else if ((busStop1.getY() < busStop2.getY()) && (busStop1.getX() >= busStop2.getX())){
                return 1;
            } else if ((busStop1.getY() > busStop2.getY()) && (busStop1.getX() > busStop2.getX()) && busStop1.getY() >= busStop1.getX()){
                return -1;
            } else if ((busStop1.getY() < busStop2.getY()) && (busStop1.getX() < busStop2.getX())){
                return -1;
            } else {
                return 0;
            }
        });

        // Reverse the bus stops list to get the correct order
        ArrayList<busStop> reversedList = new ArrayList<>(busStopInstance);
        reversedList.removeFirst();
        reversedList.removeLast();
        Collections.reverse(reversedList);

        ArrayList<busStop> newList = new ArrayList<>(busStopInstance);
        newList.addAll(reversedList);

        // Update the bus stops list with the sorted list
        return newList;
    }


    public Cell findRoadNearMiddle(Grid grid) {
    int middleRow = grid.getRows() / 2;
    int middleColumn = grid.getColumns() / 2;
    int[] dr = {0, 1, 0, -1}; // Directions for row
    int[] dc = {1, 0, -1, 0}; // Directions for column

    for (int r = 0, c = 0, di = 0, jump = 0; r < grid.getRows() && c < grid.getColumns(); jump++, di = (di + 1) % 4) {
        for (int j = 0; j < jump / 2 + 1; j++) {
            if (middleRow + r >= 0 && middleRow + r < grid.getRows() && middleColumn + c >= 0 && middleColumn + c < grid.getColumns()) {
                Cell cell = grid.getCell(middleRow + r, middleColumn + c);
                if (cell.getStyleClass().contains("road")) {
                    return cell;
                }
            }
            r += dr[di];
            c += dc[di];
        }
    }
    return null; // Return null if no road is found
}


    private void generateGems(Grid grid, int numberOfGems) {
        gameView.gemlist= new ArrayList<>();
        for (int i = 0; i < numberOfGems; i++) {
            int gemColumn;
            int gemRow;
            boolean isObstacle;
            do {
                gemColumn = (int) (Math.random() * gameView.getColumns());
                gemRow = (int) (Math.random(

                ) * gameView.getRows());
                int finalGemColumn = gemColumn;
                int finalGemRow = gemRow;

                //It is called before gemGeneration
                // obstacleCoordinates is the list of obstacles's coordinates
                isObstacle = obstacleCoordinates.stream().anyMatch(coords -> coords[0] == finalGemColumn && coords[1] == finalGemRow);
                //Check
            } while (isObstacle || (gemColumn == 0 && gemRow == 0) || grid.getCell(gemColumn, gemRow).getUserData() != null); // Ensure gem doesn't spawn at player's starting position or on another gem


            Gem gem = new Gem(gemColumn, gemRow);
            gameView.gemlist.add(gem);
            grid.add(gem, gemColumn, gemRow);
        }
    }

    public void moveTaxiTowardsPlayer(Grid grid, Taxi bus) {

        if (bus.getX() == playerUno.getCoordX() && bus.getY() == playerUno.getCoordY() && taximan.arrived && !inTaxi) {
            System.out.println("arrived at player");
            inTaxi = true;
            System.out.println("hiding player");
            playerUno.playerVisual.setVisible(false);

        }
//        System.out.println(distanceIfMoveX+"   "+distanceIfMoveY);
        else {
            if ((bus.getX() < playerUno.getCoordX() || bus.getX() > playerUno.getCoordX()) && bus.flagMove == 0) {
//            System.out.println("----------- moving x ---------");
                // Move horizontally towards the bus stop, if not blocked
                int newX = bus.getX() + (bus.getX() < playerUno.getCoordX() ? 1 : -1);
                if (canMoveBusTo(newX, bus.getY())) {
                    moveTaxi(grid, bus, newX, bus.getY());
                } else if (canMoveBusTo(bus.getX(), bus.getY() + (bus.getY() < playerUno.getCoordY() ? 1 : -1))) {
                    // Move vertically as a fallback
                    moveTaxi(grid, bus, bus.getX(), bus.getY() + (bus.getY() < playerUno.getCoordY() ? 1 : -1));
                }
            } else if (bus.getY() < playerUno.getCoordY() || bus.getY() > playerUno.getCoordY()) {
//            System.out.println("----------- moving y ---------");
                // Move vertically towards the bus stop, if not blocked
                int newY = bus.getY() + (bus.getY() < playerUno.getCoordY() ? 1 : -1);
                if (canMoveBusTo(bus.getX(), newY)) {

                    moveTaxi(grid, bus, bus.getX(), newY);
                } else if (canMoveBusTo(bus.getX() + 1, bus.getY())&&bus.movingleft==0) {
                    // Move horizontally as a fallbackf
                    if (bus.flagMove == 0) {
                        bus.flagMove = 1;
                    }
                    moveTaxi(grid, bus, bus.getX() + +1, bus.getY());

                }
                else if (canMoveBusTo(bus.getX() - 1, bus.getY())) {
                    if(bus.movingleft==0){bus.movingleft=20;
                    }

;                    // Move horizontally as a fallbackf
                    if(bus.movingleft>0){moveTaxi(grid, bus, bus.getX() -1, bus.getY());
                        bus.movingleft--;
                    }

                }
            }
            //arriving at stop logic
            else if (bus.getX() == playerUno.getCoordX() && bus.getY() == playerUno.getCoordY()) {
                System.out.println("----------- Taxi arrived ---------");
                bus.arrived = true;

                if (bus.hailed && playerUno.getCoordX() == bus.getX() && playerUno.getCoordY() == bus.getY()) {
                    System.out.println("----------- You just got in the taxia---------");
                    inTaxi = true;
                    playerUno.playerVisual.setVisible(false);
                } else if (inTaxi) {

                    System.out.println("----------- Press E to get off  ---------");
                    inTaxi = true;

                }
            } else if (bus.getY() == playerUno.getCoordY()) {
                bus.flagMove = 0;
            }

        }
    }

    public void moveBusTowardsBusStop(Bus bus, busStop stop) {
        // Calculate the Manhattan distance for both possible next steps
        int distanceIfMoveX = Math.abs((bus.getX()) - stop.getX());
        int distanceIfMoveY = Math.abs(bus.getY() - stop.getY());
//        System.out.println("--------------------");
//        System.out.println(stop.getX()+"   "+ stop.getY());

//        System.out.println(distanceIfMoveX+"   "+distanceIfMoveY);
        if ((bus.getX() < stop.getX() || bus.getX() > stop.getX()) && bus.flagMove == 0) {
//            System.out.println("----------- moving x ---------");
            // Move horizontally towards the bus stop, if not blocked
            int newX = bus.getX() + (bus.getX() < stop.getX() ? 1 : -1);
            if (canMoveBusTo(newX, bus.getY())) {
                moveBus(bus, newX, bus.getY());
            } else if (canMoveBusTo(bus.getX(), bus.getY() + (bus.getY() < stop.getY() ? 1 : -1))) {
                // Move vertically as a fallback
                moveBus(bus, bus.getX(), bus.getY() + (bus.getY() < stop.getY() ? 1 : -1));
            }
        } else if (bus.getY() < stop.getY() || bus.getY() > stop.getY()) {
//            System.out.println("----------- moving y ---------");
            // Move vertically towards the bus stop, if not blocked
            int newY = bus.getY() + (bus.getY() < stop.getY() ? 1 : -1);
            if (canMoveBusTo(bus.getX(), newY)) {

                moveBus(bus, bus.getX(), newY);
            } else if (canMoveBusTo(bus.getX() + 1, bus.getY())) {
                // Move horizontally as a fallback
                if (bus.flagMove == 0) {
                    bus.flagMove = 1;
                }
                moveBus(bus, bus.getX() + +1, bus.getY());
            }
        }
        //arriving at stop logic
        else if (bus.getX() == stop.getX() && bus.getY() == stop.getY()) {
            System.out.println("----------- ARRIVED..... GET THE FUCK OUT ---------");
            bus.waitASec();
            bus.list().add(bus.list().remove(0));
            System.out.println("now going towards :" + bus.nextStop());
            if (!this.onBus&&!playerMovementEnabled && playerUno.getCoordX() == bus.getX() && playerUno.getCoordY() == bus.getY()) {
                System.out.println("----------- You just got on the bus ---------");
                this.onBus = true;

            } else if (this.onBus) {
                System.out.println("----------- You arrived at  ---------" + stop);
                System.out.println("----------- Press E to get off  ---------");
                this.onBus = true;

            }
        } else if (bus.getY() == stop.getY()) {
            bus.flagMove = 0;
        }

    }

    public void setupKeyboardActions(KeyCode key) {
        if (this.inTaxi && playerTimeout) {
            double currentCo2 = sceneController.getCo2Gauge();
            double potentialCo2;
            switch (key) {
                case D:
                    movePlayer(2, 0);
                    potentialCo2 = currentCo2 + 1.0;
                    if (potentialCo2 >= 100.0) {
                        gameFailedCall();
                    } else {
                        sceneController.increaseCo2GaugeUpdate(1.0); // Safely increase CO2
                    }
                    break;
                case A:
                    movePlayer(-2, 0);
                    potentialCo2 = currentCo2 + 1.0;
                    if (potentialCo2 >= 100.0) {
                        gameFailedCall();
                    } else {
                        sceneController.increaseCo2GaugeUpdate(1.0); // Safely increase CO2
                    }
                    break;
                case W:
                    movePlayer(0, -2);
                    potentialCo2 = currentCo2 + 1.0;
                    if (potentialCo2 >= 100.0) {
                        gameFailedCall();
                    } else {
                        sceneController.increaseCo2GaugeUpdate(1.0); // Safely increase CO2
                    }
                    break;
                case S:
                    movePlayer(0, 2);
                    potentialCo2 = currentCo2 + 1.0;
                    if (potentialCo2 >= 100.0) {
                        gameFailedCall();
                    } else {
                        sceneController.increaseCo2GaugeUpdate(1.0); // Safely increase CO2
                    }
                    break;
                case T:
                    this.inTaxi = false;
                    playerUno.playerVisual.setVisible(true); // Hide player sprite when T is pressed
                    taximan.hailed = !taximan.hailed;
                    taximan.arrived = false;
                    break;
                case E:
                    togglePlayerMovement();
                    break;
                case C:
                    System.out.println("The player is located at coordinates: (" + playerUno.getCoordX() + ", " + playerUno.getCoordY() + ")" +
                            "\nPlayer is currently " + (onBus ? "on the bus." : "not on the bus.") +
                            "\nPlayer is " + (playerMovementEnabled ? "moving." : "waiting.") +
                            "\nBus is at coordinates: (" + busman.getX() + "," + busman.getY() + ")");
                    break;
            }

            enableMovementAfterDelay(taximan.timeSpeed);
        } else if (onBicycle && playerMovementEnabled && playerTimeout) {
            System.out.println("Bicycle time: " + cycleman.bikeTime + " you are still on bike");
            switch (key) {
                case D -> movePlayer(2, 0);
                case A -> movePlayer(-2, 0);
                case W -> movePlayer(0, -2);
                case S -> movePlayer(0, 2
                );
                case C ->
                        System.out.println("The player is located at coordinates: (" + playerUno.getCoordX() + ", " + playerUno.getCoordY() + ")" +
                                "\nPlayer is currently " + (onBicycle ? "on bicycle." : "not on the bicycle.") +
                                "\nPlayer is " + (playerMovementEnabled ? "moving." : "waiting.") +
                                "\nBicycle is at coordinates: (" + cycleman.getX() + "," + cycleman.getY() + ")");
            }
            enableMovementAfterDelay(playerUno.speedTime);
        } else if (playerMovementEnabled && playerTimeout) {
            switch (key) {
                case D -> movePlayer(1, 0);
                case A -> movePlayer(-1, 0);
                case W -> movePlayer(0, -1);
                case S -> movePlayer(0, 1);
                case T -> hailTaxi();
                case E -> togglePlayerMovement();
            }
            enableMovementAfterDelay(playerUno.speedTime);
        } else if (key == KeyCode.E) {
            togglePlayerMovement();
        } else if (key == KeyCode.E) {
            togglePlayerMovement();
        }
    }

    private void moveTaxi(Grid grid, Taxi bus, int newX, int newY) {
        // Move the bus to the new position (newX, newY) on the grid

        grid.moveCell(bus, newX, newY);

        bus.setX(newX);
        bus.setY(newY);
        //grid.add(cell,cell.getColumn(),cell.getRow());

    }

    private void moveBus(Bus bus, int newX, int newY) {
        // Move the bus to the new position (newX, newY) on the grid
//        System.out.println("BUS MOVING TO :  "+newX+"  "+newY+". GET OUT THE FUCKING WAY");
//        int x = bus.getX();
//        int y = bus.getY();
        //Cell cell = grid.getCell(x,y);
        if(this.onBus) {
            double pivX = newX*cellWidth;
            double pivY = newY*cellHeight;
            updateScalePivot(gameView.grid, pivX, pivY, bus.timeSpeed);
        }
        gameView.grid.updateCellPosition(bus, newX, newY);

        bus.setX(newX);
        bus.setY(newY);
        //grid.add(cell,cell.getColumn(),cell.getRow());

    }

    private boolean canMoveBusTo(int x, int y) {
        // Implement logic to check if the bus can move to (x, y) considering obstacles
        // Return true if it can move, false if there's an obstacle
        return obstacles.stream().noneMatch(obstacle -> obstacle.getColumn() == x && obstacle.getRow() == y);
    }
    public void updateScalePivot(Node node, double newPivotX, double newPivotY, double durationSeconds) {
        Duration duration = Duration.seconds(durationSeconds);

        double oldPivotX = node.getScaleX();
        double oldPivotY = node.getScaleY();
        double deltaX = newPivotX - oldPivotX;
        double deltaY = newPivotY - oldPivotY;

        // Adjust the pivot based on the player's movement
        double updatedPivotX = oldPivotX + deltaX;
        double updatedPivotY = oldPivotY + deltaY;

        Timeline timeline = new Timeline();
        KeyValue kvX = new KeyValue(gameView.scale.pivotXProperty(), updatedPivotX*1.5



        );
        KeyValue kvY = new KeyValue(gameView.scale.pivotYProperty(), updatedPivotY*1.5);
        KeyFrame kf = new KeyFrame(duration, kvX, kvY);

        timeline.getKeyFrames().add(kf);
        timeline.play();
    }
    private void movePlayer(int dx, int dy) {
        labelChangr();
        playerUno.setIsWalking(true);
        boolean isDoubleMove = Math.abs(dx) == 2 || Math.abs(dy) == 2;
        boolean onedist = true;
        if (isDoubleMove) {
            // Calculate the target cell coordinates
            int targetX = playerUno.getCoordX() + (dx / 2);
            int targetY = playerUno.getCoordY() + (dy / 2);

            // Check if the player can move to the target cell
            if (!canMoveTo(targetX, targetY)) {
                onedist = false;
            }
        }
        if (playerUno.getStamina() <= 0) {
            gameView.playNoStaminaSound();
            return;
        }

        int newRow = Math.min(Math.max(playerUno.getCoordY() + dy, 0), gameView.grid.getRows() - 1);
        int newColumn = Math.min(Math.max(playerUno.getCoordX() + dx, 0), gameView.grid.getColumns() - 1);
        Cell newCell = gameView.grid.getCell(newColumn, newRow);

        if (gameView.isMetroSceneActive) {
            playerUno.getCell().unhighlight();

            playerUno.getCell().highlight();
        }
//        if (playerUno.getCell() instanceof metroStop) {
//
//            gameView.isMetroSceneActive = !gameView.isMetroSceneActive;
//            gameView.switchSceneToMetro();// Metro scene is now active
//            Stage primaryStage = (Stage) gameView.grid.getScene().getWindow();
//            playerUno.isUnderground = true;
//            System.out.println(gameView.grid);
//
//
//            playerUno.setCellByCoords(gameView.grid, newColumn, newRow);
//
//        }
        if (canMoveTo(newColumn, newRow)&&(!inTaxi&&!onBicycle)) {
            playerUno.getCell().unhighlight();
            playerUno.setX(newColumn);
            playerUno.setY(newRow);
            double pivotX = playerUno.getCoordX() * cellWidth;  // cellWidth is the width of one grid cell
            double pivotY = playerUno.getCoordY() * cellHeight;

//            gameView.grid.updateCellPosition(playerUno.getCell(),playerUno.getCoordX(),playerUno.getCoordY());
            playerUno.setCell(gameView.grid.getCell(newColumn, newRow), gameView.grid);

            updateScalePivot(gameView.grid, pivotX, pivotY, playerUno.speedTime);
            // Setup to follow player

            playerUno.getCell().highlight();
            interactWithCell(playerUno.getCell());
            if (inTaxi && !(playerUno.isWalking)) {
                // Assuming taximan is accessible from here, or find a way to access it
                moveTaxi(gameView.grid, taximan, newColumn, newRow);
            }
//MoveCounter for walking and decrease stamina every 5 moves
            if (!inTaxi && playerUno.getIsWalking()) {
                moveCounter++;
                //Decrease stamina every 5 moves
                if (moveCounter >= 5) {
                    playerUno.decreaseStamina();
                    gameView.updateStamina(playerUno.getStamina());
                    moveCounter = 0;
                }
            }

        }
        interactWithCell(gameView.grid.getCell(newColumn, newRow));


        if (inTaxi&&canMoveTo(newColumn, newRow)&&onedist) {
            playerUno.getCell().unhighlight();
            playerUno.setX(newColumn);
            playerUno.setY(newRow);
            double pivotX = playerUno.getCoordX() * cellWidth;  // cellWidth is the width of one grid cell
            double pivotY = playerUno.getCoordY() * cellHeight;

//            gameView.grid.updateCellPosition(playerUno.getCell(),playerUno.getCoordX(),playerUno.getCoordY());
            playerUno.setCell(gameView.grid.getCell(newColumn, newRow), gameView.grid);

            updateScalePivot(gameView.grid, pivotX, pivotY, playerUno.speedTime);
            // Setup to follow player

            playerUno.getCell().highlight();
            interactWithCell(playerUno.getCell());
            // Assuming taximan is accessible from here, or find a way to access it
            moveTaxi(gameView.grid, taximan, newColumn, newRow);


        }
        else if(inTaxi&&onedist){
            int oneRow = Math.min(Math.max(playerUno.getCoordY() + (dy/2), 0), gameView.grid.getRows() - 1);
            int oneColumn = Math.min(Math.max(playerUno.getCoordX() + (dx/2), 0), gameView.grid.getColumns() - 1);
            if (inTaxi&&canMoveTo(oneColumn, oneRow)) {
                playerUno.getCell().unhighlight();
                playerUno.setX(oneColumn);
                playerUno.setY(oneRow);
                double pivotX = playerUno.getCoordX() * cellWidth;  // cellWidth is the width of one grid cell
                double pivotY = playerUno.getCoordY() * cellHeight;

//            gameView.grid.updateCellPosition(playerUno.getCell(),playerUno.getCoordX(),playerUno.getCoordY());
                playerUno.setCell(gameView.grid.getCell(oneColumn, oneRow), gameView.grid);

                updateScalePivot(gameView.grid, pivotX, pivotY, playerUno.speedTime);


                playerUno.getCell().highlight();
                interactWithCell(playerUno.getCell());
                // Assuming taximan is accessible from here, or find a way to access it
                moveTaxi(gameView.grid, taximan, oneColumn, oneRow);

            }}

        if (onBicycle&&canMoveTo(newColumn, newRow)&&onedist) {

            playerUno.getCell().unhighlight();
            playerUno.setX(newColumn);
            playerUno.setY(newRow);
            double pivotX = playerUno.getCoordX() * cellWidth;  // cellWidth is the width of one grid cell
            double pivotY = playerUno.getCoordY() * cellHeight;

//            gameView.grid.updateCellPosition(playerUno.getCell(),playerUno.getCoordX(),playerUno.getCoordY());
            playerUno.setCell(gameView.grid.getCell(newColumn, newRow), gameView.grid);

            updateScalePivot(gameView.grid, pivotX, pivotY, playerUno.speedTime);
            // Setup to follow player

            playerUno.getCell().highlight();
            interactWithCell(playerUno.getCell());
            // Assuming taximan is accessible from here, or find a way to access it


        }
        else if(onBicycle&&onedist){
            int oneRow = Math.min(Math.max(playerUno.getCoordY() + (dy/2), 0), gameView.grid.getRows() - 1);
            int oneColumn = Math.min(Math.max(playerUno.getCoordX() + (dx/2), 0), gameView.grid.getColumns() - 1);
            if (onBicycle&&canMoveTo(oneColumn, oneRow)) {
                playerUno.getCell().unhighlight();
                playerUno.setX(oneColumn);
                playerUno.setY(oneRow);
                double pivotX = playerUno.getCoordX() * cellWidth;  // cellWidth is the width of one grid cell
                double pivotY = playerUno.getCoordY() * cellHeight;

//            gameView.grid.updateCellPosition(playerUno.getCell(),playerUno.getCoordX(),playerUno.getCoordY());
                playerUno.setCell(gameView.grid.getCell(oneColumn, oneRow), gameView.grid);

                updateScalePivot(gameView.grid, pivotX, pivotY, playerUno.speedTime);


                playerUno.getCell().highlight();
                interactWithCell(playerUno.getCell());
                // Assuming taximan is accessible from here, or find a way to access it

            }}
    }

    private boolean canMoveTo(int x, int y) {
        return this.obstacles.stream().noneMatch(obstacle -> obstacle.getColumn() == x && obstacle.getRow() == y);
    }

    private void interactWithCell(Cell cell) {

        if ("gem".equals(cell.getUserData())) {
            collectGem(cell);
        } else if (cell instanceof busStop) {
            interactWithBusStop((busStop) cell);
        } else if (cell == finishCell) {
            finishGame();
        } else if (cell instanceof Bicycle) {
            bikeTime((Bicycle) cell);  // Cast and pass the Bicycle cell directly
        }    }
private void bikeTime(Bicycle bike){
    playerUno.playerVisual.setVisible(false);

    Image bikeman = new Image(new File("src/main/resources/images/bike.png").toURI().toString());
    playerUno.playerVisual = new ImageView(bikeman);
    ((ImageView) playerUno.playerVisual).setFitHeight(10); // Set the size as needed
    ((ImageView) playerUno.playerVisual).setFitWidth(30);
    ((ImageView) playerUno.playerVisual).setPreserveRatio(true);
    onBicycle = true;
    bike.bikeTime=300;

    System.out.println(onBicycle);

}
    private void collectGem(Cell gemCell) {
        gameView.grid.getChildren().remove(gemCell);
        gameView.grid.add(new Cell(gemCell.getColumn(), gemCell.getRow()), gemCell.getColumn(), gemCell.getRow());
//        gameController.playGemCollectSound();
        System.out.println("collected");
    gameView.gemlist.remove(gemCell);
    }

    private void interactWithBusStop(busStop stop) {
        System.out.println("Interacted with Bus Stop at: " + stop.getX() + ", " + stop.getY());
    }

    private void finishGame() {
//        gameFinished = true;
        Label levelCompleteLabel = new Label("Level Complete");
        levelCompleteLabel.setStyle("-fx-font-size: 24px;");
        StackPane root = (StackPane) gameView.grid.getScene().getRoot();
        root.getChildren().add(levelCompleteLabel);
        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(event -> ((Stage) gameView.grid.getScene().getWindow()).close());
        pause.play();
    }

    private void hailTaxi() {
        System.out.println(taximan.hailed);
        if (taximan.hailed) {
            taximan.hailed = !taximan.hailed;
        } else {
            // Hail taxi, limit Co2 not be over 100%
            double currentCo2 = sceneController.getCo2Gauge();
            double potentialCo2 = currentCo2 + 3.0;
            if (potentialCo2 > 100.0) {
                gameFailedCall();
            } else {
                sceneController.increaseCo2GaugeUpdate(10.0); // Safely increase CO2
            }
            taximan.hailed = true;
        }
    }
    private void togglePlayerMovement() {
        if (onBus) {
            int[] playerLocation = {playerUno.getCoordX(), playerUno.getCoordY()};
            boolean atBusStop = busStopCoordinates.stream()
                    .anyMatch(location -> location[0] == playerLocation[0] && location[1] == playerLocation[1]);
            System.out.println(atBusStop);
            if (atBusStop) {
                playerMovementEnabled = true;
                onBus = false;
                System.out.println("You got off the bus.");
            } else {
                System.out.println("You can only get off the bus at a bus stop.");
            }
        } else if (playerUno.getCell() instanceof busStop) {
            playerMovementEnabled = !playerMovementEnabled;
            if (!playerMovementEnabled) {
                playerUno.playerVisual.setVisible(false);
                System.out.println("Waiting for bus");
            } else {
                playerUno.playerVisual.setVisible(true);
                System.out.println("Impatient");
            } }
    }

    private void moveTaxi(Taxi bus, int newX, int newY) {
        // Move the bus to the new position (newX, newY) on the grid

        gameView.grid.moveCell(bus, newX, newY);

        bus.setX(newX);
        bus.setY(newY);
        //grid.add(cell,cell.getColumn(),cell.getRow());

    }

    public void moveTaxiTowardsPlayer(Taxi bus) {

        if (bus.getX()==playerUno.getCoordX()&&bus.getY()==playerUno.getCoordY()&&taximan.arrived&&!inTaxi){
            inTaxi = true;

        }
//        System.out.println(distanceIfMoveX+"   "+distanceIfMoveY);
        else {
            if ((bus.getX() < playerUno.getCoordX() || bus.getX() > playerUno.getCoordX()) && bus.flagMove == 0) {
//            System.out.println("----------- moving x ---------");
                // Move horizontally towards the bus stop, if not blocked
                int newX = bus.getX() + (bus.getX() < playerUno.getCoordX() ? 1 : -1);
                if (canMoveBusTo(newX, bus.getY())) {
                    moveTaxi(bus, newX, bus.getY());
                } else if (canMoveBusTo(bus.getX(), bus.getY() + (bus.getY() < playerUno.getCoordY() ? 1 : -1))) {
                    // Move vertically as a fallback
                    moveTaxi(bus, bus.getX(), bus.getY() + (bus.getY() < playerUno.getCoordY() ? 1 : -1));
                }
            } else if (bus.getY() < playerUno.getCoordY() || bus.getY() > playerUno.getCoordY()) {
//            System.out.println("----------- moving y ---------");
                // Move vertically towards the bus stop, if not blocked
                int newY = bus.getY() + (bus.getY() < playerUno.getCoordY() ? 1 : -1);
                if (canMoveBusTo(bus.getX(), newY)) {

                    moveTaxi(bus, bus.getX(), newY);
                } else if (canMoveBusTo(bus.getX() + 1, bus.getY())) {
                    // Move horizontally as a fallbackf
                    if (bus.flagMove == 0) {
                        bus.flagMove = 1;
                    }
                    moveTaxi(bus, bus.getX() + +1, bus.getY());
                }
            }
            //arriving at stop logic
            else if (bus.getX() == playerUno.getCoordX() && bus.getY() == playerUno.getCoordY()) {
                System.out.println("----------- Taxi arrived ---------");
                bus.arrived = true;

                if (bus.hailed && playerUno.getCoordX() == bus.getX() && playerUno.getCoordY() == bus.getY()) {
                    System.out.println("----------- You just got in the taxia---------");
                    inTaxi = true;
                    bus.timeSpeed = playerUno.speedTime;

                } else if (inTaxi) {

                    System.out.println("----------- Press E to get off  ---------");
                    inTaxi = true;

                }
            } else if (bus.getY() == playerUno.getCoordY()) {
                bus.flagMove = 0;
            }

        }}

    // Methods to Save and Load Game


    public void setFlagLoadGame(Boolean flagLoadGameNew) {
        flagLoadGame = flagLoadGameNew;
    }

    /**
     * This method is used to save the current state of the game.
     * It creates a new SaveGame object with the current positions of the player, the bus and more,
     * as well as the current gem count. This object is then serialized and written to a file named "gameSave.ser".
     * If the game state is saved successfully, a message is printed to the console.
     * If an IOException occurs during this process, the stack trace is printed and a failure message is displayed.
     */
    private void saveGameState() {
        // Create a new SaveGame object with the current game state
        SaveGame saveGame = new SaveGame(
                playerUno.getCoordX(),
                playerUno.getCoordY(),
                busman.getX(),
                busman.getY(),
                gameView.getGemCount(),
                gameView.getStageClearFlags()
        );

        // Try to write the SaveGame object to a file
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("gameSave.ser"))) {
            oos.writeObject(saveGame);
            System.out.println("Game saved successfully.");
        } catch (IOException e) {
            // Print the stack trace and a failure message if an IOException occurs
            e.printStackTrace();
            System.out.println("Failed to save game.");
        }
    }

    /**
     * This method is used to load the saved state of the game.
     * It reads a serialized SaveGame object from a file named "gameSave.ser".
     * The player's position, the bus's position, the gem locations, and the gem counter are restored from the SaveGame object.
     * If the game state is loaded successfully, a message is printed to the console.
     * If an IOException or ClassNotFoundException occurs during this process, the stack trace is printed and a failure message is displayed.
     */
    private void loadGameState() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("gameSave.ser"))) {
            // Read the SaveGame object from the file
            SaveGame saveGame = (SaveGame) ois.readObject();

            // Restore the player's position
            playerUno.setCellByCoords(gameView.grid, saveGame.getPlayerX(), saveGame.getPlayerY());

            // Restore the bus's position
            busman.setX(saveGame.getBusX());
            busman.setY(saveGame.getBusY());

            // Restore the gem locations
            recreateGems(saveGame.getGemCounter());

            // Restore the gem counter
            gameView.setGemCoount(saveGame.getGemCounter());

            // Restore the stage clear flags
            gameView.setStageClearFlags(saveGame.getStageClearFlags());

            System.out.println("Game loaded successfully.");
        } catch (IOException | ClassNotFoundException e) {
            // Print the stack trace and a failure message if an exception occurs
            e.printStackTrace();
            System.out.println("Failed to load game.");
        }
    }

    /**
     * This method is used to recreate the gems in the game.
     * It first removes all the existing gems from the game grid.
     * Then, it generates new gems at random locations.
     * The number of new gems is determined by subtracting the number of collected gems (gemCounts) from the initial number of gems.
     *
     * @param gemCounts The number of gems that have been collected by the player.
     */
    private void recreateGems(int gemCounts) {
        // Clear current gems from the game grid
        gameView.grid.getChildren().removeIf(cell -> cell instanceof Gem);

        // Generate new random gems at different locations
        // The number of new gems is based on the initial number of gems minus the number of collected gems
        generateGems(gameView.grid, numberOfInitialGems - gemCounts);
    }

    private void checkAndIncreaseStamina() {
        if (isGameStarted && playerUno.getStamina() < 100) {
            if (playerUno.getCoordX() == lastX && playerUno.getCoordY() == lastY) {
                stationaryTime += 1;
                if (stationaryTime >= 1) {
                    playerUno.increaseStamina();  // 스태미나 회복
                    gameView.updateStamina(playerUno.getStamina());
                    stationaryTime = 0;
                }
            } else {
                stationaryTime = 0;
                lastX = playerUno.getCoordX();
                lastY = playerUno.getCoordY();
            }
        }
    }

    private void gameFailedCall() {
        sceneController.setCo2Gauge(100.0); // Set CO2 to maximum if overflown
        sceneController.missionFail();  // Call mission fail function
    }
}