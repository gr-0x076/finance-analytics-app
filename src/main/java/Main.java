import java.util.*;
import java.io.*;

public class Main {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine();

            if (input.equals("exit")) {
                break;
            }

            else if (input.startsWith("echo ")) {
                System.out.println(input.substring(5));
            }

            else if (input.startsWith("type ")) {
                String command = input.substring(5);

                if (command.equals("echo")
                        || command.equals("exit")
                        || command.equals("type")) {

                    System.out.println(command + " is a shell builtin");
                } else {
                    String path = findExecutable(command);

                    if (path != null) {
                        System.out.println(command + " is " + path);
                    } else {
                        System.out.println(command + ": not found");
                    }
                }
            }

            else {
                String[] parts = input.split(" ");
                String command = parts[0];

                String executablePath = findExecutable(command);

                if (executablePath != null) {
                    List<String> commandWithArgs = new ArrayList<>();

                    commandWithArgs.add(executablePath);

                    for (int i = 1; i < parts.length; i++) {
                        commandWithArgs.add(parts[i]);
                    }

                    ProcessBuilder pb = new ProcessBuilder(commandWithArgs);
                    pb.inheritIO();

                    Process process = pb.start();
                    process.waitFor();
                } else {
                    System.out.println(command + ": command not found");
                }
            }
        }
    }

    private static String findExecutable(String command) {
        String pathEnv = System.getenv("PATH");

        if (pathEnv == null) {
            return null;
        }

        String[] directories = pathEnv.split(File.pathSeparator);

        for (String dir : directories) {
            File file = new File(dir, command);

            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }

        return null;
    }
}