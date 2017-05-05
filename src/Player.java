public class Player {
    private String name;
    private String address;
    private String port;

    public Player(String name, String address, String port) {
        this.name = name;
        this.address = address;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
