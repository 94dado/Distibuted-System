import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;


public class HTTPRequestCreator {
    public static final String POST = "POST";
    public static final String GET = "GET";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final String CONTENT_TYPE = "Content-Type";

    private HttpURLConnection connection;

    HTTPRequestCreator(String method, String content_type, String url_string) throws Exception{
        URL url = new URL(url_string);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty(CONTENT_TYPE,content_type);
    }

    public void putParams(Map<String,String> params) throws Exception{
        String params_string = getQueryFromMap(params);
        connection.setDoOutput(true);
        DataOutputStream stream = new DataOutputStream(connection.getOutputStream());
        stream.writeBytes(params_string);
        stream.flush();
        stream.close();
    }

    public String getAnswer() throws Exception{
        connection.connect();
        int responseCode = connection.getResponseCode();
        if(responseCode == 200){
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder builder = new StringBuilder();
            while((line = reader.readLine())!=null){
                builder.append(line);
            }
            reader.close();
            return builder.toString();
        }
        return null;
    }

    private static String getQueryFromMap(Map <String,String> params){
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for ( Map.Entry <String,String> entry: params.entrySet())
        {
            if (first)
                first = false;
            else
                result.append("&");
            //provo a fare la decodifica dei valori e generare la stringa dei parametri da mandare
            try{
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }catch(Exception e)
            {
                return "";
            }
        }

        return result.toString();
    }
}
