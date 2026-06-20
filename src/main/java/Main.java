
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final TerminalMode terminalMode = new TerminalMode();
    private static File currentDirectory = new File(System.getProperty("user.dir"));
    private static final java.util.Map<String, String> completions = new java.util.HashMap<>();

    public static void main(String[] args) throws Exception {

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
                } else if (command.equals("complete")) {
                    if (tokens.size() >= 4 && tokens.get(1).equals("-C")) {
                        completions.put(tokens.get(3), tokens.get(2));
                    } else if (tokens.size() >= 3 && tokens.get(1).equals("-p")) {
                        String targetCmd = tokens.get(2);
                        String completer = completions.get(targetCmd);
                        if (completer != null) {
                            System.out.println("complete -C '" + completer + "' " + targetCmd);
                        } else {
                            System.out.println("complete: " + targetCmd + ": no completion specification");
                        }
                    }
                    // other flags: no-op for now
                } else if (command.equals("type")) {

                    if (tokens.size() < 2) {
                        continue;
                    }

                    String target = tokens.get(1);

                    if (target.equals("echo")
                            || target.equals("exit")
                            || target.equals("type")
                            || target.equals("pwd")
                            || target.equals("cd")
                            || target.equals("complete")) {

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

                        System.out.flush();
                        System.err.flush();
                        Process process = pb.start();
                        process.waitFor();

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
        terminalMode.enableRawMode();
        try {
            StringBuilder buffer = new StringBuilder();
            System.out.print("$ ");
            System.out.flush();
            boolean lastWasTab = false;

            while (true) {
                int ch = System.in.read();
                if (ch == -1) {
                    terminalMode.disableRawMode();
                    return null;
                }

                if (ch == '\r') {
                    int next = System.in.read();
                    if (next != '\n' && next != -1) {
                        // ignore extra character if present
                    }
                    terminalMode.disableRawMode();
                    System.out.println();
                    return buffer.toString();
                }

                if (ch == '\n') {
                    terminalMode.disableRawMode();
                    System.out.println();
                    return buffer.toString();
                }

                if (ch == '\t') {
                    String text = buffer.toString();
                    if (!text.contains(" ")) {
                        java.util.TreeSet<String> matches = getMatchingCommands(text);
                        if (matches.isEmpty()) {
                            System.out.print("\u0007");
                            System.out.flush();
                            lastWasTab = false;
                        } else if (matches.size() == 1) {
                            String completion = matches.first() + " ";
                            while (buffer.length() > 0) {
                                System.out.print("\b \b");
                                buffer.setLength(buffer.length() - 1);
                            }
                            buffer.append(completion);
                            System.out.print(completion);
                            System.out.flush();
                            lastWasTab = false;
                        } else {
                            String lcp = getLongestCommonPrefix(matches);
                            if (lcp.length() > buffer.length()) {
                                while (buffer.length() > 0) {
                                    System.out.print("\b \b");
                                    buffer.setLength(buffer.length() - 1);
                                }
                                buffer.append(lcp);
                                System.out.print(lcp);
                                System.out.flush();
                                lastWasTab = false;
                            } else {
                                if (lastWasTab) {
                                    terminalMode.disableRawMode();
                                    System.out.println();
                                    System.out.println(String.join("  ", matches));
                                    System.out.print("$ " + buffer.toString());
                                    System.out.flush();
                                    terminalMode.enableRawMode();
                                    lastWasTab = false;
                                } else {
                                    System.out.print("\u0007");
                                    System.out.flush();
                                    lastWasTab = true;
                                }
                            }
                        }
                    } else {
                        int lastSpaceIdx = text.lastIndexOf(' ');
                        String rawToken = text.substring(lastSpaceIdx + 1);
                        String base = text.substring(0, lastSpaceIdx + 1);
                        String command = text.substring(0, text.indexOf(' '));
                        String completer = completions.get(command);

                        if (completer != null) {
                            String previousWord = "";
                            List<String> previousTokens = parseCommand(
                                    text.substring(0, lastSpaceIdx));
                            if ((!rawToken.isEmpty() && !previousTokens.isEmpty())
                                    || previousTokens.size() > 1) {
                                previousWord = previousTokens.get(previousTokens.size() - 1);
                            }

                            List<String> candidates = runCompleter(
                                    completer, command, rawToken, previousWord, text);
                            if (candidates.size() == 1) {
                                String candidate = candidates.get(0);
                                String completion = base + candidate + " ";
                                while (buffer.length() > 0) {
                                    System.out.print("\b \b");
                                    buffer.setLength(buffer.length() - 1);
                                }
                                buffer.append(completion);
                                System.out.print(completion);
                                System.out.flush();
                                lastWasTab = false;
                            } else if (candidates.isEmpty()) {
                                System.out.print("\u0007");
                                System.out.flush();
                                lastWasTab = false;
                            } else if (lastWasTab) {
                                java.util.Collections.sort(candidates);
                                terminalMode.disableRawMode();
                                System.out.println();
                                System.out.println(String.join("  ", candidates));
                                System.out.print("$ " + buffer.toString());
                                System.out.flush();
                                terminalMode.enableRawMode();
                                lastWasTab = false;
                            } else {
                                System.out.print("\u0007");
                                System.out.flush();
                                lastWasTab = true;
                            }
                            continue;
                        }

                        String dirPathStr = "";
                        String prefix = rawToken;
                        File searchDir = currentDirectory;

                        if (rawToken.contains("/")) {
                            int lastSlashIdx = rawToken.lastIndexOf('/');
                            dirPathStr = rawToken.substring(0, lastSlashIdx + 1);
                            prefix = rawToken.substring(lastSlashIdx + 1);
                            if (dirPathStr.startsWith("~/")) {
                                String home = System.getenv("HOME");
                                searchDir = new File(home, dirPathStr.substring(2));
                            } else if (dirPathStr.startsWith("/")) {
                                searchDir = new File(dirPathStr);
                            } else {
                                searchDir = new File(currentDirectory, dirPathStr);
                            }
                        }

                        java.util.TreeSet<String> fileMatches = new java.util.TreeSet<>();
                        if (searchDir.exists() && searchDir.isDirectory()) {
                            File[] files = searchDir.listFiles();
                            if (files != null) {
                                for (File file : files) {
                                    String name = file.getName();
                                    if (name.startsWith(prefix)) {
                                        fileMatches.add(name);
                                    }
                                }
                            }
                        }

                        if (fileMatches.isEmpty()) {
                            System.out.print("\u0007");
                            System.out.flush();
                            lastWasTab = false;
                        } else if (fileMatches.size() == 1) {
                            String matchedName = fileMatches.first();
                            File matchedFile = new File(searchDir, matchedName);
                            String suffix = matchedFile.isDirectory() ? "/" : " ";
                            String completion = base + dirPathStr + matchedName + suffix;
                            while (buffer.length() > 0) {
                                System.out.print("\b \b");
                                buffer.setLength(buffer.length() - 1);
                            }
                            buffer.append(completion);
                            System.out.print(completion);
                            System.out.flush();
                            lastWasTab = false;
                        } else {
                            String lcp = getLongestCommonPrefix(fileMatches);
                            if (lcp.length() > prefix.length()) {
                                String completion = base + dirPathStr + lcp;
                                while (buffer.length() > 0) {
                                    System.out.print("\b \b");
                                    buffer.setLength(buffer.length() - 1);
                                }
                                buffer.append(completion);
                                System.out.print(completion);
                                System.out.flush();
                                lastWasTab = false;
                            } else {
                                if (lastWasTab) {
                                    List<String> displayMatches = new ArrayList<>();
                                    for (String name : fileMatches) {
                                        File f = new File(searchDir, name);
                                        displayMatches.add(f.isDirectory() ? name + "/" : name);
                                    }
                                    terminalMode.disableRawMode();
                                    System.out.println();
                                    System.out.println(String.join("  ", displayMatches));
                                    System.out.print("$ " + buffer.toString());
                                    System.out.flush();
                                    terminalMode.enableRawMode();
                                    lastWasTab = false;
                                } else {
                                    System.out.print("\u0007");
                                    System.out.flush();
                                    lastWasTab = true;
                                }
                            }
                        }
                    }
                    continue;
                }

                if (ch == 8 || ch == 127) {
                    lastWasTab = false;
                    if (buffer.length() > 0) {
                        buffer.setLength(buffer.length() - 1);
                        System.out.print("\b \b");
                        System.out.flush();
                    }
                    continue;
                }

                lastWasTab = false;
                buffer.append((char) ch);
                System.out.print((char) ch);
                System.out.flush();
            }
        } finally {
            terminalMode.disableRawMode();
        }
    }

    private static List<String> runCompleter(
            String completer, String command, String currentWord,
            String previousWord, String commandLine) {
        List<String> candidates = new ArrayList<>();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    completer, command, currentWord, previousWord);
            processBuilder.environment().put("COMP_LINE", commandLine);
            processBuilder.environment().put(
                    "COMP_POINT",
                    String.valueOf(commandLine.getBytes(java.nio.charset.StandardCharsets.UTF_8).length));
            Process process = processBuilder.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String candidate;
            while ((candidate = reader.readLine()) != null) {
                candidates.add(candidate);
            }
            process.waitFor();
        } catch (Exception ignored) {
        }
        return candidates;
    }

    private static java.util.TreeSet<String> getMatchingCommands(String partial) {
        java.util.TreeSet<String> matches = new java.util.TreeSet<>();
        if (partial.isEmpty()) {
            return matches;
        }
        List<String> builtins = List.of("echo", "exit", "type", "pwd", "cd", "complete");

        for (String b : builtins) {
            if (b.startsWith(partial)) {
                matches.add(b);
            }
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String[] directories = pathEnv.split(File.pathSeparator);
            for (String dir : directories) {
                File folder = new File(dir);
                if (folder.exists() && folder.isDirectory()) {
                    File[] files = folder.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile() && file.canExecute()) {
                                String name = file.getName();
                                if (name.startsWith(partial)) {
                                    matches.add(name);
                                }
                            }
                        }
                    }
                }
            }
        }

        return matches;
    }

    private static String getLongestCommonPrefix(java.util.Collection<String> strings) {
        if (strings.isEmpty()) {
            return "";
        }
        String first = strings.iterator().next();
        int minLen = first.length();
        for (String s : strings) {
            minLen = Math.min(minLen, s.length());
        }
        for (int i = 0; i < minLen; i++) {
            char c = first.charAt(i);
            for (String s : strings) {
                if (s.charAt(i) != c) {
                    return first.substring(0, i);
                }
            }
        }
        return first.substring(0, minLen);
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
