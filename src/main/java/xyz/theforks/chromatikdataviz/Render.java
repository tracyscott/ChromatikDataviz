package xyz.theforks.chromatikdataviz;


/**
 * Simplified rendering interface for strips of points.
 * 
 * @param LX lx The LX instance.
 * @param int[] colors The array of colors to render into.  
 * @param int numFixtureBars The number of bars to render.
 * @param int barNum The index of the bar to render.
 * @param float value Size of the bar from 0 to 1.
 * @param int color The color to render the bar in.
 * @param boolean mirror Whether to mirror the bar.
 */
public class Render {
    static public void renderBarChart(int[] colors, int numFixtureBars, int barNum, float value, int color, boolean mirror) {
        int ledsPerBar = colors.length / numFixtureBars;
        int start = barNum * ledsPerBar;
        int end = start + ledsPerBar;
        
        if (mirror) {
            // For mirrored bars, start from the end of the bar and work backwards
            int valueLength = (int) (value * ledsPerBar);
            int valueStart = end - valueLength;
            if (valueStart < start) {
                valueStart = start;
            }
            for (int i = valueStart; i < end; i++) {
                colors[i] = color;
            }
        } else {
            // Original left-to-right behavior
            int valueEnd = start + (int) (value * ledsPerBar);
            if (valueEnd >= colors.length) {
                valueEnd = colors.length - 1;
            }
            for (int i = start; i < valueEnd; i++) {
                colors[i] = color;
            }
        }
    }
}
