import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import javax.swing.*;
import java.util.*;

class MessageWatcher implements Runnable{
    JTextArea chat_area;
    SocketChannel channel;
    ByteBuffer buffer;
    Charset charset;
    CharsetDecoder decoder;
    boolean flag = false;

    MessageWatcher(JTextArea chat_area, SocketChannel channel){
        this.chat_area = chat_area;
        this.channel = channel;
        this.buffer = ByteBuffer.allocate(16384);
    }

    public void printMessage(final String message){this.chat_area.append(message);}

    public void run(){
        while(true)
            try {readMessage();} 
            catch (IOException e){e.printStackTrace();}
    }

    public void readMessage() throws IOException{
        buffer.clear();
        channel.read(buffer);
        buffer.flip();

        charset = Charset.forName("UTF8");
        decoder = charset.newDecoder();

        String message = decoder.decode(buffer).toString();
        System.out.println(message);
        String formated_message = this.formatMessage(message);
        printMessage(formated_message);
        if(flag) System.exit(1);
    }

    public String formatMessage(String message){
        if(message.charAt(message.length() - 1) == '\n') message = message.substring(0,message.length()-1);
        String code, user, leftover;
        code = user = leftover = "";
        String[] msg = message.split(" ");
        code = msg[0];
        if(msg.length >= 2){
            user = msg[1];
            if(msg.length > 2)
                for(int i = 2; i < msg.length; i++) leftover += msg[i] + " ";
        }
        String sending_message = "";
        switch(code){
            case "OK": sending_message = "Order completed.\n"; break;
            case "NEWNICK": sending_message = user +" changed username for "+leftover+".\n"; break;
            case "PRIVATE": sending_message = user+" (direct message): "+leftover+"\n"; break;
            case "JOINED": sending_message = user+" joined lobby "+leftover+".\n"; break;
            case "LEFT": sending_message = user+" left lobby.\n"; break;
            case "MESSAGE": sending_message = user+": "+leftover+"\n"; break;
            case "ERROR": sending_message = "Order can not be fulfilled\n"; break;
            case "BYE": sending_message = "Bye, closing connection.\n"; flag=true; break;
            default: sending_message = message; break;//Message
        }
        return sending_message;
    }
}