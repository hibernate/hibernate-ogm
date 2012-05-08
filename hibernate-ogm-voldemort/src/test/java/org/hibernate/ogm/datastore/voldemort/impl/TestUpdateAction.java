package org.hibernate.ogm.datastore.voldemort.impl;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

import voldemort.client.StoreClient;

public class TestUpdateAction extends VoldemortUpdateAction {

	private static final Log log = LoggerFactory.make();

	public TestUpdateAction() {

	}

	public TestUpdateAction(Object key, Object value) {
		super( key, value );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void update(StoreClient client) {
		log.info( "dummy " + this.getClass().getCanonicalName() + ".update() was called with key: " + this.getKey()
				+ " value: " + this.getValue() );

	}

}
