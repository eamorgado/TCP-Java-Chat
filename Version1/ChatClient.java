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
    private JTextArea chatArea = new JTextArea();

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
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
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
        this.buffer.clear();
        buffer.rewind();
        message += "\n";
        buffer.put(message.getBytes());
        buffer.flip();
        channel.write(buffer);
    }

    
    public void run() throws IOException {
        InetSocketAddress host = new InetSocketAddress(this.server, this.port);
        channel = SocketChannel.open(host);
        MessageWatcher watcher = new MessageWatcher(chatArea,channel);
        watcher.run();
    }
    

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}