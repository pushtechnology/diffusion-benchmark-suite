package experiments;

import monitoring.ExperimentCounters;
import util.Factory;
import clients.ExperimentClient;
import clients.MessageCountingClient;
import clients.TopicCountingClient;

public class DefaultClientFactory implements
        Factory<ExperimentClient> {
    private final CommonClientSettings clientSettings;
    private final ExperimentCounters experimentCounters;

    public DefaultClientFactory(
            CommonClientSettings clientSettingsP,
            ExperimentCounters experimentCounters) {
        super();
        this.clientSettings = clientSettingsP;
        this.experimentCounters = experimentCounters;
    }

    @Override
    public ExperimentClient create() {
        if (experimentCounters.connectionAttemptsCounter
                .get() == 0) {
            return new TopicCountingClient(experimentCounters,
                    clientSettings.getRootTopic());
        } else {
            return new MessageCountingClient(experimentCounters,
                    false,
                    clientSettings.getRootTopic());
        }
    }
}