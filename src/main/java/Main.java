
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

            List<String> tokens = parseCommand(input);

            if (tokens.isEmpty()) {
                continue;
            }

            String command = tokens.get(0);

            if (command.equals("exit")) {
                break;
            } else if (command.equals("echo")) {

                StringBuilder sb = new StringBuilder();

                for (int i = 1; i < tokens.size(); i++) {
                    if (i > 1) {
                        sb.append(" ");
                    }
                    sb.append(tokens.get(i));
                }

                System.out.println(sb);
            } else if (command.equals("pwd")) {
                System.out.println(currentDirectory.getCanonicalPath());
            } else if (command.equals("cd")) {

                if (tokens.size() < 2) {
                    continue;
                }

                String path = tokens.get(1);

                File newDirectory;

                if (path.equals("~")) {
                    newDirectory = new File(System.getenv("HOME"));
                } else if (path.startsWith("/")) {
                    newDirectory = new File(path);
                } else {
                    newDirectory = new File(currentDirectory, path);
                }

                if (newDirectory.exists() && newDirectory.isDirectory()) {
                    currentDirectory = newDirectory.getCanonicalFile();
                } else {
                    System.out.println("cd: " + path + ": No such file or directory");
                }
            } else if (command.equals("type")) {

                if (tokens.size() < 2) {
                    continue;
                }

                String target = tokens.get(1);

                if (target.equals("echo")
                        || target.equals("exit")
                        || target.equals("type")
                        || target.equals("pwd")
                        || target.equals("cd")) {

                    System.out.println(target + " is a shell builtin");

                } else {

                    String executablePath = findExecutable(target);

                    if (executablePath != null) {
                        System.out.println(target + " is " + executablePath);
                    } else {
                        System.out.println(target + ": not found");
                    }
                }
            } else {

                String executablePath = findExecutable(command);

                if (executablePath != null) {

                    ProcessBuilder pb = new ProcessBuilder(tokens);

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

    private static List<String> parseCommand(String input) {

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < input.length(); i++) {

            char ch = input.charAt(i);

            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            // Inside double quotes
            if (inDoubleQuote && ch == '\\') {

                if (i + 1 < input.length()) {

                    char next = input.charAt(i + 1);

                    if (next == '"' || next == '\\') {
                        current.append(next);
                        i++;
                        continue;
                    } else {
                        current.append('\\');
                        current.append(next);
                        i++;
                        continue;
                    }
                }
            }

            // Outside quotes
            if (!inSingleQuote && !inDoubleQuote && ch == '\\') {

                if (i + 1 < input.length()) {
                    current.append(input.charAt(i + 1));
                    i++;
                }

                continue;
            }

            if (Character.isWhitespace(ch)
                    && !inSingleQuote
                    && !inDoubleQuote) {

                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
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
