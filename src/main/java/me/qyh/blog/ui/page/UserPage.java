/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.ui.page;

import java.sql.Timestamp;

import me.qyh.blog.entity.Space;

public class UserPage extends Page {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String name;
	private String description;
	private Timestamp createDate;
	private String alias;
	private Boolean allowComment;// 是否允许评论
	private boolean registrable;// 是否可注册，如果为true，那么将直接被注册到RequestMapping中

	public UserPage() {
		super();
	}

	public UserPage(Integer id) {
		super(id);
	}

	public UserPage(String alias) {
		this.alias = alias;
	}

	public UserPage(Space space, String alias) {
		this(space, alias, false);
	}

	public UserPage(Space space, String alias, boolean registrable) {
		super(space);
		this.alias = alias;
		this.registrable = registrable;
	}

	public UserPage(UserPage page) {
		super(page);
		this.name = page.name;
		this.description = page.description;
		this.createDate = page.createDate;
		this.alias = page.alias;
		this.allowComment = page.allowComment;
		this.registrable = page.registrable;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Timestamp getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public Boolean getAllowComment() {
		return allowComment;
	}

	public void setAllowComment(Boolean allowComment) {
		this.allowComment = allowComment;
	}

	public boolean isRegistrable() {
		return registrable;
	}

	public void setRegistrable(boolean registrable) {
		this.registrable = registrable;
	}

	@Override
	public final PageType getType() {
		return PageType.USER;
	}

	public Page toExportPage() {
		UserPage page = new UserPage();
		page.setTpl(getTpl());
		page.setType(PageType.USER);
		page.setAlias(alias);
		page.setRegistrable(registrable);
		return page;
	}

	@Override
	public String toString() {
		return "UserPage [name=" + name + ", description=" + description + ", createDate=" + createDate + ", alias="
				+ alias + ", allowComment=" + allowComment + ", registrable=" + registrable + "]";
	}

}
