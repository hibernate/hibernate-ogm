package org.hibernate.ogm.test.loader;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
public class Feeling {
	@Id
	@GeneratedValue(generator = "uuid") @GenericGenerator( name="uuid", strategy = "uuid2")
	public String getUUID() { return uuid; }
	public void setUUID(String uuid) { this.uuid = uuid; }
	private String uuid;

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	private String name;
}
