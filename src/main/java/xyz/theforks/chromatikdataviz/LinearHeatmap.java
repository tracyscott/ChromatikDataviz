package xyz.theforks.chromatikdataviz;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.color.LXColor;

/**
 * Renders values from a CSV file as a linear heatmap.  Each row in the CSV file is
 * a new datapoint.  The intensity of each LED is determined by the normalized value
 * of the row datapoint that corresponds the LED's normalized position along the strip and
 * the normalized position of the row datapoint relative to the total number of rows.
 * Hue and Saturation are from ColorParameters that can be set by the user. If there 
 * are at least 4 columns in the CSV file, each strip 
 * of LEDs will be assigned its own dataset.  If there are 3 columns, each strip will
 * receive it's own dataset and one strip will show nothing. If there are 2 columns,
 * each column dataset should be rendered on 2 strips of LEDs.  If there are more
 * than 4 columns, the remaining columns will be ignored.  We can assume that there
 * are 4 strips of LEDs, so their indices can be determined by dividing the length
 * of the colors array by 4.
 */
@LXCategory("Custom")
@LXComponentName("Linear Heatmap")
public class LinearHeatmap extends LXPattern {
    public final DiscreteParameter csvFile =
        new DiscreteParameter("CSV File", new String[] {"test1.csv", "test2.csv"});

    public final ColorParameter barColor1 = new ColorParameter("Bar 1", 0xFFFFC000); // Orange
    public final ColorParameter barColor2 = new ColorParameter("Bar 2", 0xFF00C0FF); // Cyan
    public final ColorParameter barColor3 = new ColorParameter("Bar 3", 0xFF80FF00); // Lime
    public final ColorParameter barColor4 = new ColorParameter("Bar 4", 0xFFFF0080); // Pink

    private CsvData csvData = null;

    public LinearHeatmap(LX lx) {
        super(lx);
        addParameter("csv", this.csvFile);
        addParameter("barColor1", this.barColor1);
        addParameter("barColor2", this.barColor2);
        addParameter("barColor3", this.barColor3);
        addParameter("barColor4", this.barColor4);
        csvFile.setOptions(Datastore.getInstance(lx).getDatasourceNames());
        csvFile.setValue(0);
        csvData = (CsvData) Datastore.getInstance(lx).getDatasource(csvFile.getValuei());
        Datastore.getInstance(lx).acquire();
    }

    @Override
    public void dispose() {
        super.dispose();
        Datastore.getInstance(lx).release();
    }

    @Override
    public void run(double deltaMs) {
        // Rebind the cvsData in case it has been reloaded
        csvData = (CsvData) Datastore.getInstance(lx).getDatasource(csvFile.getValuei());
        if (csvData == null || csvData.getNumRows() == 0) {
            return;
        }

        // Get the number of columns and rows in the CSV file
        int numColumns = csvData.getNumCols();
        int numRows = csvData.getNumRows();
        
        // Calculate the number of LEDs per strip (total length divided by 4 strips)
        int ledsPerStrip = colors.length / 4;
        
        // Process data based on number of columns
        for (int stripIndex = 0; stripIndex < 4; stripIndex++) {
            // Determine which column to use for this strip
            int dataColumn;
            if (numColumns == 2) {
                dataColumn = stripIndex / 2; // Maps columns [0,0,1,1] to strips [0,1,2,3]
            } else if (numColumns == 3 && stripIndex < 3) {
                dataColumn = stripIndex; // Maps columns [0,1,2] to first 3 strips
            } else if (numColumns >= 4) {
                dataColumn = stripIndex; // Maps columns [0,1,2,3] to strips
            } else {
                dataColumn = 0; // Skip if not enough columns
            }
            
            // Skip strip 3 for 3-column case
            if (numColumns == 3 && stripIndex == 3) {
                continue;
            }
            
            // Get the color parameter for this strip
            ColorParameter stripColor = switch (dataColumn) {
                case 0 -> barColor1;
                case 1 -> barColor2;
                case 2 -> barColor3;
                case 3 -> barColor4;
                default -> barColor1;
            };
            
            // Set colors for each LED in the strip
            for (int ledIndex = 0; ledIndex < ledsPerStrip; ledIndex++) {
                // Calculate normalized position of LED in strip (0.0 to 1.0)
                float normalizedLedPos = (float) ledIndex / ledsPerStrip;
                
                // Find the closest row index based on normalized position
                int rowIndex = Math.min((int)(normalizedLedPos * numRows), numRows - 1);
                
                // Get the data value and normalize it (assuming values are numeric)
                float normalizedValue = csvData.getN(dataColumn, rowIndex);
                
                // Calculate the final LED index in the colors array
                int finalLedIndex = stripIndex * ledsPerStrip + ledIndex;
                
                // Set the LED color using the strip's color parameter and the normalized value
                int baseColor = stripColor.getColor();
                colors[finalLedIndex] = LXColor.hsb(
                    LXColor.h(baseColor),
                    LXColor.s(baseColor),
                    100 * normalizedValue // brightness based on normalized value
                );
            }
        }
    }
}