package xyz.theforks.chromatikdataviz;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import heronarts.lx.LX;

/**
 * Manages datasources.  Provides a mechanism for registering datasources and
 * retrieving by name.  Also provides a reload mechanism.
 */
public class Datastore {
    // Singleton instance
    private static volatile Datastore INSTANCE;
    private final LX lx;
    private int refCount = 0;
    
    // Private constructor to prevent instantiation
    private Datastore(LX lx) {
        this.lx = lx;
        initializeDatastore();
    }
    
    private void initializeDatastore() {
        try {
            DatavizUtil.exportDatavizFiles(lx);
            
            // Scan directory and load CSV files
            File dataVizDir = new File(DatavizUtil.getDatavizDir(lx));
            if (dataVizDir.exists() && dataVizDir.isDirectory()) {
                File[] csvFiles = dataVizDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
                if (csvFiles != null) {
                    for (File csvFile : csvFiles) {
                        String name = csvFile.getName().replaceFirst("[.][^.]+$", ""); // Remove extension
                        CsvData datasource = CsvData.load(csvFile.getAbsolutePath());
                        register(name, datasource, true, 60000); // Default 1-minute refresh
                    }
                }
            }
            LX.log("ChromatikDataviz Datastore initialized with " + datasources.size() + " csv files.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Datastore", e);
        }
    }
    
    // Public method to get the singleton instance
    public static synchronized Datastore getInstance(LX lx) {
        if (INSTANCE == null) {
            if (lx == null) {
                throw new IllegalArgumentException("LX instance cannot be null");
            }
            INSTANCE = new Datastore(lx);
        }
        return INSTANCE;
    }

    public static synchronized Datastore getInstance() {
        return getInstance(null);
    }

    public class DatasourceEntry {
        private Datasource datasource;
        private final boolean reload;
        private final float refreshIntervalMs;
        
        public DatasourceEntry(Datasource datasource, boolean reload, float refreshIntervalMs) {
            this.datasource = datasource;
            this.reload = reload;
            this.refreshIntervalMs = refreshIntervalMs;
        }
        
        public void reload() {
            Datasource reloaded = datasource.reload();
            if (reloaded != null) {
                datasource = reloaded;
            }
        }
    }

    private final Map<String, DatasourceEntry> datasources = new HashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    
    public Datasource getDatasource(String name) {
        return datasources.get(name).datasource;
    }

    public String[] getDatasourceNames() {
        List<String> sortedNames = new ArrayList<>(datasources.keySet());
        Collections.sort(sortedNames);
        return sortedNames.toArray(new String[0]);
    }

    public Datasource getDatasource(int namesIndex) {
        if (namesIndex < 0 || namesIndex >= getDatasourceNames().length) {
            return null;
        }
        return datasources.get(getDatasourceNames()[namesIndex]).datasource;
    }

    public void register(String name, Datasource datasource, boolean reload, float refreshIntervalMs) {
        if (datasource == null) {
            throw new IllegalArgumentException("Datasource cannot be null");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (refreshIntervalMs <= 0) {
            throw new IllegalArgumentException("Refresh interval must be greater than 0");
        }
        
        // Check if datasource is already registered
        if (datasources.containsKey(name)) {
            throw new IllegalArgumentException("Datasource already registered: " + name);
        }
        
        // Create a new datasource entry
        DatasourceEntry entry = new DatasourceEntry(datasource, reload, refreshIntervalMs);
        
        // Add the entry to the map
        datasources.put(name, entry);
        
        // Schedule the first reload if needed
        if (reload) {
            scheduleReload(name, refreshIntervalMs);
        }   
    }

    public void unregister(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        // Check if datasource is registered
        if (!datasources.containsKey(name)) {
            throw new IllegalArgumentException("Datasource not registered: " + name);
        }
        
        // Remove the entry from the map
        datasources.remove(name);
    }

    public void scheduleReload(String name, float refreshIntervalMs) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (refreshIntervalMs <= 0) {
            throw new IllegalArgumentException("Refresh interval must be greater than 0");
        }
        
        // Check if datasource is registered
        if (!datasources.containsKey(name)) {
            throw new IllegalArgumentException("Datasource not registered: " + name);
        }
        
        // Schedule the reload
        Runnable reloadTask = () -> {
            DatasourceEntry entry = datasources.get(name);
            if (entry != null) {
                entry.reload();
            }
        };
        
        // Schedule the task to run periodically    
        executor.scheduleAtFixedRate(reloadTask, 0L, (long)refreshIntervalMs, TimeUnit.MILLISECONDS);
    }

    public void reload(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty"); 
        }
        
        // Check if datasource is registered
        if (!datasources.containsKey(name)) {
            throw new IllegalArgumentException("Datasource not registered: " + name);
        } 
        
        // Get the datasource entry
        DatasourceEntry entry = datasources.get(name);
        if (entry == null) {
            throw new IllegalArgumentException("Datasource not registered: " + name);
        }
        
        // Reload the datasource
        entry.reload();
    }
    
    public void shutdown() {
        executor.shutdown();
    }

    public void shutdownNow() {
        executor.shutdownNow();
    }

    // Since we don't have a way to hook the shutdown event, we need to manually manage
    // the reference count to a datstore so that once all the patterns requiring a
    // datastore have released their reference, the datastore will be disposed.
    // Otherwise, our background executor will keep the java process alive indefinitely.
    public void acquire() {
        ++refCount;
    }

    public void release() {
        --refCount;
        if (refCount <= 0) {
            dispose();
        }
    }

    public void dispose() {
        // Clean up resources
        shutdown();
    }
}
