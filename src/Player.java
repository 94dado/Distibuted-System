public class Player {
    private String name;
    private String address;
    private int port;

    public Player(String name, String address, int port) {
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

    public boolean equals(Player other){
        return other != null && name.equals(other.name);
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
