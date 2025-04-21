include("csv.js")


/**
 * Define knobs and toggles with a variable name, label, description and default value
 * The variable name will be populated for the render method with a value from 0-1 for
 * knobs and true/false for toggles.
 */
knob("delay", "Delay", "Delay between bars", 0);
toggle("mirror", "Mirror", "Mirror the bars", false);
knob("hue1", "Hue 1", "Hue 1", 0);
knob("hue2", "Hue 2", "Hue 2", 0);
knob("hue3", "Hue 3", "Hue 3", 0);
knob("hue4", "Hue 4", "Hue 4", 0);


var csvData;
var table;
var timeSinceLastFrame = 0;
var frameNumber = 0;

var init = function(model) {
  table = loadCSVAsTable("C:\\Users\\tracy\\Chromatik\\dataviz\\test1.csv", ",");
};

var run = function(deltaMs, nowMillis, model, colors, enabledAmount) {
  var color1 = hsb(hue1 * 360, 100, 100);
  var color2 = hsb(hue2 * 360, 100, 100);
  var color3 = hsb(hue3 * 360, 100, 100);
  var color4 = hsb(hue4 * 360, 100, 100);
  render_bar_chart(colors, 4, 0, table[frameNumber][0], color1, mirror); 
  render_bar_chart(colors, 4, 1, table[frameNumber][1], color2, mirror); 
  render_bar_chart(colors, 4, 2, table[frameNumber][2], color3, mirror); 
  render_bar_chart(colors, 4, 3, table[frameNumber][3], color4 , mirror); 
  timeSinceLastFrame += deltaMs;
  if (timeSinceLastFrame >= delay*10000) {
    frameNumber++;
    timeSinceLastFrame = 0;
  }
  if (frameNumber >= table.length) {
    frameNumber = 0;
  }
}


