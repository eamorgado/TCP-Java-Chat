import java.awt.*;
import java.awt.Color;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import javax.swing.*;
import javax.swing.text.*;

import java.util.*;

class MessageWatcher implements Runnable{
    /**
     * 
     *  Note: for some reason, when we use ´ º « or » in a message it creates an error
     * 
     */
    JTextPane chat_area;
    JTextPane private_chat;
    JTextPane chat_info;
    SocketChannel channel;
    ByteBuffer buffer;
    Charset charset = Charset.forName("UTF8");
    CharsetDecoder decoder = charset.newDecoder();
    boolean flag = false;

    MessageWatcher(JTextPane chat_area,JTextPane private_chat, JTextPane chat_info, SocketChannel channel){
        this.chat_area = chat_area;
        this.private_chat = private_chat;
        this.chat_info = chat_info;
        this.channel = channel;
        this.buffer = ByteBuffer.allocate(16384);
    }

    public void append(String s, int color_foreground, int color_background,boolean bold,int monitor){
        StyledDocument doc;
        if(monitor == 0) doc = this.chat_area.getStyledDocument(); //lobby message
        else if(monitor == 1) doc = this.private_chat.getStyledDocument(); //private message
        else doc = this.chat_info.getStyledDocument();
        StyleContext contex = new StyleContext();
        Style word = contex.addStyle("test", null);
        switch(color_foreground){
            case 1: StyleConstants.setForeground(word, Color.RED); break;
            case 2: StyleConstants.setForeground(word, Color.BLUE); break;
            case 3: StyleConstants.setForeground(word, Color.GREEN); break;
            default: StyleConstants.setForeground(word, Color.BLACK); break;
        }
        switch(color_background){
            case 1: StyleConstants.setBackground(word, Color.RED); break;
            case 2: StyleConstants.setBackground(word, Color.BLUE); break;
            case 3: StyleConstants.setBackground(word, Color.GREEN); break;
            case 4: StyleConstants.setBackground(word, Color.BLACK); break;
            case 5: StyleConstants.setBackground(word, Color.YELLOW); break;
            default: break;
        }
        if(bold) StyleConstants.setBold(word, true);
        try{doc.insertString(doc.getLength(),s, word);}
        catch(Exception e) {System.out.println(e);}
    }
    public void printMessage(StyleMessage message){
        append(message.getString(),message.getForeground(),message.getBackground(),message.getBold(),message.getMonitor());
    }

    public void run(){
        while(true)
            try {readMessage();} 
            catch (IOException e){e.printStackTrace();}
    }

    public void readMessage() throws IOException{
        buffer.clear();
        channel.read(buffer);
        buffer.flip();

        String message = decoder.decode(buffer).toString();
        System.out.print(message);
        printMessage(this.formatMessage(message));
        if(flag) System.exit(1);
    }

    public StyleMessage formatMessage(String message){
        String code, user, leftover;
        String sending_message = "";
        int color_foreground = 0, color_background = 0;
        boolean bold = false;
        int monitor = 0;
        code = user = leftover = "";
        if(message.length()==0){
            code = "ERROR";
        }
        else{
            if(message.charAt(message.length() - 1) == '\n') message = message.substring(0,message.length()-1);
            String[] msg = message.split(" ");
            code = msg[0];
            if(msg.length >= 2){
                user = msg[1];
                if(msg.length > 2)
                    for(int i = 2; i < msg.length; i++) leftover += msg[i] + " ";
            }
        }
        
        switch(code){
            case "OK": sending_message = "Order completed.\n"; color_foreground = 3; bold = true; monitor = 2; break;
            case "NEWNICK": sending_message = user +" changed username for "+leftover+".\n"; color_background = 5; bold = true; break;
            case "PRIVATE": sending_message = user+" (direct message): "+leftover+"\n"; color_foreground = 2; bold = true; monitor = 1; break;
            case "JOINED": sending_message = user+" joined lobby.\n"; color_background = 5; bold = true; break;
            case "LEFT": sending_message = user+" left lobby.\n"; color_background = 5; bold = true; break;
            case "MESSAGE": sending_message = user+": "+leftover+"\n"; bold = true; break;
            case "ERROR": sending_message = "Order can not be fulfilled\n"; color_foreground = 1; bold = true; monitor = 2; break;
            case "BYE": sending_message = "Bye, closing connection.\n"; flag=true; break;
            default: sending_message = message; break;//Message
        }

        return new StyleMessage(sending_message,color_foreground,color_background,bold,monitor);
    }
}


class StyleMessage{
    private int color_foreground, color_background;
    private String s;
    private boolean bold;
    private int monitor;

    StyleMessage(String s,int color_foreground,int color_background,boolean bold,int monitor){
        this.s = s; 
        this.color_foreground = color_foreground; this.color_background = color_background;
        this.bold = bold;
        this.monitor = monitor;
    }

    public String getString(){return this.s;}
    public int getForeground(){return this.color_foreground;}
    public int getBackground(){return this.color_background;}
    public boolean getBold(){return this.bold;}
    public int getMonitor(){return this.monitor;}
}