import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatClient2 {
    String name = "temp";
    boolean leader = false;
    String serverAddress;
    Scanner in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(30, 50);
    JButton button = new JButton("quit");
    public ChatClient2() {
        this.serverAddress = getName("IP (localhost for example)");

        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, BorderLayout.CENTER);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.NORTH);
        frame.getContentPane().add(button, BorderLayout.SOUTH);
        frame.pack();
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println("/quit");
            }
        });
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }

        });
    }
    private String getName(String var) {
        return JOptionPane.showInputDialog(
            frame,
            "Choose a "+ var +" :",
            "Screen selection",
            JOptionPane.PLAIN_MESSAGE
        );
    }

    private void run() throws IOException {
        try {
            int port = Integer.parseInt(getName("port (59001 for example)"));
            Socket socket = new Socket(serverAddress, port);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);

            while (in.hasNextLine()) {
                String line = in.nextLine();
                if (line.startsWith("SUBMITNAME")) {
                    out.println(getName("name or ID"));
                } else if (line.startsWith("NAMEACCEPTED")) {
                    name = line.substring(13);
                    this.frame.setTitle("Chatter - " + name);
                    textField.setEditable(true);
                } else if (line.startsWith("MESSAGE")) {
                    messageArea.append(line.substring(8) + "\n");
                } else if (line.startsWith("PRIVATE" + name)) {
                messageArea.append(line.substring(7 + name.length()) + "\n");
                } else if (line.startsWith("ROLL")) {
                    if (!leader){
                        out.println("HERE" + name);
                    }
                } else if (line.startsWith("BOSS")) {
                    leader = true;
                    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                    executor.scheduleAtFixedRate(check, 0, 20, TimeUnit.SECONDS);
                }else if (line.startsWith("INTRO")) {
                    out.println("hi my ID is " + name + " my IP address is "  + InetAddress.getLocalHost() + " and im talking through port "+ port);

                }
            }
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }
    Runnable check = new Runnable() {
        public void run() {
            out.println("HERE"+ name);
        }
    };

    public static void main(String[] args) throws Exception {
        ChatClient2 client = new ChatClient2();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}
