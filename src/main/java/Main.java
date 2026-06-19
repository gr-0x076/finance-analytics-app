import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        File currentDirectory = new File(System.getProperty("user.dir"));

        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine();

            if (input.equals("exit")) {
                break;
            }

            else if (input.startsWith("echo ")) {
                System.out.println(input.substring(5));
            }

            else if (input.equals("pwd")) {
                System.out.println(currentDirectory.getCanonicalPath());
            }

            else if (input.startsWith("cd ")) {

                String path = input.substring(3);

                File newDirectory;

                if (path.equals("~")) {
                    newDirectory = new File(System.getenv("HOME"));
                }
                else if (path.startsWith("/")) {
                    newDirectory = new File(path);
                }
                else {
                    newDirectory = new File(currentDirectory, path);
                }

                if (newDirectory.exists() && newDirectory.isDirectory()) {
                    currentDirectory = newDirectory.getCanonicalFile();
                } else {
                    System.out.println("cd: " + path + ": No such file or directory");
                }
            }

            else if (input.startsWith("type ")) {

                String command = input.substring(5);

                if (command.equals("echo")
                        || command.equals("exit")
                        || command.equals("type")
                        || command.equals("pwd")
                        || command.equals("cd")) {

                    System.out.println(command + " is a shell builtin");

                } else {

                    String executablePath = findExecutable(command);

                    if (executablePath != null) {
                        System.out.println(command + " is " + executablePath);
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

                    commandWithArgs.add(command);

                    for (int i = 1; i < parts.length; i++) {
                        commandWithArgs.add(parts[i]);
                    }

                    ProcessBuilder pb = new ProcessBuilder(commandWithArgs);

                    pb.directory(currentDirectory);
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