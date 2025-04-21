// First, let's add the Java types we need for file handling
var Files = Java.type("java.nio.file.Files");
var Paths = Java.type("java.nio.file.Paths");
var Charset = Java.type("java.nio.charset.Charset");
var BufferedReader = Java.type("java.io.BufferedReader");
var InputStreamReader = Java.type("java.io.InputStreamReader");
var FileInputStream = Java.type("java.io.FileInputStream");

/**
 * Loads a CSV file and returns an array of objects
 * @param {string} filePath - The path to the CSV file
 * @param {string} delimiter - The delimiter used in the CSV (default: ",")
 * @param {boolean} hasHeader - Whether the CSV has a header row (default: true)
 * @return {Array} Array of objects where keys are column names
 */
function loadCSV(filePath, delimiter, hasHeader) {
  delimiter = delimiter || ",";
  hasHeader = (hasHeader !== false); // Default to true
  
  var result = [];
  var headers = [];
  
  try {
    // Create a BufferedReader for the file
    var fileStream = new FileInputStream(filePath);
    var reader = new BufferedReader(new InputStreamReader(fileStream, Charset.forName("UTF-8")));
    
    var line;
    var lineNumber = 0;
    
    // Read the file line by line
    while ((line = reader.readLine()) != null) {
      // Skip empty lines
      if (line.trim() === "") continue;
      
      // Split the line by delimiter
      var values = line.split(delimiter);
      
      // If this is the first line and we have a header
      if (lineNumber === 0 && hasHeader) {
        // Use header values as keys
        for (var i = 0; i < values.length; i++) {
          headers.push(values[i].trim());
        }
      } else {
        // Create an object for this row
        var row = {};
        
        // If no headers, use indices as keys
        if (headers.length === 0) {
          for (var i = 0; i < values.length; i++) {
            row["column" + i] = values[i].trim();
          }
        } else {
          // Map values to headers
          for (var i = 0; i < Math.min(headers.length, values.length); i++) {
            row[headers[i]] = values[i].trim();
          }
        }
        
        result.push(row);
      }
      
      lineNumber++;
    }
    
    // Close the reader
    reader.close();
    fileStream.close();
    
  } catch (e) {
    System.out.println("Error loading CSV: " + e.message);
    return [];
  }
  
  return result;
}

/**
 * Converts string values to appropriate types (numbers, booleans)
 * @param {Array} data - The array of data objects to convert
 * @return {Array} The converted array
 */
function convertCSVTypes(data) {
  return data.map(function(row) {
    var convertedRow = {};
    
    Object.keys(row).forEach(function(key) {
      var value = row[key];
      
      // Try to convert to number
      if (/^-?\d+(\.\d+)?$/.test(value)) {
        convertedRow[key] = parseFloat(value);
      } 
      // Convert boolean values
      else if (value.toLowerCase() === "true") {
        convertedRow[key] = true;
      }
      else if (value.toLowerCase() === "false") {
        convertedRow[key] = false;
      }
      // Keep as string
      else {
        convertedRow[key] = value;
      }
    });
    
    return convertedRow;
  });
}

/**
 * Renders a bar chart visualization
 * @param {Array} colors - Array of colors to modify
 * @param {number} numFixtureBars - Number of bars to render
 * @param {number} barNum - Index of the current bar
 * @param {number} value - Value to render (0.0 to 1.0)
 * @param {number} color - Color value to set
 * @param {boolean} mirror - Whether to mirror the bar direction
 */
function render_bar_chart(colors, numFixtureBars, barNum, value, color, mirror) {
    const ledsPerBar = Math.floor(colors.length / numFixtureBars);
    const start = barNum * ledsPerBar;
    const end = start + ledsPerBar;
    
    if (mirror) {
        // For mirrored bars, start from the end of the bar and work backwards
        let valueLength = Math.floor(value * ledsPerBar);
        let valueStart = end - valueLength;
        if (valueStart < start) {
            valueStart = start;
        }
        for (let i = valueStart; i < end; i++) {
            colors[i] = color;
        }
    } else {
        // Original left-to-right behavior
        let valueEnd = start + Math.floor(value * ledsPerBar);
        if (valueEnd >= colors.length) {
            valueEnd = colors.length - 1;
        }
        for (let i = start; i < valueEnd; i++) {
            colors[i] = color;
        }
    }
}

/**
 * Loads a CSV file and returns a two-dimensional array of values
 * @param {string} filePath - The path to the CSV file
 * @param {string} delimiter - The delimiter used in the CSV (default: ",")
 * @return {Array<Array<string>>} 2D array of values from the CSV
 */
function loadCSVAsTable(filePath, delimiter) {
  delimiter = delimiter || ",";
  var result = [];
  
  try {
    // Create a BufferedReader for the file
    var fileStream = new FileInputStream(filePath);
    var reader = new BufferedReader(new InputStreamReader(fileStream, Charset.forName("UTF-8")));
    
    var line;
    
    // Read the file line by line
    while ((line = reader.readLine()) != null) {
      // Skip empty lines
      if (line.trim() === "") continue;
      
      // Split the line by delimiter and trim values
      var values = line.split(delimiter).map(function(value) {
        return value.trim();
      });
      
      result.push(values);
    }
    
    // Close the reader
    reader.close();
    fileStream.close();
    
  } catch (e) {
    System.out.println("Error loading CSV: " + e.message);
    return [];
  }
  
  return result;
}