/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import java.util.Map;
import java.util.Set;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
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
	 *
	 * @param context Provides access to metadata describing the schema to be validated
	 */
	void validateMapping(SchemaDefinitionContext context);

	/**
	 * Initializes the schema in the datastore.
	 *
	 * @param context Provides access to metadata describing the schema to be initialized
	 */
	void initializeSchema(SchemaDefinitionContext context);

	/**
	 * Provides contextual information about the schema objects to be created. Schema initialization should primarily be
	 * driven via the objects retrievable via {@link Database} and the different types of meta-data.
	 * <p>
	 * Please get in touch in case you need access to the session factory for something else than obtaining the current
	 * datastore provider.
	 */
	interface SchemaDefinitionContext {
		Database getDatabase();
		Set<EntityKeyMetadata> getAllEntityKeyMetadata();
		Set<AssociationKeyMetadata> getAllAssociationKeyMetadata();
		Set<IdSourceKeyMetadata> getAllIdSourceKeyMetadata();
		SessionFactoryImplementor getSessionFactory();

		/**
		 * Note that it only returns one entity type per physical table in the case several entity types share the same table.
		 *
		 * This method is used to get the options attached to an entity and as options are inherited from the superclasses
		 * and the options for the physical table are likely to be set in the common superclass, this seems like an
		 * acceptable tradeoff.
		 */
		Map<String, Class<?>> getTableEntityTypeMapping();
	}
}
