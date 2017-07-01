

public class PlayerCoordinate{
    private int x;
    private int y;

    public PlayerCoordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean equals(PlayerCoordinate obj) {
        return x == obj.getX() && y == obj.getY();
    }

    public boolean isValidCoordinate(int sizeLimit){
        return x >= 0 && y >= 0 && x < sizeLimit && y < sizeLimit;
    }

    @Override
    public String toString() {
        return "("+x+","+y+")";
    }
}
