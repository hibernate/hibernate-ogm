/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.util.Experimental;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;

/**
 * Contract for implementing schema creation and validation routines.
 * <p>
 * Implementations can vary from simply validating the entity model to creating physical structures in the underlying
 * datastore. As this is a {@link Service} contract, implementations can optionally implement service facts such as
 * {@link Configurable} or {@link ServiceRegistryAwareService} etc. Implementations should be derived from
 * {@link BaseSchemaDefiner} rather than implementing this interface directly.
 * <p>
 * The initializer type to be used for a given datastore is retrieved via
 * {@link DatastoreProvider#getSchemaDefinerType()}.
 *
 * @author Gunnar Morling
 */
@Experimental(
	"The initializeSchema() method may be replaced by more specific fine-grained hooks in the future. A drop method " +
	"will be added in the future."
)
public interface SchemaDefiner extends Service {

	/**
	 * Validates the mapped objects such as entities, id generators etc. against any specific requirements of the
	 * current datastore.
	 */
	void validateMapping(SessionFactoryImplementor factory);

	/**
	 * Initializes the schema in the datastore.
	 */
	void initializeSchema(Configuration configuration, SessionFactoryImplementor factory);
}
