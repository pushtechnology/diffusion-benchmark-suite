package com.pushtechnology.benchmarks.experiments;

import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource.Updater;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.TopicSource.Updater.UpdateCallback;

/**
 * Implementation of {@link AddCallback} that publishes the same value for
 * all topics once the topic has been added.
 */
public final class PublishValueOnTopicCreation extends AddCallback.Default {
    /**
     * Initial content.
     */
    private final Content initialContent;
    /**
     * Updater.
     */
    private final Updater updater;
    /**
     * Update callback for publish.
     */
    private final UpdateCallback updateCallback;

    /**
     * Constructor.
     * @param initialContentP Value to publish
     * @param updaterP The updater to use to publish the message
     */
    public PublishValueOnTopicCreation(final Content initialContentP,
            Updater updaterP) {
        initialContent = initialContentP;
        updater = updaterP;
        updateCallback = new UpdateCallback.Default() {
            @Override
            public String toString() {
                return getClass().getSimpleName() + ":UpdateCallback";
            }
        };
    }

    @Override
    public void onTopicAdded(String topic) {
        updater.update(topic, initialContent, updateCallback);
        super.onTopicAdded(topic);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + initialContent.asString();
    }
}
