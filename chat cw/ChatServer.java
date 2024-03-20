import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static Set<String> names = new HashSet<>();
    static volatile List<String> convo = new ArrayList<String>();
    static volatile  String leader = "";
    private static Set<PrintWriter> writers = new HashSet<>();

    public static void main(String[] args) throws Exception {
        System.out.println("The group server is running on port 57001");
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(59001)) {
            while (true) {
                pool.execute(new Handler(listener.accept()));
            }
        }
    }
    private static class Handler implements Runnable {
        private String name;
        private Socket socket;
        private Scanner in;
        private PrintWriter out;
        public Handler(Socket socket) {
            this.socket = socket;
        }
        public void speak(String line) {
            for (PrintWriter writer : writers) {
                writer.println(line);
            }
            convo.add(line);
        }

        public void run() {
            try {
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

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

                    } else if (input.startsWith("/req")) {
                        if (name == leader){
                            speak("INTRO");
                        }
                    } else {
                        LocalDateTime currentDateTime = LocalDateTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
                        speak("MESSAGE " + name + ": " + input + " (" + formatter.format(currentDateTime) + ")");
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
