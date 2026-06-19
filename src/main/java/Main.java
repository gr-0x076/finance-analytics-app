
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

                String stdoutRedirectFile = null;
                String stderrRedirectFile = null;

                StringBuilder sb = new StringBuilder();
                boolean first = true;

                for (int i = 1; i < tokens.size(); i++) {
                    String token = tokens.get(i);

                    if (token.equals(">")
                            || token.equals("1>")
                            || token.equals("2>")) {

                        if (i + 1 < tokens.size()) {
                            if (token.equals("2>")) {
                                stderrRedirectFile = tokens.get(i + 1);
                            } else {
                                stdoutRedirectFile = tokens.get(i + 1);
                            }

                            i++;
                        }

                        break;
                    }

                    if (!first) {
                        sb.append(" ");
                    }

                    sb.append(token);
                    first = false;
                }

                if (stdoutRedirectFile != null) {

                    File outputFile;

                    if (stdoutRedirectFile.startsWith("/")) {
                        outputFile = new File(stdoutRedirectFile);
                    } else {
                        outputFile = new File(currentDirectory, stdoutRedirectFile);
                    }

                    java.io.PrintWriter writer = new java.io.PrintWriter(outputFile);
                    writer.println(sb);
                    writer.close();

                } else {
                    System.out.println(sb);
                }

                if (stderrRedirectFile != null) {

                    File errorFile;

                    if (stderrRedirectFile.startsWith("/")) {
                        errorFile = new File(stderrRedirectFile);
                    } else {
                        errorFile = new File(currentDirectory, stderrRedirectFile);
                    }

                    new java.io.PrintWriter(errorFile).close();
                }
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

                    String stdoutRedirectFile = null;
                    String stderrRedirectFile = null;

                    List<String> processArgs = new ArrayList<>();
                    processArgs.add(executablePath);

                    for (int i = 1; i < tokens.size(); i++) {
                        String token = tokens.get(i);

                        if (token.equals(">") || token.equals("1>") || token.equals("2>")) {
                            if (i + 1 < tokens.size()) {
                                String fileName = tokens.get(i + 1);

                                if (token.equals("2>")) {
                                    stderrRedirectFile = fileName;
                                } else {
                                    stdoutRedirectFile = fileName;
                                }

                                i++;
                                continue;
                            }
                        }

                        processArgs.add(token);
                    }

                    ProcessBuilder pb = new ProcessBuilder(processArgs);

                    pb.directory(currentDirectory);

                    if (stdoutRedirectFile != null) {

                        File outputFile = stdoutRedirectFile.startsWith("/")
                                ? new File(stdoutRedirectFile)
                                : new File(currentDirectory, stdoutRedirectFile);

                        pb.redirectOutput(outputFile);

                    } else {

                        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    }

                    if (stderrRedirectFile != null) {

                        File errorFile = stderrRedirectFile.startsWith("/")
                                ? new File(stderrRedirectFile)
                                : new File(currentDirectory, stderrRedirectFile);

                        pb.redirectError(errorFile);

                    } else {

                        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                    }

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
