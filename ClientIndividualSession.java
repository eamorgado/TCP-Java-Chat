import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/*------------------------------------------------------------------------------
ClientIndividualSession:
    For our server to support multiple users we need to save each individual info
        for every active/connected user. Every user has a specific SocketChannel
        and a SelectionKey being those the connection fields
    
    A user connected to the server has a specific and unique username/nick as
        such we must also save its nick for every active user.
    
    A user connected to the server can be in a lobby or not, as such we must 
        save its connection state as well as the lobby it is connected to if 
        it is in a lobby state
------------------------------------------------------------------------------*/
class ClientIndividualSession{
    //Connection Fields
    SocketChannel channel; //save user channel
    SelectionKey key; //save user key

    //User Lobby and info fields
    String username; //saves user nick
    String lobby; //saves lobby where user is connected
    String connnection_state;

    //Constructor--default, no lobby/info fields given
    ClientIndividualSession(SocketChannel channel, SelectionKey key){
        this.channel = channel;
        this.key = key;
        //Set connection status
        this.connnection_state = "INIT";
    }

    //Getters-------------------------------------------------------------------
    SocketChannel getChannel(){return this.channel;}
    SelectionKey getKey(){return this.key;}

    String getUsername(){return this.username;}
    String getLobby(){return this.lobby;}
    String getConState(){return this.connnection_state;}

    //Setters-------------------------------------------------------------------
    void setUsername(String username){this.username = username;}
    void setConState(String state){this.connnection_state = state;}
    void setLobby(String lobby){this.lobby = lobby;}
    //diconnect user
    void userDisconnect(){
        try {
            if(this.key != null) key.cancel();
            if(this.channel != null) return;
            this.channel.close();
        } catch (Throwable t) {
            System.out.println("ERROR=>ClientIndividualSession: error disconnecting user "+t.toString());
        }
    }

}