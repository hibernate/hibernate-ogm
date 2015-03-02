/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.exception.spi.ErrorHandler;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes the {@link ErrorHandlerService} in case an {@link ErrorHandler} has been configured by the user.
 *
 * @author Gunnar Morling
 */
@SuppressWarnings("rawtypes")
public class ErrorHandlerServiceInitiator implements StandardServiceInitiator<ErrorHandlerService> {

	public static final ErrorHandlerServiceInitiator INSTANCE = new ErrorHandlerServiceInitiator();

	private ErrorHandlerServiceInitiator() {
	}

	@Override
	public Class<ErrorHandlerService> getServiceInitiated() {
		return ErrorHandlerService.class;
	}

	@Override
	public ErrorHandlerService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationValues, registry.getService( ClassLoaderService.class ) );

		ErrorHandler errorHandler = propertyReader.property( OgmProperties.ERROR_HANDLER, ErrorHandler.class )
				.instantiate()
				.getValue();

		if ( errorHandler == null ) {
			return null;
		}
		else {
			return new ErrorHandlerService( registry.getService( GridDialectInvocationCollector.class ), errorHandler );
		}
	}
}
