/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id.embeddable;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Gunnar Morling
 */
@Entity
@Indexed
public class SingleBoardComputer {

	@Id
	@FieldBridge(impl = SingleBoardComputerPk.SingleBoardComputerPkFieldBridge.class)
	private SingleBoardComputerPk id;

	private String name;

	SingleBoardComputer() {
	}

	public SingleBoardComputer(SingleBoardComputerPk id, String name) {
		this.id = id;
		this.name = name;
	}

	public SingleBoardComputerPk getId() {
		return id;
	}

	public void setId(SingleBoardComputerPk id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
