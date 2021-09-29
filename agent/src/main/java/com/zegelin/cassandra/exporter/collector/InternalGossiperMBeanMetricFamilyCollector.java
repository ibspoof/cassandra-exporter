package com.zegelin.cassandra.exporter.collector;

import com.zegelin.cassandra.exporter.MetadataFactory;
import com.zegelin.prometheus.domain.Labels;
import com.zegelin.prometheus.domain.NumericMetric;
import org.apache.cassandra.gms.EndpointState;
import org.apache.cassandra.gms.Gossiper;
import org.apache.cassandra.locator.InetAddressAndPort;

import java.net.InetAddress;
import java.util.stream.Stream;

import static com.zegelin.cassandra.exporter.CassandraObjectNames.GOSSIPER_MBEAN_NAME;
import static com.zegelin.cassandra.exporter.MetricValueConversionFunctions.millisecondsToSeconds;

public class InternalGossiperMBeanMetricFamilyCollector extends GossiperMBeanMetricFamilyCollector {
    public static Factory factory(final MetadataFactory metadataFactory) {
        return mBean -> {
            if (!GOSSIPER_MBEAN_NAME.apply(mBean.name))
                return null;

            return new InternalGossiperMBeanMetricFamilyCollector((Gossiper) mBean.object, metadataFactory);
        };
    };

    private final Gossiper gossiper;
    private final MetadataFactory metadataFactory;

    private InternalGossiperMBeanMetricFamilyCollector(final Gossiper gossiper, final MetadataFactory metadataFactory) {
        this.gossiper = gossiper;
        this.metadataFactory = metadataFactory;
    }

    @Override
    protected void collect(final Stream.Builder<NumericMetric> generationNumberMetrics, final Stream.Builder<NumericMetric> downtimeMetrics, final Stream.Builder<NumericMetric> activeMetrics) {

        for (final InetAddressAndPort endpointState : gossiper.getEndpoints()) {
            final InetAddress endpoint = endpointState.address;
            final EndpointState state = gossiper.getEndpointStateForEndpoint(endpointState);

            final Labels labels = metadataFactory.endpointLabels(endpoint);

            generationNumberMetrics.add(new NumericMetric(labels, gossiper.getCurrentGenerationNumber(endpointState)));
            downtimeMetrics.add(new NumericMetric(labels, millisecondsToSeconds(gossiper.getEndpointDowntime(endpointState))));
            activeMetrics.add(new NumericMetric(labels, state.isAlive() ? 1 : 0));
        }

    }
}
