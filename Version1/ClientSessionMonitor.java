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

    public boolean isUsernameInUse(String username){return this.usernames.contains(username);}
    public boolean doesLobbyExist(String lobby){return this.lobbies.containsKey(lobby);}
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

    String commandExecutionCode(String cmd, SelectionKey key,String lobby){
        /**
         * Analyses command and returns messages
         */
        cmd = cmd.toLowerCase();
        //retrieve client
        ClientIndividualSession user = (ClientIndividualSession)key.attachment();
        String code = "";
        switch(cmd){
            case "nick":  
                if(!this.isUsernameInUse(user.getUsername()))
                    code = "nick-ok";
                else 
                    code = "nick-error";
            break;
            case "join":
                if(user.getUsername() != null){
                    if(this.doesLobbyExist(lobby))
                        code = ((!user.getConState().equals("INSIDE"))? "join-ok" : "join-inside-ok");
                    else
                        code = (!(user.getConState().equals("INSIDE"))? "join-newlobby-ok" : "join-newlobby-inside-ok");
                }
                else code = "cmd-error";
            break;
            case "priv": 
                code = ((user.getUsername() != null)? "priv-ok" : "cmd-error");
            break;
            case "leave":  
                code = ((user.getConState().equals("INSIDE"))? "leave-lobby" : "out-lobby");
            break;
            case "bye": 
                code = ((!user.getConState().equals("INSIDE"))? "bye-ok" : "bye-lobby-ok");
            break;
            default: code = "cmd-error";
        }
        return code;
    }

    void cmdNick(ClientIndividualSession user,ByteBuffer buffer,String username){
        this.sendMessageUser("OK", user.getKey(), buffer);
        if(user.getUsername() != null){
            String old = user.getUsername();
            this.usernames.remove(user.getUsername());
            for(ClientIndividualSession u : this.active_users)
                if(u.getUsername().equals(user.getUsername()))
                    u.setUsername(username);
            if(!user.getConState().equals("OUTSIDE"))
                for(ClientIndividualSession u : this.lobbies.get(user.getLobby()))
                    if(u.getUsername().equals(user.getUsername()))
                        u.setUsername(username);
            this.sendMessageUser("NEWNICK "+old+" "+username, user.getKey(), buffer);
        }else{
            user.setUsername(username);
            this.usernames.add(username);
            this.active_users.add(user);
        }
    }
    void cmdPriv(ClientIndividualSession user,ByteBuffer buffer,String username, String message){
        SelectionKey dest = this.findUserKey(username);
        if(dest != null){
            this.sendMessageUser("OK", user.getKey(), buffer);
            this.sendMessageUser("PRIVATE "+user.getUsername()+" "+message, dest, buffer);
        }
        else
            this.sendMessageUser("ERROR", user.getKey(), buffer);
    }
    void cmdJoin(ClientIndividualSession user,ByteBuffer buffer,String lobby){
        ArrayList<ClientIndividualSession> subscribers = this.lobbies.get(lobby);
        user.setConState("INSIDE");
        user.setLobby(lobby);
        subscribers.add(user);
        this.lobbies.put(lobby,subscribers);
        this.sendMessageToAll("JOINED "+user.getUsername(), user, buffer);
        this.sendMessageUser("OK", user.getKey(), buffer);
    }
    void cmdCreateLobby(ClientIndividualSession user,ByteBuffer buffer,String lobby){
        ArrayList<ClientIndividualSession> subscribers = new ArrayList<>();
        user.setConState("INSIDE");
        user.setLobby(lobby);
        subscribers.add(user);
        this.lobbies.put(lobby,subscribers);
        this.sendMessageUser("OK", user.getKey(), buffer);
    }
    void cmdLeave(ClientIndividualSession user,ByteBuffer buffer){
        ArrayList<ClientIndividualSession> subscribers = this.lobbies.get(user.getLobby());
        subscribers.remove(user);
        if(subscribers.size() != 0) 
            this.lobbies.put(user.getLobby(),subscribers);
        else 
            this.lobbies.remove(user.getLobby());
        user.setConState("OUTSIDE");
        this.sendMessageToAll("LEFT "+user.getUsername(), user, buffer);
        this.sendMessageUser("OK", user.getKey(), buffer);
    }
    void cmdBye(ClientIndividualSession user,ByteBuffer buffer){
        if(user.getUsername() != null){
            this.usernames.remove(user.getUsername());
            this.active_users.remove(user);
        }
        this.sendMessageUser("BYE", user.getKey(), buffer);
        user.userDisconnect();
    }
}