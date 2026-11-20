import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class GameEngine extends Application {
    private final Pane gameRoot = new Pane();
    private final StackPane mainRoot = new StackPane();
    private static final int GAME_WIDTH = 960, GAME_HEIGHT = 480;
    private static final int VIEWPORT_WIDTH = 480, VIEWPORT_HEIGHT = 480;
    private Label scoreLabel;
    private Label spikesLabel;
    private Label applesLabel;

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean upPressed;
    private boolean spacePressed;

    private int scoreCount;
    private int spikeCount;
    private int appleCount;
    private double velocityY;
    private boolean isOnGround;
    private int collectedApple;
    private long lastSpawnedTime;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws FileNotFoundException {
        // Uses a StackPane to overlay the UI components atop the game world.
        mainRoot.getChildren().add(gameRoot);
        reset();

        Scene scene = new Scene(mainRoot, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        scene.setOnKeyPressed(e -> {
            if(e.getCode()==KeyCode.A || e.getCode()==KeyCode.LEFT){
                leftPressed=true;
            }
            if(e.getCode()==KeyCode.D || e.getCode()==KeyCode.RIGHT){
                rightPressed=true;
            }
            if(e.getCode()==KeyCode.W || e.getCode()==KeyCode.UP){
                upPressed=true;
            }
            if(e.getCode()==KeyCode.SPACE){
                spacePressed=true;
            }
        });

        scene.setOnKeyReleased(e -> {
            if(e.getCode()==KeyCode.A || e.getCode()==KeyCode.LEFT){
                leftPressed=false;
            }
            if(e.getCode()==KeyCode.D || e.getCode()==KeyCode.RIGHT){
                rightPressed=false;
            }
            if(e.getCode()==KeyCode.W || e.getCode()==KeyCode.UP){
                upPressed=false;
            }
            if(e.getCode()==KeyCode.SPACE){
                spacePressed=false;
            }
        });

        // Configures stage properties and displays the primary scene.
        stage.setTitle("Platformer");
        stage.setScene(scene);
        stage.show();
        gameRoot.requestFocus();

        // Initializes and starts the primary game loop.
        new GameLoop().start();
    }
    private List<Terrain> terrList=new ArrayList<>();
    private List<Block> blockList=new ArrayList<>();
    private List<Spike> spikeList=new ArrayList<>();
    private List<Apple> appleList=new ArrayList<>();
    private Player player;

    public void setObject(GameObject obj,double x,double y){
        obj.setLayoutX(x);
        obj.setLayoutY(y);
        gameRoot.getChildren().add(obj);
    }

    private void reset() throws FileNotFoundException {
        // Resets the game world state and reinitializes UI labels.
        gameRoot.getChildren().clear();
        ImageView bg = new ImageView(new Image(new File("assets/Background(960x480x1).png").toURI().toString()));
        bg.setFitWidth(GAME_WIDTH);
        bg.setFitHeight(GAME_HEIGHT);
        gameRoot.getChildren().add(0, bg);  // send to back
        gameRoot.setTranslateX(0);
        gameRoot.setTranslateY(0);

        if (scoreLabel == null) {
            scoreLabel = new Label("Score: 0");
            scoreLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");
            StackPane.setAlignment(scoreLabel, Pos.BOTTOM_LEFT);
            StackPane.setMargin(scoreLabel, new Insets(0, 0, 5, 10));
            mainRoot.getChildren().add(scoreLabel);

            spikesLabel = new Label("Spikes: 0");
            spikesLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");
            StackPane.setAlignment(spikesLabel, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(spikesLabel, new Insets(0, 10, 5, 0));
            mainRoot.getChildren().add(spikesLabel);

            applesLabel = new Label("Apples: 0");
            applesLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");
            StackPane.setAlignment(applesLabel, Pos.BOTTOM_CENTER);
            StackPane.setMargin(applesLabel, new Insets(0, 0, 5, 0));
            mainRoot.getChildren().add(applesLabel);
        } else {
            scoreLabel.setText("Score: 0");
            spikesLabel.setText("Spikes: 0");
            applesLabel.setText("Apples: 0");
        }

        scoreLabel.toFront();
        spikesLabel.toFront();
        applesLabel.toFront();

        terrList.clear();
        blockList.clear();
        spikeList.clear();
        appleList.clear();
        scoreCount = 0;
        spikeCount = 0;
        appleCount = 0;
        collectedApple = 0;
        velocityY = 0;
        isOnGround = false;
        lastSpawnedTime = 0;

        File file=new File("level.txt");
        Scanner input=new Scanner(file);

        while(input.hasNextLine()){
            String line=input.nextLine();
            String[] inputList=line.split(",");

            if(inputList.length!=3){
                break;
            }
            String objectName=inputList[0];
            double xCoord=Double.parseDouble(inputList[1]);
            double yCoord=Double.parseDouble(inputList[2]);

            if(objectName.equals("Terrain")){
                Terrain tObj=new Terrain();
                setObject(tObj,xCoord,yCoord);
                terrList.add(tObj);
            }
            else if(objectName.equals("Block")){
                Block blObj=new Block();
                setObject(blObj,xCoord,yCoord);
                blockList.add(blObj);
            }
            else if (objectName.equals("Spike")){
                Spike sObj=new Spike();
                setObject(sObj,xCoord,yCoord);
                spikeList.add(sObj);
            }
            else if(objectName.equals("Apple")){
                Apple aObj=new Apple();
                setObject(aObj,xCoord,yCoord);
                appleList.add(aObj);
            }
            else if (objectName.equals("Player")){
                player =new Player();
                setObject(player,xCoord,yCoord);
            }
        }
    }

    // Coordinate system: x grows to the right and y grows downward.
    // (0,0) is the top-left corner of the screen, and each object's position
    // is its own top-left corner.

    private boolean isInvalid(GameObject obj) {
        if((obj.getLayoutX()<0 || obj.getLayoutX()+obj.getObjectWidth()>GAME_WIDTH) ||
                (obj.getLayoutY()<0 || obj.getLayoutY()+obj.getObjectHeight()>GAME_HEIGHT)){
            return true;
        }
        // shrink each edge by 0.1 so objects that only touch do not count as overlapping
        double objLeft = obj.getLayoutX() + 0.1;
        double objRight = obj.getLayoutX() + obj.getObjectWidth() - 0.1;
        double objTop = obj.getLayoutY() + 0.1;
        double objBottom = obj.getLayoutY() + obj.getObjectHeight() - 0.1;

        for(Terrain terr:terrList){
            double terrLeft = terr.getLayoutX();
            double terrRight = terr.getLayoutX() + terr.getObjectWidth();
            double terrTop = terr.getLayoutY();
            double terrBottom = terr.getLayoutY() + terr.getObjectHeight();
            if(obj!=terr && objLeft < terrRight && objRight > terrLeft && objTop < terrBottom && objBottom > terrTop){
                return true;
            }
        }
        for(Block blc:blockList){
            double blcLeft = blc.getLayoutX();
            double blcRight = blc.getLayoutX() + blc.getObjectWidth();
            double blcTop = blc.getLayoutY();
            double blcBottom = blc.getLayoutY() + blc.getObjectHeight();

            if (obj!=blc && objLeft < blcRight && objRight > blcLeft && objTop < blcBottom && objBottom > blcTop) {
                return true;
            }
        }
        return false;
    }
    private Random random = new Random();
    private void spawn() {
        // ---- spawn a spike on top of a random platform ----
        List<GameObject> mergeTerrBlock = new ArrayList<>();  // all platforms (terrain + blocks)
        mergeTerrBlock.addAll(terrList);
        mergeTerrBlock.addAll(blockList);

        for(int i=0;i<mergeTerrBlock.size();i++){
            int index = random.nextInt(mergeTerrBlock.size());
            GameObject platform = mergeTerrBlock.get(index);  // pick a random platform

            Spike newSpike = new Spike();

            double upPlatformX = platform.getLayoutX();
            double upPlatformY = platform.getLayoutY();

            double spikeXmin = upPlatformX;
            double spikeXmax = upPlatformX + platform.getObjectWidth() - newSpike.getObjectWidth();
            double spikeX = random.nextDouble() * (spikeXmax - spikeXmin) + spikeXmin;
            double spikeY = upPlatformY - newSpike.getObjectHeight();

            newSpike.setLayoutX(spikeX);
            newSpike.setLayoutY(spikeY);

            if (!isInvalid(newSpike)) {  // valid position: add the spike
                spikeList.add(newSpike);
                gameRoot.getChildren().add(newSpike);
                spikeCount++;
                spikesLabel.setText("Spikes: "+spikeCount);
                break;
            }
            // otherwise try another platform
        }

        // ---- spawn 2 apples at random empty positions (they can float) ----
        for(int i=0;i<2;i++){  // 2 apples
            for(int j=0;j<500;j++){  // up to 500 attempts per apple
                double appleX=random.nextDouble()*GAME_WIDTH;
                double appleY=random.nextDouble()*GAME_HEIGHT;
                Apple newApple=new Apple();
                newApple.setLayoutX(appleX);
                newApple.setLayoutY(appleY);
                if(!isInvalid(newApple)){
                    appleList.add(newApple);
                    gameRoot.getChildren().add(newApple);
                    appleCount++;
                    applesLabel.setText("Apples: "+appleCount);
                    break;
                }
            }
        }
    }

    // Called automatically in every animation frame.
    private void update() throws FileNotFoundException {
        // Updates the state of all active game entities (animations).
        for (Node node : gameRoot.getChildren()) {
            if (node instanceof GameObject) {
                ((GameObject) node).update();
            }
        }

        // ---- horizontal movement ----
        double currX=player.getLayoutX();
        if(leftPressed){
            player.setLayoutX(currX-1);  // move 1 px left
            player.setScaleX(-1);
            if(isInvalid(player)){
                player.setLayoutX(currX);  // blocked: undo the move
            }
        }
        if(rightPressed){
            player.setLayoutX(currX+1);  // move 1 px right
            player.setScaleX(1);
            if(isInvalid(player)){
                player.setLayoutX(currX);
            }
        }

        // ---- jumping and gravity ----
        if(upPressed && isOnGround){  // jump only when standing on the ground
            velocityY=-3.8;  // initial upward velocity
            isOnGround=false;  // now in the air
        }
        velocityY += 0.1;  // gravity, applied every frame
        double currY=player.getLayoutY();
        player.setLayoutY(currY+velocityY);  // new vertical position

        if(isInvalid(player)){
            player.setLayoutY(currY);
            if(velocityY>0){  // was falling: landed on something
                isOnGround=true;
                velocityY=0;
            }
            if(velocityY<0){  // was rising: hit a ceiling, still in the air
                isOnGround=false;
                velocityY=0;
            }
        }

        // ---- touching a spike restarts the level ----
        for(Spike spk:spikeList){
            if(player.getBoundsInParent().intersects(spk.getBoundsInParent())){
                reset();
                break;
            }
        }

        // ---- apple collection: every 2 apples remove a random spike and add 1 point ----
        int sizeOfAppleList=appleList.size();
        for(int i=0;i<sizeOfAppleList;i++){
            Apple apple=appleList.get(i);
            if(player.getBoundsInParent().intersects(apple.getBoundsInParent())){
                gameRoot.getChildren().remove(apple);
                appleList.remove(apple);
                sizeOfAppleList=appleList.size();
                i--;
                appleCount--;
                applesLabel.setText("Apples: "+appleCount);
                collectedApple++;
                if(collectedApple % 2 ==0 && !spikeList.isEmpty()){
                    int indx= random.nextInt(spikeList.size());
                    Spike randomSpike=spikeList.get(indx);
                    gameRoot.getChildren().remove(randomSpike);
                    spikeList.remove(randomSpike);
                    spikeCount--;
                    scoreCount++;
                    spikesLabel.setText("Spikes: "+spikeCount);
                    scoreLabel.setText("Score: "+scoreCount);
                }
            }
        }
        // ---- spawn new items every 3 seconds ----
        long currTime=System.currentTimeMillis();
        if((currTime-lastSpawnedTime)>=3000){
            spawn();
            lastSpawnedTime=currTime;
        }

        // ---- camera follows the player, clamped to the world edges ----
        double centerScreenX=VIEWPORT_WIDTH/2;
        double scrolledAmount=centerScreenX-player.getLayoutX();
        double leftBoundary=VIEWPORT_WIDTH/2;
        double rightBoundary=GAME_WIDTH-VIEWPORT_WIDTH/2;

        if(player.getLayoutX()<leftBoundary){  // near the left edge: camera stays fixed
            gameRoot.setTranslateX(0);
        }
        if (player.getLayoutX()>leftBoundary && player.getLayoutX()<rightBoundary){
            gameRoot.setTranslateX(scrolledAmount);  // in between: keep the player centered
        }
        if(player.getLayoutX()>rightBoundary){
            gameRoot.setTranslateX(VIEWPORT_WIDTH-GAME_WIDTH);
        }

        // ---- spike destroyer: with score >= 5, SPACE removes the nearest spike for 5 points ----

        double minDistance=Double.MAX_VALUE;
        Spike nearestSpike = null;
        for(Spike spike:spikeList){
            double distanceX=spike.getLayoutX()-player.getLayoutX();
            double distanceY=spike.getLayoutY()-player.getLayoutY();
            double distance=Math.sqrt(distanceY*distanceY + distanceX*distanceX);  // Euclidean distance
            if(distance<minDistance){
                minDistance=distance;
                nearestSpike=spike;
            }
        }
        if(scoreCount>=5 && spacePressed && nearestSpike!=null){
            gameRoot.getChildren().remove(nearestSpike);
            spikeList.remove(nearestSpike);
            spikeCount--;
            scoreCount -=5;
            spikesLabel.setText("Spikes: "+spikeCount);
            scoreLabel.setText("Score: "+scoreCount);
            spacePressed=false;
        }
    }

    private class GameLoop extends AnimationTimer {
        @Override
        public void handle(long now) {
            try {
                update();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
