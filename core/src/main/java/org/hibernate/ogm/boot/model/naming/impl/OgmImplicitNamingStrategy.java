/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.boot.model.naming.impl;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * An {@link ImplicitNamingStrategy} which exposes component property names via their full path (e.g. "newsId.author")
 * rather than only the actual property name (e.g. "author").
 *
 * @author Davide D'Alto
 * @author Gunnar Morling
 */
public class OgmImplicitNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {

	@Override
	protected String transformAttributePath(AttributePath attributePath) {
		if ( attributePath.isPartOfCollectionElement() ) {
			return componentPath( attributePath );
		}
		return attributePath.getFullPath();
	}

	private String componentPath(AttributePath attributePath) {
		AttributePath parentAttributePath = attributePath;
		StringBuilder builder = new StringBuilder( parentAttributePath.getProperty() );
		while ( !attributePath.getParent().isCollectionElement() ) {
			attributePath = attributePath.getParent();
			builder.insert( 0, "." );
			builder.insert( 0, attributePath.getProperty() );
		}
		String componentPath = builder.toString();
		return componentPath;
	}
}
