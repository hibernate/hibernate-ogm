package org.hibernate.ogm.teiid;

import java.util.Map;

import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

class TeiidConnectionProviderInitiator implements BasicServiceInitiator<ConnectionProvider>{
	
	static TeiidConnectionProviderInitiator INSTANCE = new TeiidConnectionProviderInitiator();
	
	@Override
	public Class<ConnectionProvider> getServiceInitiated() {
		return ConnectionProvider.class;
	}

	@Override
	public ConnectionProvider initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new TeiidConnectionProvider(configurationValues, registry);
	}

}
