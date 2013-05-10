package experiments;

import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.util.Properties;

import util.PropertiesUtil;

import com.pushtechnology.diffusion.api.Logs;

/**
 * A main class for all experiments.
 * 
 * @author nitsanw
 * 
 */
public final class ExperimentRunner {
    /**
     * Never create me...
     */
    private ExperimentRunner() {

    }

    /**
     * @param args expect experiment class name
     * @throws Exception on grrr
     */
    public static void main(final String[] args) throws Exception {
        try {
            Class<?> experimentClass = Class.forName(args[0]);
            Class<?> settingsClass = CommonExperimentSettings.class;
            try {
                String settingsClassName = args[0] + "$Settings";
                settingsClass = Class.forName(settingsClassName);
                Logs.info("Using " + settingsClassName + " settings class...");
            } catch (ClassNotFoundException e) {
                Logs.info("Using default settings class...");
            }
            Properties experimentProperties = PropertiesUtil.load(args[1]);
            CommonExperimentSettings settings =
                    (CommonExperimentSettings) settingsClass.getConstructor(
                            Properties.class).newInstance(
                            experimentProperties);
            FileOutputStream out = new FileOutputStream(args[1]);
            experimentProperties.store(out, "");
            out.close();

            Constructor<?> constructor =
                    experimentClass.
                            getConstructor(settingsClass);

            Runnable experiment = (Runnable) constructor.newInstance(settings);
            experiment.run();
        } finally {
            System.exit(0);
        }
        
    }
}
