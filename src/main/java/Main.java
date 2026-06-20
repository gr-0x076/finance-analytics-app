
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        File currentDirectory = new File(System.getProperty("user.dir"));
        TerminalMode terminalMode = new TerminalMode();
        terminalMode.enableRawMode();

        try {
            while (true) {

                String input = readLineWithTabCompletion();
                if (input == null) {
                    break;
                }

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
                    boolean stdoutAppend = false;
                    boolean stderrAppend = false;

                    StringBuilder sb = new StringBuilder();
                    boolean first = true;

                    for (int i = 1; i < tokens.size(); i++) {
                        String token = tokens.get(i);

                        if (token.equals(">")
                                || token.equals("1>")
                                || token.equals(">>")
                                || token.equals("1>>")
                                || token.equals("2>")
                                || token.equals("2>>")) {

                            if (i + 1 < tokens.size()) {
                                if (token.equals("2>") || token.equals("2>>")) {
                                    stderrRedirectFile = tokens.get(i + 1);
                                    stderrAppend = token.equals("2>>");
                                } else {
                                    stdoutRedirectFile = tokens.get(i + 1);
                                    stdoutAppend = token.equals(">>") || token.equals("1>>");
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

                        java.io.PrintWriter writer = new java.io.PrintWriter(
                                new java.io.FileWriter(outputFile, stdoutAppend));
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

                        java.io.PrintWriter writer = new java.io.PrintWriter(
                                new java.io.FileWriter(errorFile, stderrAppend));
                        writer.close();
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
                        boolean stdoutAppend = false;
                        boolean stderrAppend = false;

                        List<String> processArgs = new ArrayList<>();
                        processArgs.add(command);

                        for (int i = 1; i < tokens.size(); i++) {
                            String token = tokens.get(i);

                            if (token.equals(">") || token.equals("1>") || token.equals(">>") || token.equals("1>>") || token.equals("2>") || token.equals("2>>")) {
                                if (i + 1 < tokens.size()) {
                                    String fileName = tokens.get(i + 1);

                                    if (token.equals("2>") || token.equals("2>>")) {
                                        stderrRedirectFile = fileName;
                                        stderrAppend = token.equals("2>>");
                                    } else {
                                        stdoutRedirectFile = fileName;
                                        stdoutAppend = token.equals(">>") || token.equals("1>>");
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

                            if (stdoutAppend) {
                                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outputFile));
                            } else {
                                pb.redirectOutput(outputFile);
                            }

                        } else {

                            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                        }

                        if (stderrRedirectFile != null) {

                            File errorFile = stderrRedirectFile.startsWith("/")
                                    ? new File(stderrRedirectFile)
                                    : new File(currentDirectory, stderrRedirectFile);

                            if (stderrAppend) {
                                pb.redirectError(ProcessBuilder.Redirect.appendTo(errorFile));
                            } else {
                                pb.redirectError(errorFile);
                            }

                        } else {

                            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                        }

                        terminalMode.disableRawMode();
                        Process process = pb.start();
                        process.waitFor();
                        terminalMode.enableRawMode();

                    } else {
                        System.out.println(command + ": command not found");
                    }
                }
            }
        } finally {
            terminalMode.disableRawMode();
        }
    }

    private static class TerminalMode {

        private String originalTerminalConfig;

        void enableRawMode() {
            try {
                originalTerminalConfig = execCommand("stty -g < /dev/tty");
                execCommand("stty raw -echo min 1 time 0 < /dev/tty");
            } catch (Exception ignored) {
                originalTerminalConfig = null;
            }
        }

        void disableRawMode() {
            if (originalTerminalConfig != null) {
                try {
                    execCommand("stty " + originalTerminalConfig + " < /dev/tty");
                } catch (Exception ignored) {
                }
            }
        }

        private String execCommand(String command) throws Exception {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            java.io.InputStream in = process.getInputStream();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            int ch;
            while ((ch = in.read()) != -1) {
                out.write(ch);
            }
            process.waitFor();
            return out.toString().trim();
        }
    }

    private static String readLineWithTabCompletion() throws java.io.IOException {
        StringBuilder buffer = new StringBuilder();
        System.out.print("$ ");
        System.out.flush();

        while (true) {
            int ch = System.in.read();
            if (ch == -1) {
                return null;
            }

            if (ch == '\r') {
                int next = System.in.read();
                if (next != '\n' && next != -1) {
                    // ignore extra character if present
                }
                System.out.println();
                return buffer.toString();
            }

            if (ch == '\n') {
                System.out.println();
                return buffer.toString();
            }

            if (ch == '\t') {
                if (!buffer.toString().contains(" ")) {
                    String completion = completeBuiltin(buffer.toString());
                    if (completion != null) {
                        while (buffer.length() > 0) {
                            System.out.print("\b \b");
                            buffer.setLength(buffer.length() - 1);
                        }
                        buffer.append(completion);
                        System.out.print(completion);
                        System.out.flush();
                    }
                }
                continue;
            }

            if (ch == 8 || ch == 127) {
                if (buffer.length() > 0) {
                    buffer.setLength(buffer.length() - 1);
                    System.out.print("\b \b");
                    System.out.flush();
                }
                continue;
            }

            buffer.append((char) ch);
            System.out.print((char) ch);
            System.out.flush();
        }
    }

    private static String completeBuiltin(String partial) {
        if (partial.length() > 0 && "echo".startsWith(partial) && !partial.equals("echo")) {
            return "echo ";
        }
        if (partial.length() > 0 && "exit".startsWith(partial) && !partial.equals("exit")) {
            return "exit ";
        }
        return null;
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
