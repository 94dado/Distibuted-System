public enum GridColors {
    GREEN, RED, BLUE, YELLOW;


    @Override
    public String toString() {
        switch (this){
            case RED:
                return "rosso";
            case BLUE:
                return "blu";
            case GREEN:
                return "verde";
            case YELLOW:
                return "giallo";
            default:
                return "";
        }
    }
}
