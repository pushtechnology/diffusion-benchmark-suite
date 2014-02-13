/*
 * Copyright 2013 Push Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.pushtechnology.benchmarks.experiments;

import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.util.Properties;

import com.pushtechnology.benchmarks.util.PropertiesUtil;
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
     * Entry point for experiments.
     * 
     * @param args expect experiment class name
     */
    @SuppressWarnings("deprecation")
    public static void main(final String[] args) {
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
            final FileOutputStream out = new FileOutputStream(args[1]);
            experimentProperties.store(out, "");
            out.close();

            final Constructor<?> constructor =
                    experimentClass.
                            getConstructor(settingsClass);

            Runnable experiment = (Runnable) constructor.newInstance(settings);
            experiment.run();
        } catch (Throwable t) {
            // Could be a problem with logging
            // Do not log this error, print it
            t.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
