package xyz.theforks.chromatikdataviz;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.pattern.LXPattern;

/**
 * LXPattern that reads a CSV file and renders a bar chart.
 * Each row in the CSV is a frame of animation.
 * Uses CsvData for data loading and access.
 */
@LXCategory("Custom")
@LXComponentName("CSV Bar Chart")
public class CsvBarChartPattern extends LXPattern {

  public final DiscreteParameter csvFile =
    new DiscreteParameter("CSV File", new String[] {"test1.csv", "test2.csv"});

  public final CompoundParameter frameDelayMs =
    new CompoundParameter("FrameDelay", 500, 100, 5000)
      .setDescription("Delay between animation frames in milliseconds");

  public enum InterpolationMode {
    Jump, Linear, Smooth
  }
  public final EnumParameter<InterpolationMode> interpolationMode =
    new EnumParameter<InterpolationMode>("Interp", InterpolationMode.Jump)
      .setDescription("Bar interpolation: Jump, Linear, or Smooth");

  public final BooleanParameter mirrorBars =
    new BooleanParameter("Mirror", false)
      .setDescription("Mirror the bars");

  public final ColorParameter barColor1 = new ColorParameter("Bar 1", 0xFFFFC000); // Orange
  public final ColorParameter barColor2 = new ColorParameter("Bar 2", 0xFF00C0FF); // Cyan
  public final ColorParameter barColor3 = new ColorParameter("Bar 3", 0xFF80FF00); // Lime
  public final ColorParameter barColor4 = new ColorParameter("Bar 4", 0xFFFF0080); // Pink

  private CsvData csvData = null;

  private int frameIndex = 0;
  private int prevFrameIndex = 0;
  private double elapsedMs = 0;

  public CsvBarChartPattern(LX lx) {
    super(lx);
    addParameter("csv", this.csvFile);
    addParameter("frameDelayMs", this.frameDelayMs);
    addParameter("interpolationMode", this.interpolationMode);
    addParameter("mirror", this.mirrorBars);
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
  protected void run(double deltaMs) {
    // Rebind the cvsData in case it has been reloaded
    csvData = (CsvData) Datastore.getInstance(lx).getDatasource(csvFile.getValuei());
    if (csvData == null || csvData.getNumRows() == 0) {
      LX.log("CSV data is null or empty");
      return;
    }

    elapsedMs += deltaMs;
    double delay = frameDelayMs.getValue();
    double progress = Math.min(elapsedMs / delay, 1.0);

    if (elapsedMs >= delay) {
      prevFrameIndex = frameIndex;
      frameIndex = (frameIndex + 1) % csvData.getNumRows();
      elapsedMs = 0;
      progress = 0.0;
    }

    int numBars = csvData.getNumCols();
    float[] barsPrev = new float[numBars];
    float[] barsNext = new float[numBars];
    for (int i = 0; i < numBars; ++i) {
      barsPrev[i] = csvData.get(i, prevFrameIndex);
      barsNext[i] = csvData.get(i, frameIndex);
    }

    InterpolationMode mode = InterpolationMode.values()[interpolationMode.getValuei()];

    for (int i = 0; i < numBars; ++i) {
      float value;
      switch (mode) {
        case Linear: {
          float t = (float) progress;
          value = barsPrev[i] + (barsNext[i] - barsPrev[i]) * t;
          break;
        }
        case Smooth: {
          float t = (float) progress;
          float s = 3 * t * t - 2 * t * t * t;
          value = barsPrev[i] + (barsNext[i] - barsPrev[i]) * s;
          break;
        }
        case Jump:
        default:
          value = barsNext[i];
          break;
      }
      
      int baseColor;
      switch (i) {
        case 0: baseColor = barColor1.getColor(); break;
        case 1: baseColor = barColor2.getColor(); break;
        case 2: baseColor = barColor3.getColor(); break;
        case 3: baseColor = barColor4.getColor(); break;
        default: baseColor = 0xFFFFFFFF; break;
      }
      
      Render.renderBarChart(colors, 4, i, value, baseColor, mirrorBars.getValueb());
    }
  }
}
