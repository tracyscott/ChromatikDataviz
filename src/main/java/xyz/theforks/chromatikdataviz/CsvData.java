package xyz.theforks.chromatikdataviz;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import heronarts.lx.LX;

/**
 * CsvData loads a CSV file of floating point values and provides access to raw and normalized data.
 */
public class CsvData implements Datasource {
    private float[][] data;
    private float[][] normalizedData;
    private int numRows;
    private int numCols;
    private String filename;
    private long lastModified;

    /*
     * Reloads the data from the file.  This method will be periodically invoked by the
     * Datasource.
     */
    public CsvData reload() {
        File file = new File(filename);
        if (file.exists() && file.lastModified() > lastModified) {
            try {
                return load(filename);
            } catch (IOException e) {
                LX.log("Failed to reload CSV data from " + filename);
            }
        }
        return null;
    }

    /**
     * Loads a CSV file. All values are parsed as floats.
     * @param filename Path to the CSV file.
     * @throws IOException If file cannot be read or parsed.
     */
    static public CsvData load(String filename) throws IOException {
    
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            CsvData csvData =loadFromBufferedReader(br);
            csvData.filename = filename;
            return csvData;
        }
    }

    /**
     * Loads CSV data from an InputStream (e.g., resource).
     * @param is InputStream to read CSV data from.
     * @throws IOException If the stream cannot be read or parsed.
     */
    public static CsvData loadResource(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new java.io.InputStreamReader(is))) {
            return loadFromBufferedReader(br);
        }
    }

    /**
     * Internal helper to load CSV data from a BufferedReader.
     */
    private static CsvData loadFromBufferedReader(BufferedReader br) throws IOException {
        ArrayList<float[]> rows = new ArrayList<>();
        String line;
        CsvData csvData = new CsvData();
        while ((line = br.readLine()) != null) {
            // Skip empty lines
            if (line.trim().isEmpty()) continue;
            String[] tokens = line.split(",");
            float[] row = new float[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                row[i] = Float.parseFloat(tokens[i].trim());
            }
            rows.add(row);
        }
        csvData.numRows = rows.size();
        csvData.numCols = csvData.numRows > 0 ? rows.get(0).length : 0;
        csvData.data = new float[csvData.numRows][csvData.numCols];
        for (int r = 0; r < csvData.numRows; r++) {
            csvData.data[r] = rows.get(r);
        }
        csvData.computeNormalized();
        return csvData;
    }

    /**
     * Returns the floating point value at the given column and row.
     * @param column Zero-based column index.
     * @param row Zero-based row index.
     * @return The value at (column, row).
     * @throws IndexOutOfBoundsException If indices are out of bounds.
     */
    public float get(int column, int row) {
        if (row < 0 || row >= numRows || column < 0 || column >= numCols) {
            throw new IndexOutOfBoundsException("Invalid row or column index");
        }
        return data[row][column];
    }

    /**
     * Returns the normalized value (0..1) at the given column and row.
     * 1.0 is the max value across all data.
     * @param column Zero-based column index.
     * @param row Zero-based row index.
     * @return The normalized value at (column, row).
     * @throws IndexOutOfBoundsException If indices are out of bounds.
     */
    public float getN(int column, int row) {
        if (row < 0 || row >= numRows || column < 0 || column >= numCols) {
            throw new IndexOutOfBoundsException("Invalid row or column index");
        }
        return normalizedData[row][column];
    }

    /**
     * Computes normalized values for all data (0..1, 1 = max value).
     */
    private void computeNormalized() {
        float max = Float.NEGATIVE_INFINITY;
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                if (data[r][c] > max) {
                    max = data[r][c];
                }
            }
        }
        normalizedData = new float[numRows][numCols];
        if (max == 0.0f || max == Float.NEGATIVE_INFINITY) {
            // All values are zero or no data; set all normalized to 0
            for (int r = 0; r < numRows; r++) {
                for (int c = 0; c < numCols; c++) {
                    normalizedData[r][c] = 0.0f;
                }
            }
        } else {
            for (int r = 0; r < numRows; r++) {
                for (int c = 0; c < numCols; c++) {
                    normalizedData[r][c] = data[r][c] / max;
                }
            }
        }
    }

    /**
     * @return Number of rows in the data.
     */
    public int getNumRows() {
        return numRows;
    }

    /**
     * @return Number of columns in the data.
     */
    public int getNumCols() {
        return numCols;
    }

    // --- Column statistics for raw values ---

    public float getColumnAverage(int col) {
        checkCol(col);
        float sum = 0.0f;
        for (int r = 0; r < numRows; r++) {
            sum += data[r][col];
        }
        return sum / numRows;
    }

    public float getColumnStdDev(int col) {
        checkCol(col);
        float mean = getColumnAverage(col);
        float sumSq = 0.0f;
        for (int r = 0; r < numRows; r++) {
            float diff = data[r][col] - mean;
            sumSq += diff * diff;
        }
        return (float) Math.sqrt(sumSq / (numRows - 1));
    }

    public float getColumnMedian(int col) {
        checkCol(col);
        float[] vals = new float[numRows];
        for (int r = 0; r < numRows; r++) {
            vals[r] = data[r][col];
        }
        java.util.Arrays.sort(vals);
        if (numRows % 2 == 1) {
            return vals[numRows / 2];
        } else {
            return (vals[numRows / 2 - 1] + vals[numRows / 2]) / 2.0f;
        }
    }

    public float getColumnMin(int col) {
        checkCol(col);
        float min = Float.POSITIVE_INFINITY;
        for (int r = 0; r < numRows; r++) {
            if (data[r][col] < min) min = data[r][col];
        }
        return min;
    }

    public float getColumnMax(int col) {
        checkCol(col);
        float max = Float.NEGATIVE_INFINITY;
        for (int r = 0; r < numRows; r++) {
            if (data[r][col] > max) max = data[r][col];
        }
        return max;
    }

    // --- Column statistics for normalized values ---

    public float getColumnAverageN(int col) {
        checkCol(col);
        float sum = 0.0f;
        for (int r = 0; r < numRows; r++) {
            sum += normalizedData[r][col];
        }
        return sum / numRows;
    }

    public float getColumnStdDevN(int col) {
        checkCol(col);
        float mean = getColumnAverageN(col);
        float sumSq = 0.0f;
        for (int r = 0; r < numRows; r++) {
            float diff = normalizedData[r][col] - mean;
            sumSq += diff * diff;
        }
        return (float) Math.sqrt(sumSq / (numRows - 1));
    }

    public float getColumnMedianN(int col) {
        checkCol(col);
        float[] vals = new float[numRows];
        for (int r = 0; r < numRows; r++) {
            vals[r] = normalizedData[r][col];
        }
        java.util.Arrays.sort(vals);
        if (numRows % 2 == 1) {
            return vals[numRows / 2];
        } else {
            return (vals[numRows / 2 - 1] + vals[numRows / 2]) / 2.0f;
        }
    }

    public float getColumnMinN(int col) {
        checkCol(col);
        float min = Float.POSITIVE_INFINITY;
        for (int r = 0; r < numRows; r++) {
            if (normalizedData[r][col] < min) min = normalizedData[r][col];
        }
        return min;
    }

    public float getColumnMaxN(int col) {
        checkCol(col);
        float max = Float.NEGATIVE_INFINITY;
        for (int r = 0; r < numRows; r++) {
            if (normalizedData[r][col] > max) max = normalizedData[r][col];
        }
        return max;
    }

    // --- Helper ---

    private void checkCol(int col) {
        if (col < 0 || col >= numCols) {
            throw new IndexOutOfBoundsException("Invalid column index");
        }
    }
}
