/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.shared.spi;


/**
 * Options specific to the datastore for a given index.
 *
 * @author Guillaume Smet
 */
public class IndexOption {

	/**
	 * The target index name.
	 */
	private String targetIndexName;

	/**
	 * The index options. Typically, might be a JSON object.
	 */
	private String options;

	IndexOption() {
	}

	IndexOption(String targetIndexName) {
		this.targetIndexName = targetIndexName;
	}

	IndexOption(org.hibernate.ogm.options.shared.IndexOption annotation) {
		this( annotation.forIndex() );
		this.options = annotation.options();
	}

	public String getTargetIndexName() {
		return targetIndexName;
	}

	public String getOptions() {
		return options;
	}

}
