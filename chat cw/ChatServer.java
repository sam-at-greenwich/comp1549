import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

/**
 * A multithreaded chat room server. When a client connects the server requests a screen
 * name by sending the client the text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received. After a client submits a unique name, the server acknowledges
 * with "NAMEACCEPTED". Then all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name. The broadcast messages are prefixed
 * with "MESSAGE".
 *
 * This is just a teaching example so it can be enhanced in many ways, e.g., better
 * logging. Another is to accept a lot of fun commands, like Slack.
 */
public class ChatServer {

    // All client names, so we can check for duplicates upon registration.
    private static Set<String> names = new HashSet<>();
    static volatile List<String> convo = new ArrayList<String>();
    static volatile  String leader = "";

     // The set of all the print writers for all the clients, used for broadcast.
    private static Set<PrintWriter> writers = new HashSet<>();

    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running...");
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(59001)) {
            while (true) {
                pool.execute(new Handler(listener.accept()));
            }
        }
    }

    /**
     * The client handler task.
     */
    private static class Handler implements Runnable {
        private String name;
        private Socket socket;
        private Scanner in;
        private PrintWriter out;






        /**
         * Constructs a handler thread, squirreling away the socket. All the interesting
         * work is done in the run method. Remember the constructor is called from the
         * server's main method, so this has to be as short as possible.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a screen name until a
         * unique one has been submitted, then acknowledges the name and registers the
         * output stream for the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void speak(String line) {
            for (PrintWriter writer : writers) {
                writer.println(line);
            }
            convo.add(line);
        }
        /**Runnable check = new Runnable() {
            public void run() {
                Set<String> checked = names;
                for (PrintWriter writer : writers) {
                    writer.println("ROLL");
                    String tester = in.nextLine();
                    if (tester.startsWith("HERE")) {
                        checked.remove(tester.substring(4));
                    }
                }
                for (String absent : checked) {
                    speak("MESSAGE " + absent + " did not check in");
                }
                speak("PRIVATE" + leader + "check complete");
            }
        };*/

        public void run() {
            try {
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

                // Keep requesting a name until we get a unique one.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.nextLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!name.isEmpty() && !names.contains(name)) {
                            names.add(name);
                            break;
                        }
                    }
                }
                // Now that a successful name has been chosen, add the socket's print writer
                // to the set of all writers so this client can receive broadcast messages.
                // But BEFORE THAT, let everyone else know that the new person has joined!
                out.println("NAMEACCEPTED " + name);
                speak("MESSAGE " + name + " has joined");
                if (leader.isEmpty()) {
                    leader = name;
                    speak("MESSAGE " + name + " has been appointed group  coordinator");
                    out.println("BOSS");
                }
                for (String mesg : convo) {
                    out.println(mesg);
                }
                writers.add(out);

                // Accept messages from this client and broadcast them.
                while (true) {
                    String input = in.nextLine();
                    if (leader.isEmpty()) {
                        leader = name;
                        speak("MESSAGE " + name + " has been appointed group  coordinator");
                        out.println("BOSS");
                    }
                    if (input.toLowerCase().startsWith("/quit")) {
                        return;
                    }
                    if (input.toLowerCase().startsWith("/check") && name == leader) {
                        Set<String> checked = names;
                        for (PrintWriter writer : writers) {
                            writer.println("ROLL");
                            String tester = in.nextLine();
                            if (tester.startsWith("HERE")) {
                                checked.remove(tester.substring(4));
                            }
                        }
                        for (String absent : checked) {
                            speak("MESSAGE " + absent + " did not check in");
                        }
                        speak("PRIVATE" + leader + "check complete");
                    } else if (input.toLowerCase().startsWith("/msg")) {
                        boolean found = false;
                        for (String test : names) {
                            if (input.contains(test)){
                                speak("PRIVATE" + test +name +" whispered to you "+ input.substring(5 + test.length()));
                            found = true;
                            break;
                            }
                        }
                        if (!found){
                            out.println("couldn't find who you were looking for");
                        }
                    } else if (input.startsWith("HERE")) {

                    } else {
                        speak("MESSAGE " + name + ": " + input + " (sent at " + new Date().toString() + ")");
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            } finally {
                if (out != null) {
                    writers.remove(out);
                }
                if (leader == name){
                    leader = "";
                }
                if (name != null) {
                    System.out.println(name + " is leaving");
                    names.remove(name);
                    speak("MESSAGE " + name + " has left");
                }
                try { socket.close(); } catch (IOException e) {}
            }
        }
    }
}
