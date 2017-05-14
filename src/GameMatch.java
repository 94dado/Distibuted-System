

public class GameMatch {
    private int dimension;
    private PlayerCoordinate position;

    public GameMatch(int dimension) {
        this.dimension = dimension;
        int x = (int) (Math.random() * (dimension - 1));
        int y = (int) (Math.random() * (dimension - 1));
        position = new PlayerCoordinate(x,y);
    }

    public int getDimension() {
        return dimension;
    }

    public PlayerCoordinate getPosition() {
        return position;
    }

    public GridColor getColor(){
        boolean left_x = position.getX() < dimension/2;
        boolean up_y = position.getY() < dimension/2;
        GridColor value;
        if (left_x){
            if(up_y) value = GridColor.GREEN;
            else value = GridColor.BLUE;
        }else{
            if(up_y) value = GridColor.RED;
            else value = GridColor.YELLOW;
        }
        return value;
    }
}
