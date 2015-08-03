/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.options;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import org.hibernate.ogm.datastore.redis.options.impl.TTLConverter;
import org.hibernate.ogm.options.spi.MappingOption;

/**
 * Specifies the <a href="http://redis.io/commands/pexpireat">expiry</a> to be applied on the specific entity.
 * The expiry period starts after persisting the entity. Subsequent persists reset the expiry to the specified value.
 * <p>
 * When given on the property-level, this setting will only take effect when the property represents an association. If
 * given for non-association properties, the setting on the property-level will be ignored and the setting from the
 * entity will be applied.
 *
 * @author Mark Paluch
 */
@Target( {TYPE, METHOD, FIELD } )
@Retention(RUNTIME)
@MappingOption(TTLConverter.class)
public @interface TTL {

	/**
	 * Specifies the expiry duration after persisting the annotated entity or
	 * property.
	 *
	 * @return the TTL duration
	 */
	long value();

	/**
	 * Specifies the unit of duration for expiring the key. Defaults to {@link java.util.concurrent.TimeUnit#SECONDS}
	 *
	 * @return the TTL time unit
	 */
	TimeUnit unit() default TimeUnit.SECONDS;
}
