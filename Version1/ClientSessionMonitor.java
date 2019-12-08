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
        if(this.lobbies.containsKey(user.getLobby())){
            System.out.println("Sending messages to all users");
            ArrayList<ClientIndividualSession> users = this.lobbies.get(user.getLobby());
            for(ClientIndividualSession u : users)
                this.sendMessageUser(message, u.getKey(), buffer);
        }
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
                //System.out.println("Lob["+lobby+"]");
                //System.out.println("Usernames=["+this.usernames+"]");
                if(!this.usernames.contains(lobby) && !lobby.equals(""))
                    code = "nick-ok";
                else 
                    code = "nick-error";
            break;
            case "join":
                if(user.getUsername() != null && !lobby.equals(""))
                    code = ((!user.getConState().equals("INSIDE"))? "join-ok" : "join-inside-ok");
                else code = "cmd-error";
            break;
            case "priv":
                if(user.getUsername() != null && !lobby.equals("")){
                    if(this.usernames.contains(lobby))
                        code = "priv-ok";
                    else code = "cmd-error";
                }
                else code = "cmd-error";
            break;
            case "leave":  
                if(user.getUsername() != null)
                    code = ((user.getConState().equals("INSIDE"))? "leave-lobby" : "out-lobby");
                else code = "cmd-error";
            break;
            case "bye": 
                code = ((!user.getConState().equals("INSIDE"))? "bye-ok" : "bye-lobby-ok");
            break;
            default: code = "cmd-error";
        }
        return code;
    }

    void cmdNick(ClientIndividualSession user,ByteBuffer buffer,String username){
        if(user.getUsername() != null){
            String old = user.getUsername();
            this.usernames.remove(user.getUsername());
            for(ClientIndividualSession u : this.active_users){
                if(u.getUsername().equals(user.getUsername())){
                    u.setUsername(username); break;
                }
            }
            if(user.getConState().equals("INSIDE"))
                for(ClientIndividualSession u : this.lobbies.get(user.getLobby()))
                    if(u.getUsername().equals(user.getUsername()))
                        u.setUsername(username);
            user.setUsername(username);
            if(user.getConState().equals("INSIDE"))
                this.sendMessageToAll("NEWNICK "+old+" "+username, user, buffer);
            else
                this.sendMessageUser("NEWNICK "+old+" "+username, user.getKey(), buffer);
        }else{
            user.setUsername(username);
            this.active_users.add(user);
        }
        this.usernames.add(username);
        this.sendMessageUser("OK", user.getKey(), buffer);
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
        ArrayList<ClientIndividualSession> subscribers;
        if(!this.lobbies.containsKey(lobby)) subscribers = new ArrayList<>();
        else subscribers = this.lobbies.get(lobby);
        user.setConState("INSIDE");
        user.setLobby(lobby);
        subscribers.add(user);
        this.lobbies.put(lobby,subscribers);   
        this.sendMessageToAll("JOINED "+user.getUsername()+" "+lobby, user, buffer);     
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