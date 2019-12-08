import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChatClient{
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JPanel main_panel;
    private JTextPane chatArea = new JTextPane();
    private JTextPane chatMessages = new JTextPane();
    private JTextPane privateMessages = new JTextPane();
    private int port;
    private String server;
    SocketChannel channel;
    ByteBuffer buffer;

    
    // Construtor
    public ChatClient(String server, int port) throws IOException {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.add(chatBox);
            panel.setSize(800,100);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);

        main_panel = new JPanel();
        main_panel.setLayout(new GridLayout(1,2));
        main_panel.setBackground(Color.WHITE);
        
        JPanel second = new JPanel();
        second.setLayout(new GridLayout(2,1));
        second.setBackground(Color.WHITE);

        JScrollPane p1 = new JScrollPane(chatArea);
        chatArea.setBackground(Color.GRAY);
        
        JScrollPane p2 = new JScrollPane(privateMessages);
        privateMessages.setBackground(Color.GRAY);
        
        JScrollPane p3 = new JScrollPane(chatMessages);
        chatMessages.setBackground(Color.GRAY);

        second.add(p2);
        second.add(p3);

        main_panel.setSize(800,400);
        main_panel.add(p1);
        main_panel.add(second);

        frame.add(main_panel);
        frame.setSize(800, 500);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatMessages.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {chatBox.setText("");}
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e){chatBox.requestFocus();}
        });

        this.server = server;
        this.port = port;
        this.buffer = ByteBuffer.allocate(16384);

    }

    //User made new message => send to server
    public void newMessage(String message) throws IOException {
        buffer.clear();
        buffer.rewind();
        message += "\n";
        buffer.put(message.getBytes());
        buffer.flip();
        channel.write(buffer);
    }

    
    public void run() throws IOException {
        InetSocketAddress host = new InetSocketAddress(this.server, this.port);
        channel = SocketChannel.open(host);
        MessageWatcher watcher = new MessageWatcher(chatArea,privateMessages,chatMessages,channel);
        watcher.run();
    }
    

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}