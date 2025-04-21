package xyz.theforks.chromatikdataviz;

public interface Datasource {
    
   public Datasource reload();
   
  /**
     * Returns the floating point value at the given column and row.
     * @param column Zero-based column index.
     * @param row Zero-based row index.
     * @return The value at (column, row).
     * @throws IndexOutOfBoundsException If indices are out of bounds.
     */
    public float get(int column, int row);

    /**
     * Returns the normalized value (0..1) at the given column and row.
     * 1.0 is the max value across all data.
     * @param column Zero-based column index.
     * @param row Zero-based row index.
     * @return The normalized value at (column, row).
     * @throws IndexOutOfBoundsException If indices are out of bounds.
     */
    public float getN(int column, int row);


    /**
     * @return Number of rows in the data.
     */
    public int getNumRows();
    /**
     * @return Number of columns in the data.
     */
    public int getNumCols();
    // --- Column statistics for raw values ---

    public float getColumnAverage(int col);

    public float getColumnStdDev(int col);

    public float getColumnMedian(int col);

    public float getColumnMin(int col);

    public float getColumnMax(int col);

    // --- Column statistics for normalized values ---

    public float getColumnAverageN(int col);

    public float getColumnStdDevN(int col);

    public float getColumnMedianN(int col);

    public float getColumnMinN(int col);

    public float getColumnMaxN(int col); 
}
