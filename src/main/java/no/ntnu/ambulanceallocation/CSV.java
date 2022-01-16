package no.ntnu.ambulanceallocation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CSV {

    public void saveDataToFile(String fileName, List<List<String>> list) {
        saveDataToFile(fileName, null, list);
    }

    public void saveDataToFile(String fileName, List<String> header, List<List<String>> data) {
        File file = new File(String.format("output/simulation/%s.csv", fileName));

        try (PrintWriter printWriter = new PrintWriter(file)) {
            int rows = data.stream().mapToInt(List::size).max().orElse(0);

            if (header != null && header.size() > 0) {
                printWriter.println(convertToCSV(header));
            } else {
                printWriter.println(fileName);
            }

            for (int row = 0; row < rows; row++) {
                List<String> line = new ArrayList<>();

                for (List<String> column : data) {
                    try {
                        line.add(column.get(row));
                    } catch (IndexOutOfBoundsException exception) {
                        line.add("");
                    }
                }

                if (row < rows - 1) {
                    printWriter.println(convertToCSV(line));
                } else {
                    printWriter.print(convertToCSV(line));
                }
            }

        } catch (IOException exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }

    private String escapeSpecialCharacters(String text) {
        String escapedData = text.replaceAll("\\R", " ");
        if (text.contains(",") || text.contains("\"") || text.contains("'")) {
            text = text.replace("\"", "\"\"");
            escapedData = "\"" + text + "\"";
        }
        return escapedData;
    }

    private String convertToCSV(List<String> text) {
        return text
                .stream()
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

}
