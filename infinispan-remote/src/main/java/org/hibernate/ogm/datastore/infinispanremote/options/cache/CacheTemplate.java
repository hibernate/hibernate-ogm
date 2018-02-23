/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.options.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties;
import org.hibernate.ogm.datastore.infinispanremote.options.cache.impl.CacheTemplateConverter;
import org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType;
import org.hibernate.ogm.options.spi.MappingOption;

/**
 * Define the template to use when the cache associated to the entity is created.
 * <p>
 * Overrides the global default template set using {@link InfinispanRemoteProperties#NEW_CACHE_TEMPLATE}
 * set using for the specific entity cache.
 * <p>
 * This annotation is used only when {@link CacheMappingType#CACHE_PER_TABLE} strategy is used.
 *
 * @author Davide D'Alto
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@MappingOption( CacheTemplateConverter.class )
public @interface CacheTemplate {
	String value();
}
