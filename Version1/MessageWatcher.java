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
    Charset charset = Charset.forName("UTF8");
    CharsetDecoder decoder = charset.newDecoder();

    MessageWatcher(JTextArea chat_area, SocketChannel channel){
        this.chat_area = chat_area;
        this.channel = channel;
        this.buffer = ByteBuffer.allocate(16384);
    }

    public void printMessage(final String message){this.chat_area.append(message);}

    public void run(){
        while(true)
            try {
                readMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void readMessage() throws IOException{
        String message;
        buffer.clear();
        channel.read(buffer);
        buffer.flip();
        message = decoder.decode(buffer).toString();
        System.out.println(message);
        String formated_message = this.formatMessage(message);
        printMessage(formated_message);
    }

    public String formatMessage(String message){
        String code, user, leftover;
        code = user = leftover = "";
        String[] msg = message.split(" ");
        code = msg[0];
        int index = 1;
        if(msg.length > 2){
            user = msg[1];
            index = 2;
        }
        for(int i = index; i < msg.length; i++)
            leftover += msg[i] + " ";
        ArrayList<String> ss = new ArrayList<>();
        ss.add(code); ss.add(user); ss.add(leftover);

        int i = 0;
        for(String s : ss)
            if(s.charAt(s.length()-1) == '\n') ss.set(i++,s.substring(0,s.length()-1));
        String sending_message = "";
        code = ss.get(0); user = ss.get(1); leftover = ss.get(2);
        switch(code){
            case "OK": sending_message = "Order completed.\n"; break;
            case "NEWNICK": sending_message = user +" changed username for "+leftover+".\n"; break;
            case "PRIVATE": sending_message = user+" (direct message): "+leftover+"\n"; break;
            case "JOIN": sending_message = user+" joined lobby.\n"; break;
            case "LEFT": sending_message = user+" left lobby.\n"; break;
            case "MESSAGE": sending_message = user+": "+leftover+"\n"; break;
            case "ERROR": sending_message = "Order can not be fulfilled\n"; break;
            case "BYE": sending_message = "Bye, closing connection.\n"; break;
            default: sending_message = message; break;//Message
        }
        return sending_message;
    }
}