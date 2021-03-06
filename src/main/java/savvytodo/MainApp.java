package savvytodo;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import com.google.common.eventbus.Subscribe;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import savvytodo.commons.core.Config;
import savvytodo.commons.core.EventsCenter;
import savvytodo.commons.core.LogsCenter;
import savvytodo.commons.core.Version;
import savvytodo.commons.events.ui.ExitAppRequestEvent;
import savvytodo.commons.exceptions.DataConversionException;
import savvytodo.commons.util.ConfigUtil;
import savvytodo.commons.util.StringUtil;
import savvytodo.logic.Logic;
import savvytodo.logic.LogicManager;
import savvytodo.model.Model;
import savvytodo.model.ModelManager;
import savvytodo.model.ReadOnlyTaskManager;
import savvytodo.model.TaskManager;
import savvytodo.model.UserPrefs;
import savvytodo.model.util.SampleDataUtil;
import savvytodo.storage.Storage;
import savvytodo.storage.StorageManager;
import savvytodo.ui.Ui;
import savvytodo.ui.UiManager;

/**
 * The main entry point to the application.
 */
public class MainApp extends Application {
    private static final Logger logger = LogsCenter.getLogger(MainApp.class);

    public static final Version VERSION = new Version(5, 0, 0, true);

    private static MainApp runningInstance = null;

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;
    protected Config config;
    protected UserPrefs userPrefs;

    public String configFile = Config.DEFAULT_CONFIG_FILE;

    //@@author A0140036X
    /**
     * Gets running JavaFX Application instance
     * @return
     */
    public static MainApp getRunningInstance() {
        return runningInstance;
    }

    @Override
    public void init() throws Exception {
        logger.info(
                "=============================[ Initializing Task Manager ]===========================");
        super.init();

        runningInstance = this;

        initEventsCenter();

        initApplicationFromConfig(getApplicationParameter("config"), true);
    }

    //@@author A0140036X
    /**
     * Sets up application UI.
     * If useSampleDataIfStorageFileNotFound is true sample data will be loaded if storage file is not found.
     * @author A0140036X
     * @param configFilePath File path of json file containing configurations
     * @param useSampleDataIfStorageFileNotFound
     */
    public void initApplicationFromConfig(String configFilePath,
            boolean useSampleDataIfStorageFileNotFound) {
        config = initConfig(configFilePath);

        storage = new StorageManager(config.getTaskManagerFilePath(),
                config.getUserPrefsFilePath());

        userPrefs = initPrefs(config);

        initLogging(config);

        model = initModelManager(storage, userPrefs,
                useSampleDataIfStorageFileNotFound ? null : new TaskManager());
        logic = new LogicManager(model, storage);
        ui = new UiManager(logic, config, userPrefs);
    }

    private String getApplicationParameter(String parameterName) {
        Map<String, String> applicationParameters = getParameters().getNamed();
        return applicationParameters.get(parameterName);
    }

    //@@author A0140036X
    /**
     * Initializes model based on storage.
     * If storage file is not found, default task manager provided will be used.
     * If task manager is null, sample task manager will be created.
     * @param storage Storage that is to be used by model
     * @param userPrefs User preferences for application
     * @param defaultTaskManager see method description
     * @return initialized Model
     */
    private Model initModelManager(Storage storage, UserPrefs userPrefs,
            TaskManager defaultTaskManager) {
        ReadOnlyTaskManager initialData;
        try {
            initialData = getTaskManagerFromStorage(storage, defaultTaskManager);
        } catch (DataConversionException e) {
            logger.warning(
                    "Data file not in the correct format. Will be starting with an empty TaskManager");
            initialData = new TaskManager();
        } catch (IOException e) {
            logger.warning(
                    "Problem while reading from the file. Will be starting with an empty TaskManager");
            initialData = new TaskManager();
        }

        return new ModelManager(initialData, userPrefs);
    }

    //@@author A0140036X
    private ReadOnlyTaskManager getTaskManagerFromStorage(Storage storage2,
            ReadOnlyTaskManager defaultTaskManager) throws DataConversionException, IOException {
        Optional<ReadOnlyTaskManager> taskManagerOptional = storage.readTaskManager();
        if (!taskManagerOptional.isPresent()) {
            logger.info("Data file not found. Will be starting with "
                    + ((defaultTaskManager == null ? "a sample " : "provided ") + "TaskManager"));
        }
        logger.info("Data file found " + storage.getTaskManagerFilePath());
        ReadOnlyTaskManager initialData = taskManagerOptional.orElseGet(defaultTaskManager == null
                ? new SampleDataUtil()::getSampleTaskManager : () -> new TaskManager());
        return initialData;
    }

