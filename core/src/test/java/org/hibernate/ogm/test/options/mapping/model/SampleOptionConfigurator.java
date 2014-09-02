/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.mapping.model;

import java.lang.annotation.ElementType;

import org.hibernate.ogm.cfg.Configurable;
import org.hibernate.ogm.cfg.OptionConfigurator;

/**
 * @author Gunnar Morling
 */
public class SampleOptionConfigurator extends OptionConfigurator {

	@Override
	public void configure(Configurable configurable) {
		configurable.configureOptionsFor( SampleNoSqlDatastore.class )
			.entity( Refrigerator.class )
				.force( true )
				.property( "temperature", ElementType.FIELD )
					.embed( "Embedded" )
			.entity( Microwave.class )
				.name( "test" );
	}
}
