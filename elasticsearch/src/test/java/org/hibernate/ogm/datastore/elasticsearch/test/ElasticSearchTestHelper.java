package org.hibernate.ogm.datastore.elasticsearch.test;

import org.elasticsearch.client.transport.TransportClient;

public class ElasticSearchTestHelper {

	private TransportClient client;

	public ElasticSearchTestHelper(TransportClient client) {
		super();
		this.client = client;
	}

	public long getNumberOfEntities(String indice, String type) {
		return client.prepareCount(indice).setTypes(type).execute().actionGet().getCount();
	}
}
