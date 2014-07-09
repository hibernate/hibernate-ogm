/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernConverter;
import org.hibernate.ogm.options.spi.MappingOption;

/**
 * Specifies the <a href="http://docs.mongodb.org/manual/core/write-concern/">write concern</a> to be applied when
 * performing write operations to the annotated entity or property. Can either be given using a pre-configured write
 * concern such as {@link WriteConcernType#JOURNALED} or by specifying the type of a custom {@link WriteConcern}
 * implementation.
 * <p>
 * When given on the property-level, this setting will only take effect when the property represents an association. If
 * given for non-association properties, the setting on the property-level will be ignored and the setting from the
 * entity will be applied.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@MappingOption(WriteConcernConverter.class)
public @interface WriteConcern {

	/**
	 * Specifies the write concern to be applied when performing write operations to the annotated entity or property.
	 * <p>
	 * Use {@link WriteConcernType#CUSTOM} in conjunction with {@link #type()} to specify a custom {@link WriteConcern}
	 * implementation. This is useful in cases where the pre-defined configurations are not sufficient, e.g. if you want
	 * to ensure that writes are propagated to a specific number of replicas or given "tag set".
	 */
	WriteConcernType value();

	/**
	 * Specifies a custom {@link com.mongodb.WriteConcern} implementation. Only takes effect if {@link #value()} is set
	 * to {@link WriteConcernType#CUSTOM}. The specified type must have a default (no-args) constructor.
	 */
	Class<? extends com.mongodb.WriteConcern> type() default com.mongodb.WriteConcern.class;
}
