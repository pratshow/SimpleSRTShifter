import java.io.*;
import java.util.regex.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;

public class SRTShift {
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})");
    private static final long TWO_HOURS_IN_MILLIS = 2 * 60 * 60 * 1000;

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: java SRTShift <input_directory> <output_directory> <seconds_to_shift> <pause_length>");
            System.out.println("Use positive or negative decimal numbers to shift forward/backward (e.g., -9.5)");
            return;
        }

        Path inputPath = Paths.get(args[0]);
        Path outputPath = Paths.get(args[1]);
        double secondsToShift = Double.parseDouble(args[2]);
        long PAUSE_THRESHOLD = (long)(Double.parseDouble(args[3]) * 1000); // Convert to milliseconds

        File folderPath = new File(args[0]);
        File[] listOfFiles = folderPath.listFiles();
        
        if (listOfFiles == null) {
            System.err.println("Error: Invalid directory or directory is empty");
            return;
        }

        for (File file : listOfFiles) {
            if (!file.getName().toLowerCase().endsWith(".srt")) {
                continue; // Skip non-SRT files
            }

            try {
                // Read all lines from file
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                System.out.println("Processing: " + file.getName());
                System.out.println("Read " + lines.size() + " lines");

                // First pass: analyze timestamps to find long pauses
                List<TimeEntry> timeEntries = new ArrayList<>();
                long pauseStartTime = -1;
                
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    Matcher matcher = TIME_PATTERN.matcher(line);
                    if (matcher.find()) {
                        // Parse start and end times
                        String[] times = line.split(" --> ");
                        long startTime = parseTimeToMillis(times[0]);
                        long endTime = parseTimeToMillis(times[1]);
                        
                        if (!timeEntries.isEmpty()) {
                            long gap = startTime - timeEntries.get(timeEntries.size() - 1).endTime;
                            if (gap >= PAUSE_THRESHOLD && pauseStartTime == -1) {
                                pauseStartTime = startTime;
                                System.out.println("Found pause of " + (gap/1000.0) + " seconds at " + 
                                                 convertToTimestamp(startTime));
                            }
                        }
                        
                        timeEntries.add(new TimeEntry(startTime, endTime, i));
                    }
                }

                // Process and write output
                List<String> outputLines = new ArrayList<>();
                int modifiedLines = 0;

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    Matcher matcher = TIME_PATTERN.matcher(line);
                    if (matcher.find()) {
                        boolean shouldShift = pauseStartTime == -1 || 
                            parseTimeToMillis(line.split(" --> ")[0]) >= pauseStartTime;
                        
                        if (shouldShift) {
                            String adjustedLine = adjustTimestamp(line, secondsToShift);
                            outputLines.add(adjustedLine);
                            modifiedLines++;
                            System.out.println("Modified: " + line + " -> " + adjustedLine);
                        } else {
                            outputLines.add(line);
                            System.out.println("Kept original: " + line);
                        }
                    } else {
                        outputLines.add(line);
                    }
                }

                Files.createDirectories(outputPath);
                Files.write(outputPath.resolve(file.getName()), outputLines, StandardCharsets.UTF_8);
                System.out.println("Successfully processed " + file.getName());
                System.out.println("Modified " + modifiedLines + " timestamp lines");

            } catch (IOException e) {
                System.err.println("Error processing file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static class TimeEntry {
        long startTime;
        long endTime;
        int lineIndex;

        TimeEntry(long startTime, long endTime, int lineIndex) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.lineIndex = lineIndex;
        }
    }

    private static String adjustTimestamp(String line, double secondsToShift) {
        Matcher matcher = TIME_PATTERN.matcher(line);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            long timestamp = convertToMillis(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                Integer.parseInt(matcher.group(4))
            );
            
            // Handle both positive and negative shifts with decimal precision
            long adjustedTime = timestamp + (long)(secondsToShift * 1000.0);
            
            // Handle negative times by adding 2 hours
            if (adjustedTime < 0) {
                adjustedTime = TWO_HOURS_IN_MILLIS + adjustedTime;
            }
            
            String newTimestamp = convertToTimestamp(adjustedTime);
            matcher.appendReplacement(result, newTimestamp);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    private static long parseTimeToMillis(String timeStr) {
        Matcher matcher = TIME_PATTERN.matcher(timeStr);
        if (matcher.find()) {
            return convertToMillis(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                Integer.parseInt(matcher.group(4))
            );
        }
        return 0;
    }

    private static long convertToMillis(int hours, int minutes, int seconds, int millis) {
        return ((hours * 60L * 60L * 1000L) +
                (minutes * 60L * 1000L) +
                (seconds * 1000L) +
                millis);
    }

    private static String convertToTimestamp(long millis) {
        long hours = millis / (60 * 60 * 1000);
        millis %= (60 * 60 * 1000);
        long minutes = millis / (60 * 1000);
        millis %= (60 * 1000);
        long seconds = millis / 1000;
        millis %= 1000;

        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis);
    }
}