
package org.hibernate.ogm.example;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

@Entity
public class G2 {
	private G1 g1;
	private int e1;
	private String e2;
	
	public G2(int e1, String e2) {
		this.e1 = e1;
		this.e2 = e2;
	}
	
	@Id
	@Column(name = "e1")
	public int getE1() { return e1; }
	public void setE1(int value) { e1 = value; }
	
	@Column(name = "e2")
	public String getE2() { return e2; }
	public void setE2(String value) { e2 = value; }
	
	@OneToOne
	@PrimaryKeyJoinColumn
	public G1 getG1() {return g1;}
	public void setG1(G1 g1) {this.g1 = g1;}
}
