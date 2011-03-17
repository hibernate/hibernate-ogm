package org.hibernate.ogm.test.jpa;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
public class Poem {
	@Id @GeneratedValue(generator = "uuid") @GenericGenerator( name = "uuid", strategy = "uuid2")
	public String getId() { return id; }
	public void setId(String id) {  this.id = id; }
	private String id;

	public String getName() { return name; }
	public void setName(String name) {  this.name = name; }
	private String name;
}
