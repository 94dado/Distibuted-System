/**
 * Created by dado_ on 28/06/2017.
 */
public class Message {
    private MessageType type;
    private String jsonMessage;

    public Message(MessageType type, String jsonMessage) {
        this.type = type;
        this.jsonMessage = jsonMessage;
    }

    public MessageType getType() {
        return type;
    }

    public String getJsonMessage() {
        return jsonMessage;
    }
}
