package no.ntnu.ambulanceallocation.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import no.ntnu.ambulanceallocation.CSV;

public class Result {

    private final List<String> header = Collections.synchronizedList(new ArrayList<>());
    private final List<List<String>> results = Collections.synchronizedList(new ArrayList<>());
    private final CSV CSV = new CSV();

    public void saveColumn(String columnName, List<?> column) {
        if (!header.contains(columnName)) {
            header.add(columnName);
            results.add(column.stream().map(Object::toString).toList());
        }
    }

    public void saveResults(String filename) {
        CSV.saveDataToFile(filename, header, results);
    }

}
