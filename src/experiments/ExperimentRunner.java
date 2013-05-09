package experiments;

import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.Properties;

import util.PropertiesUtil;

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
        Class<?> experimentClass = Class.forName(args[0]);
        Class<?> settingsClass = CommonExperimentSettings.class;
        try {
            String settingsClassName = args[0] + "$Settings";
            settingsClass = Class.forName(settingsClassName);
            System.out.println(new Date()
                    + " - Using " + settingsClassName + " settings class...");
        } catch (ClassNotFoundException e) {
            System.out.println(new Date()
                    + " - Using default settings class...");
            e.printStackTrace();
        }
        Properties experimentProperties = PropertiesUtil.load(args[1]);
        CommonExperimentSettings settings =
                (CommonExperimentSettings) settingsClass.getConstructor(
                        Properties.class).newInstance(
                        experimentProperties);
        FileOutputStream out = new FileOutputStream(args[1]);
        settings.getFinalSettings().store(out, "");
        out.close();

        Constructor<?> constructor =
                experimentClass.
                        getConstructor(settingsClass);

        Runnable experiment = (Runnable) constructor.newInstance(settings);
        experiment.run();
        System.exit(0);
    }
}
