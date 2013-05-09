package experiments;

import java.io.FileOutputStream;
import java.lang.reflect.Constructor;

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
        CommonExperimentSettings settings =
                new CommonExperimentSettings(PropertiesUtil.load(args[1]));
        FileOutputStream out = new FileOutputStream(args[1]);
        settings.getFinalSettings().store(out, "");
        out.close();
        Constructor<?> constructor = 
                Class.forName(args[0]).
                getConstructor(CommonExperimentSettings.class);
        
        Runnable experiment = (Runnable) constructor.newInstance(settings);
        experiment.run();
        System.exit(0);
    }
}
