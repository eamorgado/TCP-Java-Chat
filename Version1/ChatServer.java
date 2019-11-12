import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer{
    //buffer to receive data
    private static final ByteBuffer buffer = ByteBuffer.allocate(16384);
    //decoder for incoming text=>assume UTF8
    private static final Charset charset = Charset.forName("UTF8");
    private static final CharsetDecoder decoder = charset.newDecoder();


    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            //non-blocking => select
                ssc.configureBlocking(false);
            //get socket connected to chanel and bind listening port
                ServerSocket ss = ssc.socket();
                InetSocketAddress isa = new InetSocketAddress(port);
                ss.bind(isa);
            //create selector
                Selector selector = Selector.open();
            //Register channel => listen incoming connections
                ssc.register(selector,SelectionKey.OP_ACCEPT);
                System.out.println("Listening on port "+port);
            //Listening
            while(true){
                //Check if there was any activity
                    int num = selector.select();
                    if(num == 0) continue;
                
                Set<SelectionKey> keys = selector.selectedKeys();
                for(SelectionKey key : keys){
                    if(key.isAcceptable()){
                        //Incoming connection => register socket with selector
                            Socket s = ss.accept();
                            System.out.println("Got connection from "+s);
                        
                        //Make it non-blocking
                            SocketChannel sc = s.getChannel();
                            sc.configureBlocking(false);
                        
                        sc.register(selector,SelectionKey.OP_READ);
                    }
                    else if(key.isReadable()){
                        //incoming data
                        SocketChannel sc = null;
                        try {
                            s = sc.socket();
                            System.out.println("Closing connection ");
                        } catch (IOException e) {
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
            }
        }catch (IOException e) {
            System.err.println(e);
        }
    }
}