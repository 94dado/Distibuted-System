import com.google.gson.Gson;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("")
public class ServerThread {
    private Gson gson;

    public ServerThread(){
        gson = new Gson();
    }

    //only to check if the server is online
    @GET
    @Produces("text/plain")
    public String prova(){
        return "ok";
    }

    //return the list of all started matches
    @Path("getMatches")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMatchList(){
        return Response.status(200).entity(gson.toJson(GameServerManager.getInstance().getMatches())).build();
    }

    //add a new match in the list of all matches
    @Path("addMatch")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addNewMatch (@DefaultValue("{}") @FormParam(value = "match") String jsonMatch){
        Match newMatch = gson.fromJson(jsonMatch,Match.class);
        return Response.status(200).entity(gson.toJson(GameServerManager.getInstance().addMatch(newMatch))).build();
    }

    //add a player to a existing match
    @Path("addPlayerToMatch")
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addPlayerToExistingMatch(
            @DefaultValue("{}") @FormParam(value = "player") String playerJson,
            @DefaultValue("{}") @FormParam(value = "match") String matchName){
        Player pl = gson.fromJson(playerJson,Player.class);
        return Response.status(200).entity(gson.toJson(GameServerManager.getInstance().addPlayerToMatch(pl,matchName))).build();
    }

    //get detail of a match
    @Path("getMatchDetail")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDetailOfMatch(@QueryParam("name") String matchName){
        return Response.status(200).entity(gson.toJson(GameServerManager.getInstance().getMatchDetail(matchName))).build();
    }

    //remove player from a match
    @Path("removePlayerFromMatch/{match}/{player}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response removePlayerFromMatch(
            @DefaultValue("") @PathParam("player") String playerName,
            @DefaultValue("") @PathParam("match") String matchName){
        return Response.status(200).entity(gson.toJson(GameServerManager.getInstance().removePlayerFromMatch(playerName,matchName))).build();
    }

    //remove a match
    @Path("removeMatch/{match}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeMatch(@DefaultValue("") @PathParam("match") String matchName){
        return Response.status(200).entity(gson.toJson(GameServerManager.getInstance().removeMatch(matchName))).build();
    }
}