    private void initLogging(Config config) {
        LogsCenter.init(config);
    }

    /**
     * Loads config file from configFilePath. If not specified, MainApp.configFile will be used.
     * @param configFilePath
     * @return initialized Config
     */
    protected Config initConfig(String configFilePath) {
        Config initializedConfig;

        if (configFilePath != null) {
            logger.info("Custom Config file specified " + configFilePath);
            configFile = configFilePath;
        }

        logger.info("Using config file : " + configFile);

        try {
            Optional<Config> configOptional = ConfigUtil.readConfig(configFile);
            initializedConfig = configOptional.orElse(new Config());
        } catch (DataConversionException e) {
            logger.warning("Config file at " + configFile + " is not in the correct format. "
                    + "Using default config properties");
            initializedConfig = new Config();
        }

        //Update config file in case it was missing to begin with or there are new/unused fields
        try {
            ConfigUtil.saveConfig(initializedConfig, configFile);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }
        return initializedConfig;
    }

    /**
     * Initialized user preferences from file stored in a Config object.
     * Guarantees a UserPrefs
     * @param config
     * @return user preferences
     */
    protected UserPrefs initPrefs(Config config) {
        assert config != null;

        String prefsFilePath = config.getUserPrefsFilePath();
        logger.info("Using prefs file : " + prefsFilePath);

        UserPrefs initializedPrefs = loadUserPrefsFromFile(prefsFilePath);

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        saveUserPrefs(initializedPrefs);

        return initializedPrefs;
    }

    //@@author A0140036X
    /**
     * Attempt to load user preferences from file
     * Guarantees a UserPrefs even if file not found
     * @return user preferences
     */
    private UserPrefs loadUserPrefsFromFile(String prefsFilePath) {
        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataConversionException e) {
            logger.warning("UserPrefs file at " + prefsFilePath + " is not in the correct format. "
                    + "Using default user prefs");
            initializedPrefs = new UserPrefs();
        } catch (IOException e) {
            logger.warning(
                    "Problem while reading from the file. Will be starting with an empty TaskManager");
            initializedPrefs = new UserPrefs();
        }
        return initializedPrefs;
    }

    //@@author A0140036X
    /**
     * Attempt to save user preferences to storage
     * @param prefs
     * @return true if successful
     */
    protected boolean saveUserPrefs(UserPrefs prefs) {
        try {
            storage.saveUserPrefs(prefs);
            return true;
        } catch (IOException e) {
            logger.warning("Failed to save preference file : " + StringUtil.getDetails(e));
            return false;
        }
    }

    private void initEventsCenter() {
        EventsCenter.getInstance().registerHandler(this);
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting TaskManager " + MainApp.VERSION);
        ui.start(primaryStage);
    }

    @Override
    public void stop() {
        logger.info(
                "============================ [ Stopping Task Manager ] =============================");
        ui.stop();
        try {
            storage.saveUserPrefs(userPrefs);
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        }
        Platform.exit();
        System.exit(0);
    }

    @Subscribe
    public void handleExitAppRequestEvent(ExitAppRequestEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        this.stop();
    }

    //@@author A0140036X
    /**
     * Loads a new task manager file.
     * 1. Update and save config file with new storage file path
     * 2. Update UI with new logic
     * @throws IOException
     * @throws DataConversionException
     */
    public void loadTaskManagerFile(String filePath) throws DataConversionException, IOException {
        logger.info("Loading new file " + filePath);
        storage.setTaskManagerStorageFilePath(filePath);
        model.resetData(getTaskManagerFromStorage(storage, new TaskManager()));
        config.setTaskManagerFilePath(filePath);
        saveConfig();
        ui.refresh();
    }

    //@@author A0140036X
    /**
     * Saves config to file.
     */
    private void saveConfig() {
        try {
            ConfigUtil.saveConfig(config, configFile);
        } catch (IOException e) {
            logger.severe("Failed to save config " + StringUtil.getDetails(e));
            this.stop();
        }
    }

    //@@author A0140036X
    /**
     * Saves storage to filepath.
     * @throws IOException Error saving task manager
     * @throws DataConversionException
     */
    public void saveTaskManagerToFile(String filePath) throws IOException, DataConversionException {
        logger.info("Saving to file " + filePath);
        storage.saveTaskManager(model.getTaskManager(), filePath);
        loadTaskManagerFile(filePath);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
