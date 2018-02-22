/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.configuration.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.hibernate.ogm.util.configurationreader.spi.PropertyValidator;

/**
 * Collects the {@link PropertyValidator}s for Infinipan Remote configuration file
 *
 * @author Davide D'Alto
 */
public class InfinispanRemoteValidators {

	public static final PropertyValidator<String> SCHEMA_FILE_NAME = new PropertyValidator<String>() {

		@Override
		public void validate(String schemaFileName) throws HibernateException {
			if ( !( schemaFileName.endsWith( ".proto" ) ) ) {
				throw log.invalidProtoFileName( schemaFileName );
			}
		}
	};

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private InfinispanRemoteValidators() {
	};
}
