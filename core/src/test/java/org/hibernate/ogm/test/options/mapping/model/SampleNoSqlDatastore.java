/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.mapping.model;

import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;
import org.hibernate.ogm.test.options.mapping.model.SampleOptionModel.SampleGlobalContext;

/**
 * @author Gunnar Morling
 */
public class SampleNoSqlDatastore implements DatastoreConfiguration<SampleOptionModel.SampleGlobalContext> {

	@Override
	public SampleGlobalContext getConfigurationBuilder(ConfigurationContext context) {
		return SampleOptionModel.createGlobalContext( context );
	}
}
