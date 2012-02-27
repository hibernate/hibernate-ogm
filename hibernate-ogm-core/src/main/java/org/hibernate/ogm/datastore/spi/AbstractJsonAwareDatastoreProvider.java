package org.hibernate.ogm.datastore.spi;

public abstract class AbstractJsonAwareDatastoreProvider extends
		AbstractDatastoreProvider {

	private final JSONHelper jsonHelper = new JSONHelper();

	public JSONHelper getJsonHelper() {
		return this.jsonHelper;
	}
}
