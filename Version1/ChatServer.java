import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer{
    //A pre-allocated buffer for the received data
    private static final ByteBuffer buffer = ByteBuffer.allocate(16384);
    //decoder for incoming text=>assume UTF8
    private static final Charset charset = Charset.forName("UTF8");
    private static final CharsetDecoder decoder = charset.newDecoder();

    static String leftover = "";


/*------------------------------------------------------------------------------
                            Auxiliary Functions
------------------------------------------------------------------------------*/
    private static boolean processInput(SocketChannel sc, SelectionKey key,ClientSessionMonitor user_servers) throws IOException{
        ClientIndividualSession user = (ClientIndividualSession)key.attachment();
        buffer.clear();
        sc.read(buffer);
        buffer.flip();

        if(buffer.limit() == 0) return false;

        String message = decoder.decode(buffer).toString();
        leftover += message;
        if((int)message.charAt(message.length()-1) != 10) return true;
        else message = leftover;

        if(message.charAt(0) == '/'){
            //command
            String command, send_msg = "", priv_send_msg = "", attribute;
            send_msg = message.replace("/","");
            String[] splited = send_msg.split("\\s+"); 
            command = splited[0];
            if(splited.length <= 2) attribute = splited[1]; //command ____
            else{//command ___ {---------}
                attribute = splited[1];
                for(int i = 2; i < splited.length; i++) priv_send_msg += splited[i] + " ";
            }
            //parse command
            String s = ((command.equalsIgnoreCase("join"))? attribute : "");
            switch(user_servers.commandExecutionCode(command,key,s)){
                case "nick-ok": user_servers.cmdNick(user, buffer, attribute); break;
                case "join-ok": user_servers.cmdJoin(user, buffer, attribute); break;
                case "join-inside-ok": 
                    user_servers.cmdLeave(user, buffer);
                    user_servers.cmdJoin(user, buffer, attribute);
                break;
                case "join-newlobby-ok": user_servers.cmdCreateLobby(user, buffer, attribute); break;
                case "join-newlobby-inside-ok": 
                    user_servers.cmdLeave(user, buffer);
                    user_servers.cmdJoin(user, buffer, attribute);
                break;
                case "priv-ok": user_servers.cmdPriv(user, buffer, attribute, priv_send_msg); break;
                case "leave-lobby": user_servers.cmdLeave(user, buffer); break;
                case "bye-ok": user_servers.cmdBye(user, buffer); break;
                case "bye-lobby-ok":
                    user_servers.cmdLeave(user, buffer);
                    user_servers.cmdBye(user, buffer);
                break;
                default: user_servers.sendMessageUser("ERROR", user.getKey(), buffer); break;
            }            
        }
        else{
            if(user.getUsername() == null){
                user_servers.sendMessageUser("ERROR", user.getKey(), buffer);
                return true;
            }
            if(user.getConState().equals("INSIDE"))
                user_servers.sendMessageToAll("MESSAGE "+user.getUsername()+" "+message, user, buffer);
        }
        leftover = "";
        return true;
    }


//Main--------------------------------------------------------------------------
    public static void main(String[] args) {
        //Parse prot from command line
        int port = Integer.parseInt(args[0]);
        try {
            // Instead of creating a ServerSocket, create a ServerSocketChannel
            ServerSocketChannel ssc = ServerSocketChannel.open();

            //non-blocking => select
                ssc.configureBlocking(false);

            //get socket connected to chanel and bind listening port
                ServerSocket ss = ssc.socket();
                InetSocketAddress isa = new InetSocketAddress(port);
                ss.bind(isa);

            //create selector
                Selector selector = Selector.open();
                ClientSessionMonitor user_servers = new ClientSessionMonitor(selector);

            //Register channel => listen incoming connections
                ssc.register(selector,SelectionKey.OP_ACCEPT);
                System.out.println("Listening on port "+port);
                
            //Listening
            while(true){
                //Check if there was any activity (opening connection/incoming data)
                    int num = selector.select();
                    if(num == 0) continue; //no activity loop around and wait
                

                //Get keys corresponding to activity detected, process one by one
                Set<SelectionKey> keys = selector.selectedKeys();
                for(SelectionKey key : keys){
                    //See type of activity
                    if(key.isAcceptable()){
                        //Incoming connection => register socket with selector
                            Socket s = ss.accept();
                            System.out.println("Got connection from "+s);
                        
                        //Make it non-blocking
                            SocketChannel sc = s.getChannel();
                            sc.configureBlocking(false);
                        
                        //Register connection with selector for reading
                            SelectionKey user_key = sc.register(selector,SelectionKey.OP_READ);
                            ClientIndividualSession user = new ClientIndividualSession(sc, user_key);
                            user_key.attach(user);
                            //user_servers.addUser(user);
                    }
                    else if(key.isReadable()){
                        //incoming data
                        SocketChannel sc = null;
                        try {
                            sc = (SocketChannel)key.channel();
                            boolean ok = processInput(sc,key,user_servers);

                            //if connection dead => remove from selector
                            if(!ok){
                                key.cancel();
                                Socket s = null;
                                try{
                                    s = sc.socket();
                                    System.out.println( "Closing connection to "+s );
                                    s.close();
                                }catch(IOException ie){
                                    System.err.println( "Error closing socket "+s+": "+ie );
                                }
                            }
                        } catch (IOException e) {
                            // On exception, remove this channel from the selector
                            key.cancel();
                            try {
                                sc.close();
                            } catch (IOException e2) {
                                System.out.println(e2);
                            }
                            System.out.println("Closed "+sc);
                        }
                    }
                }
                // We remove the selected keys, because we've dealt with them.
                keys.clear();
            }
        }catch (IOException e) {
            System.err.println(e);
        }
    }
}