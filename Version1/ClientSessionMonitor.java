import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/*------------------------------------------------------------------------------
ClientSessionMonitor:
    This class is responsible to "monitor" all active users and implementing 
        lobbies, as such, it will need to store the list of ClientIndividualSession
        for all users
    It will save users usernames' and make sure they are unique 
------------------------------------------------------------------------------*/
class ClientSessionMonitor{
    //User monitoring
    Set<String> usernames;
    ArrayList<ClientIndividualSession> active_users;

    //Lobby monitoring. Each lobby will have an array of connected users
    Map<String,ArrayList<ClientIndividualSession>> lobbies;

    //Constructor
    ClientSessionMonitor(){
        this.usernames = new HashSet<>();
        this.active_users = new ArrayList<>();
        this.lobbies = new HashMap<>();
    }

    Set<String> getUsernames(){return this.usernames;}
    ArrayList<ClientIndividualSession> getActiveUsers(){return this.active_users;}
    Map<String,ArrayList<ClientIndividualSession>> getLobbies(){return this.lobbies;}
}