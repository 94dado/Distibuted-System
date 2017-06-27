public class Player {
    private static String dot = ";";
    private String name;
    private String address;
    private int port;

    public Player(String name, String address, int port) {
        this.name = name;
        this.address = address;
        this.port = port;
    }

    public Player(String sendableString){
        String[] values = sendableString.split(dot);
        name = values[0];
        address = values[1];
        port = Integer.parseInt(values[2]);
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

    public String toSendableString(){
        return name + dot + address + dot + port;
    }

    public int getPort() {
        return port;
    }
}
