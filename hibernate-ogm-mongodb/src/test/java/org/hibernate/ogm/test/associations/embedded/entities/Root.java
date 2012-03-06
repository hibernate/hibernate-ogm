package org.hibernate.ogm.test.associations.embedded.entities;

import java.io.Serializable;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class Root implements Serializable {
	private static final long serialVersionUID = 1L;

	@Embedded
	private EmbeddedObject embeddedObject;

	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	private String id;

	private String rootValue;

	public Root() {
		super();
	}

	
	public Root(String rootValue, EmbeddedObject embeddedObject) {
		this();
		this.embeddedObject = embeddedObject;
		this.rootValue = rootValue;
	}


	public EmbeddedObject getEmbeddedObject() {
		return embeddedObject;
	}

	public String getId() {
		return id;
	}

	public String getRootValue() {
		return rootValue;
	}

	public void setEmbeddedObject(EmbeddedObject embeddedObject) {
		this.embeddedObject = embeddedObject;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setRootValue(String rootValue) {
		this.rootValue = rootValue;
	}

}
