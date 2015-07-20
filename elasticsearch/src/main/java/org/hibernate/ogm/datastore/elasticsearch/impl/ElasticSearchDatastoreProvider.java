package org.hibernate.ogm.datastore.elasticsearch.impl;

import java.util.List;
import java.util.Map;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.base.Function;
import org.elasticsearch.common.base.Objects;
import org.elasticsearch.common.base.Splitter;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.elasticsearch.ElasticSearchDialect;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

public class ElasticSearchDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable, ServiceRegistryAwareService, Configurable {

	private Settings settings = ImmutableSettings.settingsBuilder().build();
	private String transportAdresses;
	private TransportClient client;
	private String database;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return ElasticSearchDialect.class;
	}

	@SuppressWarnings({ "rawtypes", "deprecation" })
	@Override
	public void configure(Map configurationValues) {
		transportAdresses = String.valueOf(configurationValues.get(OgmProperties.HOST));
		database = String.valueOf(Objects.firstNonNull(configurationValues.get(OgmProperties.DATABASE), "default"));
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {

	}

	@Override
	public void stop() {
		client.close();
	}

	@SuppressWarnings("resource")
	@Override
	public void start() {

		List<InetSocketTransportAddress> transportadressesLit = Lists.transform(Splitter.on(",").omitEmptyStrings().splitToList(transportAdresses), new Function<String, InetSocketTransportAddress>() {
			@Override
			public InetSocketTransportAddress apply(String input) {
				String[] address = input.split(":");
				if (address.length != 2) {
					throw new IllegalArgumentException("Unable to parse host \"" + input + "\"");
				}
				return new InetSocketTransportAddress(address[0], Integer.parseInt(address[1]));
			}
		});

		client = new TransportClient(settings).addTransportAddresses(transportadressesLit.toArray(new TransportAddress[transportadressesLit.size()]));
	}

	public TransportClient getClient() {
		return client;
	}

	public String getDatabase() {
		return database;
	}

}
