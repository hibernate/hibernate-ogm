package org.hibernate.ogm.datastore.ignite.dialect.criteria.spi;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * vk: 09.02.16. убрал его использование. удалить
 */
public class CriteriaGridDialectInitiator implements StandardServiceInitiator<CriteriaGridDialect> {

	public static final CriteriaGridDialectInitiator INSTANCE = new CriteriaGridDialectInitiator();

	@Override
	public Class<CriteriaGridDialect> getServiceInitiated() {
		return CriteriaGridDialect.class;
	}

	@Override
	public CriteriaGridDialect initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		GridDialect gridDialect = registry.getService(GridDialect.class);
		if (GridDialects.hasFacet(gridDialect, CriteriaGridDialect.class)) {
			return (CriteriaGridDialect)gridDialect;
		}
		return null;
	} 
	
	

}
