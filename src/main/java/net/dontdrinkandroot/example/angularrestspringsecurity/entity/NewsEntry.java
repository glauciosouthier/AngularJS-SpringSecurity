package net.dontdrinkandroot.example.angularrestspringsecurity.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.ws.rs.FormParam;
import javax.xml.bind.annotation.XmlRootElement;

import net.dontdrinkandroot.example.angularrestspringsecurity.JsonViews;

import org.codehaus.jackson.map.annotate.JsonView;


/**
 * JPA Annotated Pojo that represents a news entry.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
@XmlRootElement
@javax.persistence.Entity(name="newsentry")
public class NewsEntry implements Entity
{

	private static final long serialVersionUID = 1089315240370752445L;

	@Id
	@GeneratedValue
	@Column(name="id")
	@FormParam("id")
	private Long id;

	@Column(name="date")
	@FormParam("date")
	private Date date;

	@Column(name="content")
	@FormParam("content")
	private String content;


	public NewsEntry()
	{
		this.date = new Date();
	}


	@JsonView(JsonViews.Admin.class)
	public Long getId()
	{
		return this.id;
	}

	public void setId(Long id)
	{
		this.id=id;
	}
	
	@JsonView(JsonViews.User.class)
	public Date getDate()
	{
		return this.date;
	}


	public void setDate(Date date)
	{
		this.date = date;
	}


	@JsonView(JsonViews.User.class)
	public String getContent()
	{
		return this.content;
	}


	public void setContent(String content)
	{
		this.content = content;
	}


	@Override
	public String toString()
	{
		return String.format("NewsEntry[%d, %s]", this.id, this.content);
	}

}