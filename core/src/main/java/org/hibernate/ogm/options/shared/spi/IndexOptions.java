/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.shared.spi;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.ogm.util.impl.Immutable;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * Provide a way to specify options specific to a datastore.
 *
 * @author Guillaume Smet
 */
public class IndexOptions {

	@Immutable
	private final Map<String, IndexOption> indexOptions;

	public IndexOptions() {
		this.indexOptions = Collections.emptyMap();
	}

	public IndexOptions(org.hibernate.ogm.options.shared.IndexOptions annotation) {
		Map<String, IndexOption> indexOptions = CollectionHelper.newHashMap( annotation.value().length );
		for ( org.hibernate.ogm.options.shared.IndexOption optionAnnotation : annotation.value() ) {
			indexOptions.put( optionAnnotation.forIndex(), new IndexOption( optionAnnotation ) );
		}
		this.indexOptions = Collections.unmodifiableMap( indexOptions );
	}

	public IndexOption getOptionForIndex(String indexName) {
		if ( StringHelper.isNullOrEmptyString( indexName ) ) {
			return new IndexOption();
		}
		if ( !indexOptions.containsKey( indexName ) ) {
			return new IndexOption( indexName );
		}

		return indexOptions.get( indexName );
	}

	public Set<String> getReferencedIndexes() {
		return Collections.unmodifiableSet( indexOptions.keySet() );
	}

}
