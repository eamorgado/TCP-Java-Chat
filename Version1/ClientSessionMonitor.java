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
    Selector selector;
    //User monitoring
    Set<String> usernames;
    ArrayList<ClientIndividualSession> active_users;

    //Lobby monitoring. Each lobby will have an array of connected users
    Map<String,ArrayList<ClientIndividualSession>> lobbies;

    //Constructor
    ClientSessionMonitor(Selector selector){
        this.selector = selector;
        this.usernames = new HashSet<>();
        this.active_users = new ArrayList<>();
        this.lobbies = new HashMap<>();
    }

    Selector getSelect(){return this.selector;}
    Set<String> getUsernames(){return this.usernames;}
    ArrayList<ClientIndividualSession> getActiveUsers(){return this.active_users;}
    Map<String,ArrayList<ClientIndividualSession>> getLobbies(){return this.lobbies;}

    void addUser(ClientIndividualSession user){
        this.active_users.add(user);
    }

    SelectionKey findUserKey(String username){
        for(ClientIndividualSession user : this.active_users)
            if(user.getUsername().equals(username))
                return user.getKey();
        return null;
    }

    void sendMessageUser(String message, SelectionKey key, ByteBuffer buffer){
        byte[] to_buffer;
        buffer.clear(); //position = 0 limit capacity
        buffer.rewind(); //position = 0

        if((int)message.charAt(message.length()-1) != 10) message += "\n";
        to_buffer = message.getBytes();
        buffer.put(to_buffer);
        buffer.flip();
        SocketChannel channel = (SocketChannel)key.channel();
        try{
            channel.write(buffer);
        }catch(IOException e){
            System.out.println("ERROR=>sendMessageUser: trying to send message to user but failed");
            e.printStackTrace();
        }
    }
    void sendMessageToAll(String message,ClientIndividualSession user,ByteBuffer buffer){
        ArrayList<ClientIndividualSession> users = lobbies.get(user.getLobby());
        for(ClientIndividualSession u : users)
            this.sendMessageUser(message, u.getKey(), buffer);
    }

    public boolean isUsernameInUse(String username){
        return this.usernames.contains(username);
    }
    
    public boolean doesLobbyExist(String lobby){
        return this.lobbies.containsKey(lobby);
    }
}